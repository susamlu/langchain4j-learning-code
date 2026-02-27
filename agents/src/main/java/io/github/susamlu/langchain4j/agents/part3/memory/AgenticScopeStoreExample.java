package io.github.susamlu.langchain4j.agents.part3.memory;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgenticScopePersister;
import dev.langchain4j.agentic.scope.AgenticScopeStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.nio.file.Path;

/**
 * AgenticScope 持久化示例：演示如何配置 AgenticScopeStore 将状态持久化到存储介质
 * <p>
 * 本示例演示了：
 * 1. 编程式配置：通过 AgenticScopePersister.setStore() 设置自定义 AgenticScopeStore
 * 2. 使用 FileBasedAgenticScopeStore 将 AgenticScope 持久化到文件系统
 * 3. 启用记忆功能后，AgenticScope 会同时写入内存注册表和持久化存储
 * <p>
 * SPI 配置方式：创建 META-INF/services/dev.langchain4j.agentic.scope.AgenticScopeStore 文件，
 * 内容为 AgenticScopeStore 实现类的全限定名，框架会自动加载。
 */
public class AgenticScopeStoreExample {

    public interface AssistantWithMemory {

        @UserMessage("""
                你是一位友好的助手。
                请回答用户的问题：'{{request}}'。
                """)
        @Agent("助手")
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
        // 方式一：编程式设置 AgenticScopeStore
        // 将 AgenticScope 持久化到 ./agentic-scope-storage 目录
        Path storagePath = Path.of(System.getProperty("java.io.tmpdir"), "agentic-scope-storage");
        AgenticScopeStore fileStore = new FileBasedAgenticScopeStore(storagePath);
        AgenticScopePersister.setStore(fileStore);

        System.out.println("AgenticScope 持久化目录: " + storagePath.toAbsolutePath());
        System.out.println();

        ChatModel baseModel = baseModel();

        AssistantWithMemory assistant = AgenticServices
                .agentBuilder(AssistantWithMemory.class)
                .chatModel(baseModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .outputKey("response")
                .build();

        AssistantWithMemory rootAgent = AgenticServices
                .sequenceBuilder(AssistantWithMemory.class)
                .subAgents(assistant)
                .outputKey("response")
                .build();

        // 执行对话，AgenticScope 会被持久化到文件系统
        String memoryId = "persistent-session-001";
        System.out.println("=== 带持久化的对话 ===");
        String response = rootAgent.ask(memoryId, "请记住：我最喜欢的颜色是蓝色");
        System.out.println("助手回答：" + response);
        System.out.println();

        // 应用重启后，可从 FileBasedAgenticScopeStore 加载之前的 AgenticScope
        // 实现会话恢复（需在应用启动时配置相同的 AgenticScopeStore）
        System.out.println("=== 持久化完成，AgenticScope 已保存到 " + storagePath + " ===");
    }

}
