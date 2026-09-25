package com.longdrange.ldattribute.core.ring;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class RingAttributeReader {

    public static Map<String, String> parseItemLore(ItemStack item) {
        Map<String, String> map = new LinkedHashMap<>();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return map;
        for (String line : item.getItemMeta().getLore()) {
            String plain = ChatColor.stripColor(line).trim();
            if (plain.isEmpty()) continue;
            if (plain.contains(RingItemUtil.ITEM_TYPE_KEY)) continue;
            if (plain.contains("魂珠ID") || plain.contains("魂珠id")) continue;
            boolean skip = false;
            for (String k : RingItemUtil.MAX_KEYS) if (plain.startsWith(k)) { skip = true; break; }
            if (skip) continue;
            if (plain.contains("======")) continue;
            if (plain.contains("放入") || plain.contains("稀有度")) continue;
            if (plain.startsWith("等级") || plain.startsWith("Lv.") || plain.startsWith("Lv ")) continue;

            int idx = plain.indexOf(':');
            if (idx < 0) idx = plain.indexOf('\uFF1A');
            if (idx < 0) continue;
            String key = plain.substring(0, idx).trim();
            String val = plain.substring(idx + 1).trim();
            if (key.isEmpty() || val.isEmpty()) continue;
            map.put(key, val);
        }
        return map;
    }

    public static Map<String, String> parseTextLines(List<String> lines) {
        Map<String, String> map = new LinkedHashMap<>();
        if (lines == null) return map;
        for (String line : lines) {
            String plain = ChatColor.stripColor(line).trim();
            int idx = plain.indexOf(':');
            if (idx < 0) idx = plain.indexOf('\uFF1A');
            if (idx < 0) continue;
            String key = plain.substring(0, idx).trim();
            String val = plain.substring(idx + 1).trim();
            if (key.isEmpty() || val.isEmpty()) continue;
            map.put(key, val);
        }
        return map;
    }

    public static Map<String, Double> multiply(Map<String, String> single, int amount) {
        Map<String, Double> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : single.entrySet()) {
            try {
                double v = Double.parseDouble(e.getValue().replace("+", "").replace("%", "").trim());
                out.put(e.getKey(), v * amount);
            } catch (Exception ignored) {}
        }
        return out;
    }

    /** 汇总玩家所有魂珠属性（含等级倍数） */
    public static Map<String, Double> sumAll(RingData data) {
        Map<String, Double> total = new LinkedHashMap<>();
        if (data == null) return total;
        for (RingData.Slot slot : data.getAllSlots()) {
            Map<String, String> single = parseItemLore(slot.template);
            double bonus = RingUpgradeConfig.getBonusPerLevel(slot.ringType);
            double multiplier = 1.0 + (slot.level - 1) * bonus;
            for (Map.Entry<String, String> e : single.entrySet()) {
                try {
                    double v = Double.parseDouble(e.getValue().replace("+", "").replace("%", "").trim());
                    total.merge(e.getKey(), v * slot.count * multiplier, Double::sum);
                } catch (Exception ignored) {}
            }
        }
        return total;
    }
}