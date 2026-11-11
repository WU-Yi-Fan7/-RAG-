package cn.edu.hubu.hubuactivities.controller;

import cn.edu.hubu.hubuactivities.service.AiChatService;
import cn.edu.hubu.hubuactivities.vo.ResultVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/ai")
@RestController
public class AiController {

    /**
     * SSE 连接超时时间：3 分钟，避免长回答被中途掐断
     */
    private static final long SSE_TIMEOUT = 180_000L;

    private final AiChatService aiChatService;
    private final ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/chat")
    public ResultVO<String> chat(@RequestBody Map<String, String> params) {
        // 前端传过来的JSON格式{question: "最近有什么活动"}
        String question = params.get("question");

        if (question == null || question.trim().isEmpty()) {
            return ResultVO.error(400, "提问不能为空");
        }

        String answer = aiChatService.chatWithActivities(question);

        return ResultVO.success("回答成功", answer);
    }

    /**
     * 流式对话：模型生成一段就推一段，前端可边接收边渲染
     * <p>
     * 返回的每个 SSE 事件 data 都是一段 JSON：
     * {"content":"增量文本"}  -- 正文片段
     * {"done":true}          -- 本轮回答结束
     * {"error":"错误信息"}    -- 发生异常
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    public SseEmitter chatStream(@RequestBody Map<String, String> params) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        String question = params.get("question");
        if (question == null || question.trim().isEmpty()) {
            sendData(emitter, toJson("error", "提问不能为空"));
            emitter.complete();
            return emitter;
        }

        aiChatService.streamChatWithActivities(
                question,
                // 每收到一段增量内容就推送给前端
                content -> sendData(emitter, toJson("content", content)),
                // 回答结束
                () -> {
                    sendData(emitter, "{\"done\":true}");
                    emitter.complete();
                },
                // 异常兜底
                error -> {
                    sendData(emitter, toJson("error", "抱歉，AI助手现在有点累，请稍后再试。"));
                    emitter.complete();
                });

        return emitter;
    }

    @GetMapping("/clear")
    public String clearCache() {
        // 方式一：如果你知道确切的 Key，可以直接删
        // redisTemplate.delete("ai:chat:e10adc3949ba59abbe56e057f20f883e");

        // 方式二（企业常用）：模糊匹配删除所有以 ai:chat: 开头的 Key
        // 这里用到 keys() 命令，生产环境数据量大时建议用 scan 代替，防止阻塞 Redis 线程
        Set<String> keys = redisTemplate.keys("ai:chat:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            return "成功清空 AI 缓存，共删除了 " + keys.size() + " 条数据！";
        }
        return "缓存已是空的，无需清理。";
    }

    /**
     * 发送一条 SSE 事件；客户端断开连接时直接忽略，避免刷错误日志
     */
    private void sendData(SseEmitter emitter, String data) {
        try {
            emitter.send(SseEmitter.event().data(data));
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE 发送失败（客户端可能已断开）: {}", e.getMessage());
        }
    }

    private String toJson(String key, String value) {
        try {
            return objectMapper.writeValueAsString(Collections.singletonMap(key, value));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("SSE 数据序列化失败", e);
        }
    }
}
