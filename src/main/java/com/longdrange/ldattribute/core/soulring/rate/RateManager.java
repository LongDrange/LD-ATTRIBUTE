package com.longdrange.ldattribute.core.soulring.rate;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RateManager {

    private final LDAttribute plugin;
    private final Map<UUID, RateData> cache = new ConcurrentHashMap<>();

    public RateManager(LDAttribute plugin) { this.plugin = plugin; }

    public void reload() {
        cache.clear();
        RateConfig.load(plugin);
    }

    public RateData get(Player player) {
        UUID id = player.getUniqueId();
        RateData d = cache.get(id);
        if (d != null) return d;
        d = new RateData(plugin, id);
        cache.put(id, d);
        return d;
    }

    public void unload(UUID uuid) { cache.remove(uuid); }
    public void saveAll() { }

    public static double getBaseRate(Player player) {
        if (player == null) return 1.0;

        boolean opBypass = RateConfig.isIgnoreOp() && player.isOp();

        double permissionRate = RateConfig.getDefaultRate();
        double stackSum = 0;
        double stackBest = RateConfig.getDefaultRate();

        if (!opBypass) {
            for (RateConfig.Rule rule : RateConfig.allRules()) {
                if (!rule.permission.isEmpty() && !player.hasPermission(rule.permission)) continue;
                if (rule.stack) stackSum += rule.value;
                else if (rule.value > stackBest) stackBest = rule.value;
            }
            permissionRate = Math.max(stackBest, stackSum > 0 ? stackSum : 0);
        }

        double tempRate = 1.0;
        try {
            tempRate = LDAttribute.getInstance().getRateManager().get(player).getBestTempRate();
        } catch (Throwable ignored) {}

        return Math.max(permissionRate, tempRate);
    }

    /** 取玩家幸运属性值（含所有来源） */
    public static double getLuck(Player player) {
        if (player == null) return 0;
        try {
            com.longdrange.ldattribute.data.attribute.LDAttributeData data =
                    com.longdrange.ldattribute.card.StatsDataRead.loadPlayerStats(player);
            if (data == null) return 0;
            LDSubAttribute luck = data.getSubAttribute(RateConfig.getLuckAttributeName());
            if (luck == null) return 0;
            return luck.getValue();
        } catch (Throwable t) {
            return 0;
        }
    }

    /** 幸运净加成值 = max(0, 幸运 - BaseLuck) / Divisor */
    public static double getLuckBonus(Player player) {
        if (!RateConfig.isLuckEnabled()) return 0;
        double luck = getLuck(player);
        double net = luck - RateConfig.getBaseLuck();
        if (net <= 0) return 0;
        return net / RateConfig.getLuckDivisor();
    }

    /** 最终倍率 = 基础倍率 + 幸运加成 */
    public static double getRate(Player player) {
        double base = getBaseRate(player);
        double bonus = getLuckBonus(player);
        if (bonus <= 0) return base;
        if ("MULTIPLY".equals(RateConfig.getLuckMode())) {
            return base * (1.0 + bonus);
        } else {
            return base + bonus;
        }
    }

    public static List<String> getActiveSources(Player player) {
        List<String> out = new ArrayList<>();
        if (player == null) return out;

        boolean opBypass = RateConfig.isIgnoreOp() && player.isOp();

        if (!opBypass) {
            for (RateConfig.Rule rule : RateConfig.allRules()) {
                if (!rule.permission.isEmpty() && !player.hasPermission(rule.permission)) continue;
                out.add(rule.name + " §7(x" + rule.value + ")");
            }
        } else {
            out.add("§7(OP 跳过权限倍率，可用限时倍率)");
        }

        try {
            RateData data = LDAttribute.getInstance().getRateManager().get(player);
            for (RateData.TempRate r : data.getActiveTempRates()) {
                long sec = r.remainSeconds();
                out.add("§e限时倍率 §7(x" + r.value + ") §7剩 " + formatTime(sec));
            }
        } catch (Throwable ignored) {}

        if (RateConfig.isLuckEnabled()) {
            double luck = getLuck(player);
            double bonus = getLuckBonus(player);
            if (bonus > 0) {
                String symbol = "MULTIPLY".equals(RateConfig.getLuckMode()) ? "×" : "+";
                out.add("§a幸运加成 §7(" + symbol + String.format("%.2f", bonus)
                        + "，幸运=" + luck + " - 基准" + RateConfig.getBaseLuck() + ")");
            } else if (luck > 0) {
                out.add("§7幸运 " + luck + " §7(≤ 基准 " + RateConfig.getBaseLuck() + "，无加成)");
            }
        }

        return out;
    }

    public static String formatTime(long sec) {
        if (sec <= 0) return "0s";
        long d = sec / 86400; sec %= 86400;
        long h = sec / 3600;  sec %= 3600;
        long m = sec / 60;    sec %= 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d");
        if (h > 0) sb.append(h).append("h");
        if (m > 0) sb.append(m).append("m");
        if (sec > 0) sb.append(sec).append("s");
        return sb.toString();
    }

    public static long parseTime(String s) {
        if (s == null || s.isEmpty()) return -1;
        s = s.trim().toLowerCase();
        long total = 0;
        StringBuilder num = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) num.append(c);
            else {
                if (num.length() == 0) return -1;
                long n;
                try { n = Long.parseLong(num.toString()); } catch (Exception e) { return -1; }
                num.setLength(0);
                switch (c) {
                    case 'd': total += n * 86400L; break;
                    case 'h': total += n * 3600L; break;
                    case 'm': total += n * 60L; break;
                    case 's': total += n; break;
                    default: return -1;
                }
            }
        }
        if (num.length() > 0) {
            try { total += Long.parseLong(num.toString()); } catch (Exception e) { return -1; }
        }
        return total > 0 ? total : -1;
    }
}