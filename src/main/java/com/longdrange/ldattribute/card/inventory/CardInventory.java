package com.longdrange.ldattribute.card.inventory;

import com.longdrange.ldattribute.card.PageConfig;
import com.longdrange.ldattribute.card.PlayerData;
import com.longdrange.ldattribute.card.StatsDataRead;
import com.longdrange.ldattribute.points.PointAPI;
import com.longdrange.ldattribute.util.Config;
import com.longdrange.ldattribute.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CardInventory {

    public static int SLOT_PREV = 45;
    public static int SLOT_COLLECTION = 46;
    public static int SLOT_SELL = 47;
    public static int SLOT_INFO = 49;
    public static int SLOT_SUIT = 51;
    public static int SLOT_STATS = 48;
    public static int SLOT_RANK = 50;
    public static int SLOT_NEXT = 53;

    public static void open(Player player) {
        open(player, PlayerData.getCurrentPage(player.getUniqueId()));
    }

    public static void open(Player player, int page) {
        int total = PageConfig.getPageCount();
        if (total == 0) {
            player.sendMessage(Message.get("Prefix") + "§c沒有設定任何卡片頁面！");
            return;
        }
        if (page < 0) page = 0;
        if (page >= total) page = total - 1;
        PlayerData.setCurrentPage(player.getUniqueId(), page);

        PageConfig.Page cfg = PageConfig.getPage(page);
        if (cfg == null) return;

        boolean unlocked = isPageUnlocked(player, page);
        String baseName = Message.get("Inventory.Open.Name");
        String title = baseName + " §7(" + (page + 1) + "/" + total + ")";
        if (title.replaceAll("§.", "").length() > 32) title = baseName;
        Inventory inv = Bukkit.createInventory(null, 54, title);

        int slots = Math.min(cfg.slots, 45);
        if (unlocked) {
            ItemStack[] items = PlayerData.getPageInventory(player.getUniqueId(), page);
            int unlockedSlots = PlayerData.getOrInitPageUnlockedSlots(
                    player.getUniqueId(), page, cfg.defaultSlots);
            for (int i = 0; i < slots; i++) {
                if (i < unlockedSlots) {
                    inv.setItem(i, items[i]);
                } else {
                    inv.setItem(i, createSlotLockedGlass(cfg, unlockedSlots));
                }
            }
        } else {
            ItemStack locked = createLockedSlot();
            for (int i = 0; i < slots; i++) inv.setItem(i, locked);
        }

        ItemStack sep = createSeparator();
        for (int i = slots; i < 45; i++) inv.setItem(i, sep);
        for (int i = 45; i < 54; i++) {
            if (i == SLOT_PREV || i == SLOT_COLLECTION || i == SLOT_SELL || i == SLOT_INFO || i == SLOT_SUIT || i == SLOT_NEXT || i == SLOT_STATS || i == SLOT_RANK) continue;
            inv.setItem(i, sep);
        }

        if (page > 0)
            inv.setItem(SLOT_PREV, createNav("§e§l◀ 上一頁", "§7點擊前往第 " + page + " 頁"));
        if (page < total - 1)
            inv.setItem(SLOT_NEXT, createNav("§e§l下一頁 ▶", "§7點擊前往第 " + (page + 2) + " 頁"));

        inv.setItem(SLOT_COLLECTION, createCollectionButton());
        inv.setItem(SLOT_SELL, createSellButton());
        inv.setItem(SLOT_SUIT, createSuitButton());
        inv.setItem(SLOT_RANK, createRankButton());

        if (unlocked) inv.setItem(SLOT_INFO, createInfoButton(player, page, total, slots));
        else inv.setItem(SLOT_INFO, createUnlockButton(cfg));

        player.openInventory(inv);
    }

    public static void save(Player player, Inventory inv, int page) {
        PageConfig.Page cfg = PageConfig.getPage(page);
        if (cfg == null) return;
        if (!isPageUnlocked(player, page)) return;
        int slots = Math.min(cfg.slots, 45);
        int unlockedSlots = PlayerData.getOrInitPageUnlockedSlots(
                player.getUniqueId(), page, cfg.defaultSlots);
        if (unlockedSlots > slots) unlockedSlots = slots;

        ItemStack[] items = new ItemStack[45];
        // 只保存已解锁格内的卡（防止红玻璃/占位符被存进去）
        for (int i = 0; i < unlockedSlots; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && it.getType() == Material.STAINED_GLASS_PANE) it = null;
            items[i] = it;
        }
        PlayerData.setPageInventory(player.getUniqueId(), page, items);
        PlayerData.savePlayer(player.getUniqueId());
        try { StatsDataRead.updatePlayer(player); } catch (Throwable ignored) {}
    }

    public static boolean isPageUnlocked(Player player, int page) {
        if (page < 0) return false;
        if (page == 0) return true;
        return PlayerData.getUnlockedPages(player.getUniqueId()) > page;
    }

    public static boolean isCardInventory(String title) {
        return title.startsWith(Message.get("Inventory.Open.Name"));
    }

    private static ItemStack createNav(String name, String lore) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createInfoButton(Player player, int page, int total, int slots) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a§l第 " + (page + 1) + " / " + total + " 頁");
        List<String> lore = new ArrayList<>();
        lore.add("§7可用格數: §e" + slots);
        lore.add("§7點券: §6" + PointAPI.getPlayerPoints(player.getName()));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createUnlockButton(PageConfig.Page cfg) {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c§l🔒 未解鎖的頁面");
        List<String> lore = new ArrayList<>();
        lore.add("§7解鎖條件：");
        if (cfg.costPoints > 0) lore.add("§7- 點券: §6" + cfg.costPoints);
        if (cfg.costVault > 0)  lore.add("§7- 金幣: §6" + (int) cfg.costVault);
        if (cfg.permission != null && !cfg.permission.isEmpty())
            lore.add("§7- 權限: §e" + cfg.permission);
        for (String s : cfg.items) lore.add("§7- 物品: §e" + s);
        lore.add("");
        lore.add("§e點擊解鎖");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createSlotLockedGlass(PageConfig.Page cfg, int currentUnlocked) {
        PageConfig.SlotTier tier = cfg.getNextTier(currentUnlocked);
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c§l🔒 未解鎖的格子");
        List<String> lore = new ArrayList<>();
        lore.add("§7已解鎖: §e" + currentUnlocked + "§7/§e" + cfg.slots);
        if (tier != null) {
            lore.add("");
            lore.add("§7下一檔解鎖 §e" + tier.count + " §7格");
            lore.add("§7解鎖條件:");
            if (tier.costPoints > 0) lore.add("§7- 點券: §6" + tier.costPoints);
            if (tier.costVault > 0)  lore.add("§7- 金幣: §6" + (int) tier.costVault);
            if (tier.permission != null && !tier.permission.isEmpty())
                lore.add("§7- 權限: §e" + tier.permission);
            for (String s : tier.items) lore.add("§7- 物品: §e" + s);
            lore.add("");
            lore.add("§e點擊解鎖");
        } else {
            lore.add("§7所有格子已解鎖");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    private static ItemStack createLockedSlot() {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§7🔒 未解鎖");
        meta.setLore(Arrays.asList("§7點擊底下中間的紅色玻璃查看解鎖條件"));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createSeparator() {
        int id = Config.getInt("CardInventory.Separator", 160);
        short data = (short) Config.getInt("CardInventory.SeparatorData", 15);
        Material mat = Material.getMaterial(id);
        if (mat == null) mat = Material.STAINED_GLASS_PANE;
        ItemStack item = new ItemStack(mat, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createSellButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Message.get("Inventory.Card.SellCard.Name"));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createCollectionButton() {
        ItemStack item = new ItemStack(Material.BOOK_AND_QUILL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d§l📖 卡片圖鑑");
        meta.setLore(Arrays.asList("§7查看已收集的卡片"));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createStatsButton() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b§l✦ 屬性面板");
        meta.setLore(Arrays.asList("§7查看你的全部屬性"));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createRankButton() {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§l✦ 排行榜");
        meta.setLore(Arrays.asList("§7卡片等級 / 點券榜"));
        item.setItemMeta(meta);
        return item;
    }
    private static ItemStack createSuitButton() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Message.get("Inventory.Card.SuitList.Name"));
        item.setItemMeta(meta);
        return item;
    }
}
