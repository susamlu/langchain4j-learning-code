package io.github.susamlu.langchain4j.agents.workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 条件工作流示例：演示如何使用 conditionalBuilder 创建条件执行的工作流
 * <p>
 * 工作流程：
 * 1. CategoryRouter - 分析用户请求并分类（legal、medical、technical 或 unknown）
 * 2. ExpertsAgent - 根据分类结果条件性地选择执行相应的专家智能体
 * - MedicalExpert - 医疗专家
 * - LegalExpert - 法律专家
 * - TechnicalExpert - 技术专家
 */
public class ConditionalWorkflowExample {

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
     * 分类路由接口：分析用户请求并分类
     */
    public interface CategoryRouter {

        @UserMessage("""
                分析以下用户请求，并将其分类为 'legal'（法律）、'medical'（医疗）或 'technical'（技术）。
                如果请求不属于以上任何类别，则分类为 'unknown'（未知）。
                只返回其中一个词，不要返回其他内容。
                用户请求是：'{{request}}'。
                """)
        @Agent("对用户请求进行分类")
        RequestCategory classify(@V("request") String request);

    }

    /**
     * 医疗专家接口：提供医疗建议
     */
    public interface MedicalExpert {

        @UserMessage("""
                你是一位医疗专家。
                请从医疗角度分析以下用户请求，并提供最佳答案。
                用户请求是：{{request}}。
                """)
        @Agent("医疗专家")
        String medical(@V("request") String request);

    }

    /**
     * 法律专家接口：提供法律建议
     */
    public interface LegalExpert {

        @UserMessage("""
                你是一位法律专家。
                请从法律角度分析以下用户请求，并提供最佳答案。
                用户请求是：{{request}}。
                """)
        @Agent("法律专家")
        String legal(@V("request") String request);

    }

    /**
     * 技术专家接口：提供技术建议
     */
    public interface TechnicalExpert {

        @UserMessage("""
                你是一位技术专家。
                请从技术角度分析以下用户请求，并提供最佳答案。
                用户请求是：{{request}}。
                """)
        @Agent("技术专家")
        String technical(@V("request") String request);

    }

    /**
     * 专家路由智能体接口：根据请求分类路由到相应的专家
     */
    public interface ExpertRouterAgent {

        @Agent
        String ask(@V("request") String request);

    }

    public static void main(String[] args) {
        // 创建对话模型
        ChatModel baseModel = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .logRequests(true)
                .logResponses(true)
                .build();

        // 创建分类路由智能体
        CategoryRouter routerAgent = AgenticServices
                .agentBuilder(CategoryRouter.class)
                .chatModel(baseModel)
                .outputKey("category")
                .build();

        // 创建医疗专家智能体
        MedicalExpert medicalExpert = AgenticServices
                .agentBuilder(MedicalExpert.class)
                .chatModel(baseModel)
                .outputKey("response")
                .build();

        // 创建法律专家智能体
        LegalExpert legalExpert = AgenticServices
                .agentBuilder(LegalExpert.class)
                .chatModel(baseModel)
                .outputKey("response")
                .build();

        // 创建技术专家智能体
        TechnicalExpert technicalExpert = AgenticServices
                .agentBuilder(TechnicalExpert.class)
                .chatModel(baseModel)
                .outputKey("response")
                .build();

        // 创建条件工作流：根据分类结果选择执行相应的专家
        UntypedAgent expertsAgent = AgenticServices.conditionalBuilder()
                .subAgents(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.MEDICAL, medicalExpert)
                .subAgents(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.LEGAL, legalExpert)
                .subAgents(agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.TECHNICAL, technicalExpert)
                .build();

        // 创建专家路由智能体：先分类，然后根据分类结果选择专家
        ExpertRouterAgent expertRouterAgent = AgenticServices
                .sequenceBuilder(ExpertRouterAgent.class)
                .subAgents(routerAgent, expertsAgent)
                .outputKey("response")
                .build();

        // 执行工作流
        String response = expertRouterAgent.ask("我摔断了腿，应该怎么办？");

        // 输出结果
        System.out.println("=== 专家回答 ===");
        System.out.println(response);
        System.out.println();
        System.out.println("=== 工作流执行完成 ===");
    }

}
