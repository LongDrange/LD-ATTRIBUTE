package com.longdrange.ldattribute.card;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.api.AttributeSource;
import com.longdrange.ldattribute.core.api.AttributeSourceRegistry;
import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import org.bukkit.entity.Player;

import java.util.*;

public class StatsSourceTracker {

    public static class Line {
        public final String source;
        public final double value;
        public Line(String source, double value) {
            this.source = source;
            this.value = value;
        }
    }

    public static Map<String, List<Line>> track(Player player) {
        Map<String, List<Line>> result = new LinkedHashMap<>();
        if (player == null) return result;

        // 完全走注册表
        for (AttributeSource src : AttributeSourceRegistry.getAll()) {
            if (!src.isEnabled()) continue;
            try {
                for (AttributeSource.Entry e : src.getEntries(player)) {
                    if (e == null || e.data == null) continue;
                    record(result, e.label, e.data);
                }
            } catch (Throwable ignored) {}
        }
        return result;
    }

    private static void record(Map<String, List<Line>> result, String source, LDAttributeData d) {
        if (d == null) return;
        for (LDSubAttribute a : d.getAttributeMap().values()) {
            double v;
            if (a.getName().equals("幸运")) {
                v = a.getAttributes()[0];
            } else {
                v = a.getValue();
            }
            if (v == 0) continue;
            result.computeIfAbsent(a.getName(), k -> new ArrayList<>())
                    .add(new Line(source, v));
        }
    }

    public static String fmt(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }
}