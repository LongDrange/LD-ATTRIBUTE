package com.longdrange.ldattribute.core.talent;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class TalentStatsProvider {

    public static Map<String, Double> getTotal(Player player) {
        if (player == null) return Collections.emptyMap();
        Map<String, Double> total = new LinkedHashMap<>();
        try {
            LDAttribute plugin = LDAttribute.getInstance();
            if (plugin == null || plugin.getCoreManager() == null) return total;
            TalentManager mgr = plugin.getCoreManager().getTalentManager();
            if (mgr == null) return total;
            TalentData data = mgr.get(player);

            for (TalentConfig.TalentDef def : TalentConfig.allTalents()) {
                int lv = data.getLevel(def.id);
                if (lv <= 0) continue;
                for (String line : def.attributes) {
                    String plain = ChatColor.stripColor(line).trim();
                    int idx = plain.indexOf(':');
                    if (idx < 0) idx = plain.indexOf('\uFF1A');
                    if (idx < 0) continue;
                    String key = plain.substring(0, idx).trim();
                    String val = plain.substring(idx + 1).trim();
                    try {
                        double v = Double.parseDouble(val.replace("+", "").replace("%", "").trim());
                        total.merge(key, v * lv, Double::sum);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Throwable t) {}
        return total;
    }

    public static List<String> getTalentLore(Player player) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Double> e : getTotal(player).entrySet())
            out.add(e.getKey() + ": " + format(e.getValue()));
        return out;
    }

    public static String format(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((long) v);
        return String.format("%.1f", v);
    }
}