package com.longdrange.ldattribute.card;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CooldownSkipData {

    /** 從 ItemStack 解析跳冷卻卷軸，不是卷軸回傳 null */
    public static CooldownSkipData parse(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        if (!item.getItemMeta().hasLore()) return null;
        List<String> lore = item.getItemMeta().getLore();
        boolean isSkip = false;
        int skipSeconds = 0;
        for (String line : lore) {
            String plain = ChatColor.stripColor(line).trim();
            if (plain.contains("物品类型") && plain.contains("跳冷卻卷軸")) { isSkip = true; continue; }
            if (plain.startsWith("跳過冷卻:")) {
                String s = plain.substring(5).trim();
                try { skipSeconds = Integer.parseInt(s); } catch (Exception ignored) {}
            }
        }
        if (!isSkip) return null;
        return new CooldownSkipData(skipSeconds);
    }

    /** 跳過的秒數（0 或 -1 = 清空全部冷卻） */
    public final int skipSeconds;
    public CooldownSkipData(int skipSeconds) { this.skipSeconds = skipSeconds; }
}
