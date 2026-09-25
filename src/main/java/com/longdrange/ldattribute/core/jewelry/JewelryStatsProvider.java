package com.longdrange.ldattribute.core.jewelry;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.entity.Player;

import java.util.*;

public class JewelryStatsProvider {

    public static Map<String, Double> getTotal(Player player) {
        if (player == null) return Collections.emptyMap();
        try {
            LDAttribute plugin = LDAttribute.getInstance();
            if (plugin == null || plugin.getCoreManager() == null) return Collections.emptyMap();
            JewelryManager mgr = plugin.getCoreManager().getJewelryManager();
            if (mgr == null) return Collections.emptyMap();
            JewelryData data = mgr.get(player);
            if (data == null) return Collections.emptyMap();
            return JewelryAttributeReader.sumAll(data);
        } catch (Throwable t) { return Collections.emptyMap(); }
    }

    public static List<String> getJewelryLore(Player player) {
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