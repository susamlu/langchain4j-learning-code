package io.github.susamlu.langchain4j.aiservice.retrieval;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.List;

/**
 * 内容检索器示例
 * 演示如何使用 DefaultRetrievalAugmentor 实现 RAG (Retrieval Augmented Generation)
 */
public class RetrievalAugmentorExample {

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

        // 创建嵌入模型
        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .apiKey(System.getenv("QWEN_API_KEY"))
                .modelName("text-embedding-v2")
                .build();

        // 创建嵌入存储
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 准备一些示例文档内容
        List<TextSegment> segments = List.of(
                TextSegment.from("Java 是一种面向对象的编程语言，由 Sun Microsystems 在 1995 年发布。"),
                TextSegment.from("Python 是一种解释型、面向对象、动态数据类型的高级程序设计语言。"),
                TextSegment.from("JavaScript 是一种轻量级的编程语言，通常用于 Web 页面的交互效果。"),
                TextSegment.from("LangChain4j 是 Java 中用于集成大语言模型的框架。"),
                TextSegment.from("RAG (Retrieval Augmented Generation) 是一种结合检索和生成的技术。")
        );

        // 将文本段添加到嵌入存储中
        for (TextSegment segment : segments) {
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
        }

        // 创建内容检索器
        ContentRetriever contentRetriever = new EmbeddingStoreContentRetriever(
                embeddingStore,
                embeddingModel
        );

        // 创建检索增强器
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .build();

        // 使用 AiServices 构建器创建助手，并配置内容检索器
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .retrievalAugmentor(retrievalAugmentor)
                .build();

        // 测试 RAG 功能
        System.out.println("用户提问: 什么是 Java?");
        String answer1 = assistant.chat("什么是 Java?");
        System.out.println("AI 回答: " + answer1);
        System.out.println();

        System.out.println("用户提问: 解释一下 RAG 技术");
        String answer2 = assistant.chat("解释一下 RAG 技术");
        System.out.println("AI 回答: " + answer2);
        System.out.println();

        System.out.println("用户提问: LangChain4j 是什么?");
        String answer3 = assistant.chat("LangChain4j 是什么?");
        System.out.println("AI 回答: " + answer3);
    }

}