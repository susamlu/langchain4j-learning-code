package io.github.susamlu.langchain4j.aiservice.memory.memoryprovider;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

/**
 * 演示如何使用 chatMemoryProvider 和 @MemoryId 注解创建多用户对话记忆
 * - 展示如何配置不同的对话记忆实例给不同的用户
 */
public class ChatMemoryProviderExample {

    interface Assistant {

        String chat(@MemoryId int memoryId, @UserMessage String message);

    }

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .build();

        // 使用 AiServices 构建器模式创建助手，并配置 chatMemoryProvider
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10)) // 为每个 memoryId 提供独立的记忆实例
                .build();

        // 不同用户使用不同的 memoryId，各自拥有独立的对话记忆
        String answerToKlaus = assistant.chat(1, "Hello, my name is Klaus");
        System.out.println("用户1 (Klaus) 说: Hello, my name is Klaus");
        System.out.println("AI 回复: " + answerToKlaus);
        System.out.println();

        String answerToFrancine = assistant.chat(2, "Hello, my name is Francine");
        System.out.println("用户2 (Francine) 说: Hello, my name is Francine");
        System.out.println("AI 回复: " + answerToFrancine);
        System.out.println();

        // 用户1再次提问，应该能回忆起之前的信息
        String klausFollowUp = assistant.chat(1, "What is my name?");
        System.out.println("用户1 (Klaus) 说: What is my name?");
        System.out.println("AI 回复: " + klausFollowUp);
        System.out.println();

        // 用户2再次提问，应该能回忆起之前的信息
        String francineFollowUp = assistant.chat(2, "What is my name?");
        System.out.println("用户2 (Francine) 说: What is my name?");
        System.out.println("AI 回复: " + francineFollowUp);
        System.out.println();

        // 再次测试用户1的记忆
        String klausAnotherQuestion = assistant.chat(1, "我们刚才聊了什么？");
        System.out.println("用户1 (Klaus) 说: 我们刚才聊了什么？");
        System.out.println("AI 回复: " + klausAnotherQuestion);
    }

}