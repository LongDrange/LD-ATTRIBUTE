package com.longdrange.ldattribute.core.soulring.gui;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.soulring.SoulRingData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SoulRingTrashGUI {

    public static final int BTN_PREV = 45;
    public static final int BTN_NEXT = 53;
    public static final int BTN_INFO = 49;
    public static final int BTN_BACK = 48;

    public static final int PER_PAGE = 45;

    private final LDAttribute plugin;
    public SoulRingTrashGUI(LDAttribute plugin) { this.plugin = plugin; }

    public void open(Player player) { open(player, 0); }

    public void open(Player player, int page) {
        String title = ChatColor.DARK_RED + "✦ 灵魂垃圾桶";
        SoulRingTrashHolder holder = new SoulRingTrashHolder(player.getUniqueId());
        holder.setPage(page);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);
        render(holder);
        player.openInventory(inv);
    }

    public void refresh(Player player, SoulRingTrashHolder holder) {
        if (player == null || holder == null) return;
        render(holder);
        try { player.updateInventory(); } catch (Throwable ignored) {}
    }

    private void render(SoulRingTrashHolder holder) {
        Inventory inv = holder.getInventory();
        if (inv == null) return;
        Player player = Bukkit.getPlayer(holder.getOwner());
        if (player == null) return;

        SoulRingData data = plugin.getSoulRingManager().get(player);
        for (int i = 0; i < 54; i++) inv.setItem(i, null);

        List<SoulRingData.Entry> all = new ArrayList<>(data.getEntries());
        int totalPages = Math.max(1, (all.size() + PER_PAGE - 1) / PER_PAGE);
        int page = holder.getPage();
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;
        holder.setPage(page);

        List<SoulRingData.Entry> pageList = new ArrayList<>();
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = start + i;
            if (idx >= all.size()) break;
            SoulRingData.Entry e = all.get(idx);
            inv.setItem(i, renderEntry(e));
            pageList.add(e);
        }
        holder.setPageEntries(pageList);

        if (page > 0) inv.setItem(BTN_PREV, icon(Material.ARROW, ChatColor.GRAY + "← 上一页"));
        else inv.setItem(BTN_PREV, glass((short) 15, ChatColor.DARK_GRAY + "已是首页"));

        if (page < totalPages - 1) inv.setItem(BTN_NEXT, icon(Material.ARROW, ChatColor.GRAY + "下一页 →"));
        else inv.setItem(BTN_NEXT, glass((short) 15, ChatColor.DARK_GRAY + "已是末页"));

        inv.setItem(BTN_BACK, icon(Material.ARROW, ChatColor.GRAY + "返回灵魂空间"));

        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.YELLOW + "第 " + (page + 1) + " / " + totalPages + " 页");
        infoLore.add(ChatColor.GRAY + "总计 " + all.size() + " 种物品");
        infoLore.add("");
        infoLore.add(ChatColor.RED + "点击物品 = 直接删除");
        infoLore.add(ChatColor.GRAY + "  左键 = 删 1 个");
        infoLore.add(ChatColor.GRAY + "  右键 = 删 16 个");
        infoLore.add(ChatColor.GRAY + "  Shift+左键 = 删 64 个");
        infoLore.add(ChatColor.GRAY + "  Shift+右键 = 删 全部");
        inv.setItem(BTN_INFO, iconLore(Material.PAPER, ChatColor.AQUA + "操作说明", infoLore));
    }

    private ItemStack renderEntry(SoulRingData.Entry e) {
        ItemStack it = e.template.clone();
        it.setAmount((int) Math.min(e.count, 64));
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.YELLOW + "数量: " + ChatColor.WHITE + e.count);
            lore.add("");
            lore.add(ChatColor.RED + "【点击删除】");
            lore.add(ChatColor.GRAY + "  左键 = 删 1");
            lore.add(ChatColor.GRAY + "  右键 = 删 16");
            lore.add(ChatColor.GRAY + "  Shift+左键 = 删 64");
            lore.add(ChatColor.GRAY + "  Shift+右键 = 删全部");
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

    private ItemStack iconLore(Material mat, String name, List<String> lore) {
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
