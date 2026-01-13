package io.github.susamlu.langchain4j.aiservice.basic.resource;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 演示如何从资源文件加载提示词模板
 * - @SystemMessage 和 @UserMessage 都支持通过 fromResource 参数从资源文件加载模板
 */
public class ResourceTemplateExample {

    interface Assistant {

        /**
         * 从资源文件加载 SystemMessage 和 UserMessage 模板
         * - @SystemMessage(fromResource = "my-system-prompt-template.txt")：从资源文件加载系统提示词
         * - @UserMessage(fromResource = "my-user-prompt-template.txt")：从资源文件加载用户消息模板
         * 模板中可以使用 {{it}} 或 {{参数名}} 来引用方法参数
         */
        @SystemMessage(fromResource = "my-system-prompt-template.txt")
        @UserMessage(fromResource = "my-user-prompt-template.txt")
        String chat(String userMessage);

    }

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .build();

        Assistant assistant = AiServices.create(Assistant.class, model);

        String answer = assistant.chat("你好，请介绍一下你自己");
        System.out.println("问题: 你好，请介绍一下你自己");
        System.out.println("回答: " + answer);
        System.out.println();

        String answer2 = assistant.chat("今天天气怎么样？");
        System.out.println("问题: 今天天气怎么样？");
        System.out.println("回答: " + answer2);
    }

}

