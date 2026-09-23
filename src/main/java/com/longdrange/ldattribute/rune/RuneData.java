package com.longdrange.ldattribute.rune;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class RuneData {

    private static final Map<UUID, Set<String>> cache = new HashMap<>();
    private static File file;
    private static YamlConfiguration data;

    public static void init(LDAttribute plugin) {
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) dataDir.mkdirs();
        file = new File(dataDir, "rune-collection.yml");
        if (!file.exists()) { try { file.createNewFile(); } catch (Exception ignored) {} }
        data = YamlConfiguration.loadConfiguration(file);
    }

    public static Set<String> get(UUID uuid) {
        if (cache.containsKey(uuid)) return cache.get(uuid);
        Set<String> set = new LinkedHashSet<>(data.getStringList(uuid.toString() + ".collected"));
        cache.put(uuid, set);
        return set;
    }

    public static boolean collect(UUID uuid, String runeId) {
        if (runeId == null || runeId.isEmpty()) return false;
        Set<String> set = get(uuid);
        if (set.contains(runeId)) return false;
        set.add(runeId);
        save(uuid);
        return true;
    }

    public static boolean has(UUID uuid, String runeId) { return get(uuid).contains(runeId); }
    public static int count(UUID uuid) { return get(uuid).size(); }

    public static void save(UUID uuid) {
        Set<String> set = cache.get(uuid);
        if (set == null) return;
        data.set(uuid.toString() + ".collected", new ArrayList<>(set));
        saveFile();
    }

    public static void saveFile() { try { data.save(file); } catch (Exception ignored) {} }

    public static void unload(UUID uuid) {
        save(uuid);
        cache.remove(uuid);
    }
}