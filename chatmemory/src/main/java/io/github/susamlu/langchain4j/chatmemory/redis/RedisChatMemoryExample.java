package io.github.susamlu.langchain4j.chatmemory.redis;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;

public class RedisChatMemoryExample {

    public static void main(String[] args) {
        // 1. 初始化 Redis 版 ChatMemoryStore
        RedisChatMemoryStore redisStore = new RedisChatMemoryStore("localhost", 6379);

        // 2. 构建带持久化的 ChatMemory
        TokenCountEstimator tokenCountEstimator = new OpenAiTokenCountEstimator("gpt-3.5-turbo");
        ChatMemory chatMemory = TokenWindowChatMemory.builder()
                .id("user-123-session-456")
                .maxTokens(1000, tokenCountEstimator)
                .chatMemoryStore(redisStore)
                .build();

        // 3. 模拟对话：添加用户消息和AI回复
        chatMemory.add(new UserMessage("你好，我是 Sam"));
        chatMemory.add(new AiMessage("你好 Sam！"));

        // 4. 验证：读取并打印记忆中的消息
        System.out.println("当前对话记忆：");
        chatMemory.messages().forEach(msg -> {
            // 根据消息类型提取文本内容
            String text = "";
            if (msg instanceof UserMessage) {
                text = ((UserMessage) msg).singleText();
            } else if (msg instanceof AiMessage) {
                text = ((AiMessage) msg).text();
            }
            System.out.printf("[%s] %s%n", msg.getClass().getSimpleName(), text);
        });

        // 5. 模拟服务重启/多实例：重新构建 ChatMemory，验证数据持久化
        TokenCountEstimator newTokenCountEstimator = new OpenAiTokenCountEstimator("gpt-3.5-turbo");
        ChatMemory newChatMemory = TokenWindowChatMemory.builder()
                .id("user-123-session-456")
                .maxTokens(1000, newTokenCountEstimator)
                .chatMemoryStore(redisStore)
                .build();

        System.out.println("\n重启后读取的对话记忆：");
        newChatMemory.messages().forEach(msg -> {
            // 根据消息类型提取文本内容
            String text = "";
            if (msg instanceof UserMessage) {
                text = ((UserMessage) msg).singleText();
            } else if (msg instanceof AiMessage) {
                text = ((AiMessage) msg).text();
            }
            System.out.printf("[%s] %s%n", msg.getClass().getSimpleName(), text);
        });

        // 6. 清理测试数据（可选）
        redisStore.deleteMessages("user-123-session-456");
        // 7. 关闭 Redis 连接池（应用退出时执行）
        redisStore.close();
    }

}
