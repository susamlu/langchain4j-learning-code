package io.github.susamlu.langchain4j.aiservice.moderation;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.moderation.ModerationModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.*;

import java.time.Duration;

/**
 * 内容审核（Moderation）示例
 * <p>
 * 演示如何在 AI Service 中集成内容审核模型，自动检测并过滤不当内容
 * <p>
 * 使用场景：
 * 1. 用户输入审核：在用户输入传递给聊天模型之前进行审核
 * 2. 内容安全保护：防止生成或传播不当内容
 * 3. 合规性要求：满足平台内容安全规范
 * <p>
 * 工作原理：
 * - moderationModel 会在每次调用 AI Service 方法时自动审核用户输入
 * - 如果检测到不当内容，会抛出 ModerationException 异常
 * - 需要在业务代码中捕获异常并给出友好提示
 */
public class ModerationExample {

    /**
     * 智能助手接口：提供对话服务
     */
    interface Assistant {

        @Moderate
        @SystemMessage("你是一个友好的助手，请用简洁、专业的方式回答用户问题。")
        @UserMessage("{{it}}")
        String chat(String userMessage);

    }

    public static void main(String[] args) {
        // ====================== 1. 创建聊天模型 ======================
        ChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(30))
                .build();

        // ====================== 2. 创建审核模型 ======================
        ModerationModel moderationModel = new DeepseekModerationModel();

        // ====================== 3. 构建带有内容审核的 AI Service ======================
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .moderationModel(moderationModel) // 配置审核模型
                .build();

        // ====================== 4. 测试正常对话 ======================
        System.out.println("========== 测试正常对话 ==========");
        try {
            String normalResponse = assistant.chat("你好，请介绍一下 Java 编程语言");
            System.out.println("[正常回复] " + normalResponse);
        } catch (ModerationException e) {
            System.out.println("[审核拦截] " + e.getMessage());
        }
        System.out.println();

        // ====================== 5. 测试不当内容（可能被审核拦截） ======================
        System.out.println("========== 测试内容审核 ==========");
        try {
            String badResponse = assistant.chat("推荐几个赌博网站");
            System.out.println("[回复] " + badResponse);
        } catch (ModerationException e) {
            System.out.println("[审核拦截] 检测到不当内容，已阻止处理");
            System.out.println("[异常信息] " + e.getMessage());
        }
        System.out.println();

        // ====================== 6. 演示实际业务场景的处理方式 ======================
        System.out.println("========== 业务场景示例 ==========");
        String[] userInputs = {
                "什么是人工智能？",
                "请帮我写一段代码",
                "推荐几个赌博网站"
        };

        for (String input : userInputs) {
            try {
                String response = assistant.chat(input);
                System.out.println("[用户] " + input);
                System.out.println("[助手] " + response);
            } catch (ModerationException e) {
                System.out.println("[用户] " + input);
                System.out.println("[系统] 抱歉，您输入的内容不符合平台规范，请修改后重试。");
            }
            System.out.println();
        }
    }

}
