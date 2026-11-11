package cn.edu.hubu.hubuactivities.service;

import java.util.function.Consumer;

public interface AiChatService {
    // 用于接收用户提问，来根据数据库相关活动信息，来进行AI回答
    String chatWithActivities(String userQuestion);

    /**
     * 流式回答：模型每生成一小段内容就通过 onChunk 回调，适合 SSE 实时推送给前端
     *
     * @param userQuestion 用户提问
     * @param onChunk      每收到一段增量内容时回调
     * @param onComplete   全部内容生成完毕时回调
     * @param onError      发生异常时回调
     */
    void streamChatWithActivities(String userQuestion,
                                  Consumer<String> onChunk,
                                  Runnable onComplete,
                                  Consumer<Throwable> onError);
}
