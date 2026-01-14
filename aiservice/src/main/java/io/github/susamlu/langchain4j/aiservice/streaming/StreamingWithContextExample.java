package io.github.susamlu.langchain4j.aiservice.streaming;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * onPartialResponseWithContext 使用示例
 * <p>
 * 演示如何使用 onPartialResponseWithContext 来处理带上下文的增量响应，
 * 并演示如何通过 context.streamingHandle().cancel() 取消流式响应
 * <p>
 * onPartialResponseWithContext 与 onPartialResponse 的区别：
 * - onPartialResponse: 只能处理增量响应，无法取消流式响应
 * - onPartialResponseWithContext: 可以访问 PartialResponseContext，通过 context.streamingHandle().cancel() 取消流式响应
 * <p>
 * 使用场景：
 * - 需要根据响应内容动态决定是否继续接收响应
 * - 需要根据某些条件（如 token 数量、时间限制等）取消流式响应
 * - 需要保存 StreamingHandle 以便在其他地方取消流式响应
 */
public class StreamingWithContextExample {

    /**
     * 流式助手接口
     */
    interface Assistant {

        /**
         * 流式对话方法
         *
         * @param message 用户消息
         * @return TokenStream 流式响应对象
         */
        TokenStream chat(String message);

    }

    public static void main(String[] args) {
        // ====================== 1. 创建 StreamingChatModel 实例 ======================
        StreamingChatModel model = OpenAiStreamingChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .logRequests(true)
                .logResponses(true)
                .build();

        // ====================== 2. 使用 AiServices 创建 Assistant 实例 ======================
        Assistant assistant = AiServices.create(Assistant.class, model);

        // ====================== 3. 使用 onPartialResponseWithContext 的示例 ======================
        System.out.println("========== onPartialResponseWithContext 示例 ==========");
        System.out.println("问题：给我讲个故事");
        System.out.println("回答：");

        TokenStream tokenStream = assistant.chat("给我讲个故事");

        CompletableFuture<ChatResponse> futureResponse = new CompletableFuture<>();

        // 用于控制是否取消流式响应
        final AtomicInteger tokenCount = new AtomicInteger(0);
        final int MAX_TOKENS = 100; // 最大 token 数量限制

        tokenStream
                .onPartialResponseWithContext((PartialResponse partialResponse, PartialResponseContext context) -> {
                    // 处理增量响应
                    process(partialResponse);

                    // 统计已接收的 token 数量（简单示例，实际应使用更精确的 token 计数）
                    int currentCount = tokenCount.addAndGet(partialResponse.text().length());

                    // 如果满足取消条件，取消流式响应
                    if (shouldCancel(currentCount, MAX_TOKENS)) {
                        System.out.println("\n\n--- 达到最大 token 限制，取消流式响应 ---");
                        context.streamingHandle().cancel();
                        futureResponse.complete(null);
                    }
                })
                .onCompleteResponse(response -> futureResponse.complete(response))
                .onError(error -> futureResponse.completeExceptionally(error))
                .start();

        // 阻塞主线程，直到流式响应完成或被取消
        try {
            ChatResponse response = futureResponse.join();
            if (response == null) {
                System.out.println("流式响应被取消");
                return;
            }

            System.out.println("\n\n--- 流式响应完成 ---");
            System.out.println("完整响应: " + response.aiMessage().text());
            if (response.tokenUsage() != null) {
                System.out.println("Token 消耗: " + response.tokenUsage());
            }
        } catch (Exception e) {
            System.err.println("等待流式响应时发生异常: " + e.getMessage());
        }
    }

    /**
     * 处理增量响应的示例方法
     * <p>
     * 这里可以添加自定义的处理逻辑，比如：
     * - 实时推送到前端（SSE/WebSocket）
     * - 保存到数据库
     * - 进行实时分析等
     *
     * @param partialResponse 增量响应
     */
    private static void process(PartialResponse partialResponse) {
        // 输出增量响应文本
        System.out.print(partialResponse.text());
    }

    /**
     * 判断是否应该取消流式响应的示例方法
     * <p>
     * 这里可以添加自定义的取消逻辑，比如：
     * - 用户点击了取消按钮
     * - 达到了某个时间限制
     * - 达到了某个 token 数量限制等
     *
     * @param currentCount 当前 token 计数
     * @param maxTokens    最大 token 数量限制
     * @return true 如果应该取消，false 否则
     */
    private static boolean shouldCancel(int currentCount, int maxTokens) {
        // 示例：如果达到最大 token 限制，返回 true
        return currentCount >= maxTokens;
    }

}

