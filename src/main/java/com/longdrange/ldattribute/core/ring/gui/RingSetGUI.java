package com.longdrange.ldattribute.core.ring.gui;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.ring.RingData;
import com.longdrange.ldattribute.core.ring.RingSetConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RingSetGUI {

    public static class SetHolder implements InventoryHolder {
        private Inventory inv;
        @Override public Inventory getInventory() { return inv; }
        public void setInventory(Inventory inv) { this.inv = inv; }
    }

    private final LDAttribute plugin;
    public RingSetGUI(LDAttribute plugin) { this.plugin = plugin; }

    public void open(Player player) {
        SetHolder holder = new SetHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, ChatColor.DARK_GRAY + "魂珠套装");
        holder.setInventory(inv);

        RingData data = plugin.getRingManager().get(player);
        List<RingSetConfig.SetDef> list = new ArrayList<>(RingSetConfig.all());
        int i = 0;
        for (RingSetConfig.SetDef def : list) {
            if (i >= 45) break;
            inv.setItem(i++, buildIcon(def, data));
        }
        if (list.isEmpty()) {
            inv.setItem(22, plain(Material.BARRIER, ChatColor.GRAY + "未配置任何套装"));
        }
        inv.setItem(49, plain(Material.ARROW, ChatColor.GRAY + "返回魂珠空间"));
        player.openInventory(inv);
    }

    private ItemStack buildIcon(RingSetConfig.SetDef def, RingData data) {
        boolean active = true;
        List<String> lore = new ArrayList<>();
        if (!def.description.isEmpty()) lore.add(ChatColor.GRAY + def.description);
        lore.add("");
        lore.add(ChatColor.WHITE + "需求:");

        for (Map.Entry<String, Integer> e : def.requirements.entrySet()) {
            int have = data.countType(e.getKey());
            int need = e.getValue();
            boolean ok = have >= need;
            if (!ok) active = false;
            lore.add((ok ? ChatColor.GREEN + "  \u2714 " : ChatColor.RED + "  \u2718 ")
                    + ChatColor.WHITE + e.getKey() + " " + have + "/" + need);
        }
        lore.add("");
        lore.add(ChatColor.WHITE + "加成:");
        for (String line : def.attributes) lore.add(ChatColor.GRAY + "  " + line);
        lore.add("");
        lore.add(active ? ChatColor.GREEN + "\u2714 已激活" : ChatColor.GRAY + "\u2718 未激活");

        Material mat = active ? Material.NETHER_STAR : Material.STAINED_GLASS_PANE;
        short dataVal = (short)(active ? 0 : 14);
        ItemStack it = new ItemStack(mat, 1, dataVal);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName((active ? ChatColor.GREEN + "\u2714 " : ChatColor.GRAY + "\u2718 ") + def.name);
            m.setLore(lore);
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
}