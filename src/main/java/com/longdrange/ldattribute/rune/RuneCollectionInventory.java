package com.longdrange.ldattribute.rune;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RuneCollectionInventory {
    public static final String TITLE_PREFIX = "\u00a78[\u00a7d\u7b26\u6587\u5716\u9451\u00a78] ";
    public static final int PER_PAGE = 45;
    public static int SLOT_BACK = 48;
    public static int SLOT_PREV = 45;
    public static int SLOT_INFO = 49;
    public static int SLOT_NEXT = 53;

    private static final Map<UUID, Integer> lastPage = new HashMap<>();

    public static void open(Player player) { open(player, 0); }

    public static void open(Player player, int page) {
        List<RuneConfig.Rune> all = new ArrayList<>(RuneConfig.getAllRunes());
        int total = all.size();
        if (total == 0) { player.sendMessage("\u00a7c\u5c1a\u672a\u8f09\u5165\u4efb\u4f55\u7b26\u6587"); return; }
        int maxPage = (total + PER_PAGE - 1) / PER_PAGE - 1;
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;

        // 先扫一遍背包，把玩家现有的符文都记进图鉴
        try {
            for (ItemStack it : player.getInventory().getContents()) {
                if (it == null) continue;
                String rid = RuneItem.getRuneId(it);
                if (rid != null && !rid.isEmpty()) RuneData.collect(player.getUniqueId(), rid);
            }
        } catch (Throwable ignored) {}

        Set<String> owned = RuneData.get(player.getUniqueId());
        int ownedCount = 0;
        for (RuneConfig.Rune r : all) if (owned.contains(r.id)) ownedCount++;

        String title = TITLE_PREFIX + "\u5df2\u6536\u96c6 " + ownedCount + "/" + total;
        if (title.replaceAll("\u00a7.", "").length() > 32)
            title = TITLE_PREFIX + ownedCount + "/" + total;

        Inventory inv = Bukkit.createInventory(null, 54, title);
        lastPage.put(player.getUniqueId(), page);
        int start = page * PER_PAGE;

        for (int i = 0; i < PER_PAGE; i++) {
            int idx = start + i;
            if (idx >= total) break;
            RuneConfig.Rune r = all.get(idx);
            boolean has = owned.contains(r.id);
            ItemStack display;
            if (has) {
                display = RuneItem.create(r, 1);
                ItemMeta m = display.getItemMeta();
                List<String> lore = m.hasLore() ? new ArrayList<>(m.getLore()) : new ArrayList<>();
                lore.add("");
                lore.add("\u00a7a\u2714 \u5df2\u6536\u96c6");
                m.setLore(lore);
                display.setItemMeta(m);
            } else {
                display = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
                ItemMeta m = display.getItemMeta();
                m.setDisplayName("\u00a77???");
                m.setLore(Arrays.asList("\u00a77\u672a\u6536\u96c6"));
                display.setItemMeta(m);
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
        bm.setDisplayName("\u00a7e\u00a7l\u25c0 \u8fd4\u56de");
        back.setItemMeta(bm);
        inv.setItem(SLOT_BACK, back);

        if (page > 0) {
            ItemStack p = new ItemStack(Material.PAPER);
            ItemMeta pm = p.getItemMeta(); pm.setDisplayName("\u00a7e\u4e0a\u4e00\u9801"); p.setItemMeta(pm);
            inv.setItem(SLOT_PREV, p);
        }
        if (page < maxPage) {
            ItemStack p = new ItemStack(Material.PAPER);
            ItemMeta pm = p.getItemMeta(); pm.setDisplayName("\u00a7e\u4e0b\u4e00\u9801"); p.setItemMeta(pm);
            inv.setItem(SLOT_NEXT, p);
        }

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("\u00a7a\u6536\u96c6\u9032\u5ea6");
        int percent = total == 0 ? 0 : (int) (ownedCount * 100L / total);
        int filled = percent / 10;
        StringBuilder bar = new StringBuilder();
        bar.append("\u00a7a");
        for (int b = 0; b < filled; b++) bar.append("\u2588");
        bar.append("\u00a77");
        for (int b = filled; b < 10; b++) bar.append("\u2588");
        bar.append(" \u00a7e").append(percent).append("%");
        im.setLore(Arrays.asList(
                "\u00a77\u5df2\u6536\u96c6: \u00a7e" + ownedCount + "\u00a77/\u00a7e" + total,
                "\u00a77\u7b2c \u00a7e" + (page + 1) + "\u00a77/\u00a7e" + (maxPage + 1) + " \u00a77\u9801",
                "",
                bar.toString()
        ));
        info.setItemMeta(im);
        inv.setItem(SLOT_INFO, info);

        player.openInventory(inv);
    }

    public static int getLastPage(UUID uuid) { return lastPage.getOrDefault(uuid, 0); }

    public static int getMaxPage() {
        int total = RuneConfig.getAllRunes().size();
        return Math.max(0, (total + PER_PAGE - 1) / PER_PAGE - 1);
    }

    public static boolean isCollection(String title) {
        return title.startsWith(TITLE_PREFIX);
    }
}