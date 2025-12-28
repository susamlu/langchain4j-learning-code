package io.github.susamlu.langchain4j.responsestreaming.websocket;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketChatController {

    private final StreamingChatModel model;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketChatController(SimpMessagingTemplate messagingTemplate) {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");

        this.model = OpenAiStreamingChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .build();
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat")
    public void handleChat(String message) {
        model.chat(message, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                // 通过 WebSocket 推送增量 token
                messagingTemplate.convertAndSend("/topic/response",
                        new StreamingMessage("partial", partialResponse));
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                // 推送完成事件
                messagingTemplate.convertAndSend("/topic/response",
                        new StreamingMessage("complete", completeResponse.aiMessage().text()));
            }

            @Override
            public void onError(Throwable error) {
                messagingTemplate.convertAndSend("/topic/response",
                        new StreamingMessage("error", error.getMessage()));
            }
        });
    }

    // 消息封装类
    public static class StreamingMessage {

        private String type;
        private String content;

        public StreamingMessage(String type, String content) {
            this.type = type;
            this.content = content;
        }

        // getters and setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

    }

}
