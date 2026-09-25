package com.longdrange.ldattribute.core.ring;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

/**
 * 魂珠识别工具
 *  - 是魂珠：lore 有 "物品类型" 且含 "魂珠"
 *  - 类型 id：优先 lore "魂珠ID: xxx" → 否则 DisplayName(剥色)
 *  - 上限：
 *      1. 物品 lore "上限: N"
 *      2. rings.yml 的 TypeLimits.<type>
 *      3. rings.yml 的 DefaultMaxStack
 */
public class RingItemUtil {

    public static final String ITEM_TYPE_KEY = "物品类型";
    public static final String RING_KEY = "魂珠";
    public static final String[] ID_KEYS = { "魂珠ID", "魂珠id", "魂珠Id", "魂珠编号" };
    public static final String[] MAX_KEYS = { "上限", "堆叠上限", "叠加上限", "最大堆叠", "最大叠加", "堆叠数" };

    public static boolean isRing(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
        for (String line : item.getItemMeta().getLore()) {
            String plain = ChatColor.stripColor(line).trim();
            if (plain.contains(ITEM_TYPE_KEY) && plain.contains(RING_KEY)) return true;
        }
        return false;
    }

    public static String getRingType(ItemStack item) {
        if (!isRing(item)) return null;
        for (String line : item.getItemMeta().getLore()) {
            String plain = ChatColor.stripColor(line).trim();
            if (plain.contains(ITEM_TYPE_KEY)) continue;
            for (String key : ID_KEYS) {
                if (plain.startsWith(key)) {
                    int idx = plain.indexOf(':');
                    if (idx < 0) idx = plain.indexOf('\uFF1A');
                    if (idx < 0) continue;
                    String v = plain.substring(idx + 1).trim();
                    if (!v.isEmpty()) return v;
                }
            }
        }
        if (item.getItemMeta().hasDisplayName())
            return ChatColor.stripColor(item.getItemMeta().getDisplayName()).trim();
        return item.getType().name();
    }

    /** 只从物品 lore 读上限，没读到返回 -1 */
    public static int getMaxStackFromLore(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return -1;
        for (String line : item.getItemMeta().getLore()) {
            String plain = ChatColor.stripColor(line).trim();
            for (String key : MAX_KEYS) {
                if (plain.startsWith(key)) {
                    int idx = plain.indexOf(':');
                    if (idx < 0) idx = plain.indexOf('\uFF1A');
                    if (idx < 0) continue;
                    String v = plain.substring(idx + 1).replaceAll("[^0-9]", "");
                    if (!v.isEmpty()) {
                        try { return Math.max(1, Integer.parseInt(v)); } catch (Exception ignored) {}
                    }
                }
            }
        }
        return -1;
    }

    /** 旧签名：lore > defaultMax */
    public static int getMaxStack(ItemStack item, int defaultMax) {
        int lore = getMaxStackFromLore(item);
        return lore > 0 ? lore : defaultMax;
    }

    /** 新签名：lore > TypeLimits[type] > defaultMax */
    public static int getMaxStack(ItemStack item, String ringType, int defaultMax) {
        int lore = getMaxStackFromLore(item);
        if (lore > 0) return lore;
        Integer typeLimit = RingConfig.getTypeLimit(ringType);
        if (typeLimit != null && typeLimit > 0) return typeLimit;
        return defaultMax;
    }
}