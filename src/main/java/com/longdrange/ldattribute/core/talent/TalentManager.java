package com.longdrange.ldattribute.core.talent;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TalentManager {

    private final LDAttribute plugin;
    private final Map<UUID, TalentData> cache = new ConcurrentHashMap<>();

    public TalentManager(LDAttribute plugin) { this.plugin = plugin; }

    public void reload() {
        cache.clear();
        TalentConfig.load(plugin);
    }

    public TalentData get(Player player) {
        UUID id = player.getUniqueId();
        TalentData d = cache.get(id);
        if (d != null) return d;
        d = new TalentData(plugin, id);
        cache.put(id, d);
        return d;
    }

    public void unload(UUID uuid, boolean save) {
        TalentData d = cache.remove(uuid);
        if (d != null && save) d.save();
    }

    public void saveAll() {
        for (TalentData d : cache.values()) d.save();
    }
}