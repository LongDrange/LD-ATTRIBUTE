package com.longdrange.ldattribute.core.dungeon.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.dungeon.DungeonConfig;
import com.longdrange.ldattribute.core.dungeon.gui.DungeonGUI;
import com.longdrange.ldattribute.core.dungeon.gui.DungeonHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class DungeonGUIListener implements Listener {

    private final LDAttribute plugin;
    public DungeonGUIListener(LDAttribute plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!(e.getInventory().getHolder() instanceof DungeonHolder)) return;
        Player player = (Player) e.getWhoClicked();
        DungeonHolder holder = (DungeonHolder) e.getInventory().getHolder();

        e.setCancelled(true);
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= e.getInventory().getSize()) return;

        // 副本图标
        if (slot < DungeonGUI.PER_PAGE) {
            if (slot >= holder.getPageIds().size()) return;
            String dungeonId = holder.getPageIds().get(slot);
            DungeonConfig.DungeonDef def = DungeonConfig.get(dungeonId);
            if (def == null) return;
            player.closeInventory();
            plugin.getDungeonManager().startDungeon(player, def);
            return;
        }

        if (slot == DungeonGUI.BTN_PREV) {
            plugin.getDungeonGUI().open(player, holder.getPage() - 1);
            return;
        }
        if (slot == DungeonGUI.BTN_NEXT) {
            plugin.getDungeonGUI().open(player, holder.getPage() + 1);
            return;
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof DungeonHolder) {
            e.setCancelled(true);
        }
    }
}