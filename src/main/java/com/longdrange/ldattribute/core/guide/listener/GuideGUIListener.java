package com.longdrange.ldattribute.core.guide.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.guide.GuideConfig;
import com.longdrange.ldattribute.core.guide.gui.GuideGUI;
import com.longdrange.ldattribute.core.guide.gui.GuideHolder;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;

public class GuideGUIListener implements Listener {

    private final LDAttribute plugin;
    public GuideGUIListener(LDAttribute plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!(e.getInventory().getHolder() instanceof GuideHolder)) return;
        Player player = (Player) e.getWhoClicked();
        GuideHolder holder = (GuideHolder) e.getInventory().getHolder();

        e.setCancelled(true);
        int rawSlot = e.getRawSlot();
        if (rawSlot < 0 || rawSlot >= e.getInventory().getSize()) return;

        if (rawSlot < 9) {
            int i = 0;
            for (GuideConfig.GroupDef g : GuideConfig.allGroups()) {
                if (i == rawSlot) {
                    plugin.getGuideGUI().open(player, g.id, 0);
                    return;
                }
                i++;
            }
            return;
        }

        if (rawSlot == GuideGUI.BTN_PREV) {
            plugin.getGuideGUI().open(player, holder.getGroupId(), holder.getPage() - 1);
            return;
        }
        if (rawSlot == GuideGUI.BTN_NEXT) {
            plugin.getGuideGUI().open(player, holder.getGroupId(), holder.getPage() + 1);
            return;
        }
        if (rawSlot == GuideGUI.BTN_INFO) {
            player.sendMessage(ChatColor.AQUA + "图鉴进度请在面板查看");
            return;
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof GuideHolder) {
            e.setCancelled(true);
        }
    }
}