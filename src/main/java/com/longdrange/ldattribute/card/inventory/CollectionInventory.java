package com.longdrange.ldattribute.card.inventory;

import com.longdrange.ldattribute.card.CardData;
import com.longdrange.ldattribute.card.CardDataManager;
import com.longdrange.ldattribute.card.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class CollectionInventory {
    public static final String TITLE_PREFIX = "§8[§b圖鑑§8] ";
    public static final int PER_PAGE = 45;
    public static int SLOT_BACK = 48;
    public static int SLOT_PREV = 45;
    public static int SLOT_INFO = 49;
    public static int SLOT_NEXT = 53;

    private static final java.util.Map<java.util.UUID, Integer> lastPage = new java.util.HashMap<>();

    public static void open(Player player) { open(player, 0); }

    public static void open(Player player, int page) {
        List<CardData> all = new ArrayList<>(CardDataManager.getAllCardOnly());
        int total = all.size();
        if (total == 0) { player.sendMessage("§c尚未載入任何卡片！"); return; }
        int maxPage = (total + PER_PAGE - 1) / PER_PAGE - 1;
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;

        Set<String> owned = PlayerData.getOwnedCardIds(player.getUniqueId());
        int ownedCount = 0;
        for (CardData c : all) if (owned.contains(c.getId())) ownedCount++;

        String title = TITLE_PREFIX + "已收集 " + ownedCount + "/" + total;
        if (title.replaceAll("§.", "").length() > 32)
            title = TITLE_PREFIX + ownedCount + "/" + total;

        Inventory inv = Bukkit.createInventory(null, 54, title);
        lastPage.put(player.getUniqueId(), page);
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = start + i;
            if (idx >= total) break;
            CardData card = all.get(idx);
            boolean has = owned.contains(card.getId());
            ItemStack display;
            if (has) {
                display = card.getItem().clone();
                ItemMeta meta = display.getItemMeta();
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("§a✔ 已收集");
                meta.setLore(lore);
                display.setItemMeta(meta);
            } else {
                display = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
                ItemMeta meta = display.getItemMeta();
                meta.setDisplayName("§7" + card.getId());
                meta.setLore(Arrays.asList("§7未收集"));
                display.setItemMeta(meta);
            }
            inv.setItem(i, display);
        }

        ItemStack sep = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta sepM = sep.getItemMeta(); sepM.setDisplayName(" "); sep.setItemMeta(sepM);
        for (int i = 45; i < 54; i++) {
            if (i == SLOT_BACK || i == SLOT_PREV || i == SLOT_INFO || i == SLOT_NEXT) continue;
            inv.setItem(i, sep);
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName("§e§l◀ 返回卡片背包");
        back.setItemMeta(bm);
        inv.setItem(SLOT_BACK, back);

        if (page > 0) {
            ItemStack p = new ItemStack(Material.PAPER);
            ItemMeta pm = p.getItemMeta(); pm.setDisplayName("§e上一頁"); p.setItemMeta(pm);
            inv.setItem(SLOT_PREV, p);
        }
        if (page < maxPage) {
            ItemStack p = new ItemStack(Material.PAPER);
            ItemMeta pm = p.getItemMeta(); pm.setDisplayName("§e下一頁"); p.setItemMeta(pm);
            inv.setItem(SLOT_NEXT, p);
        }

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§a收集進度");
        // 進度條
        int percent = total == 0 ? 0 : (int) (ownedCount * 100L / total);
        int filled = percent / 10;
        StringBuilder bar = new StringBuilder();
        bar.append("§a");
        for (int b = 0; b < filled; b++) bar.append("\u2588");
        bar.append("§7");
        for (int b = filled; b < 10; b++) bar.append("\u2588");
        bar.append(" §e").append(percent).append("%");
        im.setLore(Arrays.asList(
                "§7已收集: §e" + ownedCount + "§7/§e" + total,
                "§7第 §e" + (page + 1) + "§7/§e" + (maxPage + 1) + " §7頁",
                "",
                bar.toString()
        ));
        info.setItemMeta(im);
        inv.setItem(SLOT_INFO, info);

        player.openInventory(inv);
    }

    public static int getLastPage(java.util.UUID uuid) {
        return lastPage.getOrDefault(uuid, 0);
    }

    public static int getMaxPage() {
        int total = CardDataManager.getAllCardOnly().size();
        return Math.max(0, (total + PER_PAGE - 1) / PER_PAGE - 1);
    }

    public static int findPageByKeyword(String keyword) {
        java.util.List<CardData> all = new java.util.ArrayList<>(CardDataManager.getAllCardOnly());
        String kw = keyword.toLowerCase();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().toLowerCase().contains(kw)) return i / PER_PAGE;
        }
        return -1;
    }

    public static boolean isCollection(String title) {
        return title.startsWith(TITLE_PREFIX);
    }
}
