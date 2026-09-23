package com.longdrange.ldattribute.card;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * 玩家當前法力值持久化（data/mana.yml）
 */
public class ManaData {

    private static final Map<UUID, Integer> cache = new HashMap<>();
    private static File file;
    private static YamlConfiguration data;

    public static void init(LDAttribute plugin) {
        File dir = plugin.getDataFolder();
        if (!dir.exists()) dir.mkdirs();
        file = new File(dir, "data" + File.separator + "mana.yml");
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        if (!file.exists()) { try { file.createNewFile(); } catch (Exception ignored) {} }
        data = YamlConfiguration.loadConfiguration(file);
    }

    public static int get(UUID uuid) {
        if (cache.containsKey(uuid)) return cache.get(uuid);
        int v = data.getInt(uuid.toString(), 0);
        cache.put(uuid, v);
        return v;
    }

    public static void set(UUID uuid, int value) {
        if (value < 0) value = 0;
        cache.put(uuid, value);
        data.set(uuid.toString(), value);
    }

    public static void save() {
        try { data.save(file); } catch (Exception ignored) {}
    }

    public static void savePlayer(UUID uuid) {
        data.set(uuid.toString(), get(uuid));
        save();
    }

    public static void unload(UUID uuid) {
        savePlayer(uuid);
        cache.remove(uuid);
    }
}