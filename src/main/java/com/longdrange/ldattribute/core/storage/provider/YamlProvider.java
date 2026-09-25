package com.longdrange.ldattribute.core.storage.provider;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.data.PlayerModuleData;
import com.longdrange.ldattribute.core.storage.StorageProvider;
import com.longdrange.ldattribute.core.storage.StorageType;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class YamlProvider implements StorageProvider {

    private final LDAttribute plugin;
    private File root;

    public YamlProvider(LDAttribute plugin) { this.plugin = plugin; }

    @Override
    public void init() {
        root = new File(plugin.getDataFolder(), "core-data");
        if (!root.exists() && !root.mkdirs()) {
            plugin.getLogger().warning("[Core] 无法创建 core-data 目录: " + root.getPath());
        }
    }

    @Override public void shutdown() { }

    private File dirOf(String module) {
        File d = new File(root, module);
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private File fileOf(UUID uuid, String module) {
        return new File(dirOf(module), uuid + ".yml");
    }

    @Override
    public PlayerModuleData load(UUID uuid, String module) {
        PlayerModuleData data = new PlayerModuleData(uuid, module);
        File f = fileOf(uuid, module);
        if (!f.exists()) return data;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        for (String key : cfg.getKeys(false)) {
            data.getRaw().put(key, cfg.get(key));
        }
        data.markClean();
        return data;
    }

    @Override
    public void save(UUID uuid, String module, PlayerModuleData data) {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, Object> e : data.getRaw().entrySet()) {
            cfg.set(e.getKey(), e.getValue());
        }
        try {
            cfg.save(fileOf(uuid, module));
            data.markClean();
        } catch (IOException e) {
            plugin.getLogger().severe("[Core/YAML] 保存失败 " + uuid + "/" + module + ": " + e.getMessage());
        }
    }

    @Override
    public void delete(UUID uuid, String module) {
        File f = fileOf(uuid, module);
        if (f.exists()) f.delete();
    }

    @Override
    public boolean exists(UUID uuid, String module) {
        return fileOf(uuid, module).exists();
    }

    @Override public StorageType getType() { return StorageType.YAML; }
}