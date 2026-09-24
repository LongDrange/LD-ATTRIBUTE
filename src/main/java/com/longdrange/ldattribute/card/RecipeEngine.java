package com.longdrange.ldattribute.card;

import com.longdrange.ldattribute.points.PointAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class RecipeEngine {

    private static String fail(Player player, String msg) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        return msg;
    }

    public static String execute(Player player, RecipeConfig.Recipe r) {
        if (r.permission != null && !r.permission.isEmpty() && !player.hasPermission(r.permission))
            return "§c你沒有權限使用此配方！需要: §e" + r.permission;

        for (Map.Entry<String, Integer> e : r.inputCards.entrySet()) {
            int have = countQualified(player, e.getKey(), r);
            if (have < e.getValue()) {
                String hint = (r.requireMaxLevel || r.requireMaxStar) ? " §7(需满星满等)" : "";
                return "§c缺少卡片: §e" + e.getKey() + hint + " §7(有 " + have + "/" + e.getValue() + ")";
            }
        }

        int points = PointAPI.getPlayerPoints(player.getName());
        if (r.costPoints > 0 && points < r.costPoints)
            return "§c點券不足！需要 §6" + r.costPoints + " §c你有 §6" + points;

        if (r.costVault > 0) {
            Economy eco = getEconomy();
            if (eco == null) return fail(player, "§c未安裝 Vault 經濟！");
            if (!eco.has(player, r.costVault)) return "§c金幣不足！需要 §6" + (int) r.costVault;
        }
        for (String s : r.costItems) if (!hasItem(player, s)) return "§c缺少物品: §e" + s;

        // 保存进阶保留信息（消耗前先扫描）
        int keepLv = 0, keepStar = 0;
        if (r.keepLevel || r.keepStar) {
            for (Map.Entry<String, Integer> e : r.inputCards.entrySet()) {
                int[] maxArr = findMaxLevelStar(player, e.getKey());
                if (maxArr[0] > keepLv) keepLv = maxArr[0];
                if (maxArr[1] > keepStar) keepStar = maxArr[1];
            }
        }

        for (Map.Entry<String, Integer> e : r.inputCards.entrySet())
            removeQualified(player, e.getKey(), e.getValue(), r);
        if (r.costPoints > 0) PointAPI.takePlayerPoints(player.getName(), r.costPoints);
        if (r.costVault > 0) { Economy eco = getEconomy(); if (eco != null) eco.withdrawPlayer(player, r.costVault); }
        for (String s : r.costItems) removeItem(player, s);

        List<String> got = new ArrayList<>();
        for (Map.Entry<String, Integer> e : r.outputCards.entrySet())
            for (int i = 0; i < e.getValue(); i++) {
                giveCard(player, e.getKey(), keepLv, keepStar);
                got.add(e.getKey());
            }
        if (!r.randomPool.isEmpty()) {
            String picked = pickRandom(r.randomPool);
            if (picked != null) { giveCard(player, picked, keepLv, keepStar); got.add(picked); }
        }

        // 广播
        if (r.broadcast != null && !r.broadcast.isEmpty()) {
            try {
                String bc = r.broadcast.replace("{player}", player.getName())
                                       .replace("{card}", String.join(", ", got));
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', bc));
            } catch (Throwable ignored) {}
        }

        try { CollectionChecker.check(player); } catch (Throwable ignored) {}
        String gotStr = String.join("§7, §e", got);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        try {
            player.sendTitle("§a§l合成成功", "§e獲得 §f" + gotStr, 10, 40, 10);
        } catch (Throwable ignored) {}
        PlayerData.incrementMergeCount(player.getUniqueId());
        try { com.longdrange.ldattribute.achievement.AchievementChecker.addProgress(player, com.longdrange.ldattribute.achievement.AchievementConfig.Type.MERGE, 1); } catch (Throwable ignored) {}
        PlayerData.savePlayer(player.getUniqueId());
        return "§a合成成功！獲得: §e" + gotStr;
    }

    private static void giveCard(Player player, String cardId, int keepLv, int keepStar) {
        CardData card = CardDataManager.getCard(cardId);
        if (card == null) return;
        ItemStack item = card.getItem();
        if (keepLv > 0) {
            item = CardNBT.setLevel(item, keepLv);
            item = CardLevel.recalc(item, cardId, keepLv, CardNBT.getExp(item));
        }
        if (keepStar > 0) {
            item = CardNBT.setStar(item, keepStar);
            item = CardLevel.recalc(item, cardId, CardNBT.getLevel(item), CardNBT.getExp(item));
        }
        // 直接给玩家背包
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
            player.sendMessage("§e背包已满，产物掉落在脚下！");
        } else {
            player.getInventory().addItem(item);
        }
    }

    private static int[] findMaxLevelStar(Player player, String cardId) {
        int maxLv = 0, maxStar = 0;
        try {
            List<ItemStack> cards = PlayerData.getCards(player);
            for (ItemStack it : cards) {
                CardData cd = CardDataManager.findCard(it);
                if (cd == null || !cd.getId().equals(cardId)) continue;
                int lv = CardNBT.getLevel(it);
                int st = CardNBT.getStar(it);
                if (lv > maxLv) maxLv = lv;
                if (st > maxStar) maxStar = st;
            }
        } catch (Throwable ignored) {}
        return new int[]{maxLv, maxStar};
    }

    private static String pickRandom(Map<String, Integer> pool) {
        int total = 0;
        for (int w : pool.values()) total += w;
        if (total <= 0) return null;
        int roll = new Random().nextInt(total);
        int cur = 0;
        for (Map.Entry<String, Integer> e : pool.entrySet()) {
            cur += e.getValue();
            if (roll < cur) return e.getKey();
        }
        return pool.keySet().iterator().next();
    }

    private static boolean hasItem(Player player, String s) {
        String[] p = s.split(":");
        Material mat = Material.getMaterial(p[0].toUpperCase());
        if (mat == null) return false;
        int need = p.length >= 2 ? Integer.parseInt(p[1]) : 1;
        int c = 0;
        for (ItemStack is : player.getInventory().getContents())
            if (is != null && is.getType() == mat) c += is.getAmount();
        return c >= need;
    }

    private static void removeItem(Player player, String s) {
        String[] p = s.split(":");
        Material mat = Material.getMaterial(p[0].toUpperCase());
        if (mat == null) return;
        int need = p.length >= 2 ? Integer.parseInt(p[1]) : 1;
        int removed = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack is = contents[i];
            if (is == null || is.getType() != mat) continue;
            int take = Math.min(is.getAmount(), need - removed);
            is.setAmount(is.getAmount() - take);
            removed += take;
            if (is.getAmount() <= 0) player.getInventory().setItem(i, null);
            if (removed >= need) break;
        }
    }

    private static Economy getEconomy() {
        try {
            org.bukkit.plugin.RegisteredServiceProvider<Economy> rsp =
                    Bukkit.getServicesManager().getRegistration(Economy.class);
            return rsp != null ? rsp.getProvider() : null;
        } catch (Throwable t) { return null; }
    }

    // ==================== 进阶材料检查（满星满等） ====================

    private static int countQualified(Player player, String cardId, RecipeConfig.Recipe r) {
        int count = 0;
        try {
            List<ItemStack> bagCards = PlayerData.getCards(player);
            for (ItemStack it : bagCards) {
                CardData cd = CardDataManager.findCard(it);
                if (cd == null || !cd.getId().equals(cardId)) continue;
                if (qualified(it, cardId, r)) count++;
            }
        } catch (Throwable ignored) {}
        for (ItemStack it : player.getInventory().getContents()) {
            if (it == null) continue;
            CardData cd = CardDataManager.findCard(it);
            if (cd == null || !cd.getId().equals(cardId)) continue;
            if (qualified(it, cardId, r)) count++;
        }
        return count;
    }

    private static boolean qualified(ItemStack card, String cardId, RecipeConfig.Recipe r) {
        if (r.requireMaxLevel) {
            CardLevelConfig.CardLevel cl = CardLevelConfig.get(cardId);
            if (cl == null) return false;
            if (CardNBT.getLevel(card) < cl.maxLevel) return false;
        }
        if (r.requireMaxStar) {
            int maxStar = StarConfig.getMaxStarFor(cardId);
            if (maxStar > 0 && CardNBT.getStar(card) < maxStar) return false;
        }
        return true;
    }

    private static void removeQualified(Player player, String cardId, int amount, RecipeConfig.Recipe r) {
        int removed = 0;
        try {
            int pages = PlayerData.getUnlockedPages(player.getUniqueId());
            for (int p = 0; p < pages && removed < amount; p++) {
                ItemStack[] items = PlayerData.getPageInventory(player.getUniqueId(), p);
                boolean changed = false;
                for (int i = 0; i < items.length && removed < amount; i++) {
                    ItemStack it = items[i];
                    if (it == null) continue;
                    CardData cd = CardDataManager.findCard(it);
                    if (cd == null || !cd.getId().equals(cardId)) continue;
                    if (!qualified(it, cardId, r)) continue;
                    items[i] = null;
                    removed++;
                    changed = true;
                }
                if (changed) PlayerData.setPageInventory(player.getUniqueId(), p, items);
            }
        } catch (Throwable ignored) {}
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && removed < amount; i++) {
            ItemStack it = contents[i];
            if (it == null) continue;
            CardData cd = CardDataManager.findCard(it);
            if (cd == null || !cd.getId().equals(cardId)) continue;
            if (!qualified(it, cardId, r)) continue;
            int take = Math.min(it.getAmount(), amount - removed);
            it.setAmount(it.getAmount() - take);
            removed += take;
            if (it.getAmount() <= 0) player.getInventory().setItem(i, null);
        }
    }
}
