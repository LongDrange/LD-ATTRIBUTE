package com.longdrange.ldattribute.card;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ExpStoneData {

    public static class ExpStone {
        public final String target;      // 卡片ID / "(所有)" / "(T1所有)"
        public final int exp;
        public final double successRate; // 0.0 ~ 1.0
        public final int needCount;
        public ExpStone(String target, int exp, double successRate, int needCount) {
            this.target = target; this.exp = exp;
            this.successRate = successRate; this.needCount = needCount;
        }

        public boolean matches(String cardId, String cardType) {
            if (target == null) return false;
            if (target.equals("(所有)")) return true;
            if (target.startsWith("(") && target.endsWith("所有)")) {
                String t = target.substring(1, target.length() - 3);
                return t.equalsIgnoreCase(cardType == null ? "" : cardType);
            }
            return target.equals(cardId);
        }
    }

    /** 從 ItemStack 解析經驗石，不是經驗石回傳 null */
    public static ExpStone parse(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        if (!item.getItemMeta().hasLore()) return null;
        List<String> lore = item.getItemMeta().getLore();
        boolean isStone = false;
        String target = null;
        int exp = 0;
        double rate = 1.0;
        int need = 1;

        for (String line : lore) {
            String plain = ChatColor.stripColor(line).trim();
            if (plain.contains("物品类型") && plain.contains("經驗石")) { isStone = true; continue; }
            if (plain.startsWith("卡片:")) {
                target = plain.substring(3).trim();
            } else if (plain.startsWith("經驗:")) {
                String s = plain.substring(3).trim().replace("+", "").trim();
                try { exp = Integer.parseInt(s); } catch (Exception ignored) {}
            } else if (plain.startsWith("成功率:")) {
                String s = plain.substring(4).trim().replace("%", "").trim();
                try { rate = Double.parseDouble(s) / 100.0; } catch (Exception ignored) {}
            } else if (plain.startsWith("需要:")) {
                String s = plain.substring(3).trim().replace("個", "").replace("个", "").trim();
                try { need = Integer.parseInt(s); } catch (Exception ignored) {}
            }
        }
        if (!isStone || target == null || exp <= 0) return null;
        if (need < 1) need = 1;
        return new ExpStone(target, exp, rate, need);
    }
}
