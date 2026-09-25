package com.longdrange.ldattribute.core.jewelry;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class JewelryAttributeReader {

    public static Map<String, String> parseItemLore(ItemStack item) {
        Map<String, String> map = new LinkedHashMap<>();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return map;
        for (String line : item.getItemMeta().getLore()) {
            String plain = ChatColor.stripColor(line).trim();
            if (plain.isEmpty()) continue;
            if (plain.contains(JewelryItemUtil.ITEM_TYPE_KEY)) continue;
            boolean skip = false;
            for (String k : JewelryItemUtil.SLOT_KEYS) if (plain.startsWith(k)) { skip = true; break; }
            if (skip) continue;
            if (plain.contains("======")) continue;
            if (plain.contains("绑定") || plain.contains("稀有度")) continue;
            if (plain.contains("已绑定")) continue;

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

    public static Map<String, Double> sumAll(JewelryData data) {
        Map<String, Double> total = new LinkedHashMap<>();
        if (data == null) return total;
        for (Map.Entry<String, ItemStack> e : data.getAllItems().entrySet()) {
            Map<String, String> single = parseItemLore(e.getValue());
            for (Map.Entry<String, String> se : single.entrySet()) {
                try {
                    double v = Double.parseDouble(se.getValue().replace("+", "").replace("%", "").trim());
                    total.merge(se.getKey(), v, Double::sum);
                } catch (Exception ignored) {}
            }
        }
        return total;
    }
}