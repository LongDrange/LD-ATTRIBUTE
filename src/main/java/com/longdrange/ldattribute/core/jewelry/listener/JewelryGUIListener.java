package com.longdrange.ldattribute.core.jewelry.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.jewelry.*;
import com.longdrange.ldattribute.core.jewelry.gui.JewelryGUI;
import com.longdrange.ldattribute.core.jewelry.gui.JewelryHolder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class JewelryGUIListener implements Listener {

    private final LDAttribute plugin;
    public JewelryGUIListener(LDAttribute plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        Inventory inv = e.getInventory();
        if (!(inv.getHolder() instanceof JewelryHolder)) return;
        JewelryHolder holder = (JewelryHolder) inv.getHolder();
        String pageId = holder.getPageId();

        int rawSlot = e.getRawSlot();
        int topSize = inv.getSize();
        boolean clickedTop = rawSlot < topSize;

        // 点击背包里的饰品 → 放入
        if (!clickedTop) {
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR || !JewelryItemUtil.isJewelry(clicked)) {
                e.setCancelled(true);
                return;
            }
            e.setCancelled(true);
            ItemStack cursor = e.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                if (player.getInventory().firstEmpty() == -1) {
                    player.sendMessage(ChatColor.RED + "背包已满");
                    return;
                }
                player.getInventory().addItem(cursor);
                e.setCursor(null);
            }
            tryDeposit(player, clicked, e);
            return;
        }

        e.setCancelled(true);

        // 点击 GUI 里的槽位
        if (rawSlot >= 0 && rawSlot < 45) {
            JewelryConfig.SlotDef def = JewelryConfig.getSlotByGui(pageId, rawSlot);
            if (def == null) return;
            JewelryData data = plugin.getJewelryManager().get(player);
            if (data.has(def.id)) {
                player.sendMessage(ChatColor.RED + "饰品已绑定，无法取出");
            } else {
                player.sendMessage(ChatColor.GRAY + "把 Lore 写有 " + ChatColor.YELLOW
                        + "饰品槽位: " + def.loreKey + ChatColor.GRAY + " 的饰品点击放入");
            }
            return;
        }

        // 按钮
        if (rawSlot == JewelryGUI.BTN_PREV) {
            List<JewelryConfig.PageDef> list = new ArrayList<>(JewelryConfig.allPages());
            int idx = indexOf(list, pageId);
            if (idx > 0) plugin.getJewelryGUI().open(player, list.get(idx - 1).id);
            return;
        }
        if (rawSlot == JewelryGUI.BTN_NEXT) {
            List<JewelryConfig.PageDef> list = new ArrayList<>(JewelryConfig.allPages());
            int idx = indexOf(list, pageId);
            if (idx >= 0 && idx < list.size() - 1) plugin.getJewelryGUI().open(player, list.get(idx + 1).id);
            return;
        }
        if (rawSlot == JewelryGUI.BTN_BACK) { plugin.getJewelryGUI().open(player); return; }
        if (rawSlot == JewelryGUI.BTN_CLOSE) { player.closeInventory(); return; }
        if (rawSlot == JewelryGUI.BTN_STATS) {
            player.sendMessage(ChatColor.GOLD + "==== 饰品属性总览 ====");
            java.util.Map<String, Double> total = JewelryStatsProvider.getTotal(player);
            if (total.isEmpty()) player.sendMessage(ChatColor.GRAY + "  暂无");
            else for (java.util.Map.Entry<String, Double> en : total.entrySet())
                player.sendMessage(ChatColor.GREEN + en.getKey() + ": " + ChatColor.WHITE + JewelryStatsProvider.format(en.getValue()));
            return;
        }
        if (rawSlot == JewelryGUI.BTN_HELP) {
            player.sendMessage(ChatColor.GRAY + "\u00B7 点击背包里符合条件的饰品放入");
            player.sendMessage(ChatColor.GRAY + "\u00B7 饰品放入后无法取出");
            player.sendMessage(ChatColor.GRAY + "\u00B7 点击空槽位查看 Lore 需要写什么");
        }
    }

    private int indexOf(List<JewelryConfig.PageDef> list, String pageId) {
        for (int i = 0; i < list.size(); i++) if (list.get(i).id.equals(pageId)) return i;
        return -1;
    }

    private void tryDeposit(Player player, ItemStack stack, InventoryClickEvent e) {
        String loreKey = JewelryItemUtil.getSlotKey(stack);
        if (loreKey == null) { player.sendMessage(ChatColor.RED + "无法识别饰品槽位"); return; }

        JewelryConfig.SlotDef def = JewelryConfig.findByLoreKey(loreKey);
        if (def == null) {
            player.sendMessage(ChatColor.RED + "未知槽位: '" + loreKey + "'（未在 jewelry.yml 配置）");
            return;
        }

        if (!def.permission.isEmpty() && !player.hasPermission(def.permission)) {
            player.sendMessage(ChatColor.RED + "你没有权限放入此槽位");
            return;
        }

        JewelryData data = plugin.getJewelryManager().get(player);
        if (data.has(def.id)) {
            player.sendMessage(ChatColor.RED + "该槽位已有饰品");
            return;
        }

        ItemStack toPut = stack.clone();
        toPut.setAmount(1);
        data.set(def.id, toPut);
        data.save();

        if (stack.getAmount() <= 1) e.setCurrentItem(null);
        else {
            ItemStack copy = stack.clone();
            copy.setAmount(stack.getAmount() - 1);
            e.setCurrentItem(copy);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.6f);
        player.sendMessage(ChatColor.GREEN + "已放入饰品到 " + def.name);
        plugin.getJewelryGUI().open(player, def.pageId);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof JewelryHolder) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player) {
            JewelryData d = plugin.getJewelryManager().get((Player) e.getPlayer());
            if (d != null) d.save();
        }
    }
}