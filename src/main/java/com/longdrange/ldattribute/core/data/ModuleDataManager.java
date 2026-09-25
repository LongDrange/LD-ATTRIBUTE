package com.longdrange.ldattribute.core.data;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleDataManager {

    private final LDAttribute plugin;
    private final Map<String, PlayerModuleData> cache = new ConcurrentHashMap<>();
    private BukkitTask autoSaveTask;

    public ModuleDataManager(LDAttribute plugin) { this.plugin = plugin; }

    private static String key(UUID uuid, String module) { return uuid + ":" + module; }

    public PlayerModuleData get(UUID uuid, String module) {
        String k = key(uuid, module);
        PlayerModuleData d = cache.get(k);
        if (d != null) return d;
        d = plugin.getStorageManager().load(uuid, module);
        cache.put(k, d);
        return d;
    }

    public void unload(UUID uuid, String module, boolean save) {
        String k = key(uuid, module);
        PlayerModuleData d = cache.remove(k);
        if (d != null && save && d.isDirty()) {
            plugin.getStorageManager().save(uuid, module, d);
        }
    }

    public void unloadAll(UUID uuid, boolean save) {
        String prefix = uuid + ":";
        for (String k : cache.keySet().toArray(new String[0])) {
            if (k.startsWith(prefix)) {
                PlayerModuleData d = cache.remove(k);
                if (d != null && save && d.isDirty()) {
                    plugin.getStorageManager().save(uuid, d.getModule(), d);
                }
            }
        }
    }

    public void saveAll() {
        for (PlayerModuleData d : cache.values()) {
            if (d.isDirty()) {
                plugin.getStorageManager().save(d.getUuid(), d.getModule(), d);
            }
        }
    }

    public void savePlayer(UUID uuid) {
        for (PlayerModuleData d : cache.values()) {
            if (d.getUuid().equals(uuid) && d.isDirty()) {
                plugin.getStorageManager().save(uuid, d.getModule(), d);
            }
        }
    }

    public Set<String> listModules(UUID uuid) {
        Set<String> set = new LinkedHashSet<>();
        for (PlayerModuleData d : cache.values()) {
            if (d.getUuid().equals(uuid)) set.add(d.getModule());
        }
        return set;
    }

    public void startAutoSave() {
        int sec = plugin.getCoreManager().getCoreConfig()
                .getInt("storage.auto-save-seconds", 300);
        if (sec <= 0) return;
        long ticks = sec * 20L;
        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin, this::saveAll, ticks, ticks);
    }

    public void stopAutoSave() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
    }
}