package io.github.susamlu.langchain4j.aiservice.moderation;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.moderation.Moderation;
import dev.langchain4j.model.moderation.ModerationModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

public class DeepseekModerationModel implements ModerationModel {

    public interface ContentModerationService {

        @SystemMessage("""
                你是一个内容审核助手。请根据以下标准审核用户提交的内容：
                1. 识别并标记不当内容（暴力、色情、仇恨言论等）
                2. 判断内容是否违规
                3. 提供审核理由
                4. 建议修改方案（如果可修改）
                """)
        @UserMessage("审核以下内容：{{content}}")
        StructuredModerationResult moderateContent(@V("content") String content);

    }

    public static class StructuredModerationResult {

        @Description("是否违反规定")
        private boolean isViolation;

        @Description("风险等级：HIGH, MEDIUM, LOW")
        private String riskLevel;

        @Description("违规类别列表")
        private List<String> violationCategories;

        @Description("审核详细原因")
        private String detailedReason;

        @Description("修改建议")
        private List<String> suggestions;

        public boolean isViolation() {
            return isViolation;
        }

        public void setViolation(boolean violation) {
            isViolation = violation;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
        }

        public List<String> getViolationCategories() {
            return violationCategories;
        }

        public void setViolationCategories(List<String> violationCategories) {
            this.violationCategories = violationCategories;
        }

        public String getDetailedReason() {
            return detailedReason;
        }

        public void setDetailedReason(String detailedReason) {
            this.detailedReason = detailedReason;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }

        public void setSuggestions(List<String> suggestions) {
            this.suggestions = suggestions;
        }

        @Override
        public String toString() {
            return "StructuredModerationResult{" +
                    "isViolation=" + isViolation +
                    ", riskLevel='" + riskLevel + '\'' +
                    ", violationCategories=" + violationCategories +
                    ", detailedReason='" + detailedReason + '\'' +
                    ", suggestions=" + suggestions +
                    '}';
        }

    }

    @Override
    public Response<Moderation> moderate(String text) {
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .build();

        ContentModerationService contentModerationService = AiServices.create(
                ContentModerationService.class, model);
        StructuredModerationResult result = contentModerationService.moderateContent(text);

        if (result.isViolation) {
            System.out.println("[内容违规] " + result);
            return Response.from(Moderation.flagged(text));
        } else {
            return Response.from(Moderation.notFlagged());
        }
    }

    @Override
    public Response<Moderation> moderate(List<ChatMessage> messages) {
        ChatMessage lastMessage = messages.get(messages.size() - 1);
        return moderate(((dev.langchain4j.data.message.UserMessage) lastMessage).singleText());
    }

}
