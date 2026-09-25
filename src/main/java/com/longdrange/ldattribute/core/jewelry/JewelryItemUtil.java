package com.longdrange.ldattribute.core.jewelry;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

public class JewelryItemUtil {

    public static final String ITEM_TYPE_KEY = "物品类型";
    public static final String JEWELRY_KEY = "饰品";
    public static final String[] SLOT_KEYS = { "饰品槽位", "槽位", "部位" };

    public static boolean isJewelry(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
        for (String line : item.getItemMeta().getLore()) {
            String plain = ChatColor.stripColor(line).trim();
            if (plain.contains(ITEM_TYPE_KEY) && plain.contains(JEWELRY_KEY)) return true;
        }
        return false;
    }

    /** 读取 lore 中"饰品槽位:"后写的原始文本（中文或英文） */
    public static String getSlotKey(ItemStack item) {
        if (!isJewelry(item)) return null;
        for (String line : item.getItemMeta().getLore()) {
            String plain = ChatColor.stripColor(line).trim();
            if (plain.contains(ITEM_TYPE_KEY)) continue;
            for (String key : SLOT_KEYS) {
                if (plain.startsWith(key)) {
                    int idx = plain.indexOf(':');
                    if (idx < 0) idx = plain.indexOf('\uFF1A');
                    if (idx < 0) continue;
                    String v = plain.substring(idx + 1).trim();
                    if (!v.isEmpty()) return v;
                }
            }
        }
        return null;
    }

    public static int getStackAmount(ItemStack item) {
        return item == null ? 0 : item.getAmount();
    }
}