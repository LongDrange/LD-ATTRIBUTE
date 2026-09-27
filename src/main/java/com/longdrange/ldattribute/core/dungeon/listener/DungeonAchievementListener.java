package com.longdrange.ldattribute.core.dungeon.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.dungeon.gui.DungeonAchievementGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class DungeonAchievementListener implements Listener {

    private final LDAttribute plugin;
    public DungeonAchievementListener(LDAttribute plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getView().getTitle();
        if (!title.contains("副本成就")) return;
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        if (slot == DungeonAchievementGUI.BTN_PREV) {
            // 简化处理：重开
            plugin.getDungeonAchievementGUI().open(p, 0);
            return;
        }
        if (slot == DungeonAchievementGUI.BTN_NEXT) {
            plugin.getDungeonAchievementGUI().open(p, 1);
            return;
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTitle().contains("副本成就")) {
            e.setCancelled(true);
        }
    }
}