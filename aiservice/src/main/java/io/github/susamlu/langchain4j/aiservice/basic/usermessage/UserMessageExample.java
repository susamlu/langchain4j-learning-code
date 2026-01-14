package io.github.susamlu.langchain4j.aiservice.basic.usermessage;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public class UserMessageExample {

    /**
     * 方式1: 使用 {{it}} 引用单个参数
     * 当方法只有一个参数时，可以使用 {{it}} 来引用该参数
     */
    interface Friend1 {

        @UserMessage("你是我的好朋友，请全程用粤语回答我的问题。{{it}}")
        String chat(String userMessage);

    }

    /**
     * 方式2: 使用 @V 注解指定参数名，然后在模板中使用 {{message}}
     * 这种方式可以明确指定参数在模板中的名称
     */
    interface Friend2 {

        @UserMessage("你是我的好朋友，请全程用粤语回答我的问题。{{message}}")
        String chat(@V("message") String userMessage);

    }

    /**
     * 方式3: 直接使用参数名 {{userMessage}}
     * 需要启用 -parameters 编译选项（Java 8+）
     * 在 pom.xml 中配置 maven-compiler-plugin 的 -parameters 选项
     */
    interface Friend3 {

        @UserMessage("你是我的好朋友，请全程用粤语回答我的问题。{{userMessage}}")
        String chat(String userMessage);

    }

    public static void main(String[] args) {
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-chat")
                .logRequests(true)
                .logResponses(true)
                .build();

        // 方式1: 使用 {{it}} 引用参数
        System.out.println("========== 方式1: 使用 {{it}} ==========");
        Friend1 friend1 = AiServices.create(Friend1.class, model);
        String answer1 = friend1.chat("你好");
        System.out.println("问题: 你好");
        System.out.println("回答: " + answer1);
        System.out.println();

        // 方式2: 使用 @V 注解指定参数名
        System.out.println("========== 方式2: 使用 @V 注解 ==========");
        Friend2 friend2 = AiServices.create(Friend2.class, model);
        String answer2 = friend2.chat("最近怎么样？");
        System.out.println("问题: 最近怎么样？");
        System.out.println("回答: " + answer2);
        System.out.println();

        // 方式3: 直接使用参数名（需要 -parameters 编译选项）
        System.out.println("========== 方式3: 使用参数名 ==========");
        Friend3 friend3 = AiServices.create(Friend3.class, model);
        String answer3 = friend3.chat("最近还好吗？");
        System.out.println("问题: 最近还好吗？");
        System.out.println("回答: " + answer3);
        System.out.println();
    }

}

