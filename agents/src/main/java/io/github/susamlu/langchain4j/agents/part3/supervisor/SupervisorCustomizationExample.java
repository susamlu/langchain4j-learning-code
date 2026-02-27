package io.github.susamlu.langchain4j.agents.part3.supervisor;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorContextStrategy;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 监督者设计与定制示例：演示 SupervisorResponseStrategy、SupervisorContextStrategy 与 supervisorContext
 * <p>
 * 本示例演示了：
 * 1. SupervisorResponseStrategy：LAST（默认）、SUMMARY、SCORED 三种响应策略
 * 2. SupervisorContextStrategy：CHAT_MEMORY、SUMMARIZATION、CHAT_MEMORY_AND_SUMMARIZATION
 * 3. supervisorContext：构建时配置、调用时配置（类型化/非类型化），调用时覆盖构建时
 */
public class SupervisorCustomizationExample {

    public interface WithdrawAgent {

        @SystemMessage("你是一位银行家，只能从用户账户中提取美元(USD)。")
        @UserMessage("从 {{user}} 的账户中提取 {{amount}} 美元，并返回新余额。")
        @Agent("从账户中提取 USD 的银行家")
        String withdraw(@V("user") String user, @V("amount") Double amount);

    }

    public interface CreditAgent {

        @SystemMessage("你是一位银行家，只能向用户账户存入美元(USD)。")
        @UserMessage("向 {{user}} 的账户存入 {{amount}} 美元，并返回新余额。")
        @Agent("向账户存入 USD 的银行家")
        String credit(@V("user") String user, @V("amount") Double amount);

    }

    public interface ExchangeAgent {

        @UserMessage("""
                你是一位货币兑换操作员。将 {{amount}} {{originalCurrency}} 兑换为 {{targetCurrency}}。
                模拟兑换并返回兑换后的金额（可假设汇率为 1 EUR = 1.1 USD）。
                只返回最终金额数字，不要返回其他内容。
                """)
        @Agent("货币兑换商")
        Double exchange(@V("originalCurrency") String originalCurrency, @V("amount") Double amount, @V("targetCurrency") String targetCurrency);

    }

    /**
     * 类型化监督者接口：支持 @V("supervisorContext") 在调用时传入上下文
     */
    public interface TypedBankSupervisor {

        @Agent
        String invoke(@V("request") String request, @V("supervisorContext") String supervisorContext);

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

    private static ChatModel plannerModel() {
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
        ChatModel plannerModel = plannerModel();

        WithdrawAgent withdrawAgent = AgenticServices.agentBuilder(WithdrawAgent.class)
                .chatModel(baseModel)
                .build();

        CreditAgent creditAgent = AgenticServices.agentBuilder(CreditAgent.class)
                .chatModel(baseModel)
                .build();

        ExchangeAgent exchangeAgent = AgenticServices.agentBuilder(ExchangeAgent.class)
                .chatModel(baseModel)
                .build();

        // ========== 1. SupervisorResponseStrategy ==========
        // LAST：返回最后一个智能体的输出（默认）
        // SUMMARY：返回监督者与子智能体交互的摘要
        // SCORED：使用评分智能体比较「最后输出」与「摘要」，返回得分更高者

        System.out.println("=== 1. 响应策略：SCORED（评分智能体决策最优返回）===");
        SupervisorAgent scoredSupervisor = AgenticServices.supervisorBuilder()
                .chatModel(plannerModel)
                .subAgents(withdrawAgent, creditAgent, exchangeAgent)
                .responseStrategy(SupervisorResponseStrategy.SCORED)
                .build();
        String scoredResult = scoredSupervisor.invoke("从 Mario 的账户向 Georgios 的账户转账 100 欧元");
        System.out.println("SCORED 策略结果：" + scoredResult);
        System.out.println();

        System.out.println("=== 2. 响应策略：SUMMARY（返回监督者生成的摘要）===");
        SupervisorAgent summarySupervisor = AgenticServices.supervisorBuilder()
                .chatModel(plannerModel)
                .subAgents(withdrawAgent, creditAgent, exchangeAgent)
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .build();
        String summaryResult = summarySupervisor.invoke("从 Mario 的账户向 Georgios 的账户转账 100 欧元");
        System.out.println("SUMMARY 策略结果：" + summaryResult);
        System.out.println();

        // ========== 2. SupervisorContextStrategy ==========
        // CHAT_MEMORY：仅使用监督者本地聊天记忆（默认）
        // SUMMARIZATION：仅使用子智能体对话摘要
        // CHAT_MEMORY_AND_SUMMARIZATION：结合两种方式

        System.out.println("=== 3. 上下文策略：SUMMARIZATION（仅使用子智能体对话摘要）===");
        SupervisorAgent summarizationContextSupervisor = AgenticServices.supervisorBuilder()
                .chatModel(plannerModel)
                .subAgents(withdrawAgent, creditAgent, exchangeAgent)
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .contextGenerationStrategy(SupervisorContextStrategy.SUMMARIZATION)
                .build();
        String ctxResult = summarizationContextSupervisor.invoke("从 Mario 的账户向 Georgios 的账户转账 100 欧元");
        System.out.println("SUMMARIZATION 上下文策略结果：" + ctxResult);
        System.out.println();

        // ========== 3. supervisorContext：为监督者提供约束/策略/偏好 ==========

        // 3a. 构建时配置 supervisorContext
        System.out.println("=== 4. supervisorContext：构建时配置 ===");
        SupervisorAgent buildTimeContextSupervisor = AgenticServices.supervisorBuilder()
                .chatModel(plannerModel)
                .supervisorContext("策略：优先使用内部工具；货币必须为 USD；不使用外部 API")
                .subAgents(withdrawAgent, creditAgent, exchangeAgent)
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .build();
        String buildTimeResult = buildTimeContextSupervisor.invoke("从 Mario 的账户向 Georgios 的账户转账 100 欧元");
        System.out.println("构建时上下文结果：" + buildTimeResult);
        System.out.println();

        // 3b. 调用时配置（类型化监督者）：@V("supervisorContext") 参数
        System.out.println("=== 5. supervisorContext：调用时配置（类型化监督者）===");
        TypedBankSupervisor typedSupervisor = AgenticServices.supervisorBuilder(TypedBankSupervisor.class)
                .chatModel(plannerModel)
                .supervisorContext("默认策略：货币 USD")
                .subAgents(withdrawAgent, creditAgent, exchangeAgent)
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .build();
        // 调用时传入的 supervisorContext 将覆盖构建时配置
        String typedResult = typedSupervisor.invoke(
                "从 Mario 的账户向 Georgios 的账户转账 100 欧元",
                "策略：先转换为 USD；仅使用银行工具；不使用外部 API"
        );
        System.out.println("调用时覆盖上下文结果：" + typedResult);
        System.out.println();

        // 3c. 调用时配置（非类型化监督者）：在输入 Map 中设置 supervisorContext
        //     需使用类型化接口，定义 @V("request") 与 @V("supervisorContext") 参数，通过 invoke(Map) 传入
        //     若框架支持 UntypedAgent 风格的 invoke(Map)，可如下调用：
        //     Map<String, Object> input = Map.of(
        //         "request", "从 Mario 的账户向 Georgios 的账户转账 100 欧元",
        //         "supervisorContext", "策略：先转换为 USD；仅使用银行工具；不使用外部 API"
        //     );
        //     String result = (String) untypedSupervisor.invoke(input);
        System.out.println("=== 6. 非类型化 supervisorContext 说明 ===");
        System.out.println("非类型化监督者使用 invoke(String) 时无法传入 supervisorContext。");
        System.out.println("建议使用类型化接口 TypedBankSupervisor.invoke(request, supervisorContext) 实现调用时覆盖。");
    }

}
