package io.github.susamlu.langchain4j.responsestreaming;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.util.concurrent.CountDownLatch;

public class StreamingChatExample {

    public static void main(String[] args) throws InterruptedException {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");

        StreamingChatModel model = OpenAiStreamingChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .build();

        String userMessage = "给我讲一个笑话";

        // 创建 CountDownLatch，初始计数为 1
        CountDownLatch latch = new CountDownLatch(1);

        model.chat(userMessage, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                // 实时输出增量 token
                System.out.print(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                // 生成完成，打印完整响应和元数据
                System.out.println("\n\n--- 生成完成 ---");
                System.out.println("完整响应: " + completeResponse.aiMessage().text());
                System.out.println("Token 消耗: " + completeResponse.tokenUsage());
                // 响应完成，释放锁
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                // 错误处理
                System.err.println("发生错误: " + error.getMessage());
                // 发生错误时也要释放锁，避免主线程永久阻塞
                latch.countDown();
            }
        });

        // 阻塞主线程，等待流式响应完成
        latch.await();
    }

}

