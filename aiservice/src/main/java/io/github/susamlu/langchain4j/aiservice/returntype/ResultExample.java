package io.github.susamlu.langchain4j.aiservice.returntype;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;

import java.util.List;

/**
 * Result 类型使用示例
 * <p>
 * 演示如何在 AI Service 中使用 Result 类型获取详细的响应信息
 * <p>
 * Result 类型提供了丰富的元数据信息：
 * - content(): 获取主要内容
 * - tokenUsage(): 获取 token 使用情况
 * - sources(): 获取 RAG 检索到的内容（如果配置了 RAG）
 * - toolExecutions(): 获取工具执行信息（如果使用了工具）
 * - finishReason(): 获取完成原因
 */
public class ResultExample {

    interface Assistant {

        /**
         * 生成文章大纲方法
         *
         * @param topic 主题
         * @return Result<List < String>> 包含大纲列表和元数据信息
         */
        @UserMessage("为以下主题生成文章大纲：{{it}}")
        Result<List<String>> generateOutlineFor(String topic);

    }

    public static void main(String[] args) {
        // ====================== 1. 创建 ChatModel 实例 ======================
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .logRequests(true)
                .logResponses(true)
                .build();

        // ====================== 2. 使用 AiServices 创建 Assistant 实例 ======================
        Assistant assistant = AiServices.create(Assistant.class, model);

        // ====================== 3. 调用方法并获取 Result ======================
        System.out.println("========== 生成文章大纲示例 ==========");
        Result<List<String>> result = assistant.generateOutlineFor("Java");

        // ====================== 4. 获取主要内容 ======================
        List<String> outline = result.content();
        System.out.println("文章大纲：");
        for (int i = 0; i < outline.size(); i++) {
            System.out.println((i + 1) + ". " + outline.get(i));
        }
        System.out.println();

        // ====================== 5. 获取 token 使用情况 ======================
        System.out.println("========== token 使用情况 ==========");
        if (result.tokenUsage() != null) {
            System.out.println("输入 token 数: " + result.tokenUsage().inputTokenCount());
            System.out.println("输出 token 数: " + result.tokenUsage().outputTokenCount());
            System.out.println("总 token 数: " + result.tokenUsage().totalTokenCount());
        } else {
            System.out.println("token 使用情况不可用");
        }
        System.out.println();

        // ====================== 6. 获取 RAG 检索到的内容（如果配置了 RAG） ======================
        System.out.println("========== RAG 检索内容 ==========");
        if (result.sources() != null && !result.sources().isEmpty()) {
            System.out.println("检索到的内容数量: " + result.sources().size());
            for (int i = 0; i < result.sources().size(); i++) {
                System.out.println("来源 " + (i + 1) + ": " + result.sources().get(i));
            }
        } else {
            System.out.println("未配置 RAG 或未检索到相关内容");
        }
        System.out.println();

        // ====================== 7. 获取工具执行信息（如果使用了工具） ======================
        System.out.println("========== 工具执行信息 ==========");
        if (result.toolExecutions() != null && !result.toolExecutions().isEmpty()) {
            System.out.println("工具执行次数: " + result.toolExecutions().size());
            for (int i = 0; i < result.toolExecutions().size(); i++) {
                System.out.println("工具执行 " + (i + 1) + ": " + result.toolExecutions().get(i));
            }
        } else {
            System.out.println("未使用工具");
        }
        System.out.println();

        // ====================== 8. 获取完成原因 ======================
        System.out.println("========== 完成原因 ==========");
        if (result.finishReason() != null) {
            System.out.println("完成原因: " + result.finishReason());
        } else {
            System.out.println("完成原因不可用");
        }
        System.out.println();

        // ====================== 9. 测试不同的主题 ======================
        System.out.println("========== 测试不同主题 ==========");
        Result<List<String>> result2 = assistant.generateOutlineFor("人工智能");
        List<String> outline2 = result2.content();
        System.out.println("主题: 人工智能");
        System.out.println("大纲：");
        for (int i = 0; i < outline2.size(); i++) {
            System.out.println((i + 1) + ". " + outline2.get(i));
        }
        if (result2.tokenUsage() != null) {
            System.out.println("Token 使用: " + result2.tokenUsage().totalTokenCount());
        }
        System.out.println();
    }

}

