package com.longdrange.ldattribute.core.jewelry;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JewelryManager {

    private final LDAttribute plugin;
    private final Map<UUID, JewelryData> cache = new ConcurrentHashMap<>();

    public JewelryManager(LDAttribute plugin) { this.plugin = plugin; }

    public void reload() {
        for (JewelryData d : cache.values()) d.save();
        cache.clear();
        JewelryConfig.load(plugin);
    }

    public JewelryData get(Player player) {
        UUID id = player.getUniqueId();
        JewelryData d = cache.get(id);
        if (d != null) return d;
        d = new JewelryData(plugin, id);
        cache.put(id, d);
        return d;
    }

    public void unload(UUID uuid, boolean save) {
        JewelryData d = cache.remove(uuid);
        if (d != null && save) d.save();
    }

    public void saveAll() {
        for (JewelryData d : cache.values()) d.save();
    }
}