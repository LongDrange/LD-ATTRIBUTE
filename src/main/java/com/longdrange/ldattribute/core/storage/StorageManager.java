package com.longdrange.ldattribute.core.storage;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.data.PlayerModuleData;
import com.longdrange.ldattribute.core.storage.provider.MysqlProvider;
import com.longdrange.ldattribute.core.storage.provider.SqliteProvider;
import com.longdrange.ldattribute.core.storage.provider.YamlProvider;

import java.util.UUID;

public class StorageManager {

    private final LDAttribute plugin;
    private StorageProvider provider;
    private StorageType type;

    public StorageManager(LDAttribute plugin) { this.plugin = plugin; }

    public void init() throws Exception {
        String raw = plugin.getCoreManager().getCoreConfig()
                .getString("storage.type", "YAML");
        StorageType t = StorageType.fromString(raw);
        if (t == null) {
            plugin.getLogger().warning("[Core] 未知存储方式 '" + raw + "'，回退到 YAML");
            t = StorageType.YAML;
        }
        this.type = t;
        this.provider = createProvider(t);
        this.provider.init();
        plugin.getLogger().info("[Core] 存储已初始化: " + t);
    }

    public void shutdown() {
        if (provider != null) {
            try { provider.shutdown(); } catch (Throwable ignored) { }
            provider = null;
        }
    }

    public void reload() throws Exception {
        shutdown();
        init();
    }

    private StorageProvider createProvider(StorageType t) {
        switch (t) {
            case SQLITE: return new SqliteProvider(plugin);
            case MYSQL:  return new MysqlProvider(plugin);
            case YAML:
            default:     return new YamlProvider(plugin);
        }
    }

    public PlayerModuleData load(UUID uuid, String module) { return provider.load(uuid, module); }
    public void save(UUID uuid, String module, PlayerModuleData data) { provider.save(uuid, module, data); }
    public void delete(UUID uuid, String module) { provider.delete(uuid, module); }
    public boolean exists(UUID uuid, String module) { return provider.exists(uuid, module); }
    public StorageType getType() { return type; }
}