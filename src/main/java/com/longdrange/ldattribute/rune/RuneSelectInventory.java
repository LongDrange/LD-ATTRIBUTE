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

public class RuneSelectInventory {

    public static final String TITLE_PREFIX = "\u00a78[\u00a7d\u9078\u64c7\u7b26\u6587\u00a78] ";
    public static int SLOT_CARD = 4;
    public static int SLOT_BACK = 40;
    public static int SIZE = 45;
    public static int[] SLOT_LIST = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private static void reloadFromConfig() {
        try {
            SLOT_CARD = com.longdrange.ldattribute.card.UIConfig.getInt("RuneSelect", "Card", SLOT_CARD);
            SLOT_BACK = com.longdrange.ldattribute.card.UIConfig.getInt("RuneSelect", "Back", SLOT_BACK);
            SIZE = com.longdrange.ldattribute.card.UIConfig.getInt("RuneSelect", "Size", SIZE);
            java.util.List<Integer> slots = com.longdrange.ldattribute.card.UIConfig.getIntList(
                    "RuneSelect", "ListSlots", java.util.Arrays.asList(
                            10, 11, 12, 13, 14, 15, 16,
                            19, 20, 21, 22, 23, 24, 25,
                            28, 29, 30, 31, 32, 33, 34));
            SLOT_LIST = new int[slots.size()];
            for (int i = 0; i < slots.size(); i++) SLOT_LIST[i] = slots.get(i);
        } catch (Throwable ignored) {}
    }

    /** socketIndex：要鑲嵌的孔位索引。 */
    public static void open(Player player, CardData data, ItemStack cardItem, int socketIndex) {
        String title = TITLE_PREFIX + "\u00a7f" + data.getId();
        if (title.replaceAll("\u00a7.", "").length() > 32) title = TITLE_PREFIX + "\u00a7f\u9078\u64c7";
        reloadFromConfig();
        Inventory inv = Bukkit.createInventory(null, SIZE, title);

        inv.setItem(SLOT_CARD, cardItem.clone());

        List<String> socketIds = RuneConfig.getCardSockets(data.getId());
        if (socketIndex < 0 || socketIndex >= socketIds.size()) { player.closeInventory(); return; }
        String socketId = socketIds.get(socketIndex);
        RuneConfig.Socket sk = RuneConfig.getSocket(socketId);
        if (sk == null) { player.closeInventory(); return; }

        // 收集玩家背包里的符文
        Map<String, Integer> owned = new LinkedHashMap<>();
        for (ItemStack it : player.getInventory().getContents()) {
            String rid = RuneItem.getRuneId(it);
            if (rid == null) continue;
            RuneConfig.Rune r = RuneConfig.getRune(rid);
            if (r == null) continue;
            if (!sk.allowedTypes.contains(r.type)) continue;
            owned.put(rid, owned.getOrDefault(rid, 0) + it.getAmount());
        }

        int i = 0;
        for (Map.Entry<String, Integer> e : owned.entrySet()) {
            if (i >= SLOT_LIST.length) break;
            RuneConfig.Rune r = RuneConfig.getRune(e.getKey());
            if (r == null) continue;
            ItemStack show = RuneItem.create(r, 1);
            ItemMeta m = show.getItemMeta();
            List<String> lore = m.hasLore() ? new ArrayList<>(m.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add("\u00a77\u6301\u6709: \u00a7e" + e.getValue() + " \u500b");
            lore.add("\u00a7e\u9ede\u64ca\u947d\u5d4c\u5230\u6b64\u5b54\u4f4d");
            m.setLore(lore);
            show.setItemMeta(m);
            inv.setItem(SLOT_LIST[i], show);
            i++;
        }

        if (i == 0) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta em = empty.getItemMeta();
            em.setDisplayName("\u00a7c\u6c92\u6709\u53ef\u7528\u7b26\u6587");
            em.setLore(Arrays.asList(
                    "\u00a77\u6b64\u5b54\u4f4d\u53ef\u653e: \u00a7f" + String.join(", ", sk.allowedTypes),
                    "\u00a77\u4f60\u5305\u5305\u88e1\u6c92\u6709\u7b26\u5408\u7684\u7b26\u6587"));
            empty.setItemMeta(em);
            inv.setItem(22, empty);
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName("\u00a7c\u00a7l\u2190 \u8fd4\u56de");
        bm.setLore(Arrays.asList("\u00a77\u8fd4\u56de\u7b26\u6587\u5b54\u4f4d"));
        back.setItemMeta(bm);
        inv.setItem(SLOT_BACK, back);

        player.openInventory(inv);
    }

    public static boolean isRuneSelect(String title) {
        return title.startsWith(TITLE_PREFIX);
    }

    public static boolean isListSlot(int rawSlot) {
        for (int s : SLOT_LIST) if (s == rawSlot) return true;
        return false;
    }
}