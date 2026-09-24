package com.longdrange.ldattribute.gacha;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GachaInventory {
    public static final String TITLE_PREFIX = "\u00a78[\u00a76\u62bd\u734e\u00a78] ";
    public static int SLOT_BACK = 49;

    private static final Map<UUID, String> currentGacha = new HashMap<>();
    private static final Map<UUID, Integer> pityCache = new HashMap<>();

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PREFIX + "\u9078\u64c7\u5361\u6c60");

        List<GachaConfig.Gacha> all = new ArrayList<>(GachaConfig.getAll());
        int slot = 10;
        for (GachaConfig.Gacha g : all) {
            if (slot >= 44) break;
            inv.setItem(slot, buildGachaIcon(player, g));
            slot += 2;
        }

        // 底部
        ItemStack sep = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta sm = sep.getItemMeta(); sm.setDisplayName(" "); sep.setItemMeta(sm);
        for (int i = 45; i < 54; i++) {
            if (i == SLOT_BACK) continue;
            inv.setItem(i, sep);
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta(); bm.setDisplayName("\u00a7c\u00a7l\u2190 \u8fd4\u56de"); back.setItemMeta(bm);
        inv.setItem(SLOT_BACK, back);

        player.openInventory(inv);
    }

    private static ItemStack buildGachaIcon(Player player, GachaConfig.Gacha g) {
        ItemStack item = new ItemStack(g.icon);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName("\u00a7e\u00a7l" + g.name);

        List<String> lore = new ArrayList<>();
        lore.add("\u00a77" + g.description);
        lore.add("");
        lore.add("\u00a77\u6d88\u8017:");
        if (g.costPoints > 0) lore.add("  \u00a77- \u9ede\u5238: \u00a76" + g.costPoints);
        if (g.costVault > 0) lore.add("  \u00a77- \u91d1\u5e63: \u00a76" + (int) g.costVault);
        for (String it : g.costItems) lore.add("  \u00a77- \u7269\u54c1: \u00a7e" + it);
        lore.add("");
        lore.add("\u00a77\u734e\u6c60: \u00a7e" + g.pool.size() + " \u7a2e");
        if (g.pityEnabled) {
            int pity = GachaData.getPityCount(player.getUniqueId(), g.id);
            lore.add("\u00a77\u4fdd\u5e95: \u00a7e" + pity + "\u00a77/\u00a7e" + g.pityCount);
        }
        lore.add("");
        lore.add("\u00a7e\u9ede\u64ca\u62bd\u4e00\u6b21");
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }

    public static boolean isGacha(String title) { return title.startsWith(TITLE_PREFIX); }

    public static List<GachaConfig.Gacha> getSortedList() {
        return new ArrayList<>(GachaConfig.getAll());
    }

    /** 界面槽位 → 卡池 */
    public static GachaConfig.Gacha getBySlot(int rawSlot) {
        if (rawSlot < 10 || rawSlot >= 44) return null;
        List<GachaConfig.Gacha> list = getSortedList();
        int idx = (rawSlot - 10) / 2;
        if (idx < 0 || idx >= list.size()) return null;
        // 只接受偶数偏移
        if ((rawSlot - 10) % 2 != 0) return null;
        return list.get(idx);
    }
}