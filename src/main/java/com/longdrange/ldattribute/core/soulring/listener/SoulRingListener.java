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

        // ===== 模式分流 =====
        if (rawSlot >= 0 && rawSlot < 45) {
            List<SoulRingData.Entry> page0 = holder.getPageEntries();
            if (rawSlot < page0.size()) {
                SoulRingData.Entry e0 = page0.get(rawSlot);
                if (e0 != null && e0.count > 0) {
                    if (holder.getMode() == SoulRingHolder.Mode.DECOMPOSE) {
                        e.setCancelled(true);
                        handleDecompose(player, holder, e0, e);
                        return;
                    }
                    if (holder.getMode() == SoulRingHolder.Mode.DELETE) {
                        e.setCancelled(true);
                        handleDelete(player, holder, e0, e);
                        return;
                    }
                }
            }
        }

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
        // 模式切换
        if (rawSlot == SoulRingGUI.BTN_MODE) {
            holder.setMode(holder.getMode().next());
            com.longdrange.ldattribute.core.soulring.SoulRingLog.write(player, "ModeChange", "切换到 " + holder.getMode().label);
            holder.setPage(0);
            plugin.getSoulRingGUI().refresh(player, holder);
            try { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f); } catch (Throwable ignored) {}
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
        // 白名单/黑名单检查
        if (SoulRingConfig.isFiltered(clicked)) {
            player.sendMessage(ChatColor.RED + "此物品被过滤，无法存入灵魂空间");
            try { player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f); } catch (Throwable ignored) {}
            return;
        }
        SoulRingData data = plugin.getSoulRingManager().get(player);
        String itemName = displayName(clicked);

        if (e.isShiftClick() && e.isRightClick()) {
            long total = depositAllSameType(player, clicked, data);
            if (total <= 0) { player.sendMessage(ChatColor.RED + "存入失败"); return; }
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.6f);
            player.sendMessage(ChatColor.GREEN + "已存入 " + ChatColor.WHITE + itemName
                    + ChatColor.GREEN + " x" + total + "（全部同类）");
        com.longdrange.ldattribute.core.soulring.SoulRingLog.write(player, "Deposit", itemName + " x" + total + " (全部同类)");
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
        if (SoulRingConfig.isFiltered(sample)) return 0;
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

    /** 分解模式：点击物品 → 分解 */
    private void handleDecompose(Player player, SoulRingHolder holder,
                                  SoulRingData.Entry entry, InventoryClickEvent e) {
        if (entry == null || entry.count <= 0) return;

        com.longdrange.ldattribute.core.soulring.SoulRingDecomposeConfig.Reward reward =
                com.longdrange.ldattribute.core.soulring.SoulRingDecomposeConfig.getReward(entry.template);
        if (reward == null) {
            player.sendMessage(ChatColor.RED + "此物品没有分解配置");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
            return;
        }

        long amount;
        if (e.isShiftClick() && e.isRightClick()) amount = entry.count;
        else if (e.isShiftClick() && e.isLeftClick()) amount = 64;
        else if (e.isRightClick()) amount = 16;
        else amount = 1;
        long actual = Math.min(amount, entry.count);

        SoulRingData data = plugin.getSoulRingManager().get(player);
        String name = displayName(entry.template);
        long taken = data.takeByEntry(entry, actual);
        if (taken <= 0) {
            player.sendMessage(ChatColor.RED + "分解失败");
            return;
        }

        // 发放奖励（按数量）
        try {
            if (reward.points > 0) {
                int amt = (int) (reward.points * taken);
                com.longdrange.ldattribute.points.PointAPI.addPlayerPoints(player.getName(), amt);
            }
            if (reward.vault > 0) {
                org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
                        org.bukkit.Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                if (rsp != null) rsp.getProvider().depositPlayer(player, reward.vault * taken);
            }
            for (java.util.Map.Entry<String, Double> ve : reward.values.entrySet()) {
                double amt = ve.getValue() * taken;
                try {
                    com.longdrange.ldattribute.core.value.ValueExpression.add(player, ve.getKey(), amt);
                } catch (Throwable ignored) {}
            }
            for (String it : reward.items) {
                String[] p = it.split(":");
                if (p.length < 2) continue;
                Material mat = Material.getMaterial(p[0].toUpperCase());
                if (mat == null) continue;
                int per = 1;
                try { per = Integer.parseInt(p[1]); } catch (Throwable ignored) {}
                int total = (int) (per * taken);
                while (total > 0) {
                    int give = Math.min(total, 64);
                    player.getInventory().addItem(new ItemStack(mat, give));
                    total -= give;
                }
            }
            for (String cid : reward.cards) {
                String[] p = cid.split(":");
                if (p.length < 1) continue;
                int per = 1;
                if (p.length >= 2) { try { per = Integer.parseInt(p[1]); } catch (Throwable ignored) {} }
                com.longdrange.ldattribute.card.CardData cd =
                        com.longdrange.ldattribute.card.CardDataManager.getCard(p[0]);
                if (cd == null) continue;
                for (int i = 0; i < per * taken; i++) {
                    try {
                        com.longdrange.ldattribute.core.soulring.SoulRingData sr = plugin.getSoulRingManager().get(player);
                        sr.deposit(cd.getItem(), cd.getItem().getAmount());
                    } catch (Throwable ignored) {}
                }
            }
            for (String cmd : reward.commands) {
                try {
                    String run = cmd.replace("%player%", player.getName());
                    org.bukkit.Bukkit.dispatchCommand(player, run.startsWith("/") ? run.substring(1) : run);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable t) {
            player.sendMessage(ChatColor.RED + "发放奖励失败: " + t.getMessage());
        }

        try { player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f); } catch (Throwable ignored) {}
        player.sendMessage(ChatColor.GREEN + "已分解 " + ChatColor.WHITE + name + ChatColor.GREEN + " x" + taken);
        com.longdrange.ldattribute.core.soulring.SoulRingLog.write(player, "Decompose", name + " x" + taken);
        plugin.getSoulRingGUI().refresh(player, holder);
    }

    /** 删除模式：点击物品 → 直接删除 */
    private void handleDelete(Player player, SoulRingHolder holder,
                              SoulRingData.Entry entry, InventoryClickEvent e) {
        if (entry == null || entry.count <= 0) return;

        long amount;
        if (e.isShiftClick() && e.isRightClick()) amount = entry.count;
        else if (e.isShiftClick() && e.isLeftClick()) amount = 64;
        else if (e.isRightClick()) amount = 16;
        else amount = 1;
        long actual = Math.min(amount, entry.count);

        SoulRingData data = plugin.getSoulRingManager().get(player);
        String name = displayName(entry.template);
        long deleted = data.takeByEntry(entry, actual);
        if (deleted <= 0) { player.sendMessage(ChatColor.RED + "删除失败"); return; }

        try { player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 0.5f); } catch (Throwable ignored) {}
        player.sendMessage(ChatColor.RED + "已删除 " + ChatColor.WHITE + name + ChatColor.RED + " x" + deleted);
        com.longdrange.ldattribute.core.soulring.SoulRingLog.write(player, "Delete", name + " x" + deleted);
        plugin.getSoulRingGUI().refresh(player, holder);
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
