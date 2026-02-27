package io.github.susamlu.langchain4j.agents.part3.memory;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgenticScopeAccess;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.memory.ChatMemoryAccess;

/**
 * AgenticScope 与 ChatMemory 驱逐示例：演示如何显式移除不再需要的记忆
 * <p>
 * 重要区分：
 * - AgenticScope：工作流共享状态，由根智能体管理，通过 evictAgenticScope(memoryId) 驱逐
 * - ChatMemory：对话历史，由配置了 chatMemoryProvider 的子智能体管理，通过 evictChatMemory(memoryId) 驱逐
 * <p>
 * 当子智能体配置了 chatMemoryProvider 时，对话历史存储在子智能体的 ChatMemory 中，
 * 必须对持有 ChatMemory 的子智能体调用 evictChatMemory 才能清除对话上下文。
 */
public class AgenticScopeEvictionExample {

    /**
     * 带记忆的助手接口：agentBuilder 生成的实现会实现 ChatMemoryAccess，可调用 evictChatMemory
     */
    public interface AssistantWithMemory extends ChatMemoryAccess {

        @UserMessage("""
                你是一位友好的助手。
                请回答用户的问题：'{{request}}'。
                """)
        @Agent("助手")
        String ask(@MemoryId String memoryId, @V("request") String request);

    }

    /**
     * 根智能体接口：需实现 AgenticScopeAccess 才能调用 evictAgenticScope
     */
    public interface ChatRootAgent extends AgenticScopeAccess {

        String ask(@MemoryId String memoryId, @V("request") String request);

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

        AssistantWithMemory assistant = AgenticServices
                .agentBuilder(AssistantWithMemory.class)
                .chatModel(baseModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .outputKey("response")
                .build();

        // 使用 sequenceBuilder 创建根智能体，框架生成的实现类会实现 AgenticScopeAccess
        ChatRootAgent rootAgent = AgenticServices
                .sequenceBuilder(ChatRootAgent.class)
                .subAgents(assistant)
                .outputKey("response")
                .build();

        // 1. 执行对话，AgenticScope 会被创建并注册
        String memoryId = "user-session-001";
        System.out.println("=== 第一次对话（创建 AgenticScope）===");
        String response1 = rootAgent.ask(memoryId, "你好，请介绍一下自己");
        System.out.println("助手回答：" + response1);
        System.out.println();

        // 2. 再次对话，会复用已注册的 AgenticScope，保持上下文
        System.out.println("=== 第二次对话（复用 AgenticScope）===");
        String response2 = rootAgent.ask(memoryId, "我刚才问了什么？");
        System.out.println("助手回答：" + response2);
        System.out.println();

        // 3. 驱逐记忆：对话历史在子智能体（assistant）的 ChatMemory 中，需调用 evictChatMemory
        //    根智能体的 evictAgenticScope 仅清除工作流状态，不会清除子智能体的对话历史
        boolean chatMemoryEvicted = assistant.evictChatMemory(memoryId);
        boolean agenticScopeEvicted = rootAgent.evictAgenticScope(memoryId);
        System.out.println("=== 驱逐记忆 ===");
        System.out.println("驱逐 ChatMemory (对话历史): " + (chatMemoryEvicted ? "成功" : "失败"));
        System.out.println("驱逐 AgenticScope (工作流状态): " + (agenticScopeEvicted ? "成功" : "失败"));
        System.out.println();

        // 4. 驱逐后，再次使用同一 memoryId 会创建新的 ChatMemory，之前的对话历史已丢失
        System.out.println("=== 驱逐后的新对话（新的 AgenticScope，无历史上下文）===");
        String response3 = rootAgent.ask(memoryId, "你还记得我们之前的对话吗？");
        System.out.println("助手回答：" + response3);
    }

}
