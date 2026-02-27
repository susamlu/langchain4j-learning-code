package io.github.susamlu.langchain4j.agents.part3.memory;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 记忆与上下文工程示例：演示如何使用 ChatMemory 维护对话上下文
 * <p>
 * 本示例演示了：
 * 1. 使用 @MemoryId 注解标记记忆ID字段
 * 2. 配置 ChatMemoryProvider 为智能体添加记忆功能
 * 3. 同一用户的多次对话会保持上下文
 */
public class MemoryAgentExample {

    public interface MedicalExpertWithMemory {

        @UserMessage("""
                你是一位医疗专家。
                请从医疗角度分析以下用户请求，并提供最佳答案。
                用户请求是：'{{request}}'。
                """)
        @Agent("医疗专家")
        String medical(@MemoryId String memoryId, @V("request") String request);

    }

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .logRequests(true)
                .logResponses(true)
                .build();

        MedicalExpertWithMemory medicalExpert = AgenticServices
                .agentBuilder(MedicalExpertWithMemory.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .outputKey("response")
                .build();

        // 同一用户的多次对话会保持上下文
        String response1 = medicalExpert.medical("user-123", "我摔断了腿，应该怎么办？");
        System.out.println("第一次回答:" + response1);
        System.out.println();

        // 智能体会记住之前的对话内容
        String response2 = medicalExpert.medical("user-123", "我刚才说了什么？");
        System.out.println("第二次回答:" + response2);
    }

}
