package com.longdrange.ldattribute.combat;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CombatLog {

    private static final int MAX_ENTRIES = 10;
    private static final Map<UUID, LinkedList<String>> logs = new ConcurrentHashMap<>();
    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");

    public static void record(LivingEntity attacker, LivingEntity victim, double damage,
                              boolean critical, String elementInfo, String source) {
        String time = SDF.format(new Date());
        String atkName = getName(attacker);
        String vicName = getName(victim);

        // 给攻击者的日志
        if (attacker instanceof Player) {
            StringBuilder sb = new StringBuilder();
            sb.append("§7[").append(time).append("] §e你 §7对 §c").append(vicName)
              .append(" §7造成 §e").append(String.format("%.1f", damage)).append(" §7伤害");
            if (critical) sb.append(" §6[暴击]");
            if (source != null && !source.isEmpty()) sb.append(" §b(").append(source).append("§b)");
            if (elementInfo != null && !elementInfo.isEmpty()) sb.append(" §d[").append(elementInfo).append("§d]");
            addLog(((Player) attacker).getUniqueId(), sb.toString());
        }

        // 给受害者的日志
        if (victim instanceof Player) {
            StringBuilder sb = new StringBuilder();
            sb.append("§7[").append(time).append("] §c").append(atkName)
              .append(" §7对你造成 §e").append(String.format("%.1f", damage)).append(" §7伤害");
            if (critical) sb.append(" §6[暴击]");
            if (source != null && !source.isEmpty()) sb.append(" §b(").append(source).append("§b)");
            addLog(((Player) victim).getUniqueId(), sb.toString());
        }
    }

    private static void addLog(UUID uuid, String line) {
        LinkedList<String> list = logs.computeIfAbsent(uuid, k -> new LinkedList<>());
        list.addFirst(line);
        while (list.size() > MAX_ENTRIES) list.removeLast();
    }

    private static String getName(LivingEntity e) {
        if (e instanceof Player) return ((Player) e).getName();
        if (e.getCustomName() != null) return e.getCustomName();
        return e.getType().name();
    }

    public static List<String> get(UUID uuid) {
        LinkedList<String> list = logs.get(uuid);
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }

    public static void clear(UUID uuid) { logs.remove(uuid); }
}