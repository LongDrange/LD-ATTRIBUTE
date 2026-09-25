package com.longdrange.ldattribute.core.guide;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuideManager {

    private final LDAttribute plugin;
    private final Map<UUID, GuideData> cache = new ConcurrentHashMap<>();

    public GuideManager(LDAttribute plugin) { this.plugin = plugin; }

    public void reload() {
        cache.clear();
        GuideConfig.load(plugin);
    }

    public GuideData get(Player player) {
        UUID id = player.getUniqueId();
        GuideData d = cache.get(id);
        if (d != null) return d;
        d = new GuideData(plugin, id);
        cache.put(id, d);
        return d;
    }

    public void unload(UUID uuid, boolean save) { cache.remove(uuid); }

    public void saveAll() { }
}