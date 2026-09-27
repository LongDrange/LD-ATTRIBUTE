package com.longdrange.ldattribute.core.guide;

import org.bukkit.ChatColor;

import java.util.*;

public class GuideAttrSummary {

    public static class Entry {
        public final String name;
        public double value;
        public Entry(String name, double value) {
            this.name = name; this.value = value;
        }
    }

    public static List<Entry> summarize(Collection<GuideConfig.MonsterDef> defs, GuideData data) {
        LinkedHashMap<String, Entry> map = new LinkedHashMap<>();
        for (GuideConfig.MonsterDef def : defs) {
            if (!data.isUnlocked(def.id)) continue;
            for (String line : def.attribute) {
                parseAndMerge(line, map);
            }
        }
        return new ArrayList<>(map.values());
    }

    public static List<Entry> summarizeGroup(String groupId, GuideData data) {
        return summarize(GuideConfig.byGroup(groupId), data);
    }

    public static List<Entry> summarizeAll(GuideData data) {
        return summarize(GuideConfig.allMonsters(), data);
    }

    private static void parseAndMerge(String line, Map<String, Entry> map) {
        if (line == null) return;
        String s = ChatColor.stripColor(line).trim();
        int idx = s.indexOf(':');
        if (idx < 0) idx = s.indexOf('：');
        if (idx < 0) return;
        String name = s.substring(0, idx).trim();
        String valStr = s.substring(idx + 1).trim();
        valStr = valStr.replace("+", "").replace("%", "").trim();
        double val;
        try { val = Double.parseDouble(valStr); } catch (Exception e) { return; }
        Entry e = map.get(name);
        if (e == null) {
            map.put(name, new Entry(name, val));
        } else {
            e.value += val;
        }
    }
}