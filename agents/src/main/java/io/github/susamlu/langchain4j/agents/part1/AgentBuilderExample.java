package io.github.susamlu.langchain4j.agents.part1;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AgentBuilder 示例：演示如何使用 AgenticServices 创建代理服务
 */
public class AgentBuilderExample {

    /**
     * 创意作家接口：根据给定主题生成故事草稿
     */
    public interface CreativeWriter {

        @UserMessage("""
                你是一位创意作家。
                请围绕给定主题生成一个不超过3句话的故事草稿。
                只返回故事内容，不要返回其他内容。
                主题：{{topic}}
                """)
        @Agent("根据给定主题生成故事草稿")
        String generateStory(@V("topic") String topic);

    }

    public static void main(String[] args) {
        // 创建对话模型
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .logRequests(true)
                .logResponses(true)
                .build();

        // 创建智能体
        CreativeWriter creativeWriter = AgenticServices
                .agentBuilder(CreativeWriter.class)
                .chatModel(model)
                .outputKey("story")
                .build();

        // 测试生成故事
        String story = creativeWriter.generateStory("太空探索");
        System.out.println("生成的故事：");
        System.out.println(story);
    }

}
