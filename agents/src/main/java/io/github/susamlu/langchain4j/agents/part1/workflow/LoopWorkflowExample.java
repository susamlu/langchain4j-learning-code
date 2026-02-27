package io.github.susamlu.langchain4j.agents.part1.workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 循环工作流示例：演示如何使用 loopBuilder 创建循环执行的工作流
 * <p>
 * 工作流程：
 * 1. CreativeWriter - 根据主题生成故事草稿
 * 2. StyleReviewLoop - 循环评分和编辑故事，直到达到满意的分数
 * - StyleScorer - 根据风格评分故事
 * - StyleEditor - 根据风格编辑故事
 */
public class LoopWorkflowExample {

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

    /**
     * 风格评分接口：根据风格对故事进行评分
     */
    public interface StyleScorer {

        @UserMessage("""
                你是一位严格的评论家。
                请根据以下故事与风格 '{{style}}' 的契合程度，给出一个 0.0 到 1.0 之间的评分。
                只返回评分数字，不要返回其他内容。
                故事内容："{{story}}"
                """)
        @Agent("根据故事与给定风格的契合程度进行评分")
        double scoreStyle(@V("story") String story, @V("style") String style);

    }

    /**
     * 风格化作家接口：根据主题和风格生成故事
     */
    public interface StyledWriter {

        @Agent
        String writeStoryWithStyle(@V("topic") String topic, @V("style") String style);

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

        // 创建风格编辑智能体
        StyleEditor styleEditor = AgenticServices
                .agentBuilder(StyleEditor.class)
                .chatModel(baseModel)
                .outputKey("story")
                .build();

        // 创建风格评分智能体
        StyleScorer styleScorer = AgenticServices
                .agentBuilder(StyleScorer.class)
                .chatModel(baseModel)
                .outputKey("score")
                .build();

        // 创建循环工作流：循环评分和编辑，直到达到满意的分数
        UntypedAgent styleReviewLoop = AgenticServices
                .loopBuilder()
                .subAgents(styleScorer, styleEditor)
                .maxIterations(5)
                .testExitAtLoopEnd(true)
                .exitCondition((agenticScope, loopCounter) -> {
                    double score = agenticScope.readState("score", 0.0);
                    // 前3次循环要求分数 >= 0.8，之后要求分数 >= 0.6
                    return loopCounter <= 3 ? score >= 0.8 : score >= 0.6;
                })
                .build();

        // 创建创意作家智能体
        CreativeWriter creativeWriter = AgenticServices
                .agentBuilder(CreativeWriter.class)
                .chatModel(baseModel)
                .outputKey("story")
                .build();

        // 创建风格化作家：先生成故事，然后循环优化
        StyledWriter styledWriter = AgenticServices
                .sequenceBuilder(StyledWriter.class)
                .subAgents(creativeWriter, styleReviewLoop)
                .outputKey("story")
                .build();

        // 执行工作流
        String story = styledWriter.writeStoryWithStyle("龙与巫师", "喜剧");

        // 输出结果
        System.out.println("=== 最终生成的故事 ===");
        System.out.println(story);
        System.out.println();
        System.out.println("=== 工作流执行完成 ===");
    }

}
