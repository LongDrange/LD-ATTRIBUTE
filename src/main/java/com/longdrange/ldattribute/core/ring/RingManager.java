package com.longdrange.ldattribute.core.ring;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RingManager {

    private final LDAttribute plugin;
    private final Map<UUID, RingData> cache = new ConcurrentHashMap<>();

    public RingManager(LDAttribute plugin) { this.plugin = plugin; }

    public void reload() {
        for (RingData d : cache.values()) d.save();
        cache.clear();
        RingConfig.load(plugin);
        RingSlotConfig.load(plugin);
    }

    public RingData get(Player player) {
        UUID id = player.getUniqueId();
        RingData d = cache.get(id);
        if (d != null) return d;
        d = new RingData(plugin, id);
        cache.put(id, d);
        return d;
    }

    public void unload(UUID uuid, boolean save) {
        RingData d = cache.remove(uuid);
        if (d != null && save) d.save();
    }

    public void saveAll() {
        for (RingData d : cache.values()) d.save();
    }
}