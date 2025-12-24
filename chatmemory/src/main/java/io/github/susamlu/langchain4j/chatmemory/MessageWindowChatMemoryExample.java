package io.github.susamlu.langchain4j.chatmemory;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class MessageWindowChatMemoryExample {

    public static void main(String[] args) {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");

        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .build();

        // 构建按消息条数限制的对话记忆（保留最近20条消息）
        ChatMemory messageWindowMemory = MessageWindowChatMemory.builder()
                // 唯一标识：建议拼接 userId + sessionId 区分不同用户/会话
                .id("user-123-session-456")
                // 设置最大保留消息条数
                .maxMessages(20)
                .build();

        messageWindowMemory.add(SystemMessage.from("你是一个简洁的助手，回答尽量用要点。"));
        UserMessage u1 = UserMessage.from("我叫 Sam");
        messageWindowMemory.add(u1);
        AiMessage a1 = model.chat(messageWindowMemory.messages()).aiMessage();
        System.out.println(a1.text());
        messageWindowMemory.add(a1);

        UserMessage u2 = UserMessage.from("我叫什么名字？");
        messageWindowMemory.add(u2);
        AiMessage a2 = model.chat(messageWindowMemory.messages()).aiMessage();
        System.out.println(a2.text());
        messageWindowMemory.add(a2);
    }

}

