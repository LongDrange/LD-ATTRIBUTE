package com.longdrange.ldattribute.core.api;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.source.LoreSource;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public class AttributeSourceRegistry {

    private static final List<AttributeSource> sources = new CopyOnWriteArrayList<>();

    public static void register(AttributeSource src) {
        if (src == null) return;
        for (AttributeSource s : sources) {
            if (s.getName().equals(src.getName())) return;
        }
        sources.add(src);
    }

    public static void unregister(String name) {
        sources.removeIf(s -> s.getName().equals(name));
    }

    public static void clear() { sources.clear(); }

    public static List<AttributeSource> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(sources));
    }

    private static boolean isRegistered(String name) {
        for (AttributeSource s : sources) if (s.getName().equals(name)) return true;
        return false;
    }

    /**
     * 从 sources.yml 读取所有"简单 Lore 来源"并反射注册
     * 每一条格式：
     *   Name:     显示名
     *   Enabled:  true/false
     *   Provider: 完整类名
     *   Method:   静态方法名（签名：static List<String> xxx(Player)）
     */
    @SuppressWarnings("unchecked")
    public static void loadFromConfig(LDAttribute plugin) {
        File f = new File(plugin.getDataFolder(), "sources.yml");
        if (!f.exists()) {
            try { plugin.saveResource("sources.yml", false); } catch (Throwable ignored) {}
        }
        if (!f.exists()) return;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        List<Map<?, ?>> list = cfg.getMapList("Sources");
        int count = 0;
        for (Map<?, ?> m : list) {
            try {
                Object en = m.get("Enabled");
                if (en != null && !Boolean.parseBoolean(String.valueOf(en))) continue;

                String name = String.valueOf(m.get("Name"));
                String provider = String.valueOf(m.get("Provider"));
                String method = String.valueOf(m.get("Method"));
                if (name == null || name.isEmpty() || "null".equals(name)) continue;
                if (provider == null || provider.isEmpty() || "null".equals(provider)) continue;
                if (method == null || method.isEmpty() || "null".equals(method)) continue;
                if (isRegistered(name)) continue;

                final Class<?> cls = Class.forName(provider);
                final Method mm = cls.getMethod(method, Player.class);
                Function<Player, List<String>> fn = p -> {
                    try {
                        Object r = mm.invoke(null, p);
                        if (r instanceof List) return (List<String>) r;
                    } catch (Throwable ignored) {}
                    return Collections.emptyList();
                };
                register(new LoreSource(name, fn));
                count++;
            } catch (Throwable t) {
                plugin.getLogger().warning("[Core] sources.yml 加载来源失败: " + t.getMessage());
            }
        }
        plugin.getLogger().info("[Core] sources.yml 额外注册 " + count + " 个来源");
    }
}