package io.github.susamlu.langchain4j.responsestreaming;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.*;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class StreamingCancellationExample {

    public static void main(String[] args) throws InterruptedException {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");

        StreamingChatModel model = OpenAiStreamingChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .build();

        String userMessage = "给我讲一个很长的故事";

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger tokenCount = new AtomicInteger(0);
        AtomicReference<StreamingHandle> streamingHandleRef = new AtomicReference<>();
        final int MAX_TOKENS = 500; // 最大 token 数量限制

        model.chat(userMessage, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(PartialResponse partialResponse, PartialResponseContext context) {
                String text = partialResponse.text();
                System.out.print(text);

                // 保存 StreamingHandle 引用，供外部使用
                streamingHandleRef.compareAndSet(null, context.streamingHandle());

                // 统计已接收的 token 数量（简单示例，实际应使用更精确的 token 计数）
                int currentCount = tokenCount.addAndGet(text.length());

                // 如果达到最大 token 限制，中止流式响应
                if (currentCount >= MAX_TOKENS) {
                    System.out.println("\n\n--- 达到最大 token 限制，中止流式响应 ---");
                    context.streamingHandle().cancel();
                    latch.countDown();
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                System.out.println("\n\n--- 流式响应完成 ---");
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                System.err.println("发生错误: " + error.getMessage());
                latch.countDown();
            }
        });

        // 模拟用户操作：3秒后主动取消
        Thread cancelThread = new Thread(() -> {
            try {
                Thread.sleep(3000);
                StreamingHandle handle = streamingHandleRef.get();
                if (handle != null) {
                    System.out.println("\n\n--- 用户主动取消流式响应 ---");
                    handle.cancel();
                    latch.countDown();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        cancelThread.start();

        latch.await();
        cancelThread.join();
    }

}
