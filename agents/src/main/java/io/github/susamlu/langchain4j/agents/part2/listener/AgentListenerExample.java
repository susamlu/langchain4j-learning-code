package io.github.susamlu.langchain4j.agents.part2.listener;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.Map;

/**
 * AgentListener 示例：演示如何使用 AgentListener 监听智能体的调用过程
 * <p>
 * AgentListener 提供了在智能体调用前后进行监听的能力，可以用于：
 * - 日志记录
 * - 性能监控
 * - 调试追踪
 * - 自定义行为
 * <p>
 * 注意：AgentListener 只在通过工作流（如 sequenceBuilder、parallelBuilder 等）调用智能体时才会被触发。
 * 直接调用单个智能体时，AgentListener 不会被触发。
 */
public class AgentListenerExample {

    /**
     * 创意作家接口：根据给定主题生成故事草稿
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
        // 创建智能体
        CreativeWriter creativeWriter = AgenticServices.agentBuilder(CreativeWriter.class)
                .chatModel(baseModel())
                .outputKey("story")
                .listener(new AgentListener() {
                    @Override
                    public void beforeAgentInvocation(AgentRequest request) {
                        System.out.println("=== 智能体调用前 ===");
                        System.out.println("调用 CreativeWriter，主题：" + request.inputs().get("topic"));
                    }

                    @Override
                    public void afterAgentInvocation(AgentResponse response) {
                        System.out.println("=== 智能体调用后 ===");
                        System.out.println("CreativeWriter 生成的故事：" + response.output());
                    }
                })
                .build();

        // 通过工作流调用智能体（AgentListener 只在工作流中才会被触发）
        UntypedAgent workflowAgent = AgenticServices.sequenceBuilder()
                .subAgents(creativeWriter)
                .outputKey("story")
                .build();

        // 执行工作流
        Map<String, Object> input = Map.of("topic", "太空探索");
        Object result = workflowAgent.invoke(input);

        System.out.println("\n=== 最终结果 ===");
        System.out.println("生成的故事：" + result);
    }

}
