package io.github.susamlu.langchain4j.aiservice.streaming;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;

import java.util.concurrent.CompletableFuture;

/**
 * TokenStream 流式响应示例
 * <p>
 * 演示如何在 AI Service 中使用 TokenStream 作为返回类型来实现流式响应
 * <p>
 * TokenStream 提供了丰富的回调方法：
 * - onPartialResponse: 处理增量响应（每个 token）
 * - onPartialThinking: 处理思考过程（如果模型支持）
 * - onRetrieved: 处理 RAG 检索到的内容
 * - onIntermediateResponse: 处理中间响应
 * - beforeToolExecution: 工具执行前的回调
 * - onToolExecuted: 工具执行后的回调
 * - onCompleteResponse: 处理完整响应
 * - onError: 处理错误
 * <p>
 * 使用方式：
 * 1. 接口方法返回类型为 TokenStream
 * 2. 使用 StreamingChatModel
 * 3. 通过链式调用设置各种回调
 * 4. 调用 start() 启动流式响应
 * 5. 使用 CompletableFuture.join() 阻塞主线程等待完成
 */
public class StreamingAssistantExample {

    /**
     * 流式助手接口
     */
    interface Assistant {

        /**
         * 流式对话方法
         * <p>
         * 返回 TokenStream，通过链式调用设置回调来处理流式响应
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
                .build();

        // ====================== 2. 使用 AiServices 创建 Assistant 实例 ======================
        Assistant assistant = AiServices.create(Assistant.class, model);

        // ====================== 3. 创建 TokenStream 并设置所有回调 ======================
        System.out.println("========== TokenStream 流式响应示例 ==========");
        System.out.println("问题：给我讲个笑话");
        System.out.println("回答：");

        TokenStream tokenStream = assistant.chat("给我讲个笑话");

        CompletableFuture<ChatResponse> futureResponse = new CompletableFuture<>();

        tokenStream
                .onPartialResponse(partialResponse -> System.out.print(partialResponse))
                .onPartialThinking(partialThinking -> System.out.println(partialThinking))
                .onRetrieved(contents -> System.out.println(contents))
                .onIntermediateResponse(intermediateResponse -> System.out.println(intermediateResponse))
                // 工具执行前回调，BeforeToolExecution 包含 ToolExecutionRequest（如工具名称、工具参数等）
                .beforeToolExecution(beforeToolExecution -> System.out.println(beforeToolExecution))
                // 工具执行后回调，ToolExecution 包含 ToolExecutionRequest 和工具执行结果
                .onToolExecuted(toolExecution -> System.out.println(toolExecution))
                .onCompleteResponse(response -> futureResponse.complete(response))
                .onError(error -> futureResponse.completeExceptionally(error))
                .start();

        // 阻塞主线程，直到流式响应完成（LangChain4j 会自动在后台线程中处理流式响应的接收与回调）
        try {
            ChatResponse response = futureResponse.join();
            System.out.println("\n\n--- 流式响应完成 ---");
            System.out.println("完整响应: " + response.aiMessage().text());
            if (response.tokenUsage() != null) {
                System.out.println("Token 消耗: " + response.tokenUsage());
            }
        } catch (Exception e) {
            System.err.println("等待流式响应时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

}

