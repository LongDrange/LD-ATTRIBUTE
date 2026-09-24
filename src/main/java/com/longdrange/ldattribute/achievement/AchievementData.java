package com.longdrange.ldattribute.achievement;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class AchievementData {

    public static class PlayerAch {
        public final Map<String, Integer> progress = new HashMap<>();
        public final Set<String> completed = new HashSet<>();
        public final Set<String> claimed = new HashSet<>();
    }

    private static final Map<UUID, PlayerAch> cache = new HashMap<>();
    private static File file;
    private static YamlConfiguration data;

    public static void init(LDAttribute plugin) {
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) dataDir.mkdirs();
        file = new File(dataDir, "achievements.yml");
        if (!file.exists()) { try { file.createNewFile(); } catch (Exception ignored) {} }
        data = YamlConfiguration.loadConfiguration(file);
    }

    public static PlayerAch get(UUID uuid) {
        if (cache.containsKey(uuid)) return cache.get(uuid);
        PlayerAch pa = new PlayerAch();
        String path = uuid.toString();
        ConfigurationSection progSec = data.getConfigurationSection(path + ".progress");
        if (progSec != null) {
            for (String k : progSec.getKeys(false)) {
                pa.progress.put(k, progSec.getInt(k, 0));
            }
        }
        List<String> done = data.getStringList(path + ".completed");
        if (done != null) pa.completed.addAll(done);
        List<String> cl = data.getStringList(path + ".claimed");
        if (cl != null) pa.claimed.addAll(cl);
        cache.put(uuid, pa);
        return pa;
    }

    public static void setProgress(UUID uuid, String achId, int value) {
        PlayerAch pa = get(uuid);
        pa.progress.put(achId, value);
        AchievementConfig.Achievement a = AchievementConfig.get(achId);
        if (a != null && value >= a.target) pa.completed.add(achId);
        save(uuid);
    }

    public static int addProgress(UUID uuid, String achId, int delta) {
        PlayerAch pa = get(uuid);
        int cur = pa.progress.getOrDefault(achId, 0);
        int nv = cur + delta;
        pa.progress.put(achId, nv);
        AchievementConfig.Achievement a = AchievementConfig.get(achId);
        if (a != null && nv >= a.target) pa.completed.add(achId);
        save(uuid);
        return nv;
    }

    public static int getProgress(UUID uuid, String achId) {
        return get(uuid).progress.getOrDefault(achId, 0);
    }

    public static boolean isCompleted(UUID uuid, String achId) {
        return get(uuid).completed.contains(achId);
    }

    public static boolean isClaimed(UUID uuid, String achId) {
        return get(uuid).claimed.contains(achId);
    }

    public static void markClaimed(UUID uuid, String achId) {
        get(uuid).claimed.add(achId);
        save(uuid);
    }

    public static void save(UUID uuid) {
        PlayerAch pa = cache.get(uuid);
        if (pa == null) return;
        String path = uuid.toString();
        data.set(path + ".progress", null);
        for (Map.Entry<String, Integer> e : pa.progress.entrySet()) {
            data.set(path + ".progress." + e.getKey(), e.getValue());
        }
        data.set(path + ".completed", new ArrayList<>(pa.completed));
        data.set(path + ".claimed", new ArrayList<>(pa.claimed));
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