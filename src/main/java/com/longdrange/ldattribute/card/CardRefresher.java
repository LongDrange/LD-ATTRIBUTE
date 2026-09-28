package com.longdrange.ldattribute.card;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 卡片刷新器
 *  - 遍历在线玩家所有卡片
 *  - 按 cardId 找到新模板，保留 NBT（等级/经验/星/绑定/符文/法术）
 *  - 更新显示名 + Lore
 */
public class CardRefresher {

    /** 刷新单个玩家，返回实际刷新的卡片数 */
    public static int refreshPlayer(Player player) {
        if (player == null || !player.isOnline()) return 0;
        int count = 0;
        int unlocked = PlayerData.getUnlockedPages(player.getUniqueId());
        for (int page = 0; page < unlocked; page++) {
            ItemStack[] items = PlayerData.getPageInventory(player.getUniqueId(), page);
            boolean changed = false;
            for (int i = 0; i < items.length; i++) {
                ItemStack old = items[i];
                if (old == null) continue;
                CardData cd = findCardData(old);
                if (cd == null) continue;

                ItemStack rebuilt = rebuild(old, cd);
                if (rebuilt == null) continue;
                items[i] = rebuilt;
                changed = true;
                count++;
            }
            if (changed) {
                PlayerData.setPageInventory(player.getUniqueId(), page, items);
            }
        }
        if (count > 0) {
            PlayerData.savePlayer(player.getUniqueId());
            try { StatsDataRead.updatePlayer(player); } catch (Throwable ignored) {}
        }
        return count;
    }

    /** 刷新所有在线玩家，返回总卡片数 */
    public static int refreshAll() {
        int total = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            total += refreshPlayer(p);
        }
        return total;
    }

    /**
     * 从旧卡片找 CardData：
     *   1. 优先用 cardId NBT
     *   2. 没有则按 DisplayName 匹配
     */
    private static CardData findCardData(ItemStack old) {
        if (old == null) return null;
        // 1. cardId NBT
        try {
            String id = CardNBT.getCardId(old);
            if (id != null && !id.isEmpty()) {
                CardData cd = CardDataManager.getCard(id);
                if (cd != null) return cd;
            }
        } catch (Throwable ignored) {}
        // 2. 名字匹配
        return CardDataManager.findCard(old);
    }

    /**
     * 用新模板重建卡片，保留所有 NBT 数据
     */
    private static ItemStack rebuild(ItemStack old, CardData cd) {
        if (cd == null) return null;
        ItemStack fresh = cd.getItem().clone();

        // ===== 保留 NBT =====
        int level = CardNBT.getLevel(old);
        int exp = CardNBT.getExp(old);
        int star = CardNBT.getStar(old);
        boolean locked = CardNBT.isLocked(old);
        String boundUUID = CardNBT.getBoundUUID(old);
        String boundName = CardNBT.getBoundName(old);
        String spell = CardNBT.getSpell(old);
        int spellLevel = CardNBT.getSpellLevel(old);
        boolean runeLocked = CardNBT.isRuneLocked(old);
        boolean wasReady = CardNBT.isReady();

        // 重建
        fresh = CardNBT.setCardId(fresh, cd.getId());
        fresh = CardNBT.setLevel(fresh, level);
        fresh = CardNBT.setExp(fresh, exp);
        fresh = CardNBT.setStar(fresh, star);
        if (locked) fresh = CardNBT.setLocked(fresh, true);
        if (boundUUID != null && !boundUUID.isEmpty()) {
            fresh = CardNBT.setBound(fresh, boundUUID, boundName);
        }
        if (spell != null && !spell.isEmpty()) {
            fresh = CardNBT.setSpell(fresh, spell);
            fresh = CardNBT.setSpellLevel(fresh, spellLevel);
        }
        if (runeLocked) fresh = CardNBT.setRuneLocked(fresh, true);

        // ===== 符文：逐孔迁移 =====
        try {
            int maxSockets = 27;   // 保险最大值
            for (int i = 0; i < maxSockets; i++) {
                String runeId = CardNBT.getSocketRune(old, i);
                if (runeId != null && !runeId.isEmpty()) {
                    fresh = CardNBT.setSocketRune(fresh, i, runeId);
                    if (CardNBT.isSocketUnlocked(old, i)) {
                        fresh = CardNBT.unlockSocket(fresh, i);
                    }
                }
            }
        } catch (Throwable ignored) {}

        // ===== 重新计算 Lore（等级/经验/星） =====
        try {
            if (CardLevelConfig.isUpgradable(cd.getId())) {
                fresh = CardLevel.recalc(fresh, cd.getId(), level, exp);
            }
        } catch (Throwable ignored) {}

        return fresh;
    }

    /** 刷新提示（给玩家发消息） */
    public static void refreshWithMessage(Player player) {
        int n = refreshPlayer(player);
        if (n > 0) {
            player.sendMessage(ChatColor.GREEN + "[卡片] 已更新 " + n + " 张卡片（配置同步）");
        } else {
            player.sendMessage(ChatColor.GRAY + "[卡片] 没有需要更新的卡片");
        }
    }
}