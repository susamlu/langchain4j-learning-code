package io.github.susamlu.langchain4j.responsestreaming.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket + STOMP 核心配置
 * 启用后 Spring 会自动注册 SimpMessagingTemplate Bean
 */
@Configuration
@EnableWebSocketMessageBroker // 核心注解：启用 STOMP 消息代理，自动创建 SimpMessagingTemplate
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 配置消息代理（用于广播/点对点消息）
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 1. 启用内置的简单消息代理，处理以 /topic、/queue 开头的目标地址
        // （生产环境可替换为 RabbitMQ/ActiveMQ 等外部代理）
        config.enableSimpleBroker("/topic", "/queue");

        // 2. 配置应用前缀：客户端发送的消息需以 /app 开头，路由到 @MessageMapping 注解的方法
        config.setApplicationDestinationPrefixes("/app");

        // 3. 可选：配置点对点消息的前缀（如 /user）
        config.setUserDestinationPrefix("/user");
    }

    /**
     * 注册 WebSocket 端点（客户端连接的入口）
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 1. 注册端点：客户端通过 ws://localhost:8080/ws/chat 连接 WebSocket
        registry.addEndpoint("/ws/chat")
                // 2. 允许跨域（调试用，生产需限定域名）
                .setAllowedOriginPatterns("*")
                // 3. 启用 SockJS 降级（适配不支持 WebSocket 的浏览器）
                .withSockJS();
    }

}
