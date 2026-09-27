package com.longdrange.ldattribute.core.dungeon.gui;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.dungeon.DungeonConfig;
import com.longdrange.ldattribute.core.dungeon.DungeonRankManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DungeonRankGUI {

    public static final int PER_PAGE = 45;
    public static final int BTN_PREV = 45;
    public static final int BTN_INFO = 49;
    public static final int BTN_NEXT = 53;

    private final LDAttribute plugin;
    public DungeonRankGUI(LDAttribute plugin) { this.plugin = plugin; }

    public void open(Player p) { open(p, 0); }

    public void open(Player p, int page) {
        List<DungeonConfig.DungeonDef> list = DungeonConfig.visibleFor(p);
        int totalPages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        String title = ChatColor.DARK_GRAY + "✦ 副本排行 " + ChatColor.GRAY + "(" + (page + 1) + "/" + totalPages + ")";
        org.bukkit.inventory.InventoryHolder holder = Bukkit.createInventory(null, 54, title).getHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, title);

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = start + i;
            if (idx >= list.size()) break;
            inv.setItem(i, renderIcon(list.get(idx)));
        }

        if (page > 0) inv.setItem(BTN_PREV, icon(Material.ARROW, ChatColor.GRAY + "← 上一页"));
        else inv.setItem(BTN_PREV, glass((short) 15, ChatColor.DARK_GRAY + "已是首页"));

        if (page < totalPages - 1) inv.setItem(BTN_NEXT, icon(Material.ARROW, ChatColor.GRAY + "下一页 →"));
        else inv.setItem(BTN_NEXT, glass((short) 15, ChatColor.DARK_GRAY + "已是末页"));

        List<String> info = new ArrayList<>();
        info.add(ChatColor.YELLOW + "点击副本图标查看详细信息");
        inv.setItem(BTN_INFO, icon(Material.BOOK, ChatColor.AQUA + "副本排行", info));

        p.openInventory(inv);
    }

    public void openDetail(Player p, String dungeonId) {
        DungeonConfig.DungeonDef def = DungeonConfig.get(dungeonId);
        if (def == null) return;

        String title = ChatColor.DARK_GRAY + "✦ " + ChatColor.stripColor(def.name) + " 排行";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        DungeonRankManager.RankEntry e = plugin.getDungeonRankManager().get(dungeonId);

        // 综合信息（13 格）
        List<String> info = new ArrayList<>();
        info.add(ChatColor.YELLOW + "总通关: " + ChatColor.WHITE + e.totalClears + " 次");
        info.add(ChatColor.YELLOW + "总积分: " + ChatColor.WHITE + e.totalPoints);
        info.add("");
        if (e.fastestTime != Long.MAX_VALUE && e.fastestTime > 0) {
            info.add(ChatColor.GOLD + "最快通关: " + ChatColor.WHITE + DungeonRankManager.formatTime(e.fastestTime));
            info.add(ChatColor.GRAY + "  保持者: " + ChatColor.WHITE + e.fastestPlayer);
        } else {
            info.add(ChatColor.GRAY + "最快通关: " + ChatColor.WHITE + "无记录");
        }
        inv.setItem(13, icon(Material.NETHER_STAR, ChatColor.AQUA + "副本统计", info));

        // 玩家排行（前 10，放在 18~27）
        List<Map.Entry<String, Integer>> top = plugin.getDungeonRankManager().topPlayers(dungeonId, 10);
        int slot = 18;
        int rank = 1;
        for (Map.Entry<String, Integer> en : top) {
            if (slot >= 28) break;
            String pName = en.getKey();
            int cnt = en.getValue();

            ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                try { meta.setOwner(pName); } catch (Throwable ignored) {}
                String prefix;
                if (rank == 1) prefix = ChatColor.GOLD + "#1 ";
                else if (rank == 2) prefix = ChatColor.GRAY + "#2 ";
                else if (rank == 3) prefix = ChatColor.YELLOW + "#3 ";
                else prefix = ChatColor.DARK_GRAY + "#" + rank + " ";
                meta.setDisplayName(prefix + ChatColor.WHITE + pName);
                meta.setLore(Arrays.asList(ChatColor.YELLOW + "通关: " + ChatColor.WHITE + cnt + " 次"));
                head.setItemMeta(meta);
            }
            inv.setItem(slot, head);
            slot++;
            rank++;
        }

        if (top.isEmpty()) {
            inv.setItem(22, icon(Material.BARRIER, ChatColor.GRAY + "暂无排行数据"));
        }

        // 返回按钮
        inv.setItem(49, icon(Material.ARROW, ChatColor.GRAY + "← 返回列表"));

        p.openInventory(inv);
    }

    private ItemStack renderIcon(DungeonConfig.DungeonDef def) {
        Material mat = Material.getMaterial(def.icon);
        if (mat == null) mat = Material.STONE;
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(def.name);
            DungeonRankManager.RankEntry e = plugin.getDungeonRankManager().get(def.id);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "总通关: " + ChatColor.WHITE + e.totalClears + " 次");
            lore.add(ChatColor.YELLOW + "总积分: " + ChatColor.WHITE + e.totalPoints);
            if (e.fastestTime != Long.MAX_VALUE && e.fastestTime > 0) {
                lore.add(ChatColor.GOLD + "最快: " + ChatColor.WHITE + DungeonRankManager.formatTime(e.fastestTime)
                        + ChatColor.GRAY + " (" + e.fastestPlayer + ")");
            } else {
                lore.add(ChatColor.GRAY + "最快: 无记录");
            }
            lore.add("");
            lore.add(ChatColor.GRAY + "点击查看详细排行");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
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

    private ItemStack icon(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            m.setLore(lore);
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