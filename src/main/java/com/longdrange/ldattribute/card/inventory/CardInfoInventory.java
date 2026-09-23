package com.longdrange.ldattribute.card.inventory;

import com.longdrange.ldattribute.card.CardData;
import com.longdrange.ldattribute.card.CardLevel;
import com.longdrange.ldattribute.card.CardLevelConfig;
import com.longdrange.ldattribute.card.CardNBT;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class CardInfoInventory {

    public static final String TITLE_PREFIX = "\u00a78[\u00a7b\u5361\u7247\u8a73\u60c5\u00a78] ";
    public static int SLOT_CARD = 13;
    public static int SLOT_CLOSE = 22;
    public static int SLOT_USE_EXPSTONE = 20;
    public static int SLOT_DECOMPOSE = 24;
    public static int SLOT_LOCK = 26;
    public static int SLOT_BIND = 18;
    public static int SLOT_STAR = 16;
    public static int SLOT_RUNE = 14;

    public static void open(Player player, CardData data, ItemStack cardItem) {
        String title = TITLE_PREFIX + "\u00a7f" + data.getId();
        if (title.replaceAll("\u00a7.", "").length() > 32) title = TITLE_PREFIX + "\u00a7f\u8a73\u60c5";
        Inventory inv = Bukkit.createInventory(null, 27, title);

// 顯示卡片（若綁定其他玩家則加紅字提示）
        ItemStack display = cardItem.clone();
        if (com.longdrange.ldattribute.card.CardNBT.isBound(cardItem)) {
            String boundUUID = com.longdrange.ldattribute.card.CardNBT.getBoundUUID(cardItem);
            if (!boundUUID.equals(player.getUniqueId().toString())) {
                ItemMeta dm = display.getItemMeta();
                List<String> dl = dm.hasLore() ? new ArrayList<>(dm.getLore()) : new ArrayList<>();
                dl.add("");
                dl.add("\u00a7c\u26a0 \u9019\u5f35\u5361\u7d81\u5b9a\u7684\u662f \u00a7e" + com.longdrange.ldattribute.card.CardNBT.getBoundName(cardItem));
                dl.add("\u00a7c\u5c6c\u6027\u7121\u6548");
                dm.setLore(dl);
                display.setItemMeta(dm);
            }
        }
        inv.setItem(SLOT_CARD, display);

        // 关闭
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        cm.setDisplayName("\u00a7c\u00a7l\u2718 \u95dc\u9589");
        cm.setLore(Arrays.asList("\u00a77\u8fd4\u56de\u5361\u7247\u80cc\u5305"));
        close.setItemMeta(cm);
        inv.setItem(SLOT_CLOSE, close);

        // 使用经验石
        if (CardLevelConfig.isUpgradable(data.getId())) {
            ItemStack use = new ItemStack(Material.EXP_BOTTLE);
            ItemMeta um = use.getItemMeta();
            um.setDisplayName("\u00a7a\u00a7l\u2726 \u4f7f\u7528\u7d93\u9a57\u77f3");
            int lv = CardLevel.getLevel(cardItem);
            int exp = CardLevel.getExp(cardItem);
            int req = CardLevel.getRequiredExp(data.getId(), lv);
            um.setLore(Arrays.asList(
                    "\u00a77\u7576\u524d\u7b49\u7d1a: \u00a7e" + lv,
                    "\u00a77\u7576\u524d\u7d93\u9a57: \u00a7b" + exp + " \u00a77/ \u00a7b" + req,
                    "",
                    "\u00a7e\u9ede\u64ca\u9078\u64c7\u7d93\u9a57\u77f3"
            ));
            use.setItemMeta(um);
            inv.setItem(SLOT_USE_EXPSTONE, use);
        }

        // 分解
        com.longdrange.ldattribute.card.DecomposeConfig.Entry dc = com.longdrange.ldattribute.card.DecomposeConfig.get(data.getId());
        if (!dc.disabled) {
            ItemStack decompose = new ItemStack(Material.FLINT);
            ItemMeta dm = decompose.getItemMeta();
            dm.setDisplayName("\u00a7c\u00a7l\u2718 \u5206\u89e3\u5361\u7247");
            List<String> dl = new ArrayList<>();
            dl.add("\u00a77\u5206\u89e3\u5f8c\u7372\u5f97\uff1a");
            if (dc.points > 0) dl.add("\u00a77- \u9ede\u5238: \u00a76" + dc.points);
            if (dc.vault > 0) dl.add("\u00a77- \u91d1\u5e63: \u00a76" + (int) dc.vault);
            for (String it : dc.items) dl.add("\u00a77- \u7269\u54c1: \u00a7e" + it);
            for (java.util.Map.Entry<String, Integer> r : dc.random.entrySet())
                dl.add("\u00a77- \u96a8\u6a5f: \u00a7e" + r.getKey() + " \u00a77(" + r.getValue() + "%)");
            dl.add("");
            dl.add("\u00a7e\u9ede\u64ca\u5206\u89e3");
            dm.setLore(dl);
            decompose.setItemMeta(dm);
            inv.setItem(SLOT_DECOMPOSE, decompose);
        }

        // 锁定
        {
            boolean locked = CardNBT.isLocked(cardItem);
            ItemStack lock;
            if (locked) {
                lock = new ItemStack(Material.TRIPWIRE_HOOK);
                ItemMeta lm = lock.getItemMeta();
                lm.setDisplayName("\u00a7c\u00a7l\ud83d\udd12 \u5df2\u9396\u5b9a");
                lm.setLore(Arrays.asList("\u00a77\u9396\u5b9a\u5f8c\uff1a", "\u00a77- \u4e0d\u80fd\u53d6\u51fa", "\u00a77- \u4e0d\u80fd\u5408\u6210", "\u00a77- \u4e0d\u80fd\u5206\u89e3", "\u00a77- \u4e0d\u80fd\u8ca9\u552e", "", "\u00a7e\u9ede\u64ca\u89e3\u9396"));
                lock.setItemMeta(lm);
            } else {
                lock = new ItemStack(Material.IRON_FENCE);
                ItemMeta lm = lock.getItemMeta();
                lm.setDisplayName("\u00a7a\u00a7l\ud83d\udd13 \u672a\u9396\u5b9a");
                lm.setLore(Arrays.asList("\u00a77\u9396\u5b9a\u5f8c\u53ef\u9632\u6b62\u8aa4\u64cd\u4f5c", "", "\u00a7e\u9ede\u64ca\u9396\u5b9a"));
                lock.setItemMeta(lm);
            }
            inv.setItem(SLOT_LOCK, lock);
        }

        // 升星
        {
            int star = com.longdrange.ldattribute.card.CardNBT.getStar(cardItem);
            int maxStar = com.longdrange.ldattribute.card.StarConfig.getMaxStarFor(data.getId());
            ItemStack starItem = new ItemStack(Material.NETHER_STAR);
            ItemMeta sm = starItem.getItemMeta();
            if (star >= maxStar) {
                sm.setDisplayName("§6§l✦ 已滿星 ★★★★★");
                sm.setLore(Arrays.asList("§7已達最大星級"));
            } else {
                com.longdrange.ldattribute.card.StarConfig.Cost cost =
                        com.longdrange.ldattribute.card.StarConfig.getCost(star + 1);
                sm.setDisplayName("§6§l✦ 升星 (" + star + "→" + (star + 1) + ")");
                List<String> sl = new ArrayList<>();
                if (cost != null) {
                    sl.add("§7消耗:");
                    if (cost.points > 0) sl.add("§7- 點券: §6" + cost.points);
                    if (cost.vault > 0) sl.add("§7- 金幣: §6" + (int) cost.vault);
                    for (String it : cost.items) sl.add("§7- 物品: §e" + it);
                }
                sl.add("");
                sl.add("§e點擊升星");
                sm.setLore(sl);
            }
            starItem.setItemMeta(sm);
            inv.setItem(SLOT_STAR, starItem);
        }

        // 绑定
        {
            boolean bound = CardNBT.isBound(cardItem);
            String boundUUID = CardNBT.getBoundUUID(cardItem);
            String boundName = CardNBT.getBoundName(cardItem);
            boolean isSelf = bound && boundUUID.equals(player.getUniqueId().toString());
            ItemStack bind;
            ItemMeta bm;
            if (bound) {
                bind = new ItemStack(Material.NAME_TAG);
                bm = bind.getItemMeta();
                bm.setDisplayName("\u00a76\u00a7l\u2726 \u5df2\u7d81\u5b9a");
                List<String> bl = new ArrayList<>();
                bl.add("\u00a77\u7d81\u5b9a\u73a9\u5bb6: \u00a7e" + boundName);
                bl.add("");
                if (isSelf) {
                    if (data.allowUnbind) {
                        bl.add("\u00a7e\u9ede\u64ca\u89e3\u7d81");
                        if (data.unbindCost > 0) bl.add("\u00a77\u8cbb\u7528: \u00a76" + data.unbindCost + " \u00a77\u9ede\u5238");
                    } else {
                        bl.add("\u00a7c\u7121\u6cd5\u89e3\u7d81");
                    }
                } else {
                    bl.add("\u00a7c\u9019\u5f35\u5361\u7d81\u5b9a\u7684\u662f\u5176\u4ed6\u73a9\u5bb6");
                }
                bm.setLore(bl);
            } else {
                bind = new ItemStack(Material.NAME_TAG);
                bm = bind.getItemMeta();
                bm.setDisplayName("\u00a7e\u00a7l\u2726 \u7d81\u5b9a\u81ea\u5df1");
                List<String> bl = new ArrayList<>();
                bl.add("\u00a77\u7d81\u5b9a\u5f8c\u7121\u6cd5\u4ea4\u6613\u7d66\u4ed6\u4eba");
                if (data.bindOnPickup) bl.add("\u00a7c(\u6b64\u5361\u7372\u5f97\u6642\u81ea\u52d5\u7d81\u5b9a)");
                bl.add("");
                bl.add("\u00a7e\u9ede\u64ca\u7d81\u5b9a");
                bm.setLore(bl);
            }
            bind.setItemMeta(bm);
            inv.setItem(SLOT_BIND, bind);
        }


        // 符文孔位
        try {
            java.util.List<String> socketIds = com.longdrange.ldattribute.rune.RuneConfig.getCardSockets(data.getId());
            if (!socketIds.isEmpty()) {
                int unlockedCount = 0;
                for (int i = 0; i < socketIds.size(); i++) {
                    if (CardNBT.isSocketUnlocked(cardItem, i)) unlockedCount++;
                }
                ItemStack rune = new ItemStack(Material.NETHER_STAR);
                ItemMeta rm = rune.getItemMeta();
                rm.setDisplayName("\u00a7d\u00a7l\u2726 \u7b26\u6587\u5b54\u4f4d");
                java.util.List<String> rl = new java.util.ArrayList<>();
                rl.add("\u00a77\u5df2\u6253\u5b54: \u00a7a" + unlockedCount + " \u00a77/ \u00a7f" + socketIds.size());
                rl.add("");
                rl.add("\u00a7e\u9ede\u64ca\u7ba1\u7406\u7b26\u6587\u5b54\u4f4d");
                rm.setLore(rl);
                rune.setItemMeta(rm);
                inv.setItem(SLOT_RUNE, rune);
            }
        } catch (Throwable ignored) {}
        player.openInventory(inv);
    }

    public static boolean isCardInfo(String title) {
        return title.startsWith(TITLE_PREFIX);
    }
}
