package io.github.susamlu.langchain4j.aiservice.basic.bifunction;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

interface Assistant {

    String chat(@MemoryId String memoryId, @UserMessage String userMessage);

}

public class ChatRequestTransformerWithMemoryExample {

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .build();

        // 定义转换函数：根据 memory ID 动态修改系统消息
        BiFunction<ChatRequest, Object, ChatRequest> transformingFunction = (chatRequest, memoryId) -> {
            // 根据 memory ID 获取用户特定的配置
            String userId = memoryId != null ? memoryId.toString() : "unknown";

            // 根据用户 ID 动态生成系统消息
            String systemMessageText = "回答首句固定为：你好，{当前用户ID}。当前用户ID: " + userId;

            // 构建新的消息列表
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(systemMessageText));

            // 保留原始用户消息
            messages.addAll(chatRequest.messages());

            // 创建新的 ChatRequest
            return ChatRequest.builder()
                    .messages(messages)
                    .build();
        };

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId -> {
                    String userId = memoryId != null ? memoryId.toString() : "unknown";
                    return MessageWindowChatMemory.builder()
                            .id(userId)
                            .maxMessages(10)
                            .build();
                })
                .chatRequestTransformer(transformingFunction)
                .build();

        String answer1 = assistant.chat("user-123", "你好");
        System.out.println("回答1: " + answer1);

        String answer2 = assistant.chat("user-123", "今天天气怎么样？");
        System.out.println("回答2: " + answer2);
    }

}
