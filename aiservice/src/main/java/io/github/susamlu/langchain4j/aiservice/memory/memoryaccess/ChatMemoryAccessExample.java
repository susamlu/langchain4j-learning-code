package io.github.susamlu.langchain4j.aiservice.memory.memoryaccess;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.memory.ChatMemoryAccess;

import java.util.List;

/**
 * 演示如何使用 ChatMemoryAccess 接口访问和管理特定 memoryId 的对话记忆
 * - 展示如何获取特定用户的对话记忆
 * - 展示如何清除特定用户的对话记忆
 */
public class ChatMemoryAccessExample {

    interface Assistant extends ChatMemoryAccess {

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

        // 访问 Klaus 的对话记忆
        ChatMemory klausChatMemory = assistant.getChatMemory(1);
        List<ChatMessage> messagesWithKlaus = klausChatMemory.messages();
        System.out.println("Klaus 的对话记忆:");
        for (int i = 0; i < messagesWithKlaus.size(); i++) {
            System.out.println("  [" + (i + 1) + "] " + messagesWithKlaus.get(i));
        }
        System.out.println();

        // 访问 Francine 的对话记忆
        ChatMemory francineChatMemory = assistant.getChatMemory(2);
        List<ChatMessage> messagesWithFrancine = francineChatMemory.messages();
        System.out.println("Francine 的对话记忆:");
        for (int i = 0; i < messagesWithFrancine.size(); i++) {
            System.out.println("  [" + (i + 1) + "] " + messagesWithFrancine.get(i));
        }
        System.out.println();

        // 清除 Francine 的对话记忆
        boolean chatMemoryWithFrancineEvicted = assistant.evictChatMemory(2);
        System.out.println("清除 Francine 的对话记忆: " + chatMemoryWithFrancineEvicted);

        // 再次尝试访问 Francine 的对话记忆（应该为空）
        ChatMemory francineChatMemoryAfterEviction = assistant.getChatMemory(2);
        if (francineChatMemoryAfterEviction != null) {
            List<ChatMessage> messagesWithFrancineAfterEviction = francineChatMemoryAfterEviction.messages();
            System.out.println("清除后 Francine 的对话记忆数量: " + messagesWithFrancineAfterEviction.size());
        } else {
            System.out.println("清除后 Francine 的对话记忆已不存在");
        }

        // Francine 开始新对话
        String francineNewConversation = assistant.chat(2, "我们刚才聊了什么？");
        System.out.println("Francine 新对话: 我们刚才聊了什么？");
        System.out.println("AI 回复: " + francineNewConversation);
    }

}