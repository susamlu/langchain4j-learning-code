package io.github.susamlu.langchain4j.chatmodel;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * LangChain4j 流式对话样例，功能：流式接收千问模型返回的 token，模拟 SSE 推送给客户端，包含完整的异常处理、完成回调
 */
public class StreamingChatExample {

    // 用于阻塞主线程，避免流式回调未执行完程序退出
    private static final CountDownLatch LATCH = new CountDownLatch(1);

    public static void main(String[] args) {
        // 1. 初始化千问流式模型
        StreamingChatModel streamingModel = initQwenStreamingModel();

        // 2. 构造对话消息（可替换为业务场景的消息）
        List<ChatMessage> messages = List.of(
                SystemMessage.from("你是专业的Java技术助手，回答简洁、准确，分点说明。"),
                UserMessage.from("请详细说明 LangChain4j 流式对话的核心优势和使用注意事项")
        );

        // 3. 执行流式对话调用
        System.out.println("【开始流式输出】\n");
        streamingModel.chat(messages, new StreamingChatResponseHandler() {
            // 拼接完整响应（用于完成后落库/统计）
            private final StringBuilder fullResponse = new StringBuilder();

            /**
             * 核心回调：每接收一个 token 触发（流式输出核心）
             * @param partialResponse 模型返回的单个文本片段（千问返回的是完整语义片段，非单个字符）
             */
            @Override
            public void onPartialResponse(String partialResponse) {
                // 1. 拼接完整响应
                fullResponse.append(partialResponse);
                // 2. 模拟 SSE/WebSocket 推送给客户端（生产环境替换为实际推送逻辑）
                pushToClient(partialResponse);
            }

            /**
             * 流式生成完成回调（所有 token 接收完毕）
             * 可用于：记录 token 消耗、落库对话记录、更新 ChatMemory 等
             */
            @Override
            public void onCompleteResponse(ChatResponse response) {
                System.out.println("\n\n【流式生成完成】");
                System.out.println("完整回答：\n" + fullResponse);
                System.out.println("模型结束原因：" + response.finishReason());
                // 释放计数器，让主线程退出
                LATCH.countDown();
            }

            /**
             * 异常回调（超时、限流、API Key 错误、网络异常等）
             */
            @Override
            public void onError(Throwable error) {
                System.err.println("\n【流式调用异常】");
                // 分类处理常见异常
                if (error.getMessage().contains("401")) {
                    System.err.println("错误原因：API Key 无效或未授权，请检查千问 API Key");
                } else if (error.getMessage().contains("429")) {
                    System.err.println("错误原因：请求限流，请降低调用频率或联系阿里云提升配额");
                } else if (error.getMessage().contains("timeout")) {
                    System.err.println("错误原因：请求超时，请增大超时时间或检查网络");
                } else {
                    System.err.println("错误原因：" + error.getMessage());
                    throw new RuntimeException(error);
                }
                // 释放计数器，避免主线程卡死
                LATCH.countDown();
            }
        });

        // 阻塞主线程，等待流式回调执行完成
        try {
            LATCH.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("主线程被中断：" + e.getMessage());
        }
    }

    /**
     * 初始化通义千问流式模型，适配 qwen-plus/qwen-max（推荐 qwen-max 流式效果更佳）
     */
    private static StreamingChatModel initQwenStreamingModel() {
        // 替换为自己的通义千问 API Key（从阿里云百炼控制台获取：https://dashscope.console.aliyun.com/）
        String qwenApiKey = System.getenv("QWEN_API_KEY");
        if (qwenApiKey == null || qwenApiKey.isEmpty()) {
            qwenApiKey = "your-qwen-api-key"; // 本地测试可临时赋值，生产环境务必用环境变量
        }

        return OpenAiStreamingChatModel.builder()
                // 千问 OpenAI 兼容接口地址（固定）
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                // 千问 API Key（阿里云百炼控制台获取）
                .apiKey(qwenApiKey)
                // 千问流式模型名（qwen-plus 轻量版，qwen-max 高性能版）
                .modelName("qwen-plus")
                // 流式输出建议低温度，保证回答稳定性
                .temperature(0.1)
                // 流式请求超时时间（建议≥60秒，避免长文本生成超时）
                .timeout(Duration.ofMinutes(1))
                .build();
    }

    /**
     * 模拟将流式 token 推送给客户端（生产环境替换为 SSE/WebSocket 实际推送逻辑）
     *
     * @param token 模型返回的单个文本片段
     */
    private static void pushToClient(String token) {
        // 模拟 SSE 推送格式：data: {token}\n\n
        System.out.print(token); // 控制台实时打印（模拟客户端接收）
        // 生产环境示例（SSE 推送）：
        // response.getWriter().write("data: " + token + "\n\n");
        // response.getWriter().flush();
    }

}