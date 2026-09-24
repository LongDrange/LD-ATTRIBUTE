package com.longdrange.ldattribute.gacha;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class GachaData {

    private static final Map<UUID, Map<String, Integer>> cache = new HashMap<>();
    private static File file;
    private static YamlConfiguration data;

    public static void init(LDAttribute plugin) {
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) dataDir.mkdirs();
        file = new File(dataDir, "gacha.yml");
        if (!file.exists()) { try { file.createNewFile(); } catch (Exception ignored) {} }
        data = YamlConfiguration.loadConfiguration(file);
    }

    /** 获取某玩家在某卡池的保底计数 */
    public static int getPityCount(UUID uuid, String gachaId) {
        Map<String, Integer> m = cache.get(uuid);
        if (m == null) {
            m = new HashMap<>();
            ConfigurationSection s = data.getConfigurationSection(uuid.toString());
            if (s != null) {
                for (String k : s.getKeys(false)) {
                    m.put(k, s.getInt(k, 0));
                }
            }
            cache.put(uuid, m);
        }
        return m.getOrDefault(gachaId, 0);
    }

    public static void setPityCount(UUID uuid, String gachaId, int count) {
        Map<String, Integer> m = cache.computeIfAbsent(uuid, k -> new HashMap<>());
        m.put(gachaId, count);
        save(uuid);
    }

    public static int incrementPity(UUID uuid, String gachaId) {
        int c = getPityCount(uuid, gachaId) + 1;
        setPityCount(uuid, gachaId, c);
        return c;
    }

    public static void resetPity(UUID uuid, String gachaId) {
        setPityCount(uuid, gachaId, 0);
    }

    public static void save(UUID uuid) {
        Map<String, Integer> m = cache.get(uuid);
        if (m == null) return;
        String path = uuid.toString();
        data.set(path, null);
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            data.set(path + "." + e.getKey(), e.getValue());
        }
        saveFile();
    }

    public static void saveFile() {
        try { data.save(file); } catch (Exception ignored) {}
    }

    public static void unload(UUID uuid) {
        save(uuid);
        cache.remove(uuid);
    }
}