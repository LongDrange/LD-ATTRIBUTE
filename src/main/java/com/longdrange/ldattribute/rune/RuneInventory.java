package com.longdrange.ldattribute.rune;

import com.longdrange.ldattribute.card.CardData;
import com.longdrange.ldattribute.card.CardNBT;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RuneInventory {

    public static final String TITLE_PREFIX = "\u00a78[\u00a7d\u7b26\u6587\u5b54\u4f4d\u00a78] ";
    public static int SLOT_CARD = 4;
    public static int SLOT_BACK = 22;
    public static int SLOT_LOCK = 26;
    public static int SIZE = 27;
    public static int[] SLOT_SOCKETS = { 10, 11, 12, 13, 14, 15, 16 };

    private static void reloadFromConfig() {
        try {
            SLOT_CARD = com.longdrange.ldattribute.card.UIConfig.getInt("Rune", "Card", SLOT_CARD);
            SLOT_BACK = com.longdrange.ldattribute.card.UIConfig.getInt("Rune", "Back", SLOT_BACK);
            SIZE = com.longdrange.ldattribute.card.UIConfig.getInt("Rune", "Size", SIZE);
            java.util.List<Integer> slots = com.longdrange.ldattribute.card.UIConfig.getIntList(
                    "Rune", "SocketSlots", java.util.Arrays.asList(10, 11, 12, 13, 14, 15, 16));
            SLOT_SOCKETS = new int[slots.size()];
            for (int i = 0; i < slots.size(); i++) SLOT_SOCKETS[i] = slots.get(i);
        } catch (Throwable ignored) {}
    }

    public static void open(Player player, CardData data, ItemStack cardItem) {
        String title = TITLE_PREFIX + "\u00a7f" + data.getId();
        if (title.replaceAll("\u00a7.", "").length() > 32) title = TITLE_PREFIX + "\u00a7f\u8a73\u60c5";
        reloadFromConfig();
        Inventory inv = Bukkit.createInventory(null, SIZE, title);

        inv.setItem(SLOT_CARD, cardItem.clone());

        List<String> socketIds = RuneConfig.getCardSockets(data.getId());
        for (int i = 0; i < socketIds.size() && i < SLOT_SOCKETS.length; i++) {
            String socketId = socketIds.get(i);
            RuneConfig.Socket sk = RuneConfig.getSocket(socketId);
            if (sk == null) continue;
            inv.setItem(SLOT_SOCKETS[i], buildSocketItem(sk, cardItem, i));
        }

        // 符文锁定按钮
        {
            boolean locked = CardNBT.isRuneLocked(cardItem);
            ItemStack lockItem = new ItemStack(locked ? Material.BEDROCK : Material.IRON_FENCE);
            ItemMeta lm = lockItem.getItemMeta();
            if (locked) {
                lm.setDisplayName("\u00a7c\u00a7l\u26bf \u7b26\u6587\u5df2\u9396\u5b9a");
                lm.setLore(Arrays.asList(
                        "\u00a77\u9396\u5b9a\u5f8c\u7121\u6cd5\u6253\u5b54/\u9577\u5d4c/\u53d6\u51fa",
                        "\u00a77\u4fdd\u8b77\u4f60\u7684\u7b26\u6587\u914d\u7f6e",
                        "",
                        "\u00a7e\u9ede\u64ca\u89e3\u9396"));
            } else {
                lm.setDisplayName("\u00a7a\u00a7l\u26bf \u7b26\u6587\u672a\u9396\u5b9a");
                lm.setLore(Arrays.asList(
                        "\u00a77\u9396\u5b9a\u5f8c\u53ef\u9632\u6b62\u8aa4\u64cd\u4f5c",
                        "",
                        "\u00a7e\u9ede\u64ca\u9396\u5b9a"));
            }
            lockItem.setItemMeta(lm);
            inv.setItem(SLOT_LOCK, lockItem);
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName("\u00a7c\u00a7l\u2190 \u8fd4\u56de");
        bm.setLore(Arrays.asList("\u00a77\u8fd4\u56de\u5361\u7247\u8a73\u60c5"));
        back.setItemMeta(bm);
        inv.setItem(SLOT_BACK, back);

        player.openInventory(inv);
    }

    private static ItemStack buildSocketItem(RuneConfig.Socket sk, ItemStack cardItem, int index) {
        boolean unlocked = CardNBT.isSocketUnlocked(cardItem, index);
        String runeId = CardNBT.getSocketRune(cardItem, index);

        ItemStack item;
        if (!unlocked) {
            item = new ItemStack(Material.IRON_FENCE);
        } else if (runeId == null || runeId.isEmpty()) {
            item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 0);
        } else {
            item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 5);
        }

        ItemMeta m = item.getItemMeta();
        List<String> lore = new ArrayList<>();

        if (!unlocked) {
            m.setDisplayName("\u00a77[\u672a\u6253\u5b54] " + sk.name);
            lore.add("\u00a7e\u9ede\u64ca\u6253\u5b54");
            lore.add("");
            lore.add("\u00a77\u9700\u6c42:");
            if (sk.costPoints > 0) lore.add("\u00a77- \u9ede\u5238: \u00a76" + sk.costPoints);
            if (sk.costVault > 0) lore.add("\u00a77- \u91d1\u5e63: \u00a76" + (int) sk.costVault);
            for (String it : sk.costItems) lore.add("\u00a77- \u7269\u54c1: \u00a7e" + it);
        } else if (runeId == null || runeId.isEmpty()) {
            m.setDisplayName("\u00a7a" + sk.name);
            lore.add("\u00a77\u72c0\u614b: \u00a7a\u5df2\u6253\u5b54 \u00a78(\u7a7a)");
            lore.add("\u00a77\u53ef\u653e\u5165: \u00a7f" + String.join(", ", sk.allowedTypes));
            lore.add("");
            lore.add("\u00a7e\u9ede\u64ca\u653e\u5165\u7b26\u6587");
        } else {
            RuneConfig.Rune r = RuneConfig.getRune(runeId);
            m.setDisplayName("\u00a7a" + sk.name);
            lore.add("\u00a77\u7b26\u6587: \u00a7f" + (r != null ? r.name : runeId));
            lore.add("");
            lore.add("\u00a7e\u9ede\u64ca\u53d6\u51fa / \u66f4\u63db");
        }
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }

    public static final java.util.Map<java.util.UUID, Integer> pendingSocket = new java.util.HashMap<>();

    public static boolean isRune(String title) {
        return title.startsWith(TITLE_PREFIX);
    }

    public static int getSocketIndex(int rawSlot) {
        for (int i = 0; i < SLOT_SOCKETS.length; i++) {
            if (SLOT_SOCKETS[i] == rawSlot) return i;
        }
        return -1;
    }
}