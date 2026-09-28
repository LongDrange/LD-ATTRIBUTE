package com.longdrange.ldattribute.core.autoattack;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.data.PlayerModuleData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AutoAttackManager {

    public static final String MODULE = "autoattack";
    private final LDAttribute plugin;
    private final Map<UUID, BukkitTask> tasks = new ConcurrentHashMap<>();
    private BukkitTask tickTask;

    public AutoAttackManager(LDAttribute plugin) { this.plugin = plugin; }

    public void start() {
        if (tickTask != null) tickTask.cancel();
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 5L);
    }

    public void stop() {
        if (tickTask != null) { tickTask.cancel(); tickTask = null; }
        for (BukkitTask t : tasks.values()) try { t.cancel(); } catch (Throwable ignored) {}
        tasks.clear();
    }

    public void reload() {
        stop();
        AutoAttackConfig.load(plugin);
        AntiCheatConfig.load(plugin);
        start();
    }

    private void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean on = isEnabled(p);
            if (!on) { stopTask(p.getUniqueId()); continue; }
            AutoAttackConfig.Profile prof = AutoAttackConfig.resolve(p);
            startTask(p, prof);
        }
        Set<UUID> online = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) online.add(p.getUniqueId());
        Iterator<Map.Entry<UUID, BukkitTask>> it = tasks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, BukkitTask> e = it.next();
            if (!online.contains(e.getKey())) {
                try { e.getValue().cancel(); } catch (Throwable ignored) {}
                it.remove();
            }
        }
    }

    private void startTask(Player p, AutoAttackConfig.Profile prof) {
        if (tasks.containsKey(p.getUniqueId())) return;
        long interval = Math.max(1, prof.interval);
        final UUID uuid = p.getUniqueId();
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player pp = Bukkit.getPlayer(uuid);
            if (pp == null || !pp.isOnline()) return;
            try { attack(pp, prof); } catch (Throwable ignored) {}
        }, interval, interval);
        tasks.put(uuid, task);
    }

    private void stopTask(UUID uuid) {
        BukkitTask t = tasks.remove(uuid);
        if (t != null) try { t.cancel(); } catch (Throwable ignored) {}
    }

    private void attack(Player p, AutoAttackConfig.Profile prof) {
        if (!p.isOnline() || p.isDead()) return;
        List<LivingEntity> targets = findTargets(p, prof);
        if (targets.isEmpty()) return;
        if (prof.autoRotate) face(p, targets.get(0));

        // 豁免反作弊 500ms（让杀戮自己的攻击不算违规）
        AntiCheatManager.markExempt(p, 500L);

        for (LivingEntity t : targets) {
            if (t == null || t.isDead() || !t.isValid()) continue;
            try {
                try {
                    // 1.12.2 兼容：Player.attack 不存在，用反射
                    java.lang.reflect.Method atk = p.getClass().getMethod("attack", org.bukkit.entity.Entity.class);
                    atk.invoke(p, t);
                } catch (Throwable e1) {
                    try {
                        // 兜底：直接造成伤害（走事件）
                        t.damage(1.0, p);
                    } catch (Throwable ignored2) {}
                }
            } catch (Throwable ignored) {}
        }
    }

    private List<LivingEntity> findTargets(Player p, AutoAttackConfig.Profile prof) {
        List<LivingEntity> out = new ArrayList<>();
        double r = Math.max(1, prof.range);
        for (Entity e : p.getNearbyEntities(r, r, r)) {
            if (e == null || e instanceof Player) continue;
            if (!(e instanceof LivingEntity)) continue;
            LivingEntity le = (LivingEntity) e;
            if (le.isDead() || !le.isValid()) continue;
            if (le instanceof org.bukkit.entity.Animals) continue;
            if (le instanceof org.bukkit.entity.Villager) continue;
            if (le instanceof org.bukkit.entity.Tameable) continue;
            if (le instanceof org.bukkit.entity.ArmorStand) continue;
            out.add(le);
        }
        out.sort(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(p.getLocation())));
        if (prof.maxTargets > 0 && out.size() > prof.maxTargets) return out.subList(0, prof.maxTargets);
        return out;
    }

    private void face(Player p, LivingEntity t) {
        try {
            org.bukkit.Location loc = p.getLocation();
            org.bukkit.util.Vector dir = t.getLocation().toVector().subtract(loc.toVector());
            if (dir.lengthSquared() < 0.001) return;
            loc.setDirection(dir);
            p.teleport(loc);
        } catch (Throwable ignored) {}
    }

    public boolean isActive(Player p) { return tasks.containsKey(p.getUniqueId()); }

    public boolean isEnabled(Player p) {
        return plugin.getModuleDataManager().get(p.getUniqueId(), MODULE).getBoolean("enabled", false);
    }

    public boolean toggle(Player p) { return setEnabled(p, !isEnabled(p)); }

    public boolean setEnabled(Player p, boolean on) {
        PlayerModuleData md = plugin.getModuleDataManager().get(p.getUniqueId(), MODULE);
        md.set("enabled", on);
        plugin.getModuleDataManager().savePlayer(p.getUniqueId());
        AutoAttackConfig.Profile prof = AutoAttackConfig.resolve(p);
        if (on) {
            sendMsg(p, prof.msgOn);
            for (String c : prof.onEnableCmds) runCmd(p, c);
            startTask(p, prof);
        } else {
            sendMsg(p, prof.msgOff);
            for (String c : prof.onDisableCmds) runCmd(p, c);
            stopTask(p.getUniqueId());
        }
        return on;
    }

    public void unload(Player p) { stopTask(p.getUniqueId()); }

    private void sendMsg(Player p, String s) {
        if (s != null && !s.isEmpty()) p.sendMessage(ChatColor.translateAlternateColorCodes('&', s));
    }

    private void runCmd(Player p, String cmd) {
        try {
            String run = cmd.replace("%player%", p.getName());
            if (run.startsWith("/")) run = run.substring(1);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), run);
        } catch (Throwable ignored) {}
    }
}
