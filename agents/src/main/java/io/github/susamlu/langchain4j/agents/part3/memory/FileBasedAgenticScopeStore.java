package io.github.susamlu.langchain4j.agents.part3.memory;

import dev.langchain4j.agentic.scope.AgenticScopeKey;
import dev.langchain4j.agentic.scope.AgenticScopeSerializer;
import dev.langchain4j.agentic.scope.AgenticScopeStore;
import dev.langchain4j.agentic.scope.DefaultAgenticScope;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 基于文件系统的 AgenticScopeStore 实现示例
 * <p>
 * 将 AgenticScope 序列化为 JSON 并存储到指定目录下的文件中。
 * 适用于简单场景，生产环境建议使用数据库等持久化方案。
 */
public class FileBasedAgenticScopeStore implements AgenticScopeStore {

    private final Path storageDir;

    /**
     * 无参构造器，供 SPI 加载使用。使用默认临时目录作为存储路径。
     */
    public FileBasedAgenticScopeStore() {
        this(Path.of(System.getProperty("java.io.tmpdir"), "agentic-scope-storage"));
    }

    public FileBasedAgenticScopeStore(Path storageDir) {
        this.storageDir = storageDir;
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建存储目录: " + storageDir, e);
        }
    }

    private static final String KEY_SEPARATOR = "\u0000";

    /**
     * 使用目录结构 agentId/memoryId.json 存储，避免键冲突
     */
    private Path keyToPath(AgenticScopeKey key) {
        String safeAgentId = sanitize(key.agentId());
        String safeMemoryId = sanitize(String.valueOf(key.memoryId()));
        return storageDir.resolve(safeAgentId).resolve(safeMemoryId + ".json");
    }

    private String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

    @Override
    public boolean save(AgenticScopeKey key, DefaultAgenticScope agenticScope) {
        try {
            Path path = keyToPath(key);
            Files.createDirectories(path.getParent());
            String json = AgenticScopeSerializer.toJson(agenticScope);
            String header = key.agentId() + KEY_SEPARATOR + key.memoryId() + "\n";
            Files.writeString(path, header + json);
            return true;
        } catch (IOException e) {
            throw new RuntimeException("保存 AgenticScope 失败: " + key, e);
        }
    }

    @Override
    public Optional<DefaultAgenticScope> load(AgenticScopeKey key) {
        Path path = keyToPath(key);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try {
            String content = Files.readString(path);
            int newline = content.indexOf('\n');
            String json = newline >= 0 ? content.substring(newline + 1) : content;
            return Optional.of(AgenticScopeSerializer.fromJson(json));
        } catch (IOException e) {
            throw new RuntimeException("加载 AgenticScope 失败: " + key, e);
        }
    }

    @Override
    public boolean delete(AgenticScopeKey key) {
        try {
            return Files.deleteIfExists(keyToPath(key));
        } catch (IOException e) {
            throw new RuntimeException("删除 AgenticScope 失败: " + key, e);
        }
    }

    @Override
    public Set<AgenticScopeKey> getAllKeys() {
        Set<AgenticScopeKey> keys = new HashSet<>();
        try {
            if (!Files.exists(storageDir)) {
                return keys;
            }
            Files.list(storageDir)
                    .filter(Files::isDirectory)
                    .flatMap(agentDir -> {
                        try {
                            return Files.list(agentDir)
                                    .filter(p -> p.toString().endsWith(".json"))
                                    .map(p -> {
                                        try {
                                            String content = Files.readString(p);
                                            int newline = content.indexOf('\n');
                                            if (newline > 0) {
                                                String[] parts = content.substring(0, newline).split(KEY_SEPARATOR, 2);
                                                if (parts.length == 2) {
                                                    return Optional.of(new AgenticScopeKey(parts[0], parts[1]));
                                                }
                                            }
                                        } catch (IOException ignored) {
                                        }
                                        return Optional.<AgenticScopeKey>empty();
                                    });
                        } catch (IOException e) {
                            return java.util.stream.Stream.empty();
                        }
                    })
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(keys::add);
        } catch (IOException e) {
            throw new RuntimeException("列举 AgenticScope 失败", e);
        }
        return keys;
    }

}
