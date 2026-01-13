package io.github.susamlu.langchain4j.aiservice.multiservice;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import io.github.susamlu.langchain4j.aiservice.multiservice.MilesOfSmilesMultiServiceExample.ChatBot;
import io.github.susamlu.langchain4j.aiservice.multiservice.MilesOfSmilesMultiServiceExample.GreetingExpert;
import io.github.susamlu.langchain4j.aiservice.multiservice.MilesOfSmilesMultiServiceExample.MilesOfSmiles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * MilesOfSmilesMultiServiceExample 测试类
 * 包含单元测试（使用 Mock）和集成测试（使用真实 AI Service）
 */
@DisplayName("微笑口腔多 AI 服务测试")
class MilesOfSmilesMultiServiceExampleTest {

    // ==================== 单元测试：使用 Mock 对象 ====================

    @Test
    @DisplayName("单元测试：识别问候语并返回欢迎信息")
    void testMilesOfSmilesWithGreeting() {
        // 创建 Mock 对象
        GreetingExpert mockGreetingExpert = mock(GreetingExpert.class);
        ChatBot mockChatBot = mock(ChatBot.class);

        // 配置 Mock 行为：当输入"你好"时，isGreeting 返回 true
        when(mockGreetingExpert.isGreeting("你好")).thenReturn(true);

        // 创建测试对象
        MilesOfSmiles milesOfSmiles = new MilesOfSmiles(mockGreetingExpert, mockChatBot);

        // 执行测试
        String result = milesOfSmiles.handle("你好");

        // 验证结果
        assertEquals("您好！欢迎来到微笑口腔，有什么可以帮到您的吗？", result);

        // 验证 chatBot.reply 从未被调用（因为是问候语，不需要走 RAG 流程）
        verify(mockChatBot, never()).reply(anyString());
    }

    @Test
    @DisplayName("单元测试：非问候语应转发到 ChatBot 处理")
    void testMilesOfSmilesWithNonGreeting() {
        // 创建 Mock 对象
        GreetingExpert mockGreetingExpert = mock(GreetingExpert.class);
        ChatBot mockChatBot = mock(ChatBot.class);

        // 配置 Mock 行为
        String question = "你们提供哪些儿童口腔服务？";
        when(mockGreetingExpert.isGreeting(question)).thenReturn(false);
        when(mockChatBot.reply(question)).thenReturn("我们提供儿童口腔护理服务，包括氟化物涂覆与窝沟封闭。");

        // 创建测试对象
        MilesOfSmiles milesOfSmiles = new MilesOfSmiles(mockGreetingExpert, mockChatBot);

        // 执行测试
        String result = milesOfSmiles.handle(question);

        // 验证结果
        assertEquals("我们提供儿童口腔护理服务，包括氟化物涂覆与窝沟封闭。", result);

        // 验证 greetingExpert.isGreeting 被调用了一次
        verify(mockGreetingExpert, times(1)).isGreeting(question);

        // 验证 chatBot.reply 被调用了一次
        verify(mockChatBot, times(1)).reply(question);
    }

    @Test
    @DisplayName("单元测试：空字符串输入应返回引导信息")
    void testMilesOfSmilesWithEmptyString() {
        // 创建 Mock 对象
        GreetingExpert mockGreetingExpert = mock(GreetingExpert.class);
        ChatBot mockChatBot = mock(ChatBot.class);

        // 创建测试对象
        MilesOfSmiles milesOfSmiles = new MilesOfSmiles(mockGreetingExpert, mockChatBot);

        // 执行测试（空字符串）
        String result = milesOfSmiles.handle("");

        // 验证结果
        assertEquals("您好！请问您有什么关于口腔服务的问题想要咨询吗？", result);

        // 验证既没有调用 greetingExpert，也没有调用 chatBot
        verify(mockGreetingExpert, never()).isGreeting(anyString());
        verify(mockChatBot, never()).reply(anyString());
    }

    @Test
    @DisplayName("单元测试：纯空格输入应返回引导信息")
    void testMilesOfSmilesWithWhitespace() {
        // 创建 Mock 对象
        GreetingExpert mockGreetingExpert = mock(GreetingExpert.class);
        ChatBot mockChatBot = mock(ChatBot.class);

        // 创建测试对象
        MilesOfSmiles milesOfSmiles = new MilesOfSmiles(mockGreetingExpert, mockChatBot);

        // 执行测试（纯空格）
        String result = milesOfSmiles.handle("   ");

        // 验证结果
        assertEquals("您好！请问您有什么关于口腔服务的问题想要咨询吗？", result);

        // 验证既没有调用 greetingExpert，也没有调用 chatBot
        verify(mockGreetingExpert, never()).isGreeting(anyString());
        verify(mockChatBot, never()).reply(anyString());
    }

    // ==================== 集成测试：使用真实 AI Service ====================

    /**
     * 集成测试：测试 GreetingExpert 的真实问候语识别能力
     * <p>
     * 注意：此测试需要配置环境变量 DEEPSEEK_API_KEY
     * 如果没有配置，测试将跳过
     */
    @Test
    @DisplayName("集成测试：GreetingExpert 真实问候语识别")
    void testGreetingExpertIntegration() {
        // 检查环境变量，如果没有配置则跳过测试
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("跳过集成测试：未配置 DEEPSEEK_API_KEY 环境变量");
            return;
        }

        // 创建真实的 ChatModel
        ChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .temperature(0.0)
                .timeout(Duration.ofSeconds(5))
                .build();

        // 创建真实的 GreetingExpert
        GreetingExpert greetingExpert = AiServices.create(GreetingExpert.class, chatModel);

        // 测试各种问候语
        assertTrue(greetingExpert.isGreeting("你好"), "应识别'你好'为问候语");
        assertTrue(greetingExpert.isGreeting("喂!"), "应识别'喂!'为问候语");
        assertTrue(greetingExpert.isGreeting("您好"), "应识别'您好'为问候语");
        assertTrue(greetingExpert.isGreeting("Hi"), "应识别'Hi'为问候语");
        assertTrue(greetingExpert.isGreeting("Hello"), "应识别'Hello'为问候语");

        // 测试非问候语
        assertFalse(greetingExpert.isGreeting("你们提供哪些服务？"), "不应识别业务问题为问候语");
        assertFalse(greetingExpert.isGreeting("我想预约洗牙"), "不应识别业务需求为问候语");
    }

    /**
     * 集成测试：测试完整的 MilesOfSmiles 流程（问候语场景）
     * <p>
     * 注意：此测试需要配置环境变量 DEEPSEEK_API_KEY
     * 如果没有配置，测试将跳过
     */
    @Test
    @DisplayName("集成测试：完整流程测试 - 问候语场景")
    void testMilesOfSmilesIntegrationWithGreeting() {
        // 检查环境变量，如果没有配置则跳过测试
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("跳过集成测试：未配置 DEEPSEEK_API_KEY 环境变量");
            return;
        }

        // 创建真实的 ChatModel
        ChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .temperature(0.0)
                .timeout(Duration.ofSeconds(5))
                .build();

        // 创建真实的 GreetingExpert 和 Mock 的 ChatBot（测试问候语场景不需要真实的 ChatBot）
        GreetingExpert greetingExpert = AiServices.create(GreetingExpert.class, chatModel);
        ChatBot mockChatBot = mock(ChatBot.class);

        // 创建 MilesOfSmiles
        MilesOfSmiles milesOfSmiles = new MilesOfSmiles(greetingExpert, mockChatBot);

        // 测试问候语
        String result = milesOfSmiles.handle("你好");

        // 验证返回欢迎信息
        assertEquals("您好！欢迎来到微笑口腔，有什么可以帮到您的吗？", result);

        // 验证 ChatBot 没有被调用
        verify(mockChatBot, never()).reply(anyString());
    }

}
