package io.github.susamlu.langchain4j.aiservice.memory;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

public class ChatMemoryExample {

    interface Assistant {

        String chat(String userMessage);

    }

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();

        String answer = assistant.chat("你好，请介绍一下你自己");
        System.out.println("问题: 你好，请介绍一下你自己");
        System.out.println("回答: " + answer);
        System.out.println();

        String answer2 = assistant.chat("今天天气怎么样？");
        System.out.println("问题: 今天天气怎么样？");
        System.out.println("回答: " + answer2);
        System.out.println();

        String answer3 = assistant.chat("我们刚才聊了什么？");
        System.out.println("问题: 我们刚才聊了什么？");
        System.out.println("回答: " + answer3);
    }

}