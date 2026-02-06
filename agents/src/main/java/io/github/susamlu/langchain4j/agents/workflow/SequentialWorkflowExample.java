package io.github.susamlu.langchain4j.agents.workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.Map;

/**
 * 顺序工作流示例：演示如何使用 sequenceBuilder 创建多个智能体的顺序执行流程
 * <p>
 * 工作流程：
 * 1. CreativeWriter - 根据主题生成故事草稿
 * 2. AudienceEditor - 根据目标受众编辑故事
 * 3. StyleEditor - 根据风格编辑故事
 */
public class SequentialWorkflowExample {

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
     * 受众编辑接口：根据目标受众编辑故事
     */
    public interface AudienceEditor {

        @UserMessage("""
                你是一位专业编辑。
                分析并重写以下故事，使其更好地符合目标受众：{{audience}}。
                只返回故事内容，不要返回其他内容。
                故事内容："{{story}}"
                """)
        @Agent("编辑故事以更好地适应给定的目标受众")
        String editStory(@V("story") String story, @V("audience") String audience);

    }

    /**
     * 风格编辑接口：根据风格编辑故事
     */
    public interface StyleEditor {

        @UserMessage("""
                你是一位专业编辑。
                分析并重写以下故事，使其更好地符合并更连贯地体现 {{style}} 风格。
                只返回故事内容，不要返回其他内容。
                故事内容："{{story}}"
                """)
        @Agent("编辑故事以更好地适应给定的风格")
        String editStory(@V("story") String story, @V("style") String style);

    }

    public static void main(String[] args) {
        // 创建对话模型
        ChatModel baseModel = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .logRequests(true)
                .logResponses(true)
                .build();

        // 创建创意作家智能体
        CreativeWriter creativeWriter = AgenticServices
                .agentBuilder(CreativeWriter.class)
                .chatModel(baseModel)
                .outputKey("story")
                .build();

        // 创建受众编辑智能体
        AudienceEditor audienceEditor = AgenticServices
                .agentBuilder(AudienceEditor.class)
                .chatModel(baseModel)
                .outputKey("story")
                .build();

        // 创建风格编辑智能体
        StyleEditor styleEditor = AgenticServices
                .agentBuilder(StyleEditor.class)
                .chatModel(baseModel)
                .outputKey("story")
                .build();

        // 创建顺序工作流：按顺序执行三个智能体
        UntypedAgent novelCreator = AgenticServices
                .sequenceBuilder()
                .subAgents(creativeWriter, audienceEditor, styleEditor)
                .outputKey("story")
                .build();

        // 准备输入参数
        Map<String, Object> input = Map.of(
                "topic", "龙与巫师",
                "audience", "青年",
                "style", "奇幻"
        );

        // 执行工作流
        String story = (String) novelCreator.invoke(input);

        // 输出结果
        System.out.println("=== 最终生成的故事 ===");
        System.out.println(story);
        System.out.println();
        System.out.println("=== 工作流执行完成 ===");
    }

}
