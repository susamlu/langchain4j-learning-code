package io.github.susamlu.langchain4j.responsestreaming;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import static dev.langchain4j.model.LambdaStreamingResponseHandler.onPartialResponse;
import static dev.langchain4j.model.LambdaStreamingResponseHandler.onPartialResponseAndError;

public class LambdaStreamingExample {

    public static void main(String[] args) throws InterruptedException {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");

        StreamingChatModel model = OpenAiStreamingChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .build();

        String userMessage = "给我讲一个笑话";

        // 方式一：仅处理增量响应
        System.out.println("【开始流式输出】");
        model.chat(userMessage, onPartialResponse(System.out::print));
        Thread.sleep(10000); // 等待流式响应完成

        // 方式二：同时处理增量响应和错误
        System.out.println("\n\n【开始流式输出】");
        model.chat(userMessage, onPartialResponseAndError(
                System.out::print,                    // 处理增量响应
                Throwable::printStackTrace            // 处理错误
        ));
        Thread.sleep(10000); // 等待流式响应完成
    }

}
