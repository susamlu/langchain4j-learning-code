package io.github.susamlu.langchain4j.agents.part2.async;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.ArrayList;
import java.util.List;

/**
 * 异步顺序工作流示例：演示如何使用 async(true) 创建异步智能体，并在顺序工作流中使用 executor
 * <p>
 * 工作流程：
 * 1. FoodExpert - 根据心情推荐餐食（异步执行）
 * 2. MovieExpert - 根据心情推荐电影（异步执行）
 * 3. EveningPlannerAgent - 组合餐食和电影推荐，生成晚间计划
 */
public class AsyncSequenceWorkflowExample {

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
     * 食物专家接口：根据心情推荐餐食
     */
    public interface FoodExpert {

        @UserMessage("""
                你是一位出色的晚间计划专家。
                请根据给定的心情推荐3个匹配的餐食。
                心情：{{mood}}
                对于每个餐食，只需提供餐食名称。
                提供一个包含3个项目的列表，不要返回其他内容。
                """)
        @Agent
        List<String> findMeal(@V("mood") String mood);

    }

    /**
     * 电影专家接口：根据心情推荐电影
     */
    public interface MovieExpert {

        @UserMessage("""
                你是一位出色的晚间计划专家。
                请根据给定的心情推荐3个匹配的电影。
                心情：{{mood}}
                提供一个包含3个项目的列表，不要返回其他内容。
                """)
        @Agent
        List<String> findMovie(@V("mood") String mood);

    }

    /**
     * 晚间计划智能体接口：根据心情生成晚间计划
     */
    public interface EveningPlannerAgent {

        List<EveningPlan> plan(@V("mood") String mood);

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

        // 创建食物专家智能体（异步执行）
        FoodExpert foodExpert = AgenticServices
                .agentBuilder(FoodExpert.class)
                .chatModel(baseModel)
                .async(true)
                .outputKey("meals")
                .build();

        // 创建电影专家智能体（异步执行）
        MovieExpert movieExpert = AgenticServices
                .agentBuilder(MovieExpert.class)
                .chatModel(baseModel)
                .async(true)
                .outputKey("movies")
                .build();

        // 创建顺序工作流：先执行食物推荐，再执行电影推荐，然后组合结果
        EveningPlannerAgent eveningPlannerAgent = AgenticServices
                .sequenceBuilder(EveningPlannerAgent.class)
                .subAgents(foodExpert, movieExpert)
                .outputKey("plans")
                .output((AgenticScope agenticScope) -> {
                    List<String> movies = agenticScope.readState("movies", List.of());
                    List<String> meals = agenticScope.readState("meals", List.of());

                    List<EveningPlan> moviesAndMeals = new ArrayList<>();
                    for (int i = 0; i < movies.size(); i++) {
                        if (i >= meals.size()) {
                            break;
                        }
                        moviesAndMeals.add(new EveningPlan(movies.get(i), meals.get(i)));
                    }
                    return moviesAndMeals;
                })
                .build();

        // 执行工作流
        List<EveningPlan> plans = eveningPlannerAgent.plan("浪漫");

        // 输出结果
        System.out.println("=== 晚间计划推荐 ===");
        for (int i = 0; i < plans.size(); i++) {
            System.out.println("方案 " + (i + 1) + ": " + plans.get(i));
        }
        System.out.println();
        System.out.println("=== 工作流执行完成 ===");
    }

}
