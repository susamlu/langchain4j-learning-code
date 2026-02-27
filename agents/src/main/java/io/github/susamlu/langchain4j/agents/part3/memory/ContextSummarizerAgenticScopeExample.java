package io.github.susamlu.langchain4j.agents.part3.memory;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.github.susamlu.langchain4j.agents.part1.workflow.ConditionalWorkflowExample.RequestCategory;

/**
 * AgenticScope 上下文摘要示例：演示多智能体系统中如何通过 AgenticScope 传递跨智能体的对话上下文
 * <p>
 * 本示例演示了：
 * 1. 多智能体系统中单独配置记忆的局限性：法律专家首次被调用时无法获取医疗专家的对话历史
 * 2. 使用 ContextSummarizer 将 AgenticScope 中的调用序列摘要为精简上下文
 * 3. 通过 .context() 方法为法律专家注入历史对话摘要
 * 4. 通过 .summarizedContext() 便捷方法实现相同效果
 * <p>
 * 调用序列：
 * - 第一次：用户问「我摔断了腿，应该怎么办？」→ 路由到医疗专家
 * - 第二次：用户问「我应该起诉造成这种伤害的邻居吗？」→ 路由到法律专家
 * 法律专家通过 summarizedContext 获取医疗专家的对话摘要，从而给出更精准的法律建议
 */
public class ContextSummarizerAgenticScopeExample {

    /**
     * 上下文摘要智能体：将 AgenticScope 中的对话序列摘要为最多 2 句话
     */
    public interface ContextSummarizer {

        @UserMessage("""
                创建一个非常简短的摘要，最多2句话，关于以下 AI 智能体和用户之间的对话。
                用户对话是：'{{it}}'。
                """)
        String summarize(String conversation);

    }

    /**
     * 带记忆的医疗专家接口
     */
    public interface MedicalExpertWithMemory {

        @UserMessage("""
                你是一位医疗专家。
                请从医疗角度分析以下用户请求，并提供最佳答案。
                用户请求是：'{{request}}'。
                """)
        @Agent("医疗专家")
        String medical(@MemoryId String memoryId, @V("request") String request);

    }

    /**
     * 带记忆的法律专家接口：可获取其他智能体的历史对话摘要作为上下文
     */
    public interface LegalExpertWithMemory {

        @UserMessage("""
                你是一位法律专家。
                请从法律角度分析以下用户请求，并提供最佳答案。
                用户请求是：'{{request}}'。
                """)
        @Agent("法律专家")
        String legal(@MemoryId String memoryId, @V("request") String request);

    }

    /**
     * 带记忆的技术专家接口
     */
    public interface TechnicalExpertWithMemory {

        @UserMessage("""
                你是一位技术专家。
                请从技术角度分析以下用户请求，并提供最佳答案。
                用户请求是：'{{request}}'。
                """)
        @Agent("技术专家")
        String technical(@MemoryId String memoryId, @V("request") String request);

    }

    /**
     * 带记忆的专家路由智能体：支持跨调用的记忆与上下文传递
     */
    public interface ExpertRouterAgentWithMemory {

        @Agent
        String ask(@MemoryId String memoryId, @V("request") String request);

    }

    /**
     * 分类路由接口：分析用户请求并分类
     */
    public interface CategoryRouterWithMemory {

        @UserMessage("""
                分析以下用户请求，并将其分类为 'legal'（法律）、'medical'（医疗）或 'technical'（技术）。
                如果请求不属于以上任何类别，则分类为 'unknown'（未知）。
                只返回其中一个词，不要返回其他内容。
                用户请求是：'{{request}}'。
                """)
        @Agent("对用户请求进行分类")
        RequestCategory classify(@V("request") String request);

    }

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

        // 1. 创建 ContextSummarizer（普通 AI 服务，非 Agent，用于 .context() 方式）
        ContextSummarizer contextSummarizer = AiServices.builder(ContextSummarizer.class)
                .chatModel(baseModel)
                .build();

        // 2. 创建分类路由智能体
        CategoryRouterWithMemory routerAgent = AgenticServices
                .agentBuilder(CategoryRouterWithMemory.class)
                .chatModel(baseModel)
                .outputKey("category")
                .build();

        // 3. 创建医疗专家（带记忆）
        MedicalExpertWithMemory medicalExpert = AgenticServices
                .agentBuilder(MedicalExpertWithMemory.class)
                .chatModel(baseModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .outputKey("response")
                .build();

        // 4. 创建法律专家（带记忆 + 上下文摘要）
        // 方式一：使用 .context() 显式传入 AgenticScope 的对话摘要
        LegalExpertWithMemory legalExpert = AgenticServices
                .agentBuilder(LegalExpertWithMemory.class)
                .chatModel(baseModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .context(agenticScope -> contextSummarizer.summarize(agenticScope.contextAsConversation()))
                .outputKey("response")
                .build();

        // 方式二（备选）：使用 .summarizedContext() 便捷方法，仅摘要指定智能体的上下文
        // LegalExpertWithMemory legalExpert = AgenticServices
        //         .agentBuilder(LegalExpertWithMemory.class)
        //         .chatModel(baseModel)
        //         .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
        //         .summarizedContext("medical", "technical")
        //         .outputKey("response")
        //         .build();

        // 5. 创建技术专家（带记忆）
        TechnicalExpertWithMemory technicalExpert = AgenticServices
                .agentBuilder(TechnicalExpertWithMemory.class)
                .chatModel(baseModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .outputKey("response")
                .build();

        // 6. 创建条件工作流：根据分类结果选择执行相应的专家
        UntypedAgent expertsAgent = AgenticServices.conditionalBuilder()
                .subAgents(scope -> scope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.MEDICAL, medicalExpert)
                .subAgents(scope -> scope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.LEGAL, legalExpert)
                .subAgents(scope -> scope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.TECHNICAL, technicalExpert)
                .build();

        // 7. 创建专家路由智能体：先分类，然后根据分类结果选择专家
        ExpertRouterAgentWithMemory expertRouterAgent = AgenticServices
                .sequenceBuilder(ExpertRouterAgentWithMemory.class)
                .subAgents(routerAgent, expertsAgent)
                .outputKey("response")
                .build();

        // 8. 执行两次调用序列（同一 memoryId "1"）
        System.out.println("=== 第一次调用：医疗问题 ===");
        String response1 = expertRouterAgent.ask("1", "我摔断了腿，应该怎么办？");
        System.out.println("医疗专家回答：" + response1);
        System.out.println();

        System.out.println("=== 第二次调用：法律问题（法律专家将获取医疗专家的对话摘要作为上下文）===");
        String legalResponse1 = expertRouterAgent.ask("1", "我应该起诉造成这种伤害的邻居吗？");
        System.out.println("法律专家回答：" + legalResponse1);
        System.out.println();
        System.out.println("=== 工作流执行完成 ===");
    }

}
