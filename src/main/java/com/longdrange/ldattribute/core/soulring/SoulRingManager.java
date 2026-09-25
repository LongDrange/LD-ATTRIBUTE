package com.longdrange.ldattribute.core.soulring;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SoulRingManager {

    private final LDAttribute plugin;
    private final Map<UUID, SoulRingData> cache = new ConcurrentHashMap<>();

    public SoulRingManager(LDAttribute plugin) { this.plugin = plugin; }

    public void reload() {
        for (SoulRingData d : cache.values()) d.save();
        cache.clear();
        SoulRingConfig.load(plugin);
    }

    public SoulRingData get(Player player) {
        UUID id = player.getUniqueId();
        SoulRingData d = cache.get(id);
        if (d != null) return d;
        d = new SoulRingData(plugin, id);
        cache.put(id, d);
        return d;
    }

    public void unload(UUID uuid, boolean save) {
        SoulRingData d = cache.remove(uuid);
        if (d != null && save) d.save();
    }

    public void saveAll() {
        for (SoulRingData d : cache.values()) d.save();
    }
}