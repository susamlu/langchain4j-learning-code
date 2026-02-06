package io.github.susamlu.langchain4j.agents.streaming;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.concurrent.CompletableFuture;

/**
 * 流式智能体序列示例：演示多个流式智能体在顺序工作流中的行为
 * <p>
 * 在智能体系统内部使用时，流式智能体仅当作为最后一个被调用的智能体时，
 * 才能将其流式响应传播至整个系统。其他场景下，其行为与异步智能体一致，
 * 后续智能体需等待其流式响应完全完成后，方可获取并使用其结果。
 * <p>
 * 本示例演示了三个流式智能体的序列组合：
 * 1. StreamingCreativeWriter - 生成故事草稿
 * 2. StreamingAudienceEditor - 根据受众编辑故事
 * 3. StreamingStyleEditor - 根据风格编辑故事
 * <p>
 * 只有最后一个 StreamingStyleEditor 的流式响应会作为整个系统的流式响应向外传播。
 */
public class StreamingReviewedWriterExample {

    /**
     * 流式创意作家接口：根据给定主题生成故事草稿
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
     * 流式受众编辑接口：根据目标受众编辑故事
     */
    public interface StreamingAudienceEditor {

        @UserMessage("""
                你是一位专业编辑。
                分析并重写以下故事，使其更好地符合目标受众：{{audience}}。
                只返回故事内容，不要返回其他内容。
                故事内容："{{story}}"
                """)
        @Agent("编辑故事以更好地适应给定的目标受众")
        TokenStream editStory(@V("story") String story, @V("audience") String audience);

    }

    /**
     * 流式风格编辑接口：根据风格编辑故事
     */
    public interface StreamingStyleEditor {

        @UserMessage("""
                你是一位专业编辑。
                分析并重写以下故事，使其更好地符合并更连贯地体现 {{style}} 风格。
                只返回故事内容，不要返回其他内容。
                故事内容："{{story}}"
                """)
        @Agent("编辑故事以更好地适应给定的风格")
        TokenStream editStory(@V("story") String story, @V("style") String style);

    }

    /**
     * 流式审查作家接口：通过序列工作流组合三个流式智能体
     */
    public interface StreamingReviewedWriter {

        @Agent
        TokenStream writeStory(@V("topic") String topic, @V("audience") String audience, @V("style") String style);

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
        // 创建流式创意作家智能体
        // 注意：根据文章，应使用 streamingChatModel 方法，但如果当前版本不支持，
        // 可能需要使用 chatModel 方法并传入 StreamingChatModel 实例
        // 如果编译错误，请检查 langchain4j-agentic 版本是否支持 streamingChatModel 方法
        StreamingCreativeWriter creativeWriter = AgenticServices
                .agentBuilder(StreamingCreativeWriter.class)
                .chatModel((ChatModel) streamingBaseModel())
                // .streamingChatModel(streamingBaseModel())
                .outputKey("story")
                .build();

        // 创建流式受众编辑智能体
        StreamingAudienceEditor audienceEditor = AgenticServices
                .agentBuilder(StreamingAudienceEditor.class)
                .chatModel((ChatModel) streamingBaseModel())
                // .streamingChatModel(streamingBaseModel())
                .outputKey("story")
                .build();

        // 创建流式风格编辑智能体
        StreamingStyleEditor styleEditor = AgenticServices
                .agentBuilder(StreamingStyleEditor.class)
                .chatModel((ChatModel) streamingBaseModel())
                // .streamingChatModel(streamingBaseModel())
                .outputKey("story")
                .build();

        // 创建顺序工作流：按顺序执行三个流式智能体
        // 注意：前两个智能体的流式响应会在后续智能体调用启动前被内部完全消费，
        // 仅最后一个 styleEditor 的流式响应会作为整个系统的流式响应向外传播
        StreamingReviewedWriter novelCreator = AgenticServices
                .sequenceBuilder(StreamingReviewedWriter.class)
                .subAgents(creativeWriter, audienceEditor, styleEditor)
                .outputKey("story")
                .build();

        System.out.println("=== 流式智能体序列示例 ===");
        System.out.println("主题：龙与巫师");
        System.out.println("受众：青年");
        System.out.println("风格：奇幻");
        System.out.println("故事（流式输出，仅最后一个智能体的响应会流式传播）：");

        // 调用流式智能体系统
        TokenStream tokenStream = novelCreator.writeStory("龙与巫师", "青年", "奇幻");

        // 用于接收完整响应的 Future
        CompletableFuture<ChatResponse> futureResponse = new CompletableFuture<>();

        // 设置流式响应的回调处理
        tokenStream
                .onPartialResponse(partialResponse -> {
                    // 实时输出每个 token（仅最后一个智能体的响应）
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
            System.out.println("最终故事: " + response.aiMessage().text());
            if (response.tokenUsage() != null) {
                System.out.println("Token 消耗: " + response.tokenUsage());
            }
            System.out.println("\n注意：前两个智能体（CreativeWriter 和 AudienceEditor）的流式响应");
            System.out.println("已在内部完全消费，只有最后一个 StyleEditor 的流式响应被传播。");
        } catch (Exception e) {
            System.err.println("等待流式响应时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
