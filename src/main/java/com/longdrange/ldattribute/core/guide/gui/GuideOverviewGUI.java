package com.longdrange.ldattribute.core.guide.gui;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.guide.GuideAttrSummary;
import com.longdrange.ldattribute.core.guide.GuideConfig;
import com.longdrange.ldattribute.core.guide.GuideData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GuideOverviewGUI {

    public static final int GROUPS_START = 9;
    public static final int GROUPS_PER_PAGE = 36;

    public static final int BTN_PREV = 45;
    public static final int BTN_INFO = 49;
    public static final int BTN_SCORE = 50;
    public static final int BTN_NEXT = 53;

    private static final String TITLE_ICON_NAME = "\u00a7b【理想龙猫】\u00a72怪物圖鑑";

    private final LDAttribute plugin;
    public GuideOverviewGUI(LDAttribute plugin) { this.plugin = plugin; }

    public void open(Player player) { open(player, 0); }

    public void open(Player player, int page) {
        GuideData data = plugin.getGuideManager().get(player);

        List<GuideConfig.GroupDef> visible = GuideConfig.visibleGroups(player);

        int totalPages = Math.max(1, (visible.size() + GROUPS_PER_PAGE - 1) / GROUPS_PER_PAGE);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        String title = ChatColor.DARK_GRAY + "\u2726 " + ChatColor.AQUA + "图鉴总览 "
                + ChatColor.DARK_GRAY + "(" + (page + 1) + "/" + totalPages + ")";

        GuideOverviewHolder holder = new GuideOverviewHolder(player.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        // 0~8 标题栏
        ItemStack titleBar = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta tmeta = titleBar.getItemMeta();
        if (tmeta != null) {
            tmeta.setDisplayName("\u00a7b\u3010\u7406\u60f3\u9f99\u732b\u3011\u00a7c\u4f3a\u670d\u5668\u00a72\u602a\u7269\u5716\u9451");
            titleBar.setItemMeta(tmeta);
        }
        for (int ti = 0; ti < 9; ti++) inv.setItem(ti, titleBar);

        int start = page * GROUPS_PER_PAGE;
        for (int i = 0; i < GROUPS_PER_PAGE; i++) {
            int idx = start + i;
            if (idx >= visible.size()) break;
            inv.setItem(GROUPS_START + i, renderGroupIcon(visible.get(idx), data));
        }

        if (page > 0) inv.setItem(BTN_PREV, icon(Material.ARROW, ChatColor.GRAY + "\u2190 上一页"));
        else inv.setItem(BTN_PREV, glass((short) 15, ChatColor.DARK_GRAY + "已是首页"));

        if (page < totalPages - 1) inv.setItem(BTN_NEXT, icon(Material.ARROW, ChatColor.GRAY + "下一页 \u2192"));
        else inv.setItem(BTN_NEXT, glass((short) 15, ChatColor.DARK_GRAY + "已是末页"));

        inv.setItem(BTN_INFO, renderInfoAll(data, page, totalPages));
        inv.setItem(BTN_SCORE, renderScoreInfo(player));

        player.openInventory(inv);
    }

    private ItemStack renderInfoAll(GuideData data, int page, int totalPages) {
        ItemStack it = new ItemStack(Material.BOOK);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(TITLE_ICON_NAME);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + "\u00a7m----------------");
            lore.add(ChatColor.YELLOW + "所有图鉴生效属性:");
            List<GuideAttrSummary.Entry> attrs = GuideAttrSummary.summarizeAll(data);
            if (attrs.isEmpty()) {
                lore.add(ChatColor.GRAY + "  （暂无）");
            } else {
                for (GuideAttrSummary.Entry e : attrs) {
                    lore.add(ChatColor.GRAY + "  \u00b7 " + ChatColor.WHITE + e.name
                            + " " + ChatColor.GREEN + "+" + formatVal(e.value));
                }
            }
            lore.add(ChatColor.DARK_GRAY + "\u00a7m----------------");
            lore.add(ChatColor.GRAY + "分类页: " + ChatColor.WHITE + (page + 1) + "/" + totalPages);
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

            if (title != null && !title.attribute.isEmpty()) {
                lore.add(ChatColor.DARK_GRAY + "\u00a7m----------------");
                lore.add(ChatColor.YELLOW + "\u79f0\u53f7\u5c5e\u6027:");
                for (String a : title.attribute) {
                    lore.add(ChatColor.GRAY + "  \u00b7 " + ChatColor.WHITE + a);
                }
            }

            lore.add(ChatColor.DARK_GRAY + "\u00a7m----------------");
            lore.add(ChatColor.GRAY + "\u89e3\u9501\u66f4\u591a\u56fe\u9274\u63d0\u5347\u6536\u85cf\u5206");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private int[] countGroupProgress(String groupId, GuideData data) {
        int unlocked = 0, total = 0;
        for (GuideConfig.MonsterDef d : GuideConfig.byGroup(groupId)) {
            total++;
            if (data.isUnlocked(d.id)) unlocked++;
        }
        return new int[]{unlocked, total};
    }

    private ItemStack renderGroupIcon(GuideConfig.GroupDef g, GuideData data) {
        int[] prog = countGroupProgress(g.id, data);
        Material m = Material.getMaterial(g.icon);
        if (m == null) m = Material.CHEST;
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(g.name);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "已激活: " + ChatColor.WHITE + prog[0] + "/" + prog[1]);
            lore.add("");
            lore.add(ChatColor.GRAY + "点击查看该分类的所有图鉴");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private String formatVal(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }

    private ItemStack icon(Material mat, String name, String... lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            if (lore != null && lore.length > 0) m.setLore(Arrays.asList(lore));
            it.setItemMeta(m);
        }
        return it;
    }

    private ItemStack glass(short data, String name) {
        ItemStack it = new ItemStack(Material.STAINED_GLASS_PANE, 1, data);
        ItemMeta m = it.getItemMeta();
        if (m != null) { m.setDisplayName(name); it.setItemMeta(m); }
        return it;
    }
}
