package com.longdrange.ldattribute.card;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class StatsDataRead {

    public static final String SEPARATOR = "======";

    public static List<String> filterNormalLore(ItemStack item) {
        List<String> result = new ArrayList<>();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return result;
        for (String line : item.getItemMeta().getLore()) {
            if (line.contains(SEPARATOR)) break;
            result.add(line);
        }
        return result;
    }

    /** 檢查卡片是否對該玩家生效（綁定別人的卡無效） */
    private static boolean isUsableBy(ItemStack card, Player player) {
        if (!CardNBT.isBound(card)) return true;
        String boundUUID = CardNBT.getBoundUUID(card);
        return boundUUID.equals(player.getUniqueId().toString());
    }

    public static LDAttributeData loadPlayerStats(Player player) {
        List<ItemStack> all = PlayerData.getCards(player);
        List<ItemStack> valid = new ArrayList<>();
        for (ItemStack card : all) {
            if (!isUsableBy(card, player)) continue;
            valid.add(card);
        }

        LDAttributeData statsData = new LDAttributeData();
        for (ItemStack card : valid) {
            List<String> normalLore = filterNormalLore(card);
            statsData.add(LDAttribute.getInstance().getApi().getLoreData(player, null, normalLore));
            // 符文属性（从卡片插槽读）
            try {
                List<String> runeAttrs = com.longdrange.ldattribute.rune.RuneHelper.getCardSocketAttributes(card);
                if (!runeAttrs.isEmpty()) {
                    statsData.add(LDAttribute.getInstance().getApi().getLoreData(player, null, runeAttrs));
                }
            } catch (Throwable ignored) {}
        }
        SuitData.applySuits(player, valid, statsData);
        SynergyData.apply(player, valid, statsData);

        // 宠物属性（宠物背包里所有宠物都生效）
        try {
            List<String> petAttrs = com.longdrange.ldattribute.pet.PetManager.getAllPetsAttributes(player);
            if (!petAttrs.isEmpty()) {
                statsData.add(LDAttribute.getInstance().getApi().getLoreData(player, null, petAttrs));
            }
        } catch (Throwable ignored) {}
        // 战斗状态属性
        try {
            List<String> stateAttrs = com.longdrange.ldattribute.combat.StateManager.getActiveAttributes(player);
            if (!stateAttrs.isEmpty()) {
                statsData.add(LDAttribute.getInstance().getApi().getLoreData(player, null, stateAttrs));
            }
        } catch (Throwable ignored) {}
        // 限時 buff 加成
        List<String> buffLore = TempBuffManager.getActiveEffects(player.getUniqueId());
        if (!buffLore.isEmpty()) {
            statsData.add(LDAttribute.getInstance().getApi().getLoreData(player, null, buffLore));
        }
        return statsData;
    }

    public static void updatePlayer(Player player) {
        LDAttributeData data = loadPlayerStats(player);
        LDAttribute.getInstance().getApi().setEntityAPIData(
                LDAttribute.class, player.getUniqueId(), data);
        LDAttribute.getInstance().getApi().updateHandData(player);
    }

    // ==================== 合併刷新（性能優化）====================

    private static final java.util.Set<java.util.UUID> pendingRefresh =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public static void scheduleUpdate(Player player) {
        if (player == null || !player.isOnline()) return;
        final java.util.UUID uuid = player.getUniqueId();
        if (!pendingRefresh.add(uuid)) return;

        try {
            Bukkit.getScheduler().runTask(
                LDAttribute.getInstance(),
                () -> {
                    pendingRefresh.remove(uuid);
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        try { updatePlayer(p); } catch (Throwable ignored) {}
                    }
                }
            );
        } catch (Throwable t) {
            pendingRefresh.remove(uuid);
        }
    }
}
