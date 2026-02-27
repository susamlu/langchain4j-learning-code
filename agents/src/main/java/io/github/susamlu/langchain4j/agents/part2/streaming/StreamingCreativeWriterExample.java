package io.github.susamlu.langchain4j.agents.part2.streaming;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.concurrent.CompletableFuture;

/**
 * 流式智能体示例：演示如何使用 TokenStream 创建流式智能体
 * <p>
 * 流式智能体可以返回 TokenStream 类型的结果，支持在结果生成过程中实时消费数据，
 * 无需等待智能体调用完全结束。
 * <p>
 * 使用要点：
 * 1. 接口方法返回类型为 TokenStream
 * 2. 使用 streamingChatModel 配置智能体
 * 3. 通过链式调用设置回调来处理流式响应
 * 4. 调用 start() 启动流式响应
 */
public class StreamingCreativeWriterExample {

    /**
     * 流式创意作家接口：根据给定主题生成故事草稿（流式输出）
     */
    public interface StreamingCreativeWriter {

        @UserMessage("""
                你是一位创意作家。
                请围绕给定主题生成一个不超过3句话的故事草稿。
                只返回故事内容，不要返回其他内容。
                主题：{{topic}}
                """)
        @Agent("根据给定主题生成故事草稿")
        TokenStream generateStory(@V("topic") String topic);

    }

    /**
     * 创建流式对话模型
     *
     * @return StreamingChatModel 实例
     */
    private static StreamingChatModel streamingBaseModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    public static void main(String[] args) {
        // 创建流式智能体
        // 注意：根据文章，应使用 streamingChatModel 方法，但如果当前版本不支持，
        // 可能需要使用 chatModel 方法并传入 StreamingChatModel 实例
        // 如果编译错误，请检查 langchain4j-agentic 版本是否支持 streamingChatModel 方法
        StreamingCreativeWriter creativeWriter = AgenticServices
                .agentBuilder(StreamingCreativeWriter.class)
                // .streamingChatModel(streamingBaseModel())
                .outputKey("story")
                .build();

        System.out.println("=== 流式智能体示例 ===");
        System.out.println("主题：龙与巫师");
        System.out.println("故事（流式输出）：");

        // 调用流式智能体，获取 TokenStream
        TokenStream tokenStream = creativeWriter.generateStory("龙与巫师");

        // 用于接收完整响应的 Future
        CompletableFuture<ChatResponse> futureResponse = new CompletableFuture<>();

        // 设置流式响应的回调处理
        tokenStream
                .onPartialResponse(partialResponse -> {
                    // 实时输出每个 token
                    System.out.print(partialResponse);
                })
                .onCompleteResponse(response -> {
                    // 流式响应完成
                    futureResponse.complete(response);
                })
                .onError(error -> {
                    // 处理错误
                    System.err.println("\n发生错误: " + error.getMessage());
                    futureResponse.completeExceptionally(error);
                })
                .start(); // 启动流式响应

        // 等待流式响应完成
        try {
            ChatResponse response = futureResponse.join();
            System.out.println("\n\n=== 流式响应完成 ===");
            System.out.println("完整故事: " + response.aiMessage().text());
            if (response.tokenUsage() != null) {
                System.out.println("Token 消耗: " + response.tokenUsage());
            }
        } catch (Exception e) {
            System.err.println("等待流式响应时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
