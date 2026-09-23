package com.longdrange.ldattribute.card.inventory;

import com.longdrange.ldattribute.card.CardData;
import com.longdrange.ldattribute.card.DecomposeConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class DecomposeConfirmInventory {

    public static final String TITLE_PREFIX = "\u00a78[\u00a7c\u5206\u89e3\u78ba\u8a8d\u00a78] ";
    public static int SLOT_CARD = 13;
    public static int SLOT_CONFIRM = 11;
    public static int SLOT_CANCEL = 15;

    private static final Map<UUID, CardData> pendingCard = new HashMap<>();
    private static final Map<UUID, ItemStack> pendingItem = new HashMap<>();

    public static void open(Player player, CardData card, ItemStack cardItem) {
        pendingCard.put(player.getUniqueId(), card);
        pendingItem.put(player.getUniqueId(), cardItem.clone());

        Inventory inv = Bukkit.createInventory(null, 27, TITLE_PREFIX + "\u00a7f" + card.getId());

        inv.setItem(SLOT_CARD, cardItem.clone());

        DecomposeConfig.Entry e = DecomposeConfig.get(card.getId());

        ItemStack confirm = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 5);
        ItemMeta cm = confirm.getItemMeta();
        cm.setDisplayName("\u00a7a\u00a7l\u2714 \u78ba\u8a8d\u5206\u89e3");
        List<String> cl = new ArrayList<>();
        cl.add("\u00a77\u5206\u89e3\u5f8c\u7372\u5f97\uff1a");
        if (e.points > 0) cl.add("\u00a77- \u9ede\u5238: \u00a76" + e.points);
        if (e.vault > 0) cl.add("\u00a77- \u91d1\u5e63: \u00a76" + (int) e.vault);
        for (String it : e.items) cl.add("\u00a77- \u7269\u54c1: \u00a7e" + it);
        for (Map.Entry<String, Integer> r : e.random.entrySet())
            cl.add("\u00a77- \u96a8\u6a5f: \u00a7e" + r.getKey() + " \u00a77(" + r.getValue() + "%)");
        cm.setLore(cl);
        confirm.setItemMeta(cm);
        inv.setItem(SLOT_CONFIRM, confirm);

        ItemStack cancel = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
        ItemMeta xm = cancel.getItemMeta();
        xm.setDisplayName("\u00a7c\u00a7l\u2718 \u53d6\u6d88");
        xm.setLore(Arrays.asList("\u00a77\u8fd4\u56de\u5361\u7247\u8a73\u60c5"));
        cancel.setItemMeta(xm);
        inv.setItem(SLOT_CANCEL, cancel);

        // 其他格子填灰色玻璃板
        ItemStack sep = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta sm = sep.getItemMeta(); sm.setDisplayName(" "); sep.setItemMeta(sm);
        for (int i = 0; i < 27; i++) {
            if (i == SLOT_CARD || i == SLOT_CONFIRM || i == SLOT_CANCEL) continue;
            inv.setItem(i, sep);
        }

        player.openInventory(inv);
    }

    public static CardData getPendingCard(UUID uuid) { return pendingCard.get(uuid); }
    public static ItemStack getPendingItem(UUID uuid) { return pendingItem.get(uuid); }
    public static void clear(UUID uuid) { pendingCard.remove(uuid); pendingItem.remove(uuid); }
    public static boolean isConfirm(String title) { return title.startsWith(TITLE_PREFIX); }
}
