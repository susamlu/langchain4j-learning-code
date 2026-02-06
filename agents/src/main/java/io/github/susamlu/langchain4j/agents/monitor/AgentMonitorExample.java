package io.github.susamlu.langchain4j.agents.monitor;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.observability.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.Map;

/**
 * AgentMonitor 示例：演示如何使用 AgentMonitor 监控智能体的执行过程
 * <p>
 * AgentMonitor 是一个特殊的 AgentListener，它可以记录所有智能体的执行信息，
 * 包括输入、输出、执行时间等，方便后续分析和调试。
 */
public class AgentMonitorExample {

    /**
     * 创意作家接口：根据主题生成故事
     */
    public interface CreativeWriter {

        @UserMessage("""
                你是一位创意作家。
                请围绕给定主题生成一个不超过3句话的故事草稿。
                只返回故事内容，不要返回其他内容。
                主题：{{topic}}
                """)
        @Agent("根据给定主题生成故事草稿")
        String generateStory(@V("topic") String topic);

    }

    /**
     * 风格编辑接口：将故事编辑为指定风格
     */
    public interface StyleEditor {

        @UserMessage("""
                你是一位专业编辑。
                分析并重写以下故事，使其更好地符合并更连贯地体现 {{style}} 风格。
                只返回故事内容，不要返回其他内容。
                故事内容："{{story}}"
                """)
        @Agent("编辑故事以更好地适应给定的风格")
        String editStory(@V("story") String story, @V("style") String style);

    }

    /**
     * 风格评分接口：评估故事的风格得分
     */
    public interface StyleScorer {

        @UserMessage("""
                你是一位严格的评论家。
                请根据以下故事与风格 '{{style}}' 的契合程度，给出一个 0.0 到 1.0 之间的评分。
                只返回评分数字，不要返回其他内容。
                故事内容："{{story}}"
                """)
        @Agent("根据故事与给定风格的契合程度进行评分")
        Double scoreStyle(@V("story") String story, @V("style") String style);

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

        // 创建 AgentMonitor 实例
        AgentMonitor monitor = new AgentMonitor();

        // 创建创意作家智能体，注册监听器
        CreativeWriter creativeWriter = AgenticServices.agentBuilder(CreativeWriter.class)
                .chatModel(baseModel)
                .outputKey("story")
                .listener(new AgentListener() {
                    @Override
                    public void beforeAgentInvocation(AgentRequest request) {
                        System.out.println("调用 CreativeWriter，主题：" + request.inputs().get("topic"));
                    }
                })
                .build();

        // 创建风格编辑智能体
        StyleEditor styleEditor = AgenticServices.agentBuilder(StyleEditor.class)
                .chatModel(baseModel)
                .outputKey("story")
                .build();

        // 创建风格评分智能体
        StyleScorer styleScorer = AgenticServices.agentBuilder(StyleScorer.class)
                .name("styleScorer")
                .chatModel(baseModel)
                .outputKey("score")
                .build();

        // 创建循环工作流：迭代优化故事风格
        UntypedAgent styleReviewLoop = AgenticServices.loopBuilder()
                .subAgents(styleScorer, styleEditor)
                .maxIterations(5)
                .exitCondition(agenticScope -> agenticScope.readState("score", 0.0) >= 0.8)
                .build();

        // 创建顺序工作流：先生成故事，再优化风格
        UntypedAgent styledWriter = AgenticServices.sequenceBuilder()
                .subAgents(creativeWriter, styleReviewLoop)
                .listener(monitor)  // 注册 AgentMonitor
                .listener(new AgentListener() {
                    @Override
                    public void afterAgentInvocation(AgentResponse response) {
                        if (response.agentName().equals("styleScorer")) {
                            System.out.println("当前得分：" + response.output());
                        }
                    }

                    @Override
                    public boolean inheritedBySubagents() {
                        return true;
                    }
                })
                .outputKey("story")
                .build();

        // 执行工作流
        Map<String, Object> input = Map.of(
                "topic", "龙与巫师",
                "style", "喜剧");
        String story = (String) styledWriter.invoke(input);

        System.out.println("\n=== 最终生成的故事 ===");
        System.out.println(story);

        // 从监控器检索记录的执行并打印
        System.out.println("\n=== 监控执行记录 ===");
        if (!monitor.successfulExecutions().isEmpty()) {
            MonitoredExecution execution = monitor.successfulExecutions().get(0);
            System.out.println(execution);
        } else {
            System.out.println("没有成功的执行记录");
        }
    }

}
