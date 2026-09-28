package com.longdrange.ldattribute.core.relic;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 遗物管理器
 */
public class RelicManager {

    private final LDAttribute plugin;
    private final Map<UUID, RelicData> cache = new ConcurrentHashMap<>();

    public RelicManager(LDAttribute plugin) { this.plugin = plugin; }

    public void reload() {
        for (RelicData d : cache.values()) d.save();
        cache.clear();
        RelicConfig.load(plugin);
    }

    public RelicData get(Player player) {
        if (player == null) return null;
        UUID id = player.getUniqueId();
        RelicData d = cache.get(id);
        if (d != null) return d;
        d = new RelicData(plugin, id);
        cache.put(id, d);
        return d;
    }

    public void unload(UUID uuid, boolean save) {
        RelicData d = cache.remove(uuid);
        if (d != null && save) d.save();
    }

    public void saveAll() {
        for (RelicData d : cache.values()) d.save();
    }
}