package com.longdrange.ldattribute.core.data;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用玩家模块数据容器（key-value）
 * 所有新增模块共用，用 key 前缀区分各自的数据区。
 */
public class PlayerModuleData {

    private final UUID uuid;
    private final String module;
    private final Map<String, Object> data = new ConcurrentHashMap<>();
    private volatile boolean dirty = false;

    public PlayerModuleData(UUID uuid, String module) {
        this.uuid = uuid;
        this.module = module;
    }

    public UUID getUuid() { return uuid; }
    public String getModule() { return module; }
    public Map<String, Object> getRaw() { return data; }
    public boolean isDirty() { return dirty; }
    public void markClean() { this.dirty = false; }

    public void set(String key, Object value) {
        if (value == null) data.remove(key);
        else data.put(key, value);
        dirty = true;
    }

    public Object get(String key) { return data.get(key); }

    public String getString(String key, String def) {
        Object o = data.get(key);
        return o == null ? def : String.valueOf(o);
    }

    public int getInt(String key, int def) {
        Object o = data.get(key);
        if (o instanceof Number) return ((Number) o).intValue();
        if (o == null) return def;
        try { return Integer.parseInt(o.toString().trim()); } catch (Exception e) { return def; }
    }

    public long getLong(String key, long def) {
        Object o = data.get(key);
        if (o instanceof Number) return ((Number) o).longValue();
        if (o == null) return def;
        try { return Long.parseLong(o.toString().trim()); } catch (Exception e) { return def; }
    }

    public double getDouble(String key, double def) {
        Object o = data.get(key);
        if (o instanceof Number) return ((Number) o).doubleValue();
        if (o == null) return def;
        try { return Double.parseDouble(o.toString().trim()); } catch (Exception e) { return def; }
    }

    public boolean getBoolean(String key, boolean def) {
        Object o = data.get(key);
        if (o instanceof Boolean) return (Boolean) o;
        if (o == null) return def;
        return Boolean.parseBoolean(o.toString().trim());
    }

    public boolean has(String key) { return data.containsKey(key); }
    public void remove(String key) { data.remove(key); dirty = true; }
    public void clear() { data.clear(); dirty = true; }

    public String serialize() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, Object> e : data.entrySet()) {
            cfg.set(e.getKey(), e.getValue());
        }
        return cfg.saveToString();
    }

    public void deserialize(String s) {
        if (s == null || s.isEmpty()) return;
        YamlConfiguration cfg = new YamlConfiguration();
        try { cfg.loadFromString(s); }
        catch (InvalidConfigurationException ignored) { return; }
        for (String key : cfg.getKeys(false)) {
            data.put(key, cfg.get(key));
        }
        markClean();
    }
}