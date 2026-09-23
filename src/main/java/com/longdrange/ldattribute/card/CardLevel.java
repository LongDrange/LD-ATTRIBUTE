package com.longdrange.ldattribute.card;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class CardLevel {

    /** recalc Lore 缓存（key = NBT状态哈希，value = 计算好的 Lore 行） */
    private static final java.util.Map<String, java.util.List<String>> loreCache =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final int MAX_LORE_CACHE = 2000;

    /** 清空缓存（热重载时调用） */
    public static void clearCache() { loreCache.clear(); }
    public static int getCacheSize() { return loreCache.size(); }

    public static int getLevel(ItemStack item) { return CardNBT.getLevel(item); }
    public static int getExp(ItemStack item)   { return CardNBT.getExp(item); }

    public static int getRequiredExp(String cardId, int currentLevel) {
        CardLevelConfig.CardLevel cl = CardLevelConfig.get(cardId);
        if (cl == null) return Integer.MAX_VALUE;
        double req = cl.baseExp * Math.pow(cl.expGrowth, currentLevel - 1);
        return (int) Math.round(req);
    }

    public static boolean isMaxLevel(String cardId, int level) {
        CardLevelConfig.CardLevel cl = CardLevelConfig.get(cardId);
        if (cl == null) return true;
        return cl.maxLevel > 0 && level >= cl.maxLevel;
    }

    public static double calcAttr(CardLevelConfig.AttrConf a, int level) {
        return calcAttr(a, level, 0);
    }

    public static double calcAttr(CardLevelConfig.AttrConf a, int level, int star) {
        int lv = Math.max(0, level - 1);
        double v;
        switch (a.mode) {
            case "PERCENT": v = a.base * Math.pow(1 + a.growth, lv); break;
            case "MIXED":   v = a.base * Math.pow(1 + a.growth, lv) + a.fixed * lv; break;
            default:        v = a.base + a.fixed * lv; break;
        }
        if (star > 0) v = v * (1 + star * StarConfig.getMultiplier());
        // 上限突破：超过 Max 后按 50% 比例继续增长
        if (a.max > 0 && v > a.max) {
            double overflow = v - a.max;
            v = a.max + overflow * 0.5;
            // 硬上限
            double ultra = a.max * 2.0;
            if (v > ultra) v = ultra;
        }
        return v;
    }

    /**
     * 重算卡片 lore（從當前卡片的 lore 提取原始行，動態替換屬性行 + 加等級行）
     * 不依賴 CardDataManager.getCard()，所以可以在 CardDataManager.load() 內部呼叫
     */
    public static ItemStack recalc(ItemStack card, String cardId, int level, int exp) {
        CardLevelConfig.CardLevel cl = CardLevelConfig.get(cardId);
        if (cl == null) return card;

        // ===== 快速路径：缓存命中直接返回 =====
        List<String> origLoreCache = (card.hasItemMeta() && card.getItemMeta().hasLore())
                ? card.getItemMeta().getLore() : java.util.Collections.emptyList();
        StringBuilder runeKeySb = new StringBuilder();
        try {
            java.util.List<String> sIds = com.longdrange.ldattribute.rune.RuneConfig.getCardSockets(cardId);
            for (int i = 0; i < sIds.size(); i++) {
                runeKeySb.append(CardNBT.getSocketRune(card, i)).append("|");
            }
        } catch (Throwable ignored) {}
        String cacheKey = cardId + "|" + level + "|" + exp + "|" + CardNBT.getStar(card) + "|"
                + CardNBT.getBoundName(card) + "|" + CardNBT.getInt(card, "socket_mask", 0) + "|"
                + runeKeySb + "|" + origLoreCache.hashCode();
        java.util.List<String> cached = loreCache.get(cacheKey);
        if (cached != null) {
            ItemStack fast = card.clone();
            ItemMeta fm = fast.getItemMeta();
            if (fm != null) {
                fm.setLore(new java.util.ArrayList<>(cached));
                fast.setItemMeta(fm);
                fast = CardNBT.setLevel(fast, level);
                fast = CardNBT.setExp(fast, exp);
                ItemMeta fm2 = fast.getItemMeta();
                if (fm2 != null) {
                    for (org.bukkit.enchantments.Enchantment e : new java.util.ArrayList<>(fm2.getEnchants().keySet())) {
                        fm2.removeEnchant(e);
                    }
                    if (level >= 2) {
                        int power = Math.min(level - 1, 3);
                        fm2.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, power, true);
                    }
                    fast.setItemMeta(fm2);
                }
            }
            return fast;
        }
        ItemStack result = card.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return result;

        List<String> orig = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        List<String> newLore = new ArrayList<>();
        boolean inRuneSection = false;
        for (String line : orig) {
            String plain = ChatColor.stripColor(line).trim();
            // 符文孔位段检测
            if (plain.startsWith("符文孔位:") || plain.startsWith("符文孔位：")) {
                inRuneSection = true;
                continue;
            }
            if (inRuneSection) {
                String raw = ChatColor.stripColor(line);
                if (raw.startsWith("  ")) continue;
                inRuneSection = false;
            }
            // 跳過舊的等級 / 經驗 / 分割線行
            if (plain.startsWith("等級:") || plain.startsWith("經驗:") || plain.startsWith("星級:") || plain.startsWith("綁定:")) continue;
            if (plain.matches("-{3,}")) continue;
            if (plain.isEmpty() && !newLore.isEmpty() && newLore.get(newLore.size() - 1).isEmpty()) continue;

            // 替換屬性行
            boolean replaced = false;
            for (CardLevelConfig.AttrConf a : cl.attrs.values()) {
                if (plain.startsWith(a.name + ":") || plain.startsWith(a.name + "：")) {
                    int idx = line.indexOf(a.name);
                    if (idx < 0) break;
                    String prefix = line.substring(0, idx);
                    newLore.add(prefix + a.name + ": +" + format(calcAttr(a, level, CardNBT.getStar(card))));
                    replaced = true;
                    break;
                }
            }
            if (!replaced) newLore.add(line);
        }

        // 去掉末尾空行
        while (!newLore.isEmpty() && newLore.get(newLore.size() - 1).isEmpty()) newLore.remove(newLore.size() - 1);

        newLore.add("");
        newLore.add("§8§m----------------");
        if (cl.maxLevel > 0) {
            newLore.add("§7等級: §e" + level + " §7/ §e" + cl.maxLevel);
        } else {
            newLore.add("§7等級: §e" + level);
        }
        if (isMaxLevel(cardId, level)) {
            newLore.add("§7經驗: §aMAX");
        } else {
            newLore.add("§7經驗: §b" + exp + " §7/ §b" + getRequiredExp(cardId, level));
        }

        // 綁定顯示
        String boundName = CardNBT.getBoundName(result);
        if (boundName != null && !boundName.isEmpty()) {
            newLore.add("§7綁定: §e" + boundName);
        }

        // 星級顯示
        int star = CardNBT.getStar(result);
        int maxStarForCard = StarConfig.getMaxStarFor(cardId);
        if (maxStarForCard > 0) {
            StringBuilder sb = new StringBuilder("§7星級: ");
            for (int i = 0; i < maxStarForCard; i++) {
                sb.append(i < star ? "§6★" : "§7☆");
            }
            newLore.add(sb.toString());
        }


        // 符文孔位显示
        try {
            java.util.List<String> socketIds = com.longdrange.ldattribute.rune.RuneConfig.getCardSockets(cardId);
            if (!socketIds.isEmpty()) {
                newLore.add("§8§m----------------");
                newLore.add("§7符文孔位:");
                for (int si = 0; si < socketIds.size(); si++) {
                    String socketId = socketIds.get(si);
                    com.longdrange.ldattribute.rune.RuneConfig.Socket sk =
                            com.longdrange.ldattribute.rune.RuneConfig.getSocket(socketId);
                    if (sk == null) continue;
                    boolean unlocked = CardNBT.isSocketUnlocked(result, si);
                    if (!unlocked) {
                        newLore.add("  §8[未打孔] " + sk.name);
                    } else {
                        String runeId = CardNBT.getSocketRune(result, si);
                        if (runeId == null || runeId.isEmpty()) {
                            newLore.add("  §8[空] " + sk.name);
                        } else {
                            com.longdrange.ldattribute.rune.RuneConfig.Rune rn =
                                    com.longdrange.ldattribute.rune.RuneConfig.getRune(runeId);
                            if (rn != null) {
                                newLore.add("  §8[§f" + rn.name + "§8] " + sk.name);
                            } else {
                                newLore.add("  §8[空] " + sk.name);
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        meta.setLore(newLore);
        // 存入缓存
        if (cacheKey != null && loreCache.size() < MAX_LORE_CACHE) {
            loreCache.put(cacheKey, new java.util.ArrayList<>(newLore));
        } else if (cacheKey != null && loreCache.size() >= MAX_LORE_CACHE * 2) {
            loreCache.clear();
            loreCache.put(cacheKey, new java.util.ArrayList<>(newLore));
        }
        result.setItemMeta(meta);

                result = CardNBT.setLevel(result, level);
result = CardNBT.setExp(result, exp);

        // 動態光效（等級越高越亮）
        ItemMeta meta2 = result.getItemMeta();
        if (meta2 != null) {
            for (org.bukkit.enchantments.Enchantment e : new java.util.ArrayList<>(meta2.getEnchants().keySet())) {
                meta2.removeEnchant(e);
            }
            if (level >= 2) {
                int power = Math.min(level - 1, 3);
                meta2.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, power, true);
            }
            result.setItemMeta(meta2);
        }
        return result;
    }

    public static Result addExp(ItemStack card, String cardId, int delta) {
        CardLevelConfig.CardLevel cl = CardLevelConfig.get(cardId);
        if (cl == null) return new Result(card, 0, 0);
        int level = getLevel(card);
        int exp = getExp(card) + delta;
        int ups = 0;
        while (!isMaxLevel(cardId, level)) {
            int req = getRequiredExp(cardId, level);
            if (exp < req) break;
            exp -= req;
            level++;
            ups++;
        }
        if (isMaxLevel(cardId, level)) exp = 0;
        return new Result(recalc(card, cardId, level, exp), ups, exp);
    }

    public static class Result {
        public final ItemStack item;
        public final int levelsGained;
        public final int remainExp;
        public Result(ItemStack item, int levelsGained, int remainExp) {
            this.item = item; this.levelsGained = levelsGained; this.remainExp = remainExp;
        }
    }

    private static String format(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }
}
