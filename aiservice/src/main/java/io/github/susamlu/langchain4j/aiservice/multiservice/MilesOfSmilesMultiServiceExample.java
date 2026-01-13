package io.github.susamlu.langchain4j.aiservice.multiservice;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.time.Duration;
import java.util.List;

/**
 * 微笑口腔（Miles of Smiles）多 AI 服务示例：
 * - 基础对话模型负责问候检测与路由（低计算成本、快响应）
 * - 进阶对话模型 + 检索增强生成（RAG）负责复杂业务回答
 */
public class MilesOfSmilesMultiServiceExample {

    /**
     * 问候识别专家：使用基础对话模型判断输入是否为问候语。
     */
    interface GreetingExpert {

        @UserMessage("请仅返回true或false，判断以下文本是否为问候语：{{it}}")
        boolean isGreeting(String text);

    }

    /**
     * 智能客服机器人：结合检索到的业务信息，提供专业的口腔服务回复。
     */
    interface ChatBot {

        @SystemMessage("你是微笑口腔的智能客服助手，请严格基于检索到的业务信息回答用户问题，保持专业、友好和简洁，不编造未提及的信息。")
        String reply(String userMessage);

    }

    /**
     * 编排器：先通过问候检测判断用户意图，再决定是否触发 RAG 流程。
     */
    static class MilesOfSmiles {

        private final GreetingExpert greetingExpert;
        private final ChatBot chatBot;

        MilesOfSmiles(GreetingExpert greetingExpert, ChatBot chatBot) {
            this.greetingExpert = greetingExpert;
            this.chatBot = chatBot;
        }

        String handle(String userMessage) {
            // 预处理用户输入，避免空值/全空格导致的误判
            String trimmedMessage = userMessage.trim();
            if (trimmedMessage.isEmpty()) {
                return "您好！请问您有什么关于口腔服务的问题想要咨询吗？";
            }

            boolean isGreeting = greetingExpert.isGreeting(trimmedMessage);
            System.out.println("[路由] 检测到问候语？" + isGreeting);
            if (isGreeting) {
                return "您好！欢迎来到微笑口腔，有什么可以帮到您的吗？";
            }
            System.out.println("[路由] 转向 RAG + 进阶模型处理业务问题");
            return chatBot.reply(trimmedMessage);
        }

    }

    /**
     * 构建内存向量检索器，加载微笑口腔的核心业务文档并完成向量化存储。
     *
     * @param embeddingModel 用于文本向量化的嵌入模型
     * @return 可检索业务文档的 ContentRetriever
     */
    private static ContentRetriever milesOfSmilesContentRetriever(EmbeddingModel embeddingModel) {
        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

        List<TextSegment> docs = List.of(
                TextSegment.from("微笑口腔提供牙科健康咨询、牙齿清洁、牙齿美白与正畸方案，强调无痛体验。"),
                TextSegment.from("我们的诊所支持快速预约与复诊提醒，营业时间为每周一到周六 9:00-19:00。"),
                TextSegment.from("微笑口腔的儿童口腔护理团队专注于预防性保健，提供氟化物涂覆与窝沟封闭。"),
                TextSegment.from("诊所提供分期付款和保险直付选项，可通过官网和电话完成。"),
                TextSegment.from("微笑口腔设有洁牙、全景 X 光和个性化居家护理指导，帮助维持长期口腔健康。")
        );

        // 批量向量化并存储文档
        for (TextSegment doc : docs) {
            Embedding embedding = embeddingModel.embed(doc).content();
            store.add(embedding, doc);
        }

        // 配置检索器：返回最相关的3条结果（平衡相关性与响应速度）
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .minScore(0.5) // 过滤相似度低于 0.5 的结果，减少噪声
                .build();
    }

    public static void main(String[] args) {
        // 基础对话模型：用于简单的问候检测（配置低温度、快响应）
        // 实际场景可替换为 Ollama 部署的本地模型（如 Llama 3.1 8B），进一步降低成本
        ChatModel basicChatModel = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .temperature(0.0) // 温度设为0，确保判断结果稳定
                .timeout(Duration.ofSeconds(3)) // 短超时，适配简单判断场景
                .build();

        GreetingExpert greetingExpert = AiServices.create(GreetingExpert.class, basicChatModel);

        // 进阶对话模型：用于复杂业务问答（配置更高响应质量的模型）
        ChatModel advancedChatModel = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .temperature(0.3) // 低温度保证回答的准确性
                .maxTokens(1000) // 足够的令牌数支撑业务回答
                .timeout(Duration.ofSeconds(10))
                .build();

        // 嵌入模型：使用通义千问文本嵌入模型（适配中文场景的向量化能力）
        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1") // 通义千问兼容 OpenAI 的接口地址
                .apiKey(System.getenv("QWEN_API_KEY"))
                .modelName("text-embedding-v2")
                .build();

        // 构建业务文档检索器
        ContentRetriever contentRetriever = milesOfSmilesContentRetriever(embeddingModel);

        // 构建智能客服：集成进阶模型 + 业务文档检索
        ChatBot chatBot = AiServices.builder(ChatBot.class)
                .chatModel(advancedChatModel)
                .contentRetriever(contentRetriever)
                .build();

        // 初始化服务编排器
        MilesOfSmiles milesOfSmiles = new MilesOfSmiles(greetingExpert, chatBot);

        // 测试1：问候语检测
        String greetingReply = milesOfSmiles.handle("你好");
        System.out.println("[回复] " + greetingReply);

        // 测试2：业务问题查询
        String serviceReply = milesOfSmiles.handle("你们提供哪些儿童口腔服务？");
        System.out.println("[回复] " + serviceReply);
    }

}