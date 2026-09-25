package com.longdrange.ldattribute.core.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PapiUtil {

    private static boolean available = false;

    public static void init() {
        available = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public static boolean isAvailable() { return available; }

    /**
     * 解析字符串
     * 流程：
     *   1. 先替换 {player} {display_name} {world} 等自定义占位符
     *   2. 再交给 PAPI 解析 %xxx%
     */
    public static String parse(Player player, String text) {
        if (text == null || text.isEmpty()) return text;
        if (player == null) return text;

        // ① 自定义占位符（不用 %，避免和 PAPI 冲突）
        if (text.indexOf('{') >= 0) {
            text = text.replace("{player}", player.getName());
            String dn = player.getDisplayName();
            if (dn == null) dn = player.getName();
            text = text.replace("{display_name}", dn);
            text = text.replace("{world}", player.getWorld().getName());
        }

        // ② PAPI 解析
        if (!available) return text;
        if (text.indexOf('%') < 0) return text;
        try {
            return PlaceholderAPI.setPlaceholders(player, text);
        } catch (Throwable t) {
            return text;
        }
    }

    public static List<String> parseList(Player player, List<String> list) {
        if (list == null || list.isEmpty()) return list;
        if (player == null) return list;
        List<String> out = new ArrayList<>(list.size());
        for (String s : list) out.add(parse(player, s));
        return out;
    }

    public static ItemStack parseItem(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return item;
        if (player == null) return item;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        boolean changed = false;

        if (meta.hasDisplayName()) {
            String name = meta.getDisplayName();
            String parsed = parse(player, name);
            if (!parsed.equals(name)) {
                meta.setDisplayName(parsed);
                changed = true;
            }
        }

        if (meta.hasLore()) {
            List<String> orig = meta.getLore();
            List<String> parsed = parseList(player, orig);
            boolean any = false;
            for (int i = 0; i < orig.size(); i++) {
                if (!orig.get(i).equals(parsed.get(i))) { any = true; break; }
            }
            if (any) {
                meta.setLore(parsed);
                changed = true;
            }
        }

        if (changed) item.setItemMeta(meta);
        return item;
    }
}