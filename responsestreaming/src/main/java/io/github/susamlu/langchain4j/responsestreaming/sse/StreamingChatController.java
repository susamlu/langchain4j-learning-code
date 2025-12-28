package io.github.susamlu.langchain4j.responsestreaming.sse;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/api/chat")
public class StreamingChatController {

    private final StreamingChatModel model;

    public StreamingChatController() {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");

        this.model = OpenAiStreamingChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .build();
    }

    @CrossOrigin(origins = "*") // 调试临时配置，生产环境需限定具体跨域域名，禁止通配符
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestParam(name = "message") String message) {
        SseEmitter emitter = new SseEmitter(60000L); // 60秒超时

        model.chat(message, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                try {
                    // 发送增量 token 到前端
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(partialResponse));
                } catch (IOException e) {
                    // 连接可能已断开，标记错误并停止后续处理
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                try {
                    // 发送完成事件
                    emitter.send(SseEmitter.event()
                            .name("complete")
                            .data(completeResponse.aiMessage().text()));
                    emitter.complete();
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onError(Throwable error) {
                emitter.completeWithError(error);
            }
        });

        return emitter;
    }

}
