package io.github.susamlu.langchain4j.aiservice.basic.first;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

public class AiServiceFirstSample {

    interface Assistant {

        String chat(String userMessage);

    }

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .build();

        Assistant assistant = AiServices.create(Assistant.class, model);

        String answer = assistant.chat("Hello");
        System.out.println(answer);
    }

}
