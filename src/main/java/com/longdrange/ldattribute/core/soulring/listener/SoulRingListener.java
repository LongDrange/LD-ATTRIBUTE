package com.longdrange.ldattribute.core.soulring.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.soulring.SoulRingConfig;
import com.longdrange.ldattribute.core.soulring.SoulRingData;
import com.longdrange.ldattribute.core.soulring.gui.SoulRingGUI;
import com.longdrange.ldattribute.core.soulring.gui.SoulRingHolder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;

public class SoulRingListener implements Listener {

    private final LDAttribute plugin;
    public SoulRingListener(LDAttribute plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!(e.getInventory().getHolder() instanceof SoulRingHolder)) return;
        Player player = (Player) e.getWhoClicked();
        SoulRingHolder holder = (SoulRingHolder) e.getInventory().getHolder();

        int rawSlot = e.getRawSlot();
        int topSize = e.getInventory().getSize();
        boolean clickedTop = rawSlot < topSize;

        if (!clickedTop) {
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            e.setCancelled(true);
            handleDeposit(player, holder, clicked, e);
            return;
        }

        e.setCancelled(true);

        if (rawSlot >= 0 && rawSlot < 45) {
            List<SoulRingData.Entry> page = holder.getPageEntries();
            if (rawSlot >= page.size()) return;
            SoulRingData.Entry entry = page.get(rawSlot);
            if (entry == null || entry.count <= 0) return;
            handleWithdraw(player, holder, entry, e);
            return;
        }

        if (rawSlot == SoulRingGUI.BTN_PREV) {
            if (holder.getPage() > 0) {
                holder.setPage(holder.getPage() - 1);
                plugin.getSoulRingGUI().refresh(player, holder);
            }
            return;
        }
        if (rawSlot == SoulRingGUI.BTN_NEXT) {
            holder.setPage(holder.getPage() + 1);
            plugin.getSoulRingGUI().refresh(player, holder);
            return;
        }

        // 分类切换
        if (rawSlot == SoulRingGUI.BTN_TRASH) {
            if (!player.hasPermission("ldattribute.soulring.trash") && !player.isOp()) {
                player.sendMessage(ChatColor.RED + "你没有权限使用垃圾桶");
                return;
            }
            plugin.getSoulRingTrashGUI().open(player);
            return;
        }

        if (rawSlot == SoulRingGUI.BTN_EXCHANGE) {
            plugin.getExchangeGUI().openDefault(player);
            return;
        }
        if (rawSlot == SoulRingGUI.BTN_CAT) {
            int next = holder.getCategoryIndex() + 1;
            if (next >= SoulRingConfig.getCategoryCount()) next = 0;
            holder.setCategoryIndex(next);
            holder.setPage(0);
            plugin.getSoulRingGUI().refresh(player, holder);
            try { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f); } catch (Throwable ignored) {}
            return;
        }

        // 排序切换
        if (rawSlot == SoulRingGUI.BTN_SORT) {
            holder.setSortMode(holder.getSortMode().next());
            holder.setPage(0);
            plugin.getSoulRingGUI().refresh(player, holder);
            try { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f); } catch (Throwable ignored) {}
            return;
        }

        if (rawSlot == SoulRingGUI.BTN_INFO) {
            player.sendMessage(ChatColor.GOLD + "==== 操作说明 ====");
            player.sendMessage(ChatColor.YELLOW + "存入（点击背包）: 左键1 / 右键16 / Shift左键1组 / Shift右键全部");
            player.sendMessage(ChatColor.YELLOW + "取出（点击GUI）: 左键1 / 右键16 / Shift左键1组 / Shift右键全部");
            player.sendMessage(ChatColor.YELLOW + "分类: 点击箱子图标切换");
            player.sendMessage(ChatColor.YELLOW + "排序: 点击纸图标切换");
            return;
        }
    }

    private void handleDeposit(Player player, SoulRingHolder holder, ItemStack clicked, InventoryClickEvent e) {
        SoulRingData data = plugin.getSoulRingManager().get(player);
        String itemName = displayName(clicked);

        if (e.isShiftClick() && e.isRightClick()) {
            long total = depositAllSameType(player, clicked, data);
            if (total <= 0) { player.sendMessage(ChatColor.RED + "存入失败"); return; }
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.6f);
            player.sendMessage(ChatColor.GREEN + "已存入 " + ChatColor.WHITE + itemName
                    + ChatColor.GREEN + " x" + total + "（全部同类）");
            plugin.getSoulRingGUI().refresh(player, holder);
            return;
        }

        long amount;
        if (e.isShiftClick() && e.isLeftClick()) amount = 64;
        else if (e.isRightClick()) amount = 16;
        else amount = 1;

        long actual = Math.min(amount, clicked.getAmount());
        long added = data.deposit(clicked, actual);
        if (added <= 0) { player.sendMessage(ChatColor.RED + "存入失败"); return; }

        if (added >= clicked.getAmount()) e.setCurrentItem(null);
        else {
            ItemStack copy = clicked.clone();
            copy.setAmount((int) (clicked.getAmount() - added));
            e.setCurrentItem(copy);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.6f);
        player.sendMessage(ChatColor.GREEN + "已存入 " + ChatColor.WHITE + itemName
                + ChatColor.GREEN + " x" + added);
        plugin.getSoulRingGUI().refresh(player, holder);
    }

    private long depositAllSameType(Player player, ItemStack sample, SoulRingData data) {
        long total = 0;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack s = contents[i];
            if (s == null || s.getType() == Material.AIR) continue;
            if (!s.isSimilar(sample)) continue;

            long added = data.deposit(s, s.getAmount());
            total += added;
            if (added >= s.getAmount()) {
                player.getInventory().setItem(i, null);
            } else if (added > 0) {
                ItemStack copy = s.clone();
                copy.setAmount((int) (s.getAmount() - added));
                player.getInventory().setItem(i, copy);
            }
        }
        return total;
    }

    private void handleWithdraw(Player player, SoulRingHolder holder,
                                SoulRingData.Entry entry, InventoryClickEvent e) {
        SoulRingData data = plugin.getSoulRingManager().get(player);
        String itemName = displayName(entry.template);

        long want;
        if (e.isShiftClick() && e.isRightClick()) want = entry.count;
        else if (e.isShiftClick() && e.isLeftClick()) want = 64;
        else if (e.isRightClick()) want = 16;
        else want = 1;

        int empty = countEmptySlots(player);
        if (empty < 3) {
            player.sendMessage(ChatColor.RED + "背包至少需要留出 3 格空位（当前 " + empty + " 格）");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
            return;
        }

        ItemStack template = entry.template.clone();
        long actual = data.takeByEntry(entry, want);
        if (actual <= 0) { player.sendMessage(ChatColor.RED + "取出失败（数据为 0）"); return; }

        long remaining = actual;
        long failed = 0;
        while (remaining > 0) {
            int give = (int) Math.min(remaining, 64);
            ItemStack giveStack = template.clone();
            giveStack.setAmount(give);
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(giveStack);
            if (!leftover.isEmpty()) {
                for (ItemStack l : leftover.values()) failed += l.getAmount();
            }
            remaining -= give;
        }

        if (failed > 0) {
            data.deposit(template, failed);
            player.sendMessage(ChatColor.RED + "背包空间不足，" + ChatColor.WHITE + itemName
                    + ChatColor.RED + " x" + failed + " 未取出，已归还");
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
            player.sendMessage(ChatColor.GREEN + "已取出 " + ChatColor.WHITE + itemName
                    + ChatColor.GREEN + " x" + actual);
        }

        plugin.getSoulRingGUI().refresh(player, holder);
    }

    private static String displayName(ItemStack item) {
        if (item == null) return "未知物品";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        String mat = item.getType().name();
        String[] parts = mat.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    private static int countEmptySlots(Player p) {
        int n = 0;
        ItemStack[] contents = p.getInventory().getStorageContents();
        for (ItemStack s : contents) {
            if (s == null || s.getType() == Material.AIR) n++;
        }
        return n;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof SoulRingHolder) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player) {
            SoulRingData d = plugin.getSoulRingManager().get((Player) e.getPlayer());
            if (d != null) d.save();
        }
    }
}
