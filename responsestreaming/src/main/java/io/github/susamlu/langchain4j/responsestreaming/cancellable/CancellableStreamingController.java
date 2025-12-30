package io.github.susamlu.langchain4j.responsestreaming.cancellable;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.*;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/cancellable/chat/")
public class CancellableStreamingController {

    private final StreamingChatModel model;
    // 存储每个请求的 StreamingHandle，key 为请求 ID
    private final Map<String, StreamingHandle> streamingHandles = new ConcurrentHashMap<>();
    // 存储每个请求的 SseEmitter，key 为请求 ID
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public CancellableStreamingController() {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");

        this.model = OpenAiStreamingChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .build();
    }

    @CrossOrigin(origins = "*")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @RequestParam(name = "message") String message,
            @RequestParam(name = "requestId") String requestId) {
        System.out.println("\n--- 流式响应开始 ---");
        SseEmitter emitter = new SseEmitter(60000L);

        // 存储 emitter
        emitters.put(requestId, emitter);

        // 设置完成回调：清理资源
        emitter.onCompletion(() -> {
            System.out.println("SSE 连接完成，清理资源: " + requestId);
            emitters.remove(requestId);
            streamingHandles.remove(requestId);
        });

        // 设置超时回调：清理资源
        emitter.onTimeout(() -> {
            System.out.println("SSE 连接超时，清理资源: " + requestId);
            emitters.remove(requestId);
            streamingHandles.remove(requestId);
        });

        // 设置错误回调：清理资源
        emitter.onError((throwable) -> {
            System.out.println("SSE 连接错误，清理资源: " + requestId + ", 错误: " + throwable.getMessage());
            emitters.remove(requestId);
            streamingHandles.remove(requestId);
        });

        model.chat(message, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(PartialResponse partialResponse, PartialResponseContext context) {
                // 检查 emitter 是否还存在且有效
                SseEmitter currentEmitter = emitters.get(requestId);
                if (currentEmitter == null || currentEmitter != emitter) {
                    // emitter 已被移除或替换，说明连接已关闭，停止处理
                    System.out.println("Emitter 已关闭，停止处理: " + requestId);
                    return;
                }

                System.out.print(partialResponse.text());

                try {
                    // 保存 StreamingHandle
                    streamingHandles.put(requestId, context.streamingHandle());

                    // 发送增量 token
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(partialResponse.text()));
                } catch (IOException e) {
                    // 连接已关闭，清理资源
                    System.out.println("发送数据时连接已关闭: " + requestId);
                    cleanup(requestId);
                } catch (Exception e) {
                    // 其他异常，清理资源
                    System.out.println("发送数据时发生异常: " + requestId + ", " + e.getMessage());
                    cleanup(requestId);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                System.out.println("\n--- 流式响应完成 ---");

                // 检查 emitter 是否还存在且有效
                SseEmitter currentEmitter = emitters.get(requestId);
                if (currentEmitter == null || currentEmitter != emitter) {
                    // emitter 已被移除，说明连接已关闭
                    System.out.println("Emitter 已关闭，跳过完成事件: " + requestId);
                    cleanup(requestId);
                    return;
                }

                try {
                    emitter.send(SseEmitter.event()
                            .name("complete")
                            .data(completeResponse.aiMessage().text()));
                    emitter.complete();
                } catch (IOException e) {
                    // 连接已关闭，清理资源
                    System.out.println("发送完成事件时连接已关闭: " + requestId);
                    cleanup(requestId);
                } catch (Exception e) {
                    // 其他异常，清理资源
                    System.out.println("发送完成事件时发生异常: " + requestId + ", " + e.getMessage());
                    cleanup(requestId);
                }
            }

            @Override
            public void onError(Throwable error) {
                System.out.println("流式响应错误: " + requestId + ", " + error.getMessage());

                // 检查 emitter 是否还存在且有效
                SseEmitter currentEmitter = emitters.get(requestId);
                if (currentEmitter != null && currentEmitter == emitter) {
                    try {
                        emitter.completeWithError(error);
                    } catch (Exception e) {
                        // 忽略错误，直接清理
                        System.out.println("发送错误事件时发生异常: " + e.getMessage());
                    }
                }
                cleanup(requestId);
            }
        });

        return emitter;
    }

    // 清理资源的辅助方法
    private void cleanup(String requestId) {
        emitters.remove(requestId);
        streamingHandles.remove(requestId);
    }

    @PostMapping("/cancel")
    public void cancelStreaming(@RequestParam(name = "requestId") String requestId) {
        System.out.println("收到取消请求: " + requestId);

        // 取消 StreamingHandle
        StreamingHandle handle = streamingHandles.get(requestId);
        if (handle != null) {
            handle.cancel();
            System.out.println("已取消 StreamingHandle: " + requestId);
        }

        // 关闭并移除 SseEmitter
        SseEmitter emitter = emitters.get(requestId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                // 忽略异常，可能已经关闭
                System.out.println("关闭 emitter 时发生异常（可忽略）: " + e.getMessage());
            }
            System.out.println("已关闭 SseEmitter: " + requestId);
        }

        // 清理资源
        cleanup(requestId);
    }

}
