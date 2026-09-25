package com.longdrange.ldattribute.core.ring.gui;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.ring.RingConfig;
import com.longdrange.ldattribute.core.ring.RingCost;
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

public class RingPageUnlockGUI {

    public static final int BTN_CONFIRM = 11;
    public static final int BTN_INFO    = 13;
    public static final int BTN_CANCEL  = 15;
    public static final String TITLE = ChatColor.DARK_GRAY + "解锁页面确认";

    public static class PageUnlockHolder implements InventoryHolder {
        public final UUID owner;
        public final int targetPage;
        public final int backPage;
        private Inventory inv;
        public PageUnlockHolder(UUID owner, int targetPage, int backPage) {
            this.owner = owner; this.targetPage = targetPage; this.backPage = backPage;
        }
        @Override public Inventory getInventory() { return inv; }
        public void setInventory(Inventory inv) { this.inv = inv; }
    }

    private final LDAttribute plugin;
    public RingPageUnlockGUI(LDAttribute plugin) { this.plugin = plugin; }

    public void open(Player player, int targetPage, int backPage) {
        PageUnlockHolder holder = new PageUnlockHolder(player.getUniqueId(), targetPage, backPage);
        Inventory inv = Bukkit.createInventory(holder, 27, TITLE);
        holder.setInventory(inv);

        RingConfig.PageUnlock cfg = RingConfig.getPageUnlock(targetPage);
        RingData data = plugin.getRingManager().get(player);
        boolean already = data.getUnlockedPages() > targetPage;

        List<String> needLore = new ArrayList<>();
        needLore.add(ChatColor.GRAY + "解锁第 " + (targetPage + 1) + " 页");
        needLore.add("");
        needLore.add(ChatColor.WHITE + "解锁需求:");
        if (cfg != null) {
            needLore.addAll(RingCost.describe(cfg.permission, cfg.costPoints, cfg.costVault, cfg.items));
        } else {
            needLore.add(ChatColor.GRAY + "  无（免费）");
        }
        inv.setItem(BTN_CONFIRM, icon(Material.STAINED_GLASS_PANE,
                ChatColor.GREEN + "\u2714 确认解锁", needLore, (short) 5));

        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.GRAY + "目标页: 第 " + (targetPage + 1) + " 页");
        infoLore.add("");
        infoLore.add(already ? ChatColor.GREEN + "\u2714 已解锁" : ChatColor.RED + "\u2718 尚未解锁");
        inv.setItem(BTN_INFO, icon(Material.PAPER, ChatColor.AQUA + "页面信息", infoLore, (short) 0));

        inv.setItem(BTN_CANCEL, icon(Material.STAINED_GLASS_PANE,
                ChatColor.RED + "\u2718 取消",
                Collections.singletonList(ChatColor.GRAY + "返回魂珠空间"), (short) 14));

        player.openInventory(inv);
    }

    private ItemStack icon(Material mat, String name, List<String> lore, short data) {
        ItemStack it = new ItemStack(mat, 1, data);
        ItemMeta m = it.getItemMeta();
        if (m != null) { m.setDisplayName(name); if (lore != null) m.setLore(lore); it.setItemMeta(m); }
        return it;
    }
}