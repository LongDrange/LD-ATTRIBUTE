package com.longdrange.ldattribute.card.inventory;

import com.longdrange.ldattribute.card.CardData;
import com.longdrange.ldattribute.card.CardDataManager;
import com.longdrange.ldattribute.card.CardNBT;
import com.longdrange.ldattribute.card.ExpStoneData;
import com.longdrange.ldattribute.card.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ExpStoneSelectInventory {

    public static final String TITLE_PREFIX = "\u00a78[\u00a7b\u9078\u64c7\u5361\u7247\u00a78] ";
    public static int SLOT_BACK = 49;

    private static final Map<UUID, ItemStack> pendingStone = new HashMap<>();
    private static final Map<UUID, Map<Integer, ItemStack>> originalCards = new HashMap<>();

    public static void open(Player player, ExpStoneData.ExpStone stone, ItemStack stoneItem) {
        pendingStone.put(player.getUniqueId(), stoneItem);
        Map<Integer, ItemStack> cards = new HashMap<>();
        originalCards.put(player.getUniqueId(), cards);

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PREFIX + "\u7d93\u9a57\u77f3");

        Set<String> seen = new HashSet<>();
        int i = 0;
        int unlocked = PlayerData.getUnlockedPages(player.getUniqueId());
        for (int page = 0; page < unlocked && i < 45; page++) {
            ItemStack[] items = PlayerData.getPageInventory(player.getUniqueId(), page);
            for (int idx = 0; idx < items.length && i < 45; idx++) {
                ItemStack it = items[idx];
                if (it == null || it.getType() == Material.AIR) continue;
                CardData cd = CardDataManager.findCard(it);
                if (cd == null) continue;
                if (!stone.matches(cd.getId(), cd.getType())) continue;
                String key = cd.getId() + "|" + CardNBT.getLevel(it);
                if (!seen.add(key)) continue;
                cards.put(i, it.clone());
                inv.setItem(i, decorate(it, stone));
                i++;
            }
        }

        ItemStack sep = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta sm = sep.getItemMeta(); sm.setDisplayName(" "); sep.setItemMeta(sm);
        for (int k = 45; k < 54; k++) {
            if (k == SLOT_BACK) continue;
            inv.setItem(k, sep);
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName("\u00a7e\u00a7l\u25c0 \u8fd4\u56de\u5361\u7247\u80cc\u5305");
        back.setItemMeta(bm);
        inv.setItem(SLOT_BACK, back);

        player.openInventory(inv);
    }

    /** 只加 Lore，不改 displayName（避免 findCard 匹配失败） */
    private static ItemStack decorate(ItemStack card, ExpStoneData.ExpStone stone) {
        ItemStack result = card.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return result;
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("");
        lore.add("\u00a78\u00a7m----------------");
        lore.add("\u00a77\u4f7f\u7528: \u00a7b+" + stone.exp + " \u7d93\u9a57");
        lore.add("\u00a77\u6210\u529f\u7387: \u00a7a" + (int)(stone.successRate * 100) + "%");
        lore.add("\u00a77\u9700\u8981: \u00a7b" + stone.needCount + " \u500b");
        lore.add("");
        lore.add("\u00a7e\u9ede\u64ca\u4f7f\u7528");
        meta.setLore(lore);
        result.setItemMeta(meta);
        return result;
    }

    public static ItemStack getPendingStone(UUID uuid) { return pendingStone.get(uuid); }

    public static ItemStack getOriginalCard(UUID uuid, int slot) {
        Map<Integer, ItemStack> m = originalCards.get(uuid);
        return m == null ? null : m.get(slot);
    }

    public static void clearPending(UUID uuid) {
        pendingStone.remove(uuid);
        originalCards.remove(uuid);
    }

    public static boolean isSelect(String title) { return title.startsWith(TITLE_PREFIX); }
}
