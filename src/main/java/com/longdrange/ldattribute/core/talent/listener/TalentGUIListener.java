package com.longdrange.ldattribute.core.talent.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.talent.*;
import com.longdrange.ldattribute.core.talent.gui.TalentGUI;
import com.longdrange.ldattribute.core.talent.gui.TalentHolder;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;

import java.util.ArrayList;
import java.util.List;

public class TalentGUIListener implements Listener {

    private final LDAttribute plugin;
    public TalentGUIListener(LDAttribute plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!(e.getInventory().getHolder() instanceof TalentHolder)) return;
        Player player = (Player) e.getWhoClicked();
        TalentHolder holder = (TalentHolder) e.getInventory().getHolder();
        String pageId = holder.getPageId();

        e.setCancelled(true);
        int rawSlot = e.getRawSlot();
        if (rawSlot < 0 || rawSlot >= e.getInventory().getSize()) return;

        // 天赋格
        if (rawSlot < 45) {
            TalentConfig.TalentDef def = TalentConfig.getByGui(pageId, rawSlot);
            if (def == null) return;
            if (e.isShiftClick()) doAddMulti(player, def);
            else doAddOne(player, def);
            plugin.getTalentGUI().open(player, pageId);
            return;
        }

        // 按钮
        if (rawSlot == TalentGUI.BTN_PREV) {
            List<TalentConfig.PageDef> list = new ArrayList<>(TalentConfig.allPages());
            int idx = indexOf(list, pageId);
            if (idx > 0) plugin.getTalentGUI().open(player, list.get(idx - 1).id);
            return;
        }
        if (rawSlot == TalentGUI.BTN_NEXT) {
            List<TalentConfig.PageDef> list = new ArrayList<>(TalentConfig.allPages());
            int idx = indexOf(list, pageId);
            if (idx >= 0 && idx < list.size() - 1) plugin.getTalentGUI().open(player, list.get(idx + 1).id);
            return;
        }
        if (rawSlot == TalentGUI.BTN_BACK)  { plugin.getTalentGUI().open(player); return; }
        if (rawSlot == TalentGUI.BTN_CLOSE) { player.closeInventory(); return; }
    }

    private int indexOf(List<TalentConfig.PageDef> list, String id) {
        for (int i = 0; i < list.size(); i++) if (list.get(i).id.equals(id)) return i;
        return -1;
    }

    private void doAddOne(Player player, TalentConfig.TalentDef def) {
        TalentData data = plugin.getTalentManager().get(player);
        int pagePoints = data.getPoints(def.pageId);

        if (data.getLevel(def.id) >= def.maxLevel) {
            player.sendMessage(ChatColor.RED + "该天赋已满级");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
            return;
        }
        if (data.hasAnyRequirementUnmet(def)) {
            player.sendMessage(ChatColor.RED + "前置天赋未解锁");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
            return;
        }
        if (pagePoints < def.pointsPerLevel) {
            player.sendMessage(ChatColor.RED + "点数不足（需 " + def.pointsPerLevel + "，有 " + pagePoints + "）");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
            return;
        }

        data.takePoints(def.pageId, def.pointsPerLevel);
        data.setLevel(def.id, data.getLevel(def.id) + 1);
        data.save();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        player.sendMessage(ChatColor.GREEN + "\u2714 " + ChatColor.stripColor(def.name)
                + " \u5347\u5230 Lv." + data.getLevel(def.id));
    }

    private void doAddMulti(Player player, TalentConfig.TalentDef def) {
        TalentData data = plugin.getTalentManager().get(player);
        int added = 0;
        while (true) {
            if (data.getLevel(def.id) >= def.maxLevel) break;
            if (data.hasAnyRequirementUnmet(def)) break;
            if (data.getPoints(def.pageId) < def.pointsPerLevel) break;
            data.takePoints(def.pageId, def.pointsPerLevel);
            data.setLevel(def.id, data.getLevel(def.id) + 1);
            added++;
        }
        data.save();
        if (added == 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
            player.sendMessage(ChatColor.RED + "无法加点（点数不足/已满级/前置未满足）");
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            player.sendMessage(ChatColor.GREEN + "\u2714 连加 " + added + " 级，当前 Lv." + data.getLevel(def.id));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof TalentHolder) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player) {
            TalentData d = plugin.getTalentManager().get((Player) e.getPlayer());
            if (d != null) d.save();
        }
    }
}