package com.longdrange.ldattribute.card.inventory;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.card.CardData;
import com.longdrange.ldattribute.card.CardDataManager;
import com.longdrange.ldattribute.card.CardLevelConfig;
import com.longdrange.ldattribute.card.CardNBT;
import com.longdrange.ldattribute.card.PlayerData;
import com.longdrange.ldattribute.points.PointAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * 排行榜 GUI（卡片等级 / 点券）
 */
public class RankInventory {

    public static int SLOT_BACK = 49;
    public static int SLOT_PREV = 45;
    public static int SLOT_NEXT = 53;
    public static int SLOT_TAB_CARD = 48;
    public static int SLOT_TAB_POINTS = 50;

    public static void open(Player player) { open(player, "CARD", 1); }

    public static void open(Player player, String type, int page) {
        String title = type.equals("POINTS") ? "§8§l✦ 点券排行榜" : "§8§l✦ 卡片等级排行榜";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        List<Map.Entry<String, Integer>> sorted = type.equals("POINTS")
                ? getPointsRanking() : getCardLevelRanking();

        int perPage = 45;
        int totalPages = Math.max(1, (sorted.size() + perPage - 1) / perPage);
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, sorted.size());

        for (int i = start; i < end; i++) {
            int rank = i + 1;
            Map.Entry<String, Integer> e = sorted.get(i);
            int slot = i - start;

            ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            try {
                OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(e.getKey()));
                meta.setOwningPlayer(op);
            } catch (Throwable ignored) {}
            String name = getPlayerName(e.getKey());
            String rankPrefix = rank == 1 ? "§6§l✦ " : rank == 2 ? "§e§l✦ " : rank == 3 ? "§c§l✦ " : "§7#" + rank + " ";
            meta.setDisplayName(rankPrefix + "§f" + name);
            List<String> lore = new ArrayList<>();
            if (type.equals("POINTS")) {
                lore.add("§7点券: §6" + e.getValue());
            } else {
                lore.add("§7总等级: §6" + e.getValue());
            }
            meta.setLore(lore);
            head.setItemMeta(meta);
            inv.setItem(slot, head);
        }

        // 底部
        ItemStack sep = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta sepMeta = sep.getItemMeta();
        sepMeta.setDisplayName(" ");
        sep.setItemMeta(sepMeta);
        for (int i = 45; i < 54; i++) {
            if (i == SLOT_BACK || i == SLOT_PREV || i == SLOT_NEXT || i == SLOT_TAB_CARD || i == SLOT_TAB_POINTS) continue;
            inv.setItem(i, sep);
        }

        // 切换 tab
        ItemStack tabCard = new ItemStack(Material.PAPER);
        ItemMeta tcm = tabCard.getItemMeta();
        tcm.setDisplayName(type.equals("CARD") ? "§a§l✦ 卡片等级榜" : "§7卡片等级榜");
        tabCard.setItemMeta(tcm);
        inv.setItem(SLOT_TAB_CARD, tabCard);

        ItemStack tabPoints = new ItemStack(Material.GOLD_INGOT);
        ItemMeta tpm = tabPoints.getItemMeta();
        tpm.setDisplayName(type.equals("POINTS") ? "§a§l✦ 点券榜" : "§7点券榜");
        tabPoints.setItemMeta(tpm);
        inv.setItem(SLOT_TAB_POINTS, tabPoints);

        // 翻页
        if (page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pm = prev.getItemMeta();
            pm.setDisplayName("§e§l◀ 上一页");
            pm.setLore(Arrays.asList("§7第 " + (page - 1) + " / " + totalPages + " 页"));
            prev.setItemMeta(pm);
            inv.setItem(SLOT_PREV, prev);
        }
        if (page < totalPages) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nm = next.getItemMeta();
            nm.setDisplayName("§e§l下一页 ▶");
            nm.setLore(Arrays.asList("§7第 " + (page + 1) + " / " + totalPages + " 页"));
            next.setItemMeta(nm);
            inv.setItem(SLOT_NEXT, next);
        }

        // 返回
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName("§c§l✘ 关闭");
        back.setItemMeta(bm);
        inv.setItem(SLOT_BACK, back);

        player.openInventory(inv);
    }

    // ==================== 数据 ====================

    private static List<Map.Entry<String, Integer>> getCardLevelRanking() {
        Map<String, Integer> totals = new HashMap<>();
        for (String uuid : PlayerData.getAllPlayerUUIDs()) {
            int unlocked = PlayerData.getUnlockedPagesFromFile(uuid);
            int total = 0;
            for (int p = 0; p < unlocked; p++) {
                ItemStack[] items = PlayerData.getPageInventoryFromFile(uuid, p);
                for (ItemStack it : items) {
                    if (it == null) continue;
                    CardData cd = CardDataManager.findCard(it);
                    if (cd == null) continue;
                    if (!CardLevelConfig.isUpgradable(cd.getId())) continue;
                    total += CardNBT.getLevel(it);
                }
            }
            totals.put(uuid, total);
        }
        return sortMap(totals);
    }

    private static List<Map.Entry<String, Integer>> getPointsRanking() {
        Map<String, Integer> pts = new HashMap<>();
        try {
            // 从 PointData 读取所有玩家点券
            for (String uuid : PlayerData.getAllPlayerUUIDs()) {
                String name = getPlayerName(uuid);
                int p = PointAPI.getPlayerPoints(name);
                pts.put(uuid, p);
            }
        } catch (Throwable ignored) {}
        return sortMap(pts);
    }

    private static List<Map.Entry<String, Integer>> sortMap(Map<String, Integer> map) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(map.entrySet());
        list.removeIf(e -> e.getValue() == 0);
        list.sort((a, b) -> b.getValue() - a.getValue());
        return list;
    }

    private static String getPlayerName(String uuid) {
        try {
            OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
            if (op != null && op.getName() != null) return op.getName();
        } catch (Throwable ignored) {}
        return uuid.substring(0, 8) + "...";
    }

    public static boolean isRankInventory(String title) {
        return title.equals("§8§l✦ 点券排行榜") || title.equals("§8§l✦ 卡片等级排行榜");
    }

    public static String getType(String title) {
        return title.equals("§8§l✦ 点券排行榜") ? "POINTS" : "CARD";
    }
}