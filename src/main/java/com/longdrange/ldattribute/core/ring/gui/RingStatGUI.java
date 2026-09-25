package com.longdrange.ldattribute.core.ring.gui;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.ring.RingAttributeReader;
import com.longdrange.ldattribute.core.ring.RingData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RingStatGUI {

    public static class StatHolder implements InventoryHolder {
        private Inventory inv;
        @Override public Inventory getInventory() { return inv; }
        public void setInventory(Inventory inv) { this.inv = inv; }
    }

    private final LDAttribute plugin;
    public RingStatGUI(LDAttribute plugin) { this.plugin = plugin; }

    public void open(Player player, int page) {
        RingData data = plugin.getRingManager().get(player);
        Map<String, Double> total = RingAttributeReader.sumAll(data);
        List<Map.Entry<String, Double>> list = new ArrayList<>(total.entrySet());

        int perPage = 45;
        int totalPages = Math.max(1, (list.size() + perPage - 1) / perPage);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        int code = Math.abs(player.getUniqueId().hashCode()) % 10000;
        String title = ChatColor.DARK_GRAY + "您的属性 " + RingGUI.tag(code)
                + ChatColor.DARK_GRAY + " / " + totalPages;

        StatHolder holder = new StatHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        int start = page * perPage;
        int end = Math.min(start + perPage, list.size());
        for (int i = start; i < end; i++) {
            Map.Entry<String, Double> e = list.get(i);
            inv.setItem(i - start, statIcon(e.getKey(), e.getValue()));
        }

        if (list.isEmpty()) {
            inv.setItem(22, plain(Material.BARRIER, ChatColor.GRAY + "暂无魂珠属性"));
        }

        if (page > 0) inv.setItem(45, plain(Material.ARROW, ChatColor.GRAY + "\u2190 上一页"));
        else inv.setItem(45, plain(Material.STAINED_GLASS_PANE, ChatColor.DARK_GRAY + "\u2190 已是首页", (short) 15));

        inv.setItem(49, plain(Material.PAPER, ChatColor.AQUA + "第 " + (page + 1) + " / " + totalPages + " 页"));

        if (page < totalPages - 1) inv.setItem(53, plain(Material.ARROW, ChatColor.GRAY + "下一页 \u2192"));
        else inv.setItem(53, plain(Material.STAINED_GLASS_PANE, ChatColor.DARK_GRAY + "已是末页 \u2192", (short) 15));

        player.openInventory(inv);
    }

    private ItemStack statIcon(String key, double value) {
        ItemStack it = new ItemStack(Material.PAPER);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(ChatColor.GREEN + key + ": " + ChatColor.WHITE + RingGUI.formatNum(value));
            it.setItemMeta(m);
        }
        return it;
    }

    private ItemStack plain(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) { m.setDisplayName(name); it.setItemMeta(m); }
        return it;
    }

    private ItemStack plain(Material mat, String name, short data) {
        ItemStack it = new ItemStack(mat, 1, data);
        ItemMeta m = it.getItemMeta();
        if (m != null) { m.setDisplayName(name); it.setItemMeta(m); }
        return it;
    }
}