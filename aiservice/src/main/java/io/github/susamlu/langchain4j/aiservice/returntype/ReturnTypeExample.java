package io.github.susamlu.langchain4j.aiservice.returntype;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;

/**
 * AI Service 不同返回类型示例
 * <p>
 * 演示如何在 AI Service 中使用不同类型的返回值：
 * 1. boolean 类型：用于判断类问题
 * 2. enum 类型：用于分类问题
 * 3. 自定义类型：用于结构化数据提取
 */
public class ReturnTypeExample {

    /**
     * 情感枚举
     */
    enum Sentiment {
        POSITIVE, NEGATIVE, NEUTRAL
    }

    /**
     * 人员信息类
     */
    static class Person {

        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + "}";
        }

    }

    /**
     * 问候语判断接口
     */
    interface GreetingExpert {

        /**
         * 判断文本是否为问候语
         *
         * @param text 待判断的文本
         * @return true 如果是问候语，false 否则
         */
        @UserMessage("Is the following text a greeting? Text: {{it}}")
        boolean isGreeting(String text);

    }

    /**
     * 情感分析接口
     */
    interface SentimentAnalyzer {

        /**
         * 分析文本的情感
         *
         * @param text 待分析的文本
         * @return Sentiment 枚举值（POSITIVE, NEGATIVE, NEUTRAL）
         */
        Sentiment analyzeSentiment(String text);

    }

    /**
     * 人员信息提取接口
     */
    interface PersonExtractor {

        /**
         * 从文本中提取人员信息
         *
         * @param text 包含人员信息的文本
         * @return Person 对象，包含姓名和年龄
         */
        Person extractPerson(String text);

    }

    public static void main(String[] args) {
        // ====================== 1. 创建 ChatModel 实例 ======================
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .build();

        // ====================== 2. boolean 返回类型示例 ======================
        System.out.println("========== boolean 返回类型示例 ==========");
        GreetingExpert greetingExpert = AiServices.create(GreetingExpert.class, model);

        String[] texts = {
                "Hello, how are you?",
                "What is the weather today?",
                "Good morning!",
                "I need help with my code."
        };

        for (String text : texts) {
            boolean isGreeting = greetingExpert.isGreeting(text);
            System.out.println("文本: \"" + text + "\"");
            System.out.println("是否为问候语: " + isGreeting);
            System.out.println();
        }

        // ====================== 3. enum 返回类型示例 ======================
        System.out.println("========== enum 返回类型示例 ==========");
        SentimentAnalyzer sentimentAnalyzer = AiServices.create(SentimentAnalyzer.class, model);

        String[] sentimentTexts = {
                "I love this product! It's amazing!",
                "This is terrible. I'm very disappointed.",
                "The weather is okay today."
        };

        for (String text : sentimentTexts) {
            Sentiment sentiment = sentimentAnalyzer.analyzeSentiment(text);
            System.out.println("文本: \"" + text + "\"");
            System.out.println("情感分析结果: " + sentiment);
            System.out.println();
        }

        // ====================== 4. 自定义类返回类型示例 ======================
        System.out.println("========== 自定义类返回类型示例 ==========");
        PersonExtractor extractor = AiServices.create(PersonExtractor.class, model);

        String[] personTexts = {
                "John is 30 years old.",
                "My name is Alice and I'm 25.",
                "Bob, age 35, works as a developer."
        };

        for (String text : personTexts) {
            Person person = extractor.extractPerson(text);
            System.out.println("文本: \"" + text + "\"");
            System.out.println("提取结果: " + person);
            if (person != null) {
                System.out.println("姓名: " + person.getName());
                System.out.println("年龄: " + person.getAge());
            }
            System.out.println();
        }

        // ====================== 5. 验证提取结果 ======================
        System.out.println("========== 验证提取结果 ==========");
        Person person = extractor.extractPerson("John is 30 years old.");
        if (person != null) {
            System.out.println("person.getName() == \"John\": " + ("John".equals(person.getName())));
            System.out.println("person.getAge() == 30: " + (person.getAge() == 30));
        }
    }

}

