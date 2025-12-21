package io.github.susamlu.langchain4j.chatmodel;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class HelloChatModel {

    public static void main(String[] args) {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");

        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .build();

        UserMessage firstUserMessage = UserMessage.from("我叫 Sam");
        AiMessage firstAiMessage = model.chat(firstUserMessage).aiMessage();
        System.out.println(firstAiMessage.text());

        UserMessage secondUserMessage = UserMessage.from("我叫什么名字？");
        AiMessage secondAiMessage = model.chat(firstUserMessage, firstAiMessage, secondUserMessage)
                .aiMessage();
        System.out.println(secondAiMessage.text());
    }

}
