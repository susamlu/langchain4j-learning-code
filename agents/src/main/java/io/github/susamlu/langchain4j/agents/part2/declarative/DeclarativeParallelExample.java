package io.github.susamlu.langchain4j.agents.part2.declarative;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.declarative.ChatModelSupplier;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.ParallelAgent;
import dev.langchain4j.agentic.declarative.ParallelExecutor;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 声明式API示例：演示如何使用声明式API定义并行工作流
 * <p>
 * 声明式API通过注解来定义智能体和工作流，使代码更简洁、可读性更强。
 * <p>
 * 本示例演示了：
 * 1. 使用 @ParallelAgent 注解定义并行工作流
 * 2. 使用 @ParallelExecutor 注解指定并行执行器
 * 3. 使用 @Output 注解定义输出聚合逻辑
 * 4. 使用 @ChatModelSupplier 注解为子智能体指定不同的 ChatModel
 * 5. 使用 AgenticServices.createAgenticSystem() 创建智能体系统
 */
public class DeclarativeParallelExample {

    /**
     * 晚间计划数据类：包含电影和餐食的组合
     */
    public static class EveningPlan {

        private final String movie;
        private final String meal;

        public EveningPlan(String movie, String meal) {
            this.movie = movie;
            this.meal = meal;
        }

        public String getMovie() {
            return movie;
        }

        public String getMeal() {
            return meal;
        }

        @Override
        public String toString() {
            return "电影：" + movie + " | 餐食：" + meal;
        }

    }

    /**
     * 食物专家接口：根据心情推荐餐食（声明式定义）
     */
    public interface FoodExpert {

        @UserMessage("""
                你是一位出色的晚间计划专家。
                请根据给定的心情推荐3个匹配的餐食。
                心情：{{mood}}
                对于每个餐食，只需提供餐食名称。
                提供一个包含3个项目的列表，不要返回其他内容。
                """)
        @Agent(outputKey = "meals")
        List<String> findMeal(@V("mood") String mood);

        @ChatModelSupplier
        static ChatModel chatModel() {
            return baseModel();
        }

    }

    /**
     * 电影专家接口：根据心情推荐电影（声明式定义）
     */
    public interface MovieExpert {

        @UserMessage("""
                你是一位出色的晚间计划专家。
                请根据给定的心情推荐3个匹配的电影。
                心情：{{mood}}
                提供一个包含3个项目的列表，不要返回其他内容。
                """)
        @Agent(outputKey = "movies")
        List<String> findMovie(@V("mood") String mood);

        @ChatModelSupplier
        static ChatModel chatModel() {
            return baseModel();
        }

    }

    /**
     * 晚间计划智能体接口：使用声明式API定义并行工作流
     */
    public interface EveningPlannerAgent {

        @ParallelAgent(outputKey = "plans",
                subAgents = {FoodExpert.class, MovieExpert.class})
        List<EveningPlan> plan(@V("mood") String mood);

        @ParallelExecutor
        static Executor executor() {
            return Executors.newFixedThreadPool(2);
        }

        @Output
        static List<EveningPlan> createPlans(@V("movies") List<String> movies, @V("meals") List<String> meals) {
            List<EveningPlan> moviesAndMeals = new ArrayList<>();
            for (int i = 0; i < movies.size(); i++) {
                if (i >= meals.size()) {
                    break;
                }
                moviesAndMeals.add(new EveningPlan(movies.get(i), meals.get(i)));
            }
            return moviesAndMeals;
        }

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
        // 使用声明式API创建智能体系统
        // AgenticServices.createAgenticSystem() 方法传入的 ChatModel 会默认用于创建该智能体系统中的所有子智能体
        // 但子智能体可以通过 @ChatModelSupplier 注解覆盖默认的 ChatModel
        EveningPlannerAgent eveningPlannerAgent = AgenticServices
                .createAgenticSystem(EveningPlannerAgent.class, baseModel());

        // 执行工作流
        List<EveningPlan> plans = eveningPlannerAgent.plan("浪漫");

        // 输出结果
        System.out.println("=== 晚间计划推荐（声明式API） ===");
        for (int i = 0; i < plans.size(); i++) {
            System.out.println("方案 " + (i + 1) + ": " + plans.get(i));
        }
        System.out.println();
        System.out.println("=== 工作流执行完成 ===");
    }

}
