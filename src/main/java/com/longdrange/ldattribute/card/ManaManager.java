package com.longdrange.ldattribute.card;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 法力系統 API
 *
 * 用法：
 *   ManaManager.getCurrent(uuid)
 *   ManaManager.getMax(player)
 *   ManaManager.has(player, 20)
 *   ManaManager.take(player, 20)      // 不夠回傳 false
 *   ManaManager.add(player, 10)       // 自動上限
 *   ManaManager.set(uuid, 100)
 */
public class ManaManager {

    private static LDAttribute plugin;

    public static void init(LDAttribute pl) {
        plugin = pl;
        ManaData.init(pl);
        startRegenTask();
    }

    // ==================== 核心 API ====================

    public static int getCurrent(UUID uuid) {
        return ManaData.get(uuid);
    }

    public static int getCurrent(Player player) {
        return getCurrent(player.getUniqueId());
    }

    public static int getMax(Player player) {
        try {
            LDAttributeData data = StatsDataRead.loadPlayerStats(player);
            if (data == null) return 0;
            for (LDSubAttribute attr : data.getAttributeMap().values()) {
                if (attr.getName().equals("法力上限")) {
                    return (int) attr.getValue();
                }
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    public static void set(UUID uuid, int value) {
        ManaData.set(uuid, value);
    }

    public static void set(Player player, int value) {
        set(player.getUniqueId(), value);
    }

    public static boolean has(Player player, int amount) {
        return getCurrent(player.getUniqueId()) >= amount;
    }

    public static boolean take(Player player, int amount) {
        if (amount <= 0) return true;
        int cur = getCurrent(player.getUniqueId());
        if (cur < amount) return false;
        ManaData.set(player.getUniqueId(), cur - amount);
        return true;
    }

    public static int add(Player player, int amount) {
        int cur = getCurrent(player.getUniqueId());
        int max = getMax(player);
        int newVal = cur + amount;
        if (max > 0 && newVal > max) newVal = max;
        ManaData.set(player.getUniqueId(), newVal);
        return newVal;
    }

    public static void clear(UUID uuid) {
        ManaData.set(uuid, 0);
    }

    public static void unload(UUID uuid) {
        ManaData.unload(uuid);
    }

    // ==================== 每秒回藍 ====================

    private static void startRegenTask() {
        if (plugin == null) return;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                try {
                    int max = getMax(p);
                    if (max <= 0) continue;
                    int cur = getCurrent(p.getUniqueId());
                    if (cur >= max) continue;

                    double regen = 0;
                    LDAttributeData data = StatsDataRead.loadPlayerStats(p);
                    if (data != null) {
                        for (LDSubAttribute attr : data.getAttributeMap().values()) {
                            if (attr.getName().equals("法力恢复")) {
                                regen = attr.getValue();
                                break;
                            }
                        }
                    }
                    if (regen <= 0) continue;

                    int newVal = cur + (int) Math.max(1, Math.round(regen));
                    if (newVal > max) newVal = max;
                    ManaData.set(p.getUniqueId(), newVal);
                } catch (Throwable ignored) {}
            }
        }, 20L, 20L);
    }

    // ==================== 定時存檔 ====================

    public static void saveAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            ManaData.savePlayer(p.getUniqueId());
        }
    }
}