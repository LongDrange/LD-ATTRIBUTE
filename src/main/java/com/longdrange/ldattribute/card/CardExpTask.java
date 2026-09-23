package com.longdrange.ldattribute.card;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CardExpTask extends BukkitRunnable {

    private final LDAttribute plugin;
    /** 每張卡上次結算時間戳（內存，重啟會清） */
    private static final Map<UUID, Map<String, Long>> lastTicks = new HashMap<>();

    public CardExpTask(LDAttribute plugin) { this.plugin = plugin; }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            try { process(player, now); } catch (Throwable ignored) {}
        }
    }

    private void process(Player player, long now) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> ticks = lastTicks.computeIfAbsent(uuid, k -> new HashMap<>());
        int unlocked = PlayerData.getUnlockedPages(uuid);
        boolean dirty = false;

        for (int page = 0; page < unlocked; page++) {
            ItemStack[] items = PlayerData.getPageInventory(uuid, page);
            boolean pageDirty = false;
            for (int i = 0; i < items.length; i++) {
                ItemStack card = items[i];
                if (card == null) continue;
                CardData cd = CardDataManager.findCard(card);
                if (cd == null) continue;
                CardLevelConfig.CardLevel cl = CardLevelConfig.get(cd.getId());
                if (cl == null || cl.timeOnlineInterval <= 0) continue;
                // 綁定檢查
                if (CardNBT.isBound(card) && !CardNBT.getBoundUUID(card).equals(uuid.toString())) continue;
                // 等級檢查
                if (CardLevel.isMaxLevel(cd.getId(), CardNBT.getLevel(card))) continue;

                String key = cd.getId();
                Long last = ticks.get(key);
                if (last == null) { ticks.put(key, now); continue; }
                long intervalMs = cl.timeOnlineInterval * 1000L;
                long elapsed = now - last;
                if (elapsed < intervalMs) continue;
                int times = (int) (elapsed / intervalMs);
                int exp = times * cl.timeOnlineExp;
                CardLevel.Result r = CardLevel.addExp(card, cd.getId(), exp);
                items[i] = r.item;
                pageDirty = true;
                dirty = true;
                ticks.put(key, last + times * intervalMs);
            }
            if (pageDirty) PlayerData.setPageInventory(uuid, page, items);
        }
        if (dirty) PlayerData.savePlayer(uuid);
    }

    public static void clearPlayer(UUID uuid) { lastTicks.remove(uuid); }
}
