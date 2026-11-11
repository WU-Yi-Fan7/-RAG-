package cn.edu.hubu.hubuactivities.service.impl;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.ChatCompletionCreateParams;
import ai.z.openapi.service.model.ChatCompletionResponse;
import ai.z.openapi.service.model.ChatMessage;
import ai.z.openapi.service.model.ChatMessageRole;
import ai.z.openapi.service.model.Choice;
import ai.z.openapi.service.model.ModelData;
import cn.edu.hubu.hubuactivities.entity.Activity;
import cn.edu.hubu.hubuactivities.mapper.ActivityMapper;
import cn.edu.hubu.hubuactivities.service.AiChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private static final String MODEL = "glm-4-flash";
    private static final String CACHE_KEY_PREFIX = "ai:chat:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final ActivityMapper activityMapper;

    private ZhipuAiClient client;

    @Value("${zhipu.api-key}")
    private String apiKey;

    @PostConstruct
    public void init() {
        client = ZhipuAiClient.builder().ofZHIPU()
                .apiKey(apiKey)
                .build();
    }

    @Override
    public String chatWithActivities(String userQuestion) {

        // ================= 1. 构建语义化 Key =================
        String redisKey = buildCacheKey(userQuestion);

        // ================= 2. 查缓存 =================
        Object cachedAnswer = getCache(redisKey);
        if (cachedAnswer != null) {
            log.info("命中 Redis 缓存，直接返回");
            return cachedAnswer.toString(); // 直接返回，省掉查询数据库和请求大模型的时间和费用
        }

        // ================= 3. 缓存未命中，走大模型调用 =================
        String aiAnswer = doCallAI(userQuestion);

        // ================= 4. 回写 Redis 缓存 =================
        writeCache(redisKey, aiAnswer);

        return aiAnswer;
    }

    @Override
    public void streamChatWithActivities(String userQuestion,
                                         Consumer<String> onChunk,
                                         Runnable onComplete,
                                         Consumer<Throwable> onError) {

        String redisKey = buildCacheKey(userQuestion);

        // ================= 1. 查缓存：命中则一次性推送完整答案 =================
        Object cachedAnswer = getCache(redisKey);
        if (cachedAnswer != null) {
            log.info("命中 Redis 缓存，流式直接返回");
            onChunk.accept(cachedAnswer.toString());
            onComplete.run();
            return;
        }

        // ================= 2. 发起流式请求 =================
        ChatCompletionResponse response;
        try {
            response = client.chat().createChatCompletion(buildRequest(userQuestion, true));
        } catch (Exception e) {
            log.error("AI 流式对话请求失败", e);
            onError.accept(e);
            return;
        }

        if (!response.isSuccess() || response.getFlowable() == null) {
            String errorMsg = response.getError() != null && response.getError().getMessage() != null
                    ? response.getError().getMessage()
                    : response.getMsg();
            log.error("AI 流式对话初始化失败: {}", errorMsg);
            onError.accept(new IllegalStateException("AI 流式对话初始化失败: " + errorMsg));
            return;
        }

        // ================= 3. 订阅增量数据，边生成边回调 =================
        StringBuilder fullAnswer = new StringBuilder();
        response.getFlowable().subscribe(
                data -> {
                    String content = extractDeltaContent(data);
                    if (StringUtils.hasText(content)) {
                        fullAnswer.append(content);
                        onChunk.accept(content);
                    }
                },
                error -> {
                    log.error("AI 流式对话异常", error);
                    onError.accept(error);
                },
                () -> {
                    // 全部生成完毕后，再把完整答案写入缓存
                    writeCache(redisKey, fullAnswer.toString());
                    onComplete.run();
                }
        );
    }

    private String doCallAI(String userQuestion) {
        try {
            ChatCompletionResponse response = client.chat().createChatCompletion(buildRequest(userQuestion, false));

            if (response.getData() == null
                    || response.getData().getChoices() == null
                    || response.getData().getChoices().isEmpty()
                    || response.getData().getChoices().get(0).getMessage() == null) {
                return "AI没有返回有效的回答";
            }

            Object content = response.getData().getChoices().get(0).getMessage().getContent();
            return content == null ? "AI没有返回有效的回答" : content.toString();
        } catch (Exception e) {
            log.error("AI对话失败", e);
            return "抱歉，AI助手现在有点累，请稍后再试。";
        }
    }

    /**
     * 构建请求参数，stream=true 时走流式接口
     */
    private ChatCompletionCreateParams buildRequest(String userQuestion, boolean stream) {
        // 1.获取所有的活动信息
        List<Activity> activities = activityMapper.getAllActivities();

        // 2.把活动列表拼接成一段文本，作为背景知识给AI
        String activitiesContext = activities.stream()
                .map(a -> "活动名称：" + a.getTitle() + "， 时间" + a.getStart_time() + "， 地点："
                        + a.getLocation())
                .collect(Collectors.joining("\n"));

        // 3.构建给AI的系统提示词
        String systemPrompt = "你现在扮演的是校园活动助手角色。请你根据以下活动数据，用简明扼要的中文回答学生的问题。\n" +
                "活动数据：\n" + activitiesContext;

        // 4.创建聊天完成请求
        return ChatCompletionCreateParams.builder()
                .model(MODEL)
                .messages(Arrays.asList(
                        ChatMessage.builder()
                                .role(ChatMessageRole.SYSTEM.value())
                                .content(systemPrompt)
                                .build(),
                        ChatMessage.builder()
                                .role(ChatMessageRole.USER.value())
                                .content(userQuestion)
                                .build()
                ))
                .stream(stream)
                .temperature(0.6f)
                .maxTokens(65536)
                .build();
    }

    /**
     * 从流式返回的分片中取出本次新增的文本
     */
    private String extractDeltaContent(ModelData data) {
        if (data == null || data.getChoices() == null || data.getChoices().isEmpty()) {
            return null;
        }
        Choice choice = data.getChoices().get(0);
        if (choice == null || choice.getDelta() == null) {
            return null;
        }
        return choice.getDelta().getContent();
    }

    /**
     * 用 MD5 压缩问题长度生成缓存 Key，同时避免特殊符号导致 Redis 连接报错
     */
    private String buildCacheKey(String userQuestion) {
        return CACHE_KEY_PREFIX + DigestUtils.md5DigestAsHex(userQuestion.getBytes());
    }

    private Object getCache(String redisKey) {
        try {
            return redisTemplate.opsForValue().get(redisKey);
        } catch (Exception e) {
            // 企业规范：Redis 如果挂了，不能影响主业务，打日志告警，继续往下走查大模型
            log.warn("Redis 获取异常，降级执行后续逻辑", e);
            return null;
        }
    }

    private void writeCache(String redisKey, String answer) {
        if (!StringUtils.hasText(answer) || answer.contains("有点累") || answer.contains("有效的回答")) {
            return;
        }
        try {
            log.info("结果已存入 Redis，Key: {}", redisKey);
            // 基准过期时间 1 小时 + 随机 0~5 分钟
            long expireTime = 60 + new Random().nextInt(5);
            redisTemplate.opsForValue().set(redisKey, answer, expireTime, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("回写 Redis 失败", e);
        }
    }
}
