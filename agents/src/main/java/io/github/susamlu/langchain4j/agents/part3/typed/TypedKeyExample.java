package io.github.susamlu.langchain4j.agents.part3.typed;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.TypedKey;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;

/**
 * 强类型输入输出示例：演示如何使用 TypedKey 实现类型安全的数据传递
 * <p>
 * 本示例演示了：
 * 1. 定义强类型键（TypedKey）
 * 2. 在智能体接口中使用强类型键
 * 3. 使用强类型键创建条件工作流和顺序工作流
 */
public class TypedKeyExample {

    public enum RequestCategory {
        LEGAL,      // 法律
        MEDICAL,    // 医疗
        TECHNICAL,  // 技术
        UNKNOWN     // 未知
    }

    // 定义强类型键
    public static class UserRequest implements TypedKey<String> {

    }

    public static class ExpertResponse implements TypedKey<String> {

    }

    public static class Category implements TypedKey<RequestCategory> {

        @Override
        public RequestCategory defaultValue() {
            return RequestCategory.UNKNOWN;
        }

    }

    public interface CategoryRouter {

        @UserMessage("""
                分析以下用户请求，并将其分类为 'legal'(法律)、'medical'(医疗)或 'technical'(技术)。
                如果请求不属于以上任何类别，则分类为 'unknown'(未知)。
                只返回其中一个词，不要返回其他内容。
                用户请求是：'{{UserRequest}}'。
                """)
        @Agent(description = "对用户请求进行分类", typedOutputKey = Category.class)
        RequestCategory classify(@K(UserRequest.class) String request);

    }

    public interface MedicalExpert {

        @UserMessage("""
                你是一位医疗专家。
                请从医疗角度分析以下用户请求，并提供最佳答案。
                用户请求是：'{{UserRequest}}'。
                """)
        @Agent("医疗专家")
        String medical(@K(UserRequest.class) String request);

    }

    /**
     * 专家聊天机器人接口：类型化顺序工作流的根接口
     */
    public interface ExpertChatbot {

        @Agent
        String ask(@K(UserRequest.class) String request);

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

        CategoryRouter routerAgent = AgenticServices.agentBuilder(CategoryRouter.class)
                .chatModel(baseModel)
                .build();

        MedicalExpert medicalExpert = AgenticServices.agentBuilder(MedicalExpert.class)
                .chatModel(baseModel)
                .outputKey(ExpertResponse.class)
                .build();

        // 使用强类型键创建条件工作流
        UntypedAgent expertsAgent = AgenticServices.conditionalBuilder()
                .subAgents(scope -> scope.readState(Category.class) == RequestCategory.MEDICAL, medicalExpert)
                .build();

        // 使用强类型键创建顺序工作流（类型化接口）
        ExpertChatbot expertChatbot = AgenticServices.sequenceBuilder(ExpertChatbot.class)
                .subAgents(routerAgent, expertsAgent)
                .outputKey(ExpertResponse.class)
                .build();

        // 调用时使用类型化方法
        String response = expertChatbot.ask("我摔断了腿，应该怎么办？");
        System.out.println("回答：" + response);
    }

}
