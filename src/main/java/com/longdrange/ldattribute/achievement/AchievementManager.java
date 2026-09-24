package com.longdrange.ldattribute.achievement;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 成就奖励发放（统一入口）
 * GUI 点击 / 聊天栏点击 / 命令 → 都调 claim()
 */
public class AchievementManager {

    /**
     * 尝试领取成就奖励
     * @return true = 发放成功；false = 未完成/已领/不存在
     */
    public static boolean claim(Player player, String achId) {
        if (player == null || achId == null) return false;
        UUID uuid = player.getUniqueId();

        AchievementConfig.Achievement a = AchievementConfig.get(achId);
        if (a == null) return false;

        if (!AchievementData.isCompleted(uuid, achId)) return false;
        if (AchievementData.isClaimed(uuid, achId)) return false;

        // 标记已领（先标，防止重复发放）
        AchievementData.markClaimed(uuid, achId);

        boolean gaveAnything = false;

        // 1) 点券
        try {
            if (a.rewardPoints > 0) {
                com.longdrange.ldattribute.points.PointAPI.addPlayerPoints(
                        player.getName(), a.rewardPoints);
                player.sendMessage("§8[§6成就§8] §a+§e" + a.rewardPoints + " §a点券");
                gaveAnything = true;
            }
        } catch (Throwable ignored) {}

        // 2) 卡片
        try {
            for (String spec : a.rewardCards) {
                if (spec == null || spec.isEmpty()) continue;
                String cardId = spec;
                int count = 1;
                int colon = spec.indexOf(':');
                if (colon > 0) {
                    cardId = spec.substring(0, colon).trim();
                    try { count = Integer.parseInt(spec.substring(colon + 1).trim()); }
                    catch (Exception ignored) {}
                }
                com.longdrange.ldattribute.card.CardData cd =
                        com.longdrange.ldattribute.card.CardDataManager.getCard(cardId);
                if (cd == null) continue;
                for (int i = 0; i < count; i++) {
                    org.bukkit.inventory.ItemStack it = cd.getItem();
                    java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> left =
                            player.getInventory().addItem(it);
                    for (org.bukkit.inventory.ItemStack drop : left.values())
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                player.sendMessage("§8[§6成就§8] §a获得卡片 §e" + cardId + " ×" + count);
                gaveAnything = true;
            }
        } catch (Throwable ignored) {}

        // 3) 符文
        try {
            for (String spec : a.rewardRunes) {
                if (spec == null || spec.isEmpty()) continue;
                String runeId = spec;
                int count = 1;
                int colon = spec.indexOf(':');
                if (colon > 0) {
                    runeId = spec.substring(0, colon).trim();
                    try { count = Integer.parseInt(spec.substring(colon + 1).trim()); }
                    catch (Exception ignored) {}
                }
                com.longdrange.ldattribute.rune.RuneConfig.Rune r =
                        com.longdrange.ldattribute.rune.RuneConfig.getRune(runeId);
                if (r == null) continue;
                org.bukkit.inventory.ItemStack it =
                        com.longdrange.ldattribute.rune.RuneItem.create(r, count);
                java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> left =
                        player.getInventory().addItem(it);
                for (org.bukkit.inventory.ItemStack drop : left.values())
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                player.sendMessage("§8[§6成就§8] §a获得符文 §e" + runeId + " ×" + count);
                gaveAnything = true;
            }
        } catch (Throwable ignored) {}

        // 4) 命令（{player} 替换）
        try {
            for (String cmd : a.rewardCommands) {
                if (cmd == null || cmd.isEmpty()) continue;
                String real = cmd.replace("{player}", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), real);
                gaveAnything = true;
            }
        } catch (Throwable ignored) {}

        // 5) 播放音效
        try {
            player.playSound(player.getLocation(),
                    org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
        } catch (Throwable ignored) {}

        player.sendMessage("§8[§6成就§8] §a✓ 已领取 §e" + a.name + " §a的奖励");
        AchievementData.save(uuid);
        return true;
    }
}