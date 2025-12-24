package io.github.susamlu.langchain4j.chatmemory.redis;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 Redis 实现的 ChatMemoryStore，支持持久化对话记忆
 * 解决内存版 ChatMemory 重启丢失、多实例不共享问题
 */
public class RedisChatMemoryStore implements ChatMemoryStore {

    // Redis 键前缀，避免与其他业务键冲突
    private static final String REDIS_KEY_PREFIX = "langchain4j:chat-memory:";
    // Redis 连接池（生产环境建议通过配置文件管理参数）
    private final JedisPool jedisPool;

    // 构造方法：默认连接本地 Redis（6379）
    public RedisChatMemoryStore() {
        this("localhost", 6379);
    }

    // 构造方法：自定义 Redis 地址和端口
    public RedisChatMemoryStore(String host, int port) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        // 连接池基础配置（生产环境按需调整）
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(5);
        this.jedisPool = new JedisPool(poolConfig, host, port);
    }

    /**
     * 从 Redis 读取指定 memoryId 的所有对话消息
     */
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String redisKey = getRedisKey(memoryId);
        try (Jedis jedis = jedisPool.getResource()) {
            // Redis List 结构：按插入顺序存储消息的 JSON 字符串
            List<String> messageJsons = jedis.lrange(redisKey, 0, -1);
            // 使用官方反序列化工具解析 ChatMessage
            return messageJsons.stream()
                    .map(ChatMessageDeserializer::messageFromJson)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 将最新的消息列表全量更新到 Redis
     * （LangChain4j 驱逐策略生效后，会调用此方法更新过滤后的消息）
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String redisKey = getRedisKey(memoryId);
        try (Jedis jedis = jedisPool.getResource()) {
            // 先删除旧数据，再写入新数据（全量更新，保证与内存中一致）
            jedis.del(redisKey);
            if (!messages.isEmpty()) {
                // 使用官方序列化工具将 ChatMessage 转为 JSON 字符串
                List<String> messageJsons = messages.stream()
                        .map(ChatMessageSerializer::messageToJson)
                        .collect(Collectors.toList());
                jedis.rpush(redisKey, messageJsons.toArray(new String[0]));
            }
        }
    }

    /**
     * 删除指定 memoryId 的所有对话消息
     */
    @Override
    public void deleteMessages(Object memoryId) {
        String redisKey = getRedisKey(memoryId);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(redisKey);
        }
    }

    // 拼接 Redis 完整键名：前缀 + memoryId
    private String getRedisKey(Object memoryId) {
        return REDIS_KEY_PREFIX + memoryId.toString();
    }

    // 关闭 Redis 连接池（应用关闭时调用）
    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }

}
