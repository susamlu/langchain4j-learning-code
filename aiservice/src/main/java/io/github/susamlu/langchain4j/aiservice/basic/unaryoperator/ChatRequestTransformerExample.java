package io.github.susamlu.langchain4j.aiservice.basic.unaryoperator;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

interface Assistant {

    String chat(String userMessage);

}

public class ChatRequestTransformerExample {

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .build();

        // 定义转换函数：向用户消息追加额外的上下文信息
        UnaryOperator<ChatRequest> transformingFunction = chatRequest -> {
            List<ChatMessage> enhancedMessages = new ArrayList<>();

            // 获取当前时间并格式化
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // 遍历原始消息，对用户消息进行增强
            chatRequest.messages().forEach(message -> {
                if (message instanceof UserMessage userMessage) {
                    // 向用户消息追加上下文
                    String enhancedText = userMessage.singleText() + "\n\n[上下文：当前时间为 " + currentTime + "]";
                    enhancedMessages.add(UserMessage.from(enhancedText));
                } else {
                    // 保留非用户消息（如系统消息）
                    enhancedMessages.add(message);
                }
            });

            // 创建新的 ChatRequest，使用增强后的消息列表
            return ChatRequest.builder()
                    .messages(enhancedMessages)
                    .build();
        };

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatRequestTransformer(transformingFunction)
                .build();

        String response = assistant.chat("今天是几号？");
        System.out.println(response);
    }

}
