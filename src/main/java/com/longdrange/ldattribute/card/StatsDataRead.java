package com.longdrange.ldattribute.card;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.api.AttributeSource;
import com.longdrange.ldattribute.core.api.AttributeSourceRegistry;
import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class StatsDataRead {

    public static final String SEPARATOR = "======";

    public static List<String> filterNormalLore(ItemStack item) {
        List<String> result = new ArrayList<>();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return result;
        for (String line : item.getItemMeta().getLore()) {
            if (line.contains(SEPARATOR)) break;
            result.add(line);
        }
        return result;
    }

    public static LDAttributeData loadPlayerStats(Player player) {
        LDAttributeData statsData = new LDAttributeData();

        // ===== 所有注册来源（完全统一）=====
        for (AttributeSource src : AttributeSourceRegistry.getAll()) {
            if (!src.isEnabled()) continue;
            try {
                for (AttributeSource.Entry e : src.getEntries(player)) {
                    if (e == null || e.data == null) continue;
                    statsData.add(e.data);
                }
            } catch (Throwable t) {
                LDAttribute.getInstance().getLogger().warning(
                        "[Stats] 来源 " + src.getName() + " 失败: " + t.getMessage());
            }
        }
        // ==================================

        return statsData;
    }

    private static final Set<UUID> pendingUpdates =
            java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static volatile boolean taskScheduled = false;

    public static void updatePlayer(Player player) {
        if (player == null) return;
        pendingUpdates.add(player.getUniqueId());
        if (taskScheduled) return;
        taskScheduled = true;
        try {
            Bukkit.getScheduler().runTaskLater(
                    LDAttribute.getInstance(),
                    () -> {
                        taskScheduled = false;
                        Set<UUID> batch = new HashSet<>(pendingUpdates);
                        pendingUpdates.clear();
                        for (UUID id : batch) {
                            Player p = Bukkit.getPlayer(id);
                            if (p != null && p.isOnline()) {
                                try { updatePlayerNow(p); } catch (Throwable ignored) {}
                            }
                        }
                    },
                    10L);
        } catch (Throwable t) {
            taskScheduled = false;
            updatePlayerNow(player);
        }
    }

    public static void updatePlayerNow(Player player) {
        // ===== 血量保护：刷新属性前后保留血量 =====
        double oldHealth = player.getHealth();
        double oldMaxHealth = player.getMaxHealth();

        LDAttributeData data = loadPlayerStats(player);
        LDAttribute.getInstance().getApi().setEntityAPIData(
                LDAttribute.class, player.getUniqueId(), data);
        LDAttribute.getInstance().getApi().updateHandData(player);

        try {
            double newMaxHealth = player.getMaxHealth();
            // 如果刷新后 MaxHealth 变小了，血量不能超上限
            double targetHealth = Math.min(oldHealth, newMaxHealth);
            // 只有确实掉血了才恢复（防止覆盖玩家主动治疗）
            if (player.getHealth() < targetHealth) {
                player.setHealth(targetHealth);
            }
        } catch (Throwable ignored) {}
        // =========================================
    }

    private static final Set<UUID> pendingRefresh =
            Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public static void scheduleUpdate(Player player) {
        if (player == null || !player.isOnline()) return;
        final UUID uuid = player.getUniqueId();
        if (!pendingRefresh.add(uuid)) return;
        try {
            Bukkit.getScheduler().runTask(LDAttribute.getInstance(), () -> {
                pendingRefresh.remove(uuid);
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    try { updatePlayer(p); } catch (Throwable ignored) {}
                }
            });
        } catch (Throwable t) {
            pendingRefresh.remove(uuid);
        }
    }
}