package io.github.susamlu.langchain4j.chatmodel;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class HelloMultiModality {

    public static void main(String[] args) {
        String apiKey = System.getenv("QWEN_API_KEY");

        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .apiKey(apiKey)
                .modelName("qwen-vl-plus")
                .build();

        UserMessage userMessage = UserMessage.from(
                TextContent.from("详细描述以下图像"),
                ImageContent.from("https://picsum.photos/id/0/200")
        );
        ChatResponse response = model.chat(userMessage);
        System.out.println(response.aiMessage().text());
    }

}

