package io.github.susamlu.langchain4j.aiservice.basic.systemmessage;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

public class SystemMessageProviderExample {

    interface Friend {

        // memoryId 参数：用于标识不同的 ChatMemory 实例
        // 如果没有这个参数，LangChain4j 会默认使用 "default" 作为 memoryId
        // userMessage 参数：必须使用 @UserMessage 注解标识为用户消息
        String chat(@MemoryId String memoryId, @UserMessage String userMessage);

    }

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .build();

        // 使用 chatMemoryProvider 根据 memoryId 动态创建 ChatMemory
        // memoryId 的值来自接口方法中带有 @MemoryId 注解的参数
        Friend friend = AiServices.builder(Friend.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId -> {
                    // memoryId 的值就是调用 chat 方法时传入的第一个参数（带有 @MemoryId 注解）
                    String userId = memoryId != null ? memoryId.toString() : "unknown";
                    System.out.println("创建 ChatMemory，memoryId: " + userId);
                    return MessageWindowChatMemory.builder()
                            .id(userId)
                            .maxMessages(10)
                            .build();
                })
                .systemMessageProvider(chatMemoryId -> {
                    // chatMemoryId 的值就是调用 chat 方法时传入的第一个参数（带有 @MemoryId 注解）
                    String userId = chatMemoryId != null ? chatMemoryId.toString() : "unknown";
                    System.out.println("systemMessageProvider memoryId: " + userId);
                    return "回答首句固定为：你好，{当前用户ID}。当前用户ID：" + userId;
                })
                .build();

        // 测试对话：传入 memoryId "user-123"
        String answer1 = friend.chat("user-123", "你好");
        System.out.println("回答1: " + answer1);

        String answer2 = friend.chat("user-123", "今天天气怎么样？");
        System.out.println("回答2: " + answer2);
    }

}

