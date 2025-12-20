package io.github.susamlu.langchain4j;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class HelloChatMemory {

    public static void main(String[] args) {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");

        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .build();

        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(20);
        memory.add(SystemMessage.from("你是一个简洁的助手，回答尽量用要点。"));

        UserMessage u1 = UserMessage.from("我叫 Sam");
        memory.add(u1);
        AiMessage a1 = model.chat(memory.messages()).aiMessage();
        System.out.println(a1.text());
        memory.add(a1);

        UserMessage u2 = UserMessage.from("我叫什么名字？");
        memory.add(u2);
        AiMessage a2 = model.chat(memory.messages()).aiMessage();
        System.out.println(a2.text());
        memory.add(a2);
    }

}
