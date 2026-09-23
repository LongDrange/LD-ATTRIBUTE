package com.longdrange.ldattribute.rune;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RuneUpgradeInventory {
    public static final String TITLE_PREFIX = "\u00a78[\u00a7d\u7b26\u6587\u5347\u7d1a\u00a78] ";
    public static int SLOT_CURRENT = 13;
    public static int SLOT_CONFIRM = 22;
    public static int SLOT_BACK = 18;
    public static int SLOT_INFO = 26;

    public static void open(Player player, String runeId) {
        RuneConfig.Rune cur = RuneConfig.getRune(runeId);
        if (cur == null) { player.sendMessage("\u00a7c\u7b26\u6587\u4e0d\u5b58\u5728"); return; }
        if (cur.upgradeTo == null || cur.upgradeTo.isEmpty()) {
            player.sendMessage("\u00a7c\u6b64\u7b26\u6587\u5df2\u662f\u6700\u9ad8\u7b49\u7d1a\uff0c\u7121\u6cd5\u5347\u7d1a");
            return;
        }
        RuneConfig.Rune next = RuneConfig.getRune(cur.upgradeTo);
        if (next == null) { player.sendMessage("\u00a7c\u76ee\u6a19\u7b26\u6587\u4e0d\u5b58\u5728"); return; }

        String title = TITLE_PREFIX + "\u00a7f" + cur.name + " \u2192 " + next.name;
        if (title.replaceAll("\u00a7.", "").length() > 32)
            title = TITLE_PREFIX + "\u00a7f\u5347\u7d1a";
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // 当前符文
        ItemStack curItem = RuneItem.create(cur, 3);
        ItemMeta cm = curItem.getItemMeta();
        List<String> cl = cm.hasLore() ? new ArrayList<>(cm.getLore()) : new ArrayList<>();
        cl.add("");
        cl.add("\u00a77\u9700\u6c42: \u00a7e\u540c\u7b49\u7d1a\u7b26\u6587 \u00a7c3 \u500b");
        cm.setLore(cl);
        curItem.setItemMeta(cm);
        inv.setItem(SLOT_CURRENT, curItem);

        // 目标
        ItemStack nextItem = RuneItem.create(next, 1);
        ItemMeta nm = nextItem.getItemMeta();
        List<String> nl = nm.hasLore() ? new ArrayList<>(nm.getLore()) : new ArrayList<>();
        nl.add("");
        nl.add("\u00a7a\u5347\u7d1a\u5f8c\u7372\u5f97");
        nm.setLore(nl);
        nextItem.setItemMeta(nm);
        inv.setItem(15, nextItem);

        // 确认按钮
        int have = countRunes(player, runeId);
        ItemStack confirm;
        ItemMeta fm;
        if (have >= 3) {
            confirm = new ItemStack(Material.EMERALD_BLOCK);
            fm = confirm.getItemMeta();
            fm.setDisplayName("\u00a7a\u00a7l\u2714 \u78ba\u8a8d\u5347\u7d1a");
            fm.setLore(Arrays.asList(
                    "\u00a77\u6d88\u8017: \u00a7e3 \u00d7 " + cur.name,
                    "\u00a77\u7372\u5f97: \u00a7a1 \u00d7 " + next.name,
                    "",
                    "\u00a7e\u9ede\u64ca\u5347\u7d1a"));
        } else {
            confirm = new ItemStack(Material.REDSTONE_BLOCK);
            fm = confirm.getItemMeta();
            fm.setDisplayName("\u00a7c\u00a7l\u2718 \u7b26\u6587\u4e0d\u8db3");
            fm.setLore(Arrays.asList(
                    "\u00a77\u9700\u8981: \u00a7e3 \u500b " + cur.name,
                    "\u00a77\u4f60\u6709: \u00a7c" + have + " \u500b"));
        }
        confirm.setItemMeta(fm);
        inv.setItem(SLOT_CONFIRM, confirm);

        // 返回
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName("\u00a7c\u00a7l\u2190 \u8fd4\u56de");
        back.setItemMeta(bm);
        inv.setItem(SLOT_BACK, back);

        // 提示
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("\u00a7e\u5347\u7d1a\u8aaa\u660e");
        im.setLore(Arrays.asList(
                "\u00a773 \u500b\u540c\u7b49\u7d1a\u7b26\u6587",
                "\u00a77\u53ef\u5408\u6210 1 \u500b\u4e0b\u4e00\u7d1a\u7b26\u6587",
                "",
                "\u00a77\u4f60\u76ee\u524d\u6301\u6709: \u00a7e" + have + " \u500b " + cur.name));
        info.setItemMeta(im);
        inv.setItem(SLOT_INFO, info);

        player.openInventory(inv);
    }

    public static int countRunes(Player player, String runeId) {
        int c = 0;
        for (ItemStack it : player.getInventory().getContents()) {
            if (it == null) continue;
            String rid = RuneItem.getRuneId(it);
            if (runeId.equals(rid)) c += it.getAmount();
        }
        return c;
    }

    public static boolean isUpgrade(String title) {
        return title.startsWith(TITLE_PREFIX);
    }
}