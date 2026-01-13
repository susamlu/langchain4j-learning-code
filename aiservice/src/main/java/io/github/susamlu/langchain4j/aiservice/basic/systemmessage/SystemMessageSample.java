package io.github.susamlu.langchain4j.aiservice.basic.systemmessage;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;

public class SystemMessageSample {

    interface Friend {

        @SystemMessage("你是我的好朋友，请全程用粤语回答我的问题。")
        String chat(String userMessage);

    }

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .build();

        Friend friend = AiServices.create(Friend.class, model);

        String answer = friend.chat("你好");
        System.out.println(answer);
    }

}
