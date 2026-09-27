package com.longdrange.ldattribute.util;

import com.longdrange.ldattribute.card.CardData;
import com.longdrange.ldattribute.card.CardDataManager;
import com.longdrange.ldattribute.core.ring.RingTypeConfig;
import com.longdrange.ldattribute.rune.RuneConfig;
import com.longdrange.ldattribute.rune.RuneItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * 统一物品解析器
 *
 * 支持格式（大小写不敏感）：
 *   RING:<魂珠ID>[:数量]         → 从 配置/魂珠/ 创建魂珠
 *   CARD:<卡片ID>[:数量]         → 从 配置/卡片/item.yml 创建卡片
 *   RUNE:<符文ID>[:数量]         → 从 配置/符文/rune.yml 创建符文
 *   <材质>[:数量][:DATA][@名][|Lore1|Lore2]  → 原版物品
 *
 * 示例：
 *   ItemResolver.resolve("RING:fire_ring", 1);
 *   ItemResolver.resolve("RING:fire_ring:5", 1);
 *   ItemResolver.resolve("CARD:zhaoyun_t2:1", 1);
 *   ItemResolver.resolve("DIRT:64", 1);
 */
public class ItemResolver {

    public static class Result {
        public ItemStack item;
        public String error;       // null = 成功
        public String displayName; // 用于消息/GUI 显示
    }

    /** 解析一行物品定义，失败返回 item = null */
    public static ItemStack resolve(String line, int defaultAmount) {
        Result r = resolveEx(line, defaultAmount);
        return r.item;
    }

    /** 带详细结果 */
    public static Result resolveEx(String line, int defaultAmount) {
        Result r = new Result();
        if (line == null || line.trim().isEmpty()) {
            r.error = "空定义";
            return r;
        }
        String s = line.trim();
        String[] parts = s.split(":");
        String head = parts[0].toUpperCase();

        // ===== RING:<ID>[:数量] =====
        if (head.equals("RING")) {
            if (parts.length < 2) { r.error = "RING 缺少ID"; return r; }
            String id = parts[1];
            int amt = defaultAmount;
            if (parts.length >= 3) {
                try { amt = Math.max(1, Integer.parseInt(parts[2])); } catch (Throwable ignored) {}
            }
            RingTypeConfig.Def def = RingTypeConfig.get(id);
            if (def == null) { r.error = "未知魂珠ID: " + id; return r; }
            ItemStack it = RingTypeConfig.createItem(id, amt);
            if (it == null) { r.error = "魂珠创建失败"; return r; }
            r.item = it;
            r.displayName = ChatColor.stripColor(def.name);
            return r;
        }

        // ===== CARD:<ID>[:数量] =====
        if (head.equals("CARD")) {
            if (parts.length < 2) { r.error = "CARD 缺少ID"; return r; }
            String id = parts[1];
            int amt = defaultAmount;
            if (parts.length >= 3) {
                try { amt = Math.max(1, Integer.parseInt(parts[2])); } catch (Throwable ignored) {}
            }
            CardData cd = CardDataManager.getCard(id);
            if (cd == null) { r.error = "未知卡片ID: " + id; return r; }
            ItemStack it = cd.getItem().clone();
            it.setAmount(amt);
            r.item = it;
            r.displayName = (it.getItemMeta() != null && it.getItemMeta().hasDisplayName())
                    ? ChatColor.stripColor(it.getItemMeta().getDisplayName())
                    : id;
            return r;
        }

        // ===== RUNE:<ID>[:数量] =====
        if (head.equals("RUNE")) {
            if (parts.length < 2) { r.error = "RUNE 缺少ID"; return r; }
            String id = parts[1];
            int amt = defaultAmount;
            if (parts.length >= 3) {
                try { amt = Math.max(1, Integer.parseInt(parts[2])); } catch (Throwable ignored) {}
            }
            RuneConfig.Rune rune = RuneConfig.getRune(id);
            if (rune == null) { r.error = "未知符文ID: " + id; return r; }
            ItemStack it = RuneItem.create(rune, amt);
            if (it == null) { r.error = "符文创建失败"; return r; }
            r.item = it;
            r.displayName = ChatColor.stripColor(rune.name == null ? id : rune.name);
            return r;
        }

        // ===== 原版物品：MAT:AMOUNT[:DATA][@名][|Lore] =====
        return resolveVanilla(s, defaultAmount);
    }

    private static Result resolveVanilla(String s, int defaultAmount) {
        Result r = new Result();
        // 拆分 @名字 和 |Lore
        String head = s;
        String nameHint = null;
        java.util.List<String> loreLines = new java.util.ArrayList<>();

        int pipeIdx = s.indexOf('|');
        if (pipeIdx >= 0) {
            head = s.substring(0, pipeIdx);
            for (String l : s.substring(pipeIdx + 1).split("\\|")) {
                loreLines.add(ChatColor.translateAlternateColorCodes('&', l));
            }
        }
        int atIdx = head.lastIndexOf('@');
        if (atIdx >= 0) {
            nameHint = head.substring(atIdx + 1).trim();
            head = head.substring(0, atIdx);
        }

        String[] parts = head.split(":");
        Material mat = Material.getMaterial(parts[0].toUpperCase());
        if (mat == null) {
            // 尝试数字ID
            try { mat = Material.getMaterial(Integer.parseInt(parts[0].trim())); } catch (Throwable ignored) {}
        }
        if (mat == null) { r.error = "未知材质: " + parts[0]; return r; }

        int amt = defaultAmount;
        short data = 0;
        if (parts.length >= 2) {
            try { amt = Math.max(1, Integer.parseInt(parts[1])); } catch (Throwable ignored) {}
        }
        if (parts.length >= 3) {
            try { data = Short.parseShort(parts[2]); } catch (Throwable ignored) {}
        }

        ItemStack it = new ItemStack(mat, amt, data);
        if (nameHint != null || !loreLines.isEmpty()) {
            org.bukkit.inventory.meta.ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                if (nameHint != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', nameHint));
                }
                if (!loreLines.isEmpty()) meta.setLore(loreLines);
                it.setItemMeta(meta);
            }
        }
        r.item = it;
        r.displayName = nameHint != null ? nameHint : mat.name();
        return r;
    }

    /** 判断一行是否是 RING/CARD/RUNE 前缀 */
    public static boolean hasSpecialPrefix(String line) {
        if (line == null || line.trim().isEmpty()) return false;
        String head = line.trim().split(":")[0].toUpperCase();
        return head.equals("RING") || head.equals("CARD") || head.equals("RUNE");
    }
}