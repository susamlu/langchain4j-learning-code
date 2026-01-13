package io.github.susamlu.langchain4j.aiservice.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

/**
 * 工具使用示例
 * 演示如何使用 @Tool 注解定义工具，并在 AI Service 中使用这些工具
 */
public class ToolUsageExample {

    // 定义工具类
    static class CalculatorTools {

        @Tool("计算两个整数的加法")
        public int add(@P("第一个加数") int a, @P("第二个加数") int b) {
            System.out.println("执行加法: " + a + " + " + b);
            return a + b;
        }

        @Tool("计算两个整数的乘法")
        public int multiply(@P("第一个乘数") int a, @P("第二个乘数") int b) {
            System.out.println("执行乘法: " + a + " * " + b);
            return a * b;
        }

    }

    interface Assistant {

        String chat(String userMessage);

    }

    public static void main(String[] args) {
        // 创建对话模型
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .build();

        // 创建工具实例
        CalculatorTools tools = new CalculatorTools();

        // 使用 AiServices 构建器创建助手，并配置工具
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(tools)  // 配置工具
                .build();

        // 测试工具调用
        System.out.println("用户提问: What is 1+2 and 3*4?");
        String answer = assistant.chat("What is 1+2 and 3*4?");
        System.out.println("AI 回答: " + answer);
        System.out.println();

        // 更多测试
        System.out.println("用户提问: Calculate 5 plus 7 and 3 times 8");
        String answer2 = assistant.chat("Calculate 5 plus 7 and 3 times 8");
        System.out.println("AI 回答: " + answer2);
        System.out.println();

        System.out.println("用户提问: What's the result of 10 + 20 and 6 * 7?");
        String answer3 = assistant.chat("What's the result of 10 + 20 and 6 * 7?");
        System.out.println("AI 回答: " + answer3);
    }

}