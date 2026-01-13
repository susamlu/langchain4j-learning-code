package io.github.susamlu.langchain4j.aiservice.basic.invocationparameters;

import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * InvocationParameters 使用示例
 * <p>
 * InvocationParameters 用于在方法调用时动态设置对话请求的参数，
 * 如 temperature（温度）和 maxOutputTokens（最大输出 token 数）
 * <p>
 * 使用方式：
 * 1. 在接口方法中将 InvocationParameters 作为参数
 * 2. 使用 Map<String, Object> 创建参数映射
 * 3. 使用 new InvocationParameters(Map) 创建 InvocationParameters
 * 4. 在调用方法时传入 InvocationParameters
 */
public class InvocationParametersExample {

    interface AssistantWithInvocationParams {

        /**
         * 方法签名：第一个参数使用 @UserMessage 注解标识为用户消息，
         * 第二个参数为 InvocationParameters，用于动态设置请求参数
         */
        String chat(@UserMessage String userMessage, InvocationParameters params);

    }

    public static void main(String[] args) {
        // ====================== 1. 创建 ChatModel 实例 ======================
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .build();

        // ====================== 2. 使用 AiServices 创建 Assistant 实例 ======================
        AssistantWithInvocationParams assistant = AiServices.builder(AssistantWithInvocationParams.class)
                .chatModel(model)
                .build();

        // ====================== 3. 创建自定义的 InvocationParameters ======================
        // 注意：InvocationParameters 构造函数需要 Map<String, Object>
        // 示例1：使用较高的温度值（更随机、更有创造性）
        Map<String, Object> creativeParamsMap = new HashMap<>();
        creativeParamsMap.put("temperature", 0.85);
        creativeParamsMap.put("maxOutputTokens", 500);
        InvocationParameters creativeParams = new InvocationParameters(creativeParamsMap);

        // 示例2：使用较低的温度值（更确定、更准确）
        Map<String, Object> preciseParamsMap = new HashMap<>();
        preciseParamsMap.put("temperature", 0.3);
        preciseParamsMap.put("maxOutputTokens", 200);
        InvocationParameters preciseParams = new InvocationParameters(preciseParamsMap);

        // 示例3：使用中等/平衡参数（介于创造性和精确性之间）
        Map<String, Object> balancedParamsMap = new HashMap<>();
        balancedParamsMap.put("temperature", 0.7);
        balancedParamsMap.put("maxOutputTokens", 500);
        InvocationParameters balancedParams = new InvocationParameters(balancedParamsMap);

        // ====================== 4. 使用不同的参数调用方法 ======================

        // 测试1：使用创造性参数（高温度）
        System.out.println("========== 测试1：创造性参数 (temperature=0.85, maxOutputTokens=500) ==========");
        String answer1 = assistant.chat("写一首关于春天的短诗", creativeParams);
        System.out.println("问题: 写一首关于春天的短诗");
        System.out.println("回答: " + answer1);
        System.out.println();

        // 测试2：使用精确参数（低温度）
        System.out.println("========== 测试2：精确参数 (temperature=0.3, maxOutputTokens=200) ==========");
        String answer2 = assistant.chat("什么是机器学习？", preciseParams);
        System.out.println("问题: 什么是机器学习？");
        System.out.println("回答: " + answer2);
        System.out.println();

        // 测试3：使用中等/平衡参数
        System.out.println("========== 测试3：中等/平衡参数 (temperature=0.7, maxOutputTokens=500) ==========");
        String answer3 = assistant.chat("请简单介绍一下人工智能", balancedParams);
        System.out.println("问题: 请简单介绍一下人工智能");
        System.out.println("回答: " + answer3);
        System.out.println();

        // ====================== 5. 动态创建参数示例 ======================
        System.out.println("========== 测试4：动态创建参数 ==========");
        // 根据不同的场景动态设置参数
        Map<String, Object> dynamicParamsMap = new HashMap<>();
        dynamicParamsMap.put("temperature", 0.6);
        dynamicParamsMap.put("maxOutputTokens", 300);
        InvocationParameters dynamicParams = new InvocationParameters(dynamicParamsMap);

        String answer4 = assistant.chat("你好", dynamicParams);
        System.out.println("问题: 你好");
        System.out.println("回答: " + answer4);
        System.out.println();
    }

}

