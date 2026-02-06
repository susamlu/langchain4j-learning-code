package io.github.susamlu.langchain4j.agents.declarative;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.declarative.ActivationCondition;
import dev.langchain4j.agentic.declarative.ConditionalAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 声明式API示例：演示如何使用声明式API定义条件工作流
 * <p>
 * 声明式API通过注解来定义智能体和工作流，使代码更简洁、可读性更强。
 * <p>
 * 本示例演示了：
 * 1. 使用 @ConditionalAgent 注解定义条件工作流
 * 2. 使用 @ActivationCondition 注解定义激活条件
 * 3. 使用 @ChatModelSupplier 注解为子智能体指定不同的 ChatModel
 * 4. 使用 AgenticServices.createAgenticSystem() 创建智能体系统
 */
public class DeclarativeConditionalExample {

    /**
     * 请求分类枚举
     */
    public enum RequestCategory {
        LEGAL,      // 法律
        MEDICAL,    // 医疗
        TECHNICAL,  // 技术
        UNKNOWN     // 未知
    }

    /**
     * 医疗专家接口：提供医疗建议（声明式定义）
     * <p>
     * 注意：子智能体不需要 @ChatModelSupplier，它们会使用传入 ExpertsAgent 的默认 ChatModel
     */
    public interface MedicalExpert {

        @UserMessage("""
                你是一位医疗专家。
                请从医疗角度分析以下用户请求，并提供最佳答案。
                用户请求是：{{request}}。
                """)
        @Agent(value = "医疗专家", outputKey = "response")
        String medical(@V("request") String request);

    }

    /**
     * 技术专家接口：提供技术建议（声明式定义）
     * <p>
     * 注意：子智能体不需要 @ChatModelSupplier，它们会使用传入 ExpertsAgent 的默认 ChatModel
     */
    public interface TechnicalExpert {

        @UserMessage("""
                你是一位技术专家。
                请从技术角度分析以下用户请求，并提供最佳答案。
                用户请求是：{{request}}。
                """)
        @Agent(value = "技术专家", outputKey = "response")
        String technical(@V("request") String request);

    }

    /**
     * 法律专家接口：提供法律建议（声明式定义）
     * <p>
     * 注意：子智能体不需要 @ChatModelSupplier，它们会使用传入 ExpertsAgent 的默认 ChatModel
     */
    public interface LegalExpert {

        @UserMessage("""
                你是一位法律专家。
                请从法律角度分析以下用户请求，并提供最佳答案。
                用户请求是：{{request}}。
                """)
        @Agent(value = "法律专家", outputKey = "response")
        String legal(@V("request") String request);

    }

    /**
     * 专家智能体接口：使用声明式API定义条件工作流
     */
    public interface ExpertsAgent {

        @ConditionalAgent(outputKey = "response",
                subAgents = {MedicalExpert.class, TechnicalExpert.class, LegalExpert.class})
        String askExpert(@V("request") String request, @V("category") RequestCategory category);

        @ActivationCondition(MedicalExpert.class)
        static boolean activateMedical(@V("category") RequestCategory category) {
            return category == RequestCategory.MEDICAL;
        }

        @ActivationCondition(TechnicalExpert.class)
        static boolean activateTechnical(@V("category") RequestCategory category) {
            return category == RequestCategory.TECHNICAL;
        }

        @ActivationCondition(LegalExpert.class)
        static boolean activateLegal(@V("category") RequestCategory category) {
            return category == RequestCategory.LEGAL;
        }

    }

    /**
     * 创建基础对话模型
     *
     * @return ChatModel 实例
     */
    private static ChatModel baseModel() {
        return OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    public static void main(String[] args) {
        ChatModel baseModel = baseModel();

        try {
            // 使用声明式API创建智能体系统
            ExpertsAgent expertsAgent = AgenticServices
                    .createAgenticSystem(ExpertsAgent.class, baseModel);

            // 执行工作流 - 医疗请求
            System.out.println("=== 医疗专家咨询（声明式API） ===");
            String medicalResponse = expertsAgent.askExpert("我摔断了腿，应该怎么办？", RequestCategory.MEDICAL);
            System.out.println("回答：" + medicalResponse);
            System.out.println();

            // 执行工作流 - 技术请求
            System.out.println("=== 技术专家咨询（声明式API） ===");
            String technicalResponse = expertsAgent.askExpert("如何优化Java应用的性能？", RequestCategory.TECHNICAL);
            System.out.println("回答：" + technicalResponse);
            System.out.println();

            // 执行工作流 - 法律请求
            System.out.println("=== 法律专家咨询（声明式API） ===");
            String legalResponse = expertsAgent.askExpert("合同违约需要承担什么责任？", RequestCategory.LEGAL);
            System.out.println("回答：" + legalResponse);
            System.out.println();

            System.out.println("=== 工作流执行完成 ===");
        } finally {
            // 确保程序正常退出，停止所有后台线程（如 HTTP 客户端线程）
            // 注意：在生产环境中，应该使用更优雅的方式管理资源生命周期
            System.exit(0);
        }
    }

}
