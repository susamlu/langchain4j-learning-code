package io.github.susamlu.langchain4j.aiservice.streaming;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * Flux 响应式流式响应示例
 * <p>
 * 演示如何在 AI Service 中使用 Flux<String> 作为返回类型来实现响应式流式响应
 * <p>
 * Flux 是 Project Reactor 中的响应式流类型，提供了丰富的操作符：
 * - subscribe: 订阅流式响应
 * - doOnNext: 处理每个元素
 * - doOnComplete: 处理完成事件
 * - doOnError: 处理错误事件
 * - take: 限制元素数量
 * - buffer: 缓冲元素
 * - collect: 收集所有元素
 * - blockLast: 阻塞等待最后一个元素
 * <p>
 * 使用方式：
 * 1. 接口方法返回类型为 Flux<String>
 * 2. 使用 StreamingChatModel
 * 3. 通过 Flux 的操作符处理流式响应
 * 4. 使用 subscribe 订阅流式响应，或使用 blockLast 阻塞等待完成
 */
public class ReactiveStreamingExample {

    /**
     * 响应式流式助手接口
     */
    interface Assistant {

        /**
         * 响应式流式对话方法
         * <p>
         * 返回 Flux<String>，每个元素是模型返回的一个文本片段（token）
         *
         * @param message 用户消息
         * @return Flux<String> 响应式流式响应，每个元素是一个文本片段
         */
        Flux<String> chat(String message);

    }

    public static void main(String[] args) {
        // ====================== 1. 创建 StreamingChatModel 实例 ======================
        StreamingChatModel model = OpenAiStreamingChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .build();

        // ====================== 2. 使用 AiServices 创建 Assistant 实例 ======================
        Assistant assistant = AiServices.create(Assistant.class, model);

        // ====================== 3. 方式一：使用 subscribe 订阅流式响应 ======================
        System.out.println("========== 方式一：使用 subscribe 订阅流式响应 ==========");
        System.out.println("问题：给我讲个笑话");
        System.out.println("回答：");

        Flux<String> flux1 = assistant.chat("给我讲个笑话");

        flux1
                .doOnNext(partialResponse -> System.out.print(partialResponse))
                .doOnComplete(() -> System.out.println("\n\n--- 流式响应完成 ---"))
                .doOnError(error -> {
                    System.err.println("\n发生错误: " + error.getMessage());
                    error.printStackTrace();
                })
                .subscribe();

        // 等待流式响应完成（简单示例，实际应用中可以使用 CountDownLatch 或其他同步机制）
        try {
            Thread.sleep(5000); // 等待5秒
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\n");

        // ====================== 4. 方式二：使用 blockLast 阻塞等待完成 ======================
        System.out.println("========== 方式二：使用 blockLast 阻塞等待完成 ==========");
        System.out.println("问题：给我讲个故事");
        System.out.println("回答：");

        Flux<String> flux2 = assistant.chat("给我讲个故事");

        try {
            // blockLast 会阻塞直到流完成，并返回最后一个元素
            String lastToken = flux2
                    .doOnNext(partialResponse -> System.out.print(partialResponse))
                    .blockLast(Duration.ofSeconds(30)); // 设置30秒超时

            System.out.println("\n\n--- 流式响应完成 ---");
            System.out.println("最后一个 token: " + lastToken);
        } catch (Exception e) {
            System.err.println("等待流式响应时发生异常: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n");

        // ====================== 5. 方式三：限制处理的响应数量 ======================
        System.out.println("========== 方式三：限制处理的响应数量 ==========");
        System.out.println("问题：用一句话介绍 Java");
        System.out.println("回答（仅显示前 10 个片段）：");

        Flux<String> flux3 = assistant.chat("用一句话介绍 Java");

        flux3
                .take(10) // 只取前10个元素
                .doOnNext(partialResponse -> System.out.print(partialResponse))
                .doOnComplete(() -> System.out.println("\n\n--- 已限制为前 10 个片段 ---"))
                .subscribe();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\n");

        // ====================== 6. 方式四：收集完整响应 ======================
        System.out.println("========== 方式四：收集完整响应 ==========");
        System.out.println("问题：介绍一下人工智能");
        System.out.println("回答：");

        Flux<String> flux4 = assistant.chat("介绍一下人工智能");

        try {
            // 收集所有响应片段并拼接成完整响应
            String fullResponse = flux4
                    .doOnNext(partialResponse -> System.out.print(partialResponse))
                    .collectList() // 收集所有元素到 List
                    .map(list -> String.join("", list)) // 拼接成完整字符串
                    .block(Duration.ofSeconds(30)); // 阻塞等待完成

            System.out.println("\n\n--- 流式响应完成 ---");
            System.out.println("完整响应: " + fullResponse);
        } catch (Exception e) {
            System.err.println("等待流式响应时发生异常: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n");

        // ====================== 7. 方式五：使用 buffer 缓冲响应 ======================
        System.out.println("========== 方式五：使用 buffer 缓冲响应 ==========");
        System.out.println("问题：解释一下什么是机器学习");
        System.out.println("回答（每5个片段输出一次）：");

        Flux<String> flux5 = assistant.chat("解释一下什么是机器学习");

        flux5
                .buffer(5) // 每5个元素缓冲一次
                .doOnNext(buffer -> {
                    System.out.print("\n[缓冲] ");
                    buffer.forEach(System.out::print);
                })
                .doOnComplete(() -> System.out.println("\n\n--- 流式响应完成 ---"))
                .subscribe();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}

