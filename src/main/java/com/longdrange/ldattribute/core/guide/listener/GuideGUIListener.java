package com.longdrange.ldattribute.core.guide.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.guide.GuideConfig;
import com.longdrange.ldattribute.core.guide.gui.GuideGUI;
import com.longdrange.ldattribute.core.guide.gui.GuideHolder;
import com.longdrange.ldattribute.core.guide.gui.GuideOverviewGUI;
import com.longdrange.ldattribute.core.guide.gui.GuideOverviewHolder;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import java.util.List;

public class GuideGUIListener implements Listener {

    private final LDAttribute plugin;
    public GuideGUIListener(LDAttribute plugin) { this.plugin = plugin; }

    // ==================== 总览页 ====================
    @EventHandler(priority = EventPriority.HIGH)
    public void onOverviewClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!(e.getInventory().getHolder() instanceof GuideOverviewHolder)) return;
        Player player = (Player) e.getWhoClicked();
        GuideOverviewHolder holder = (GuideOverviewHolder) e.getInventory().getHolder();

        e.setCancelled(true);
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= e.getInventory().getSize()) return;

        // 分类图标
        if (slot >= GuideOverviewGUI.GROUPS_START
                && slot < GuideOverviewGUI.GROUPS_START + GuideOverviewGUI.GROUPS_PER_PAGE) {
            int idx = (slot - GuideOverviewGUI.GROUPS_START) + holder.getPage() * GuideOverviewGUI.GROUPS_PER_PAGE;
            List<GuideConfig.GroupDef> visible = GuideConfig.visibleGroups(player);
            if (idx >= 0 && idx < visible.size()) {
                plugin.getGuideGUI().open(player, visible.get(idx).id, 0);
            }
            return;
        }

        if (slot == GuideOverviewGUI.BTN_PREV) {
            plugin.getGuideOverviewGUI().open(player, holder.getPage() - 1);
            return;
        }
        if (slot == GuideOverviewGUI.BTN_NEXT) {
            plugin.getGuideOverviewGUI().open(player, holder.getPage() + 1);
            return;
        }
        if (slot == GuideOverviewGUI.BTN_INFO) return;
        if (slot == GuideOverviewGUI.BTN_SCORE) return;
    }

    private int[] countProgress(String groupId, Player player) {
        com.longdrange.ldattribute.core.guide.GuideData data = plugin.getGuideManager().get(player);
        int unlocked = 0, total = 0;
        for (GuideConfig.MonsterDef d : GuideConfig.byGroup(groupId)) {
            total++;
            if (data.isUnlocked(d.id)) unlocked++;
        }
        return new int[]{unlocked, total};
    }

    // ==================== 详情页 ====================
    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!(e.getInventory().getHolder() instanceof GuideHolder)) return;
        Player player = (Player) e.getWhoClicked();
        GuideHolder holder = (GuideHolder) e.getInventory().getHolder();

        e.setCancelled(true);
        int rawSlot = e.getRawSlot();
        if (rawSlot < 0 || rawSlot >= e.getInventory().getSize()) return;

        if (rawSlot == GuideGUI.BTN_PREV) {
            plugin.getGuideGUI().open(player, holder.getGroupId(), holder.getPage() - 1);
            return;
        }
        if (rawSlot == GuideGUI.BTN_NEXT) {
            plugin.getGuideGUI().open(player, holder.getGroupId(), holder.getPage() + 1);
            return;
        }
        if (rawSlot == GuideGUI.BTN_BACK) {
            plugin.getGuideOverviewGUI().open(player, 0);
            return;
        }
        if (rawSlot == GuideGUI.BTN_INFO) return;
        if (rawSlot == GuideGUI.BTN_SCORE) return;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof GuideHolder
                || e.getInventory().getHolder() instanceof GuideOverviewHolder) {
            e.setCancelled(true);
        }
    }
}
