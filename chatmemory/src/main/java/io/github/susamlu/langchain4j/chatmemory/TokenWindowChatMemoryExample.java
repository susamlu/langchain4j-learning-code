package io.github.susamlu.langchain4j.chatmemory;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;

public class TokenWindowChatMemoryExample {

    public static void main(String[] args) {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");

        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .build();

        // 初始化 token 计数器（以 OpenAI 为例，不同模型需对应不同 Estimator）
        // 注意：OpenAiTokenCountEstimator 需要指定模型名称，例如 "gpt-4" 或 "gpt-3.5-turbo"
        // 使用 gpt-3.5-turbo 作为计数模型（与 deepseek-chat 规则一致）
        TokenCountEstimator tokenCountEstimator = new OpenAiTokenCountEstimator("gpt-3.5-turbo");

        // 构建按 token 数限制的对话记忆（保留最近1000个 token）
        ChatMemory tokenWindowMemory = TokenWindowChatMemory.builder()
                // 唯一标识：建议拼接 userId + sessionId 区分不同用户/会话
                .id("user-123-session-456")
                // 设置最大保留 token 数，并注入 token 计数器
                .maxTokens(1000, tokenCountEstimator)
                .build();

        tokenWindowMemory.add(SystemMessage.from("你是一个简洁的助手，回答尽量用要点。"));
        UserMessage u1 = UserMessage.from("我叫 Sam");
        tokenWindowMemory.add(u1);
        AiMessage a1 = model.chat(tokenWindowMemory.messages()).aiMessage();
        System.out.println(a1.text());
        tokenWindowMemory.add(a1);

        UserMessage u2 = UserMessage.from("我叫什么名字？");
        tokenWindowMemory.add(u2);
        AiMessage a2 = model.chat(tokenWindowMemory.messages()).aiMessage();
        System.out.println(a2.text());
        tokenWindowMemory.add(a2);
    }

}

