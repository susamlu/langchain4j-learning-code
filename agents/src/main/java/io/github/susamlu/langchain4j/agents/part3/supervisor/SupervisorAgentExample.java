package io.github.susamlu.langchain4j.agents.part3.supervisor;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 监督者智能体示例：演示如何使用 SupervisorAgent 实现纯智能体 AI
 * <p>
 * 本示例演示了：
 * 1. 定义多个子智能体（取款、存款、货币兑换）
 * 2. 创建监督者智能体
 * 3. 监督者自主生成执行计划、决定后续调用的智能体
 */
public class SupervisorAgentExample {

    public interface WithdrawAgent {

        @SystemMessage("""
                你是一位银行家,只能从用户账户中提取美元(USD)。
                """)
        @UserMessage("""
                从 {{user}} 的账户中提取 {{amount}} 美元，并返回新余额。
                """)
        @Agent("从账户中提取 USD 的银行家")
        String withdraw(@V("user") String user, @V("amount") Double amount);

    }

    public interface CreditAgent {

        @SystemMessage("""
                你是一位银行家,只能向用户账户存入美元(USD)。
                """)
        @UserMessage("""
                向 {{user}} 的账户存入 {{amount}} 美元，并返回新余额。
                """)
        @Agent("向账户存入 USD 的银行家")
        String credit(@V("user") String user, @V("amount") Double amount);

    }

    public interface ExchangeAgent {

        @UserMessage("""
                你是一位操作员，在不同货币之间兑换资金。
                使用工具将 {{amount}} {{originalCurrency}} 兑换为 {{targetCurrency}}
                只返回工具提供的最终金额，不要返回其他内容。
                """)
        @Agent("将给定金额的资金从原始货币转换为目标货币的货币兑换商")
        Double exchange(@V("originalCurrency") String originalCurrency, @V("amount") Double amount, @V("targetCurrency") String targetCurrency);

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

    /**
     * 创建规划器对话模型
     *
     * @return ChatModel 实例
     */
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

        // 创建子智能体（需要配置工具）
        WithdrawAgent withdrawAgent = AgenticServices
                .agentBuilder(WithdrawAgent.class)
                .chatModel(baseModel)
                // .tools(bankTool)  // 需要配置银行工具
                .build();

        CreditAgent creditAgent = AgenticServices
                .agentBuilder(CreditAgent.class)
                .chatModel(baseModel)
                // .tools(bankTool)  // 需要配置银行工具
                .build();

        ExchangeAgent exchangeAgent = AgenticServices
                .agentBuilder(ExchangeAgent.class)
                .chatModel(baseModel)
                // .tools(new ExchangeTool())  // 需要配置兑换工具
                .build();

        // 创建监督者智能体
        SupervisorAgent bankSupervisor = AgenticServices
                .supervisorBuilder()
                .chatModel(plannerModel)
                .subAgents(withdrawAgent, creditAgent, exchangeAgent)
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .build();

        // 使用监督者智能体处理请求
        String result = bankSupervisor.invoke("从 Mario 的账户向 Georgios 的账户转账 100 欧元");
        System.out.println("结果：" + result);
    }

}
