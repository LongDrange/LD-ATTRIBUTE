package com.longdrange.ldattribute.core.dungeon.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.dungeon.DungeonConfig;
import com.longdrange.ldattribute.core.dungeon.gui.DungeonRankGUI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class DungeonRankListener implements Listener {

    private final LDAttribute plugin;
    public DungeonRankListener(LDAttribute plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();

        if (!title.contains("副本排行") && !title.contains("排行")) return;
        e.setCancelled(true);
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        // 返回列表
        if (slot == 49 && title.contains("排行") && !title.contains("副本排行")) {
            plugin.getDungeonRankGUI().open(p, 0);
            return;
        }

        // 翻页
        if (slot == DungeonRankGUI.BTN_PREV) {
            plugin.getDungeonRankGUI().open(p, 0);
            return;
        }
        if (slot == DungeonRankGUI.BTN_NEXT) {
            plugin.getDungeonRankGUI().open(p, 1);
            return;
        }

        // 点副本图标 → 详情
        if (title.contains("副本排行") && slot < 45) {
            // 从显示列表里反推
            java.util.List<DungeonConfig.DungeonDef> list = DungeonConfig.visibleFor(p);
            if (slot < list.size()) {
                plugin.getDungeonRankGUI().openDetail(p, list.get(slot).id);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        String title = e.getView().getTitle();
        if (title.contains("排行")) {
            e.setCancelled(true);
        }
    }
}