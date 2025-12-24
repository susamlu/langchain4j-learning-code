package io.github.susamlu.langchain4j.chatmodel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * LangChain4j 工具调用样例
 * 场景：调用自定义工具查询订单物流状态
 */
public class ToolCallExample {

    // JSON 解析工具
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        // ====================== 1. 配置模型（通义千问） ======================
        // 通义千问 qwen-plus（需替换为自己的API Key）
        ChatModel model = initQwenModel();

        // ====================== 2. 定义工具（物流查询工具） ======================
        ToolSpecification logisticsTool = ToolSpecification.builder()
                .name("query_logistics") // 工具名称（需唯一）
                .description("查询订单的物流状态，入参为订单号（order_id）") // 工具描述（让模型理解用途）
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("order_id", "订单编号，格式如 20251216001")
                        .build()) // 工具入参定义
                .build();

        // ====================== 3. 构建首次请求（声明工具+用户问题） ======================
        ChatRequest firstRequest = ChatRequest.builder()
                .messages(List.of(
                        SystemMessage.from("你是电商客服助手，仅能通过调用query_logistics工具获取物流信息，禁止虚构数据；"
                                + "工具返回结果后，用自然语言整理并回复用户。"),
                        UserMessage.from("帮我查一下订单 20251216001 的物流状态")
                ))
                .toolSpecifications(List.of(logisticsTool)) // 声明可用工具
                .build();

        // ====================== 4. 首次调用模型（获取工具调用请求） ======================
        AiMessage firstAiMessage = model.chat(firstRequest).aiMessage();
        System.out.println(
                "【首次模型返回】是否触发工具调用：" + !firstAiMessage.toolExecutionRequests()
                        .isEmpty());

        // ====================== 5. 处理工具调用 ======================
        if (!firstAiMessage.toolExecutionRequests().isEmpty()) {
            // 提取首个工具调用请求（实际场景可处理多个）
            ToolExecutionRequest toolRequest = firstAiMessage.toolExecutionRequests().get(0);
            System.out.println("【工具调用信息】工具名：" + toolRequest.name() + "，入参："
                    + toolRequest.arguments());

            // 解析工具入参（订单号）
            String orderId = parseOrderIdFromToolArgs(toolRequest.arguments());
            System.out.println("【解析入参】待查询订单号：" + orderId);

            // 执行自定义业务逻辑（模拟调用物流系统）
            String logisticsResult = queryLogistics(orderId);
            System.out.println("【工具执行结果】" + logisticsResult);

            // 封装工具执行结果消息
            ToolExecutionResultMessage toolResultMessage = ToolExecutionResultMessage.from(
                    toolRequest.id(), "query_logistics", logisticsResult);

            // ====================== 6. 二次调用模型（携带工具结果生成最终回答） ======================
            AiMessage finalAiMessage = model.chat(List.of(
                    SystemMessage.from("你是电商客服助手，仅能通过调用query_logistics工具获取物流信息，禁止虚构数据；"
                            + "工具返回结果后，用自然语言整理并回复用户。"),
                    UserMessage.from("帮我查一下订单 20251216001 的物流状态"),
                    firstAiMessage, // 首次模型返回（含工具调用请求）
                    toolResultMessage // 工具执行结果
            )).aiMessage();

            // ====================== 7. 输出最终回答 ======================
            System.out.println("\n【最终回复】");
            System.out.println(finalAiMessage.text());
        } else {
            // 未触发工具调用（模型直接回答，可能是幻觉）
            System.out.println("模型未触发工具调用，直接回复：" + firstAiMessage.text());
        }
    }

    /**
     * 初始化 通义千问 qwen-plus 模型
     */
    private static ChatModel initQwenModel() {
        // 替换为自己的通义千问 API Key（从阿里云百炼控制台获取）
        String qwenApiKey = System.getenv("QWEN_API_KEY");
        if (qwenApiKey == null || qwenApiKey.isEmpty()) {
            qwenApiKey = "your-qwen-api-key"; // 本地测试可临时赋值
        }

        return OpenAiChatModel.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1") // 千问 OpenAI 兼容接口
                .apiKey(qwenApiKey)
                .modelName("qwen-plus") // 千问工具调用推荐模型（qwen-max 效果更好）
                .temperature(0.1)
                .timeout(Duration.ofMinutes(60000))
                .build();
    }

    /**
     * 解析工具调用入参，提取订单号
     */
    private static String parseOrderIdFromToolArgs(String argsJson) {
        try {
            // 模型返回的入参为JSON字符串，解析为Map
            Map<String, Object> argsMap = OBJECT_MAPPER.readValue(argsJson, new TypeReference<>() {
            });
            return (String) argsMap.get("order_id");
        } catch (JsonProcessingException e) {
            System.err.println("解析工具入参失败：" + e.getMessage());
            return "";
        }
    }

    /**
     * 模拟业务侧物流查询工具（实际场景替换为真实接口调用）
     */
    private static String queryLogistics(String orderId) {
        // 模拟不同订单的物流状态
        Map<String, String> logisticsData = Map.of(
                "20251216001",
                "{\"order_id\":\"20251216001\",\"status\":\"已发货\",\"express_company\":\"顺丰\",\"express_no\":\"SF1234567890\",\"update_time\":\"2025-12-16 14:30:00\",\"location\":\"上海市浦东新区派送中\"}",
                "default", "{\"order_id\":\"" + orderId + "\",\"status\":\"未查询到物流信息\"}"
        );
        return logisticsData.getOrDefault(orderId, logisticsData.get("default"));
    }

}

