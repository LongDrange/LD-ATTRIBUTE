package com.longdrange.ldattribute.core.value;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ValueManager {

    private final LDAttribute plugin;
    private final Map<UUID, ValueData> cache = new ConcurrentHashMap<>();
    private BukkitTask regenTask;

    public ValueManager(LDAttribute plugin) { this.plugin = plugin; }

    public void reload() {
        cache.clear();
        ValueConfig.load(plugin);
    }

    public ValueData get(Player player) {
        UUID id = player.getUniqueId();
        ValueData d = cache.get(id);
        if (d != null) return d;
        d = new ValueData(plugin, id);
        cache.put(id, d);
        return d;
    }

    public void unload(UUID uuid, boolean save) { cache.remove(uuid); }

    public void saveAll() { }

    /** 启动定时自动恢复 */
    public void startRegen() {
        if (regenTask != null) regenTask.cancel();
        regenTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p == null || !p.isOnline()) continue;
                ValueData d = get(p);
                for (ValueConfig.ValueDef def : ValueConfig.all()) {
                    if (!def.hasRegen()) continue;
                    // 按秒判断：需 Interval 秒才恢复一次
                    // 简化：每秒检查，按次数累加（实际等价于每 Interval 秒恢复）
                    // 这里通过长期累加判断：只对 interval = 1 生效
                    // 更精确的定时下面用独立 scheduler 做
                }
            }
        }, 20L, 20L);
    }

    /** 每个值独立定时器（更精确） */
    public void startRegenTimers() {
        for (ValueConfig.ValueDef def : ValueConfig.all()) {
            if (!def.hasRegen()) continue;
            final String id = def.id;
            long periodTicks = def.regenInterval * 20L;
            Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p == null || !p.isOnline()) continue;
                    ValueData d = get(p);
                    d.regen(id);
                }
            }, periodTicks, periodTicks);
        }
    }

    public void stopRegen() {
        if (regenTask != null) regenTask.cancel();
    }
}