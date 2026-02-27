package io.github.susamlu.langchain4j.agents.part2.error;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.agent.ErrorRecoveryResult;
import dev.langchain4j.agentic.agent.MissingArgumentException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 错误处理工作流示例：演示如何使用 errorHandler 处理代理执行中的错误并自动恢复
 * <p>
 * 工作流程：
 * 1. CreativeWriter - 根据主题生成故事草稿（需要 topic 参数）
 * 2. AudienceEditor - 根据目标受众编辑故事
 * 3. StyleEditor - 根据风格编辑故事
 * <p>
 * 本示例演示了当缺少必需参数时，如何通过 errorHandler 自动补充参数并重试
 */
public class ErrorHandlingWorkflowExample {

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

        // 用于检测 errorHandler 是否被调用
        AtomicBoolean errorRecoveryCalled = new AtomicBoolean(false);

        // 创建顺序工作流：按顺序执行三个智能体，并配置错误处理器
        UntypedAgent novelCreator = AgenticServices.sequenceBuilder()
                .subAgents(creativeWriter, audienceEditor, styleEditor)
                .errorHandler(errorContext -> {
                    // 检查是否是 generateStory 代理缺少 topic 参数
                    if (errorContext.agentName().equals("generateStory") &&
                            errorContext.exception() instanceof MissingArgumentException mEx &&
                            mEx.argumentName().equals("topic")) {

                        // 向 AgenticScope 写入缺失的参数
                        errorContext.agenticScope().writeState("topic", "龙与巫师");

                        // 标记错误恢复已被调用
                        errorRecoveryCalled.set(true);

                        // 返回重试结果，让代理重新执行
                        return ErrorRecoveryResult.retry();
                    }

                    // 对于其他错误，抛出异常
                    return ErrorRecoveryResult.throwException();
                })
                .outputKey("story")
                .build();

        // 准备输入参数（故意缺少 "topic" 参数以触发错误）
        Map<String, Object> input = Map.of(
                // "topic", "龙与巫师",
                "audience", "青年",
                "style", "奇幻"
        );

        System.out.println("=== 开始执行工作流（缺少 topic 参数）===");
        System.out.println("输入参数：" + input);
        System.out.println();

        try {
            // 执行工作流
            String story = (String) novelCreator.invoke(input);

            // 输出结果
            System.out.println("=== 最终生成的故事 ===");
            System.out.println(story);
            System.out.println();

            // 检查错误处理器是否被调用
            if (errorRecoveryCalled.get()) {
                System.out.println("✓ 错误处理器成功恢复缺失的 topic 参数并重试");
            } else {
                System.out.println("⚠ 错误处理器未被触发");
            }

            System.out.println();
            System.out.println("=== 工作流执行完成 ===");
        } catch (Exception e) {
            System.err.println("=== 工作流执行失败 ===");
            System.err.println("错误信息：" + e.getMessage());
            e.printStackTrace();
        }
    }

}
