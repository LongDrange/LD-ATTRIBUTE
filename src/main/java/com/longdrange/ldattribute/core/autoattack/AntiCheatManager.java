package com.longdrange.ldattribute.core.autoattack;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 攻速反作弊
 *   - 统计玩家 1 秒内的攻击次数
 *   - 超过 MaxAPS → 记违规
 *   - 累计 WarnTimes 次 → 踢出 + 广播
 *   - 杀戮触发的攻击自动豁免
 */
public class AntiCheatManager implements Listener {

    private final LDAttribute plugin;
    private final Map<UUID, Deque<Long>> attacks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> violations = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastViolation = new ConcurrentHashMap<>();
    private final Map<UUID, Long> exemptUntil = new ConcurrentHashMap<>();

    public AntiCheatManager(LDAttribute plugin) { this.plugin = plugin; }

    /** 让某玩家在 ms 毫秒内豁免（杀戮触发时用） */
    public static void markExempt(Player p, long ms) {
        try {
            LDAttribute pl = LDAttribute.getInstance();
            if (pl == null || pl.getCoreManager() == null) return;
            AntiCheatManager m = pl.getCoreManager().getAntiCheatManager();
            if (m != null) m.exemptUntil.put(p.getUniqueId(), System.currentTimeMillis() + ms);
        } catch (Throwable ignored) {}
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!AntiCheatConfig.enabled) return;
        if (!(e.getDamager() instanceof Player)) return;
        final Player p = (Player) e.getDamager();
        if (p.hasPermission(AntiCheatConfig.exemptPerm)) return;

        long now = System.currentTimeMillis();
        Long ex = exemptUntil.get(p.getUniqueId());
        if (ex != null && now < ex) return;

        Long last = lastViolation.get(p.getUniqueId());
        if (last != null && now - last > AntiCheatConfig.resetAfterMs) {
            violations.remove(p.getUniqueId());
        }

        Deque<Long> q = attacks.computeIfAbsent(p.getUniqueId(), k -> new ArrayDeque<>());
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
        q.addLast(now);
        int aps = q.size();

        if (aps > AntiCheatConfig.maxAPS) {
            int v = violations.merge(p.getUniqueId(), 1, Integer::sum);
            lastViolation.put(p.getUniqueId(), now);

            if (v >= AntiCheatConfig.warnTimes) {
                final String kick = color(AntiCheatConfig.kickMsg);
                final String bmsg = color(AntiCheatConfig.broadcastMsg.replace("%player%", p.getName()));
                final boolean bc = AntiCheatConfig.broadcast;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        if (p.isOnline()) p.kickPlayer(kick);
                        if (bc) Bukkit.broadcastMessage(bmsg);
                    } catch (Throwable ignored) {}
                });
                violations.remove(p.getUniqueId());
                attacks.remove(p.getUniqueId());
                exemptUntil.remove(p.getUniqueId());
            } else {
                String msg = color(AntiCheatConfig.warnMsg
                        .replace("%cur%", String.valueOf(aps))
                        .replace("%max%", String.valueOf(AntiCheatConfig.maxAPS))
                        .replace("%n%", String.valueOf(v))
                        .replace("%total%", String.valueOf(AntiCheatConfig.warnTimes)));
                p.sendMessage(msg);
            }
        }
    }

    private static String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }
}