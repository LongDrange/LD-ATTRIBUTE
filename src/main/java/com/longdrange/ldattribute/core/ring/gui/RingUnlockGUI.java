package com.longdrange.ldattribute.core.ring.gui;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.ring.RingCost;
import com.longdrange.ldattribute.core.ring.RingData;
import com.longdrange.ldattribute.core.ring.RingSlotConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RingUnlockGUI {

    public static final int BTN_CONFIRM = 11;
    public static final int BTN_INFO    = 13;
    public static final int BTN_CANCEL  = 15;
    public static final String TITLE = ChatColor.DARK_GRAY + "解锁槽位确认";

    public static class UnlockHolder implements InventoryHolder {
        public final UUID owner;
        public final int slotId;
        public final int backPage;
        private Inventory inv;
        public UnlockHolder(UUID owner, int slotId, int backPage) {
            this.owner = owner; this.slotId = slotId; this.backPage = backPage;
        }
        @Override public Inventory getInventory() { return inv; }
        public void setInventory(Inventory inv) { this.inv = inv; }
    }

    private final LDAttribute plugin;
    public RingUnlockGUI(LDAttribute plugin) { this.plugin = plugin; }

    public void open(Player player, int slotId, int backPage) {
        UnlockHolder holder = new UnlockHolder(player.getUniqueId(), slotId, backPage);
        Inventory inv = Bukkit.createInventory(holder, 27, TITLE);
        holder.setInventory(inv);

        RingSlotConfig.SlotUnlock cfg = RingSlotConfig.get(slotId);
        RingData data = plugin.getRingManager().get(player);
        boolean already = data.isUnlocked(slotId);

        // 需求列表
        List<String> needLore = new ArrayList<>();
        needLore.add(ChatColor.GRAY + "解锁槽位 " + RingGUI.tag(slotId));
        needLore.add("");
        needLore.add(ChatColor.WHITE + "解锁需求:");
        if (cfg != null) {
            needLore.addAll(RingCost.describe(cfg.permission, cfg.costPoints, cfg.costVault, cfg.items));
        } else {
            needLore.add(ChatColor.GRAY + "  无（免费）");
        }
        inv.setItem(BTN_CONFIRM, icon(Material.STAINED_GLASS_PANE,
                ChatColor.GREEN + "\u2714 确认解锁", needLore, (short) 5));

        // 信息
        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.GRAY + "槽位: " + RingGUI.tag(slotId));
        infoLore.add("");
        infoLore.add(already ? ChatColor.GREEN + "\u2714 你已解锁此槽位" : ChatColor.RED + "\u2718 尚未解锁");
        inv.setItem(BTN_INFO, icon(Material.PAPER, ChatColor.AQUA + "槽位信息", infoLore, (short) 0));

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