package com.longdrange.ldattribute.core.guide.gui;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.guide.GuideAttrSummary;
import com.longdrange.ldattribute.core.guide.GuideConfig;
import com.longdrange.ldattribute.core.guide.GuideData;
import com.longdrange.ldattribute.core.util.PapiUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GuideGUI {

    public static final int BTN_PREV  = 45;
    public static final int BTN_BACK  = 48;
    public static final int BTN_INFO  = 49;
    public static final int BTN_SCORE = 50;
    public static final int BTN_NEXT  = 53;

    private static final short GLASS_LOCKED   = 7;
    private static final short GLASS_UNLOCKED = 5;

    private static final String TITLE_ICON_NAME = "\u00a7b【理想龙猫】\u00a72怪物圖鑑";

    private final LDAttribute plugin;
    public GuideGUI(LDAttribute plugin) { this.plugin = plugin; }

    public void open(Player player) { open(player, null, 0); }

    public void open(Player player, String groupId, int page) {
        GuideData data = plugin.getGuideManager().get(player);
        List<GuideConfig.MonsterDef> list = GuideConfig.byGroup(groupId);

        int perPage = 45;
        int totalPages = Math.max(1, (list.size() + perPage - 1) / perPage);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        String groupName = (groupId == null)
                ? "\u5168\u90e8"
                : ChatColor.stripColor(GuideConfig.getGroup(groupId) == null
                        ? groupId
                        : GuideConfig.getGroup(groupId).name);

        String title = ChatColor.AQUA + "\u2726 " + groupName
                + ChatColor.DARK_GRAY + " (" + (page + 1) + "/" + totalPages + ")";
        title = PapiUtil.parse(player, title);

        GuideHolder holder = new GuideHolder(player.getUniqueId(), groupId, page);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        int start = page * perPage;
        for (int s = 0; s < perPage; s++) {
            int idx = start + s;
            if (idx >= list.size()) break;
            GuideConfig.MonsterDef def = list.get(idx);
            inv.setItem(s, monsterIcon(def, data));
        }

        if (page > 0) {
            inv.setItem(BTN_PREV, icon(Material.ARROW,
                    ChatColor.GRAY + "\u2190 \u4e0a\u4e00\u9875 " + ChatColor.DARK_GRAY + "(" + page + "/" + totalPages + ")"));
        } else {
            inv.setItem(BTN_PREV, glass(GLASS_LOCKED, ChatColor.DARK_GRAY + "\u2190 \u5df2\u662f\u9996\u9875"));
        }
        if (page < totalPages - 1) {
            inv.setItem(BTN_NEXT, icon(Material.ARROW,
                    ChatColor.GRAY + "\u4e0b\u4e00\u9875 \u2192 " + ChatColor.DARK_GRAY + "(" + (page + 2) + "/" + totalPages + ")"));
        } else {
            inv.setItem(BTN_NEXT, glass(GLASS_LOCKED, ChatColor.DARK_GRAY + "\u5df2\u662f\u672b\u9875 \u2192"));
        }

        inv.setItem(BTN_BACK, icon(Material.BARRIER, ChatColor.GRAY + "\u2190 \u8fd4\u56de\u603b\u89c8"));

        inv.setItem(BTN_INFO, renderInfo(groupId, data, page, totalPages));
        inv.setItem(BTN_SCORE, renderScoreInfo(player));

        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack it = inv.getItem(slot);
            if (it != null) PapiUtil.parseItem(player, it);
        }
        player.openInventory(inv);
    }

    private ItemStack renderInfo(String groupId, GuideData data, int page, int totalPages) {
        ItemStack it = new ItemStack(Material.BOOK);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(TITLE_ICON_NAME);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + "\u00a7m----------------");
            lore.add(ChatColor.YELLOW + "\u672c\u5206\u7c7b\u751f\u6548\u5c5e\u6027:");
            List<GuideAttrSummary.Entry> attrs = GuideAttrSummary.summarizeGroup(groupId, data);
            if (attrs.isEmpty()) {
                lore.add(ChatColor.GRAY + "  \uff08\u6682\u65e0\uff09");
            } else {
                for (GuideAttrSummary.Entry e : attrs) {
                    lore.add(ChatColor.GRAY + "  \u00b7 " + ChatColor.WHITE + e.name
                            + " " + ChatColor.GREEN + "+" + formatVal(e.value));
                }
            }
            lore.add(ChatColor.DARK_GRAY + "\u00a7m----------------");
            lore.add(ChatColor.GRAY + "\u9875\u6570: " + ChatColor.WHITE + (page + 1) + "/" + totalPages);
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack renderScoreInfo(Player player) {
        com.longdrange.ldattribute.core.guide.GuideScoreManager sm =
                plugin.getCoreManager().getGuideScoreManager();
        int score = (sm == null) ? 0 : sm.getScore(player);
        com.longdrange.ldattribute.core.guide.GuideScoreConfig.TitleDef title =
                (sm == null) ? null : sm.getCurrentTitle(player);

        ItemStack it = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "\u2726 \u6536\u85cf\u5206 / \u79f0\u53f7");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + "\u00a7m----------------");
            lore.add(ChatColor.YELLOW + "\u5f53\u524d\u6536\u85cf\u5206: " + ChatColor.WHITE + score);
            lore.add(ChatColor.YELLOW + "\u5f53\u524d\u79f0\u53f7: "
                    + (title == null ? ChatColor.GRAY + "\uff08\u65e0\uff09" : title.name));
            lore.add(ChatColor.DARK_GRAY + "\u00a7m----------------");

            com.longdrange.ldattribute.core.guide.GuideScoreConfig.TitleDef next = null;
            for (com.longdrange.ldattribute.core.guide.GuideScoreConfig.TitleDef t :
                    com.longdrange.ldattribute.core.guide.GuideScoreConfig.allTitles()) {
                if (t.score > score) { next = t; break; }
            }
            if (next != null) {
                lore.add(ChatColor.YELLOW + "\u4e0b\u4e00\u79f0\u53f7: " + next.name);
                lore.add(ChatColor.GRAY + "  \u8fd8\u9700 " + ChatColor.WHITE + (next.score - score)
                        + ChatColor.GRAY + " \u5206 (" + score + "/" + next.score + ")");
            } else {
                lore.add(ChatColor.GREEN + "\u5df2\u8fbe\u5230\u6700\u9ad8\u79f0\u53f7");
            }

            lore.add(ChatColor.DARK_GRAY + "\u00a7m----------------");
            lore.add(ChatColor.GRAY + "\u89e3\u9501\u66f4\u591a\u56fe\u9274\u63d0\u5347\u6536\u85cf\u5206");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack monsterIcon(GuideConfig.MonsterDef def, GuideData data) {
        boolean unlocked = data.isUnlocked(def.id);
        int kills = data.getKills(def.id);
        int req = def.requiredKills;

        ItemStack it = new ItemStack(Material.STAINED_GLASS_PANE, 1,
                unlocked ? GLASS_UNLOCKED : GLASS_LOCKED);
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;

        String prefix = unlocked ? ChatColor.GREEN + "\u2714 " : ChatColor.GRAY + "\u2718 ";
        meta.setDisplayName(prefix + def.name);

        List<String> lore = new ArrayList<>();
        if (unlocked) lore.add(ChatColor.GREEN + "\u72b6\u6001: " + ChatColor.WHITE + "\u5df2\u89e3\u9501");
        else lore.add(ChatColor.GRAY + "\u72b6\u6001: " + ChatColor.YELLOW + "\u672a\u89e3\u9501");
        lore.add("");

        if (!unlocked) {
            int filled = Math.min(10, (int) Math.round(kills * 10.0 / Math.max(1, req)));
            StringBuilder bar = new StringBuilder();
            for (int k = 0; k < 10; k++) {
                bar.append(k < filled ? ChatColor.GREEN + "\u2588" : ChatColor.DARK_GRAY + "\u2591");
            }
            double pct = kills * 100.0 / Math.max(1, req);
            lore.add(ChatColor.WHITE + "\u51fb\u6740\u8fdb\u5ea6: "
                    + ChatColor.YELLOW + kills + ChatColor.GRAY + "/" + ChatColor.YELLOW + req
                    + ChatColor.GRAY + " (" + String.format("%.1f%%", pct) + ")");
            lore.add(ChatColor.GRAY + "  " + bar.toString());
            lore.add("");
        }

        if (def.unlockChance > 0) {
            String chanceColor = (def.unlockChance >= 0.1 ? ChatColor.GREEN
                    : def.unlockChance >= 0.02 ? ChatColor.YELLOW
                    : ChatColor.RED).toString();
            lore.add(ChatColor.WHITE + "\u6982\u7387\u89e3\u9501: " + chanceColor
                    + String.format("%.2f%%", def.unlockChance * 100)
                    + ChatColor.GRAY + "  (\u6bcf\u6b21\u51fb\u6740)");
            lore.add("");
        }

        if (!def.lore.isEmpty()) {
            lore.addAll(def.lore);
            lore.add("");
        }

        if (def.score > 0) {
            lore.add(ChatColor.WHITE + "\u6536\u85cf\u5206: " + ChatColor.GOLD + def.score);
            lore.add("");
        }
        lore.add(ChatColor.WHITE + "\u5c5e\u6027\u52a0\u6210:");
        if (def.attribute.isEmpty()) {
            lore.add(ChatColor.GRAY + "  \uff08\u65e0\uff09");
        } else {
            for (String a : def.attribute) {
                lore.add((unlocked ? ChatColor.GREEN : ChatColor.GRAY) + "  " + a);
            }
        }

        if (def.rewardPoints > 0 || def.rewardVault > 0
                || !def.rewardItems.isEmpty() || !def.rewardCommands.isEmpty()) {
            lore.add("");
            lore.add(ChatColor.WHITE + "\u89e3\u9501\u5956\u52b1:");
            if (def.rewardPoints > 0) lore.add(ChatColor.GOLD + "  \u70b9\u5238: " + def.rewardPoints);
            if (def.rewardVault > 0) lore.add(ChatColor.GOLD + "  \u91d1\u5e01: " + (long) def.rewardVault);
            for (String s : def.rewardItems) lore.add(ChatColor.YELLOW + "  \u7269\u54c1: " + s);
            for (String s : def.rewardCommands) lore.add(ChatColor.YELLOW + "  \u547d\u4ee4: " + s);
        }

        if (!unlocked) {
            lore.add("");
            lore.add(ChatColor.YELLOW + "\u5982\u4f55\u89e3\u9501:");
            lore.add(ChatColor.GRAY + "  \u00b7 \u51fb\u6740\u8be5\u602a\u7269\u7d2f\u8ba1 " + req + " \u6b21");
            if (def.unlockChance > 0)
                lore.add(ChatColor.GRAY + "  \u00b7 \u6216\u6bcf\u6b21\u51fb\u6740\u6709 " + String.format("%.2f%%", def.unlockChance * 100) + " \u6982\u7387");
            lore.add(ChatColor.GRAY + "  \u00b7 \u6216\u7528 " + ChatColor.WHITE + "\u5716\u9451\u89e3\u9396\u77f3(" + def.id + ")"
                    + ChatColor.GRAY + " \u53f3\u952e");
        }

        meta.setLore(lore);
        it.setItemMeta(meta);
        return it;
    }

    private String formatVal(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }

    private ItemStack icon(Material mat, String name, String... lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && lore.length > 0) meta.setLore(Arrays.asList(lore));
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack glass(short data, String name) {
        ItemStack it = new ItemStack(Material.STAINED_GLASS_PANE, 1, data);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); it.setItemMeta(meta); }
        return it;
    }
}
