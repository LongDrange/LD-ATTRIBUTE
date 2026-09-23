package com.longdrange.ldattribute.card;

import org.bukkit.entity.Player;

import java.util.*;

public class TempBuffManager {

    private static final Map<UUID, Map<String, Long>> active = new HashMap<>();
    private static final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private static final Map<UUID, Integer> killStreak = new HashMap<>();
    private static final Map<UUID, Long> lastKillTime = new HashMap<>();

    public static void tryTrigger(Player player, String event) {
        tryTrigger(player, event, 0);
    }

    public static void tryTrigger(Player player, String event, int param) {
        long now = System.currentTimeMillis();
        Map<String, Long> cdMap = cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        Map<String, Long> activeMap = active.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());

        for (TempBuffConfig.Buff b : TempBuffConfig.getAll()) {
            if (!b.event.equalsIgnoreCase(event)) continue;
            if (event.equals("PLAYER_LOW_HP")) {
                if (param > b.threshold) continue;
            }
            if (event.equals("PLAYER_KILL_STREAK")) {
                if (param < b.streak) continue;
            }
            Long endTime = activeMap.get(b.id);
            if (endTime != null && endTime > now) continue;
            Long last = cdMap.get(b.id);
            if (b.cooldown > 0 && last != null && now - last < b.cooldown * 1000L) continue;
            if (Math.random() > b.chance) continue;

            activeMap.put(b.id, now + b.duration * 1000L);
            cdMap.put(b.id, now);
            if (b.message != null && !b.message.isEmpty()) {
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes((char)38, b.message));
            }
            try {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_PLING, 1f, 1.5f);
            } catch (Throwable ignored) {}
            StatsDataRead.scheduleUpdate(player);
        }
    }

    public static List<String> getActiveEffects(UUID uuid) {
        List<String> result = new ArrayList<>();
        long now = System.currentTimeMillis();
        Map<String, Long> activeMap = active.get(uuid);
        if (activeMap == null) return result;
        Iterator<Map.Entry<String, Long>> it = activeMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            if (e.getValue() <= now) { it.remove(); continue; }
            TempBuffConfig.Buff b = TempBuffConfig.get(e.getKey());
            if (b != null) result.addAll(b.effects);
        }
        return result;
    }

    public static boolean tickExpire(UUID uuid) {
        long now = System.currentTimeMillis();
        Map<String, Long> activeMap = active.get(uuid);
        if (activeMap == null) return false;
        boolean changed = false;
        Iterator<Map.Entry<String, Long>> it = activeMap.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() <= now) { it.remove(); changed = true; }
        }
        return changed;
    }

    public static void addKillStreak(Player player) {
        long now = System.currentTimeMillis();
        Long last = lastKillTime.get(player.getUniqueId());
        int streak;
        if (last != null && now - last < 30000L) {
            streak = killStreak.getOrDefault(player.getUniqueId(), 0) + 1;
        } else {
            streak = 1;
        }
        killStreak.put(player.getUniqueId(), streak);
        lastKillTime.put(player.getUniqueId(), now);
        tryTrigger(player, "PLAYER_KILL_STREAK", streak);
    }

    public static boolean isCritical(Player attacker) {
        return attacker.getFallDistance() > 0.0F
                && !attacker.isOnGround()
                && !attacker.isInsideVehicle()
                && !attacker.hasPotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
    }

    public static int getKillStreak(UUID uuid) {
        return killStreak.getOrDefault(uuid, 0);
    }
    public static void clear(UUID uuid) {
        active.remove(uuid);
        cooldowns.remove(uuid);
        killStreak.remove(uuid);
        lastKillTime.remove(uuid);
    }
}
