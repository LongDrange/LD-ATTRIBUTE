package com.longdrange.ldattribute.core.soulring.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.soulring.SoulRingData;
import com.longdrange.ldattribute.core.soulring.gui.SoulRingTrashGUI;
import com.longdrange.ldattribute.core.soulring.gui.SoulRingTrashHolder;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.List;

public class SoulRingTrashListener implements Listener {

    private final LDAttribute plugin;
    public SoulRingTrashListener(LDAttribute plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!(e.getInventory().getHolder() instanceof SoulRingTrashHolder)) return;
        Player player = (Player) e.getWhoClicked();
        SoulRingTrashHolder holder = (SoulRingTrashHolder) e.getInventory().getHolder();

        e.setCancelled(true);
        int slot = e.getRawSlot();
        int topSize = e.getInventory().getSize();
        if (slot >= topSize) return;

        if (slot >= 0 && slot < 45) {
            List<SoulRingData.Entry> page = holder.getPageEntries();
            if (slot >= page.size()) return;
            SoulRingData.Entry entry = page.get(slot);
            if (entry == null || entry.count <= 0) return;

            long want;
            if (e.isShiftClick() && e.isRightClick()) want = entry.count;
            else if (e.isShiftClick() && e.isLeftClick()) want = 64;
            else if (e.isRightClick()) want = 16;
            else want = 1;

            long actual = Math.min(want, entry.count);
            SoulRingData data = plugin.getSoulRingManager().get(player);
            String name = displayName(entry.template);
            long deleted = data.takeByEntry(entry, actual);
            if (deleted <= 0) {
                player.sendMessage(com.longdrange.ldattribute.util.Message.get("SoulRing.TrashFailed"));
                return;
            }
            player.sendMessage(com.longdrange.ldattribute.util.Message.get("SoulRing.TrashDeleted", name, deleted));
            try { com.longdrange.ldattribute.util.AuditLog.write(player, "TrashDelete", name + " x" + deleted); } catch (Throwable ignored) {}
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 0.5f);
            plugin.getSoulRingTrashGUI().refresh(player, holder);
            return;
        }

        if (slot == SoulRingTrashGUI.BTN_PREV) {
            if (holder.getPage() > 0) {
                holder.setPage(holder.getPage() - 1);
                plugin.getSoulRingTrashGUI().refresh(player, holder);
            }
            return;
        }
        if (slot == SoulRingTrashGUI.BTN_NEXT) {
            holder.setPage(holder.getPage() + 1);
            plugin.getSoulRingTrashGUI().refresh(player, holder);
            return;
        }
        if (slot == SoulRingTrashGUI.BTN_BACK) {
            plugin.getSoulRingGUI().open(player);
            return;
        }
    }


    private static String displayName(ItemStack item) {
        if (item == null) return "未知物品";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return ChatColor.stripColor(item.getItemMeta().getDisplayName());
        }
        String mat = item.getType().name();
        String[] parts = mat.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (int pi = 0; pi < parts.length; pi++) {
            String p = parts[pi];
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1));
        }
        return sb.toString().trim();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof SoulRingTrashHolder) {
            e.setCancelled(true);
        }
    }
}
