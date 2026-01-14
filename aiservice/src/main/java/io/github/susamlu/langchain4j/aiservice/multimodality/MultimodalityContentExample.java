package io.github.susamlu.langchain4j.aiservice.multimodality;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.VideoContent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * 多模态内容分析示例（ImageContent、VideoContent）
 * <p>
 * 演示如何在 AI Service 中使用不同类型的内容进行多模态分析
 * <p>
 * 使用方式：
 * 1. 在接口方法中使用 ImageContent 或 VideoContent 作为参数
 * 2. 使用 @SystemMessage 注解设置系统提示词
 * 3. 使用 Content.from() 创建内容（支持 URL 或 base64）
 * 4. 调用方法时传入内容和问题
 */
public class MultimodalityContentExample {

    interface ImageAnalyzer {

        /**
         * 图像分析方法
         *
         * @param image    ImageContent 类型的图像内容，可以通过 URL 或 base64 创建
         * @param question 用户提出的关于图像的问题
         * @return 图像分析的描述结果
         */
        @SystemMessage("你是一个专业的图像分析助手。")
        String analyzeImage(@UserMessage ImageContent image, @UserMessage String question);

    }

    interface VideoAnalyzer {

        /**
         * 视频分析方法
         *
         * @param video    VideoContent 类型的视频内容，可以通过 URL 或 base64 创建
         * @param question 用户提出的关于视频的问题
         * @return 视频分析的描述结果
         */
        @SystemMessage("你是一个专业的视频分析助手。")
        String analyzeVideo(@UserMessage VideoContent video, @UserMessage String question);

    }

    public static void main(String[] args) {
        // ====================== 1. 创建支持多模态的 ChatModel 实例 ======================
        // 注意：多模态分析需要使用支持相应能力的模型，如 qwen-vl-plus、gpt-4-vision 等
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .apiKey(System.getenv("QWEN_API_KEY"))
                .modelName("qwen-vl-plus")
                .logRequests(true)
                .logResponses(true)
                .build();

        // ====================== 2. 图像分析示例（使用 base64 方式） ======================
        System.out.println("========== 图像分析示例 ==========");
        ImageAnalyzer imageAnalyzer = AiServices.create(ImageAnalyzer.class, model);

        try {
            // 从 resources 目录读取图像文件并转换为 base64
            String imageResourcePath = "sample-300x200.jpg";
            byte[] imageBytes = Files.readAllBytes(
                    Paths.get(MultimodalityContentExample.class.getClassLoader()
                            .getResource(imageResourcePath).toURI())
            );
            String base64 = Base64.getEncoder().encodeToString(imageBytes);

            // 使用 base64 创建 ImageContent
            ImageContent imageContent = ImageContent.from(base64, "image/jpeg");
            String imageDescription = imageAnalyzer.analyzeImage(
                    imageContent,
                    "详细描述这张图片"
            );
            System.out.println("图像文件: resources/" + imageResourcePath);
            System.out.println("分析结果: " + imageDescription);
        } catch (Exception e) {
            System.err.println("图像分析失败: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();

        // ====================== 3. 视频分析示例 ======================
        System.out.println("========== 视频分析示例 ==========");
        VideoAnalyzer videoAnalyzer = AiServices.create(VideoAnalyzer.class, model);

        try {
            // 注意：视频 URL 需要是可访问的视频文件地址
            // 如果 URL 方式失败，建议使用 base64 方式
            String videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4";
            String videoDescription = videoAnalyzer.analyzeVideo(
                    VideoContent.from(videoUrl),
                    "详细描述这段视频"
            );
            System.out.println("视频URL: " + videoUrl);
            System.out.println("分析结果: " + videoDescription);
        } catch (Exception e) {
            System.err.println("视频分析失败: " + e.getMessage());
            System.out.println("提示：如果 URL 方式失败，请使用 base64 方式");
            e.printStackTrace();
        }
        System.out.println();
    }

}

