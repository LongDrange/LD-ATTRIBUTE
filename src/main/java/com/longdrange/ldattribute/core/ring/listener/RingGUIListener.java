package com.longdrange.ldattribute.core.ring.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.ring.*;
import com.longdrange.ldattribute.core.ring.gui.*;
import org.bukkit.Bukkit;
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

import java.util.List;

public class RingGUIListener implements Listener {

    private final LDAttribute plugin;
    public RingGUIListener(LDAttribute plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        Inventory inv = e.getInventory();
        Object h = inv.getHolder();

        if (h instanceof RingHolder) { handleRingClick(player, e, (RingHolder) h); return; }
        if (h instanceof RingUnlockGUI.UnlockHolder) { handleUnlockClick(player, e, (RingUnlockGUI.UnlockHolder) h); return; }
        if (h instanceof RingPageUnlockGUI.PageUnlockHolder) { handlePageUnlockClick(player, e, (RingPageUnlockGUI.PageUnlockHolder) h); return; }
        if (h instanceof RingStatGUI.StatHolder) { e.setCancelled(true); return; }
        if (h instanceof RingSetGUI.SetHolder) { handleSetClick(player, e); return; }
        if (h instanceof RingUpgradeGUI.UpgradeHolder) { handleUpgradeClick(player, e, (RingUpgradeGUI.UpgradeHolder) h); return; }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        Object h = e.getInventory().getHolder();
        if (h instanceof RingHolder
                || h instanceof RingUnlockGUI.UnlockHolder
                || h instanceof RingPageUnlockGUI.PageUnlockHolder
                || h instanceof RingStatGUI.StatHolder
                || h instanceof RingSetGUI.SetHolder
                || h instanceof RingUpgradeGUI.UpgradeHolder) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player) {
            RingData d = plugin.getRingManager().get((Player) e.getPlayer());
            if (d != null) d.save();
        }
    }

    // ==================== 主界面 ====================

    private void handleRingClick(Player player, InventoryClickEvent e, RingHolder holder) {
        int rawSlot = e.getRawSlot();
        int topSize = e.getInventory().getSize();
        boolean clickedTop = rawSlot < topSize;

        if (!clickedTop) {
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR || !RingItemUtil.isRing(clicked)) {
                e.setCancelled(true);
                return;
            }
            e.setCancelled(true);
            ItemStack cursor = e.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                if (player.getInventory().firstEmpty() == -1) {
                    player.sendMessage(ChatColor.RED + "背包已满，请先放下手上的物品");
                    return;
                }
                player.getInventory().addItem(cursor);
                e.setCursor(null);
            }
            tryDeposit(player, holder, clicked, e);
            return;
        }

        e.setCancelled(true);

        if (rawSlot >= 0 && rawSlot < RingGUI.SLOT_COUNT_PER_PAGE) {
            handleSlotClick(player, holder, rawSlot, e);
            return;
        }
        if (rawSlot == RingGUI.BTN_PREV) {
            if (holder.getPage() > 0) plugin.getRingGUI().open(player, holder.getPage() - 1);
            return;
        }
        if (rawSlot == RingGUI.BTN_NEXT) {
            RingData d = plugin.getRingManager().get(player);
            int nextPage = holder.getPage() + 1;
            if (holder.getPage() < d.getUnlockedPages() - 1) {
                plugin.getRingGUI().open(player, nextPage);
            } else if (RingConfig.getPageUnlock(nextPage) != null) {
                new RingPageUnlockGUI(plugin).open(player, nextPage, holder.getPage());
            }
            return;
        }
        if (rawSlot == RingGUI.BTN_STATS) { new RingStatGUI(plugin).open(player, 0); return; }
        if (rawSlot == RingGUI.BTN_SETS)  { new RingSetGUI(plugin).open(player); return; }
        if (rawSlot == RingGUI.BTN_CLOSE) { player.closeInventory(); return; }
        if (rawSlot == RingGUI.BTN_HELP) {
            player.sendMessage(ChatColor.GRAY + "\u00B7 点击背包里的魂珠即可放入");
            player.sendMessage(ChatColor.GRAY + "\u00B7 点击已放入的魂珠可升级（需堆满）");
            player.sendMessage(ChatColor.GRAY + "\u00B7 点击未解锁格子可以解锁");
            player.sendMessage(ChatColor.GRAY + "\u00B7 点下一页可解锁新页");
        }
    }

    private void handleSlotClick(Player player, RingHolder holder, int localSlot, InventoryClickEvent e) {
        int gid = holder.getPage() * RingGUI.SLOT_COUNT_PER_PAGE + localSlot;
        RingData data = plugin.getRingManager().get(player);
        if (!data.isUnlocked(gid)) {
            new RingUnlockGUI(plugin).open(player, gid, holder.getPage());
            return;
        }
        RingData.Slot slot = data.getSlot(gid);
        if (slot == null || slot.count <= 0) return;
        new RingUpgradeGUI(plugin).open(player, gid, holder.getPage());
    }

    private void handleSetClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);
        int rawSlot = e.getRawSlot();
        int topSize = e.getInventory().getSize();
        if (rawSlot < 0 || rawSlot >= topSize) return;
        if (rawSlot == 49) {
            plugin.getRingGUI().open(player, 0);
        }
    }

    // ==================== 升级 ====================

    private void handleUpgradeClick(Player player, InventoryClickEvent e, RingUpgradeGUI.UpgradeHolder holder) {
        e.setCancelled(true);
        int rawSlot = e.getRawSlot();
        if (rawSlot < 0 || rawSlot >= e.getInventory().getSize()) return;
        if (rawSlot == RingUpgradeGUI.BTN_CANCEL) { plugin.getRingGUI().open(player, holder.backPage); return; }
        if (rawSlot == RingUpgradeGUI.BTN_CONFIRM) doUpgrade(player, holder);
    }

    private void doUpgrade(Player player, RingUpgradeGUI.UpgradeHolder holder) {
        RingData data = plugin.getRingManager().get(player);
        int slotId = holder.slotId;
        RingData.Slot slot = data.getSlot(slotId);
        if (slot == null) { player.sendMessage(ChatColor.RED + "该槽位无魂珠"); return; }

        int curLv = slot.level;
        int maxLv = RingUpgradeConfig.getMaxLevel(slot.ringType);
        int targetLv = curLv + 1;

        if (curLv >= maxLv) {
            player.sendMessage(ChatColor.RED + "已达最大等级 Lv." + maxLv);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
            return;
        }

        // 必须堆满
        int have = data.countType(slot.ringType);
        if (have < slot.maxStack) {
            player.sendMessage(ChatColor.RED + "魂珠数量需达到上限才能升级（当前 " + have + "/" + slot.maxStack + "）");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
            return;
        }

        RingUpgradeConfig.Cost cost = RingUpgradeConfig.getCost(slot.ringType, targetLv);
        if (cost == null) {
            player.sendMessage(ChatColor.RED + "该等级没有升级成本配置");
            return;
        }

        List<RingCost.ItemReq> reqs = RingCost.parseItems(cost.items);

        String err = RingCost.check(player, "", cost.points, cost.vault, reqs);
        if (err != null) {
            player.sendMessage(ChatColor.RED + err);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
            return;
        }
        RingCost.pay(player, "", cost.points, cost.vault, reqs);

        // ===== 消耗魂珠 =====
        // 剩余 = 上限 - (上限×30% + 上限×10%×BonusPerLevel)
        // 消耗 = 上限 - 剩余 = 上限×30% + 上限×10%×BonusPerLevel
        double bonus = RingUpgradeConfig.getBonusPerLevel(slot.ringType);
        double remainDouble = slot.maxStack - (slot.maxStack * 0.30 + slot.maxStack * 0.10 * bonus);
        int remainAfter = (int) Math.floor(remainDouble);
        if (remainAfter < 0) remainAfter = 0;
        int consumeAmount = slot.maxStack - remainAfter;
        if (consumeAmount < 0) consumeAmount = 0;
        if (consumeAmount > slot.count) consumeAmount = slot.count;
        int consumed = data.consumeFromSlot(slotId, consumeAmount);

        data.setLevel(slotId, targetLv);
        data.save();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.3f);
        player.sendMessage(ChatColor.GREEN + "\u2714 " + slot.ringType + " 升级到 Lv." + targetLv
                + ChatColor.GRAY + "（消耗魂珠 " + consumed + " 个，剩余 " + data.countType(slot.ringType) + "）");
        new RingUpgradeGUI(plugin).open(player, slotId, holder.backPage);
    }

    private void tryDeposit(Player player, RingHolder holder, ItemStack stack, InventoryClickEvent e) {
        String type = RingItemUtil.getRingType(stack);
        if (type == null) { player.sendMessage(ChatColor.RED + "无法识别魂珠类型"); return; }

        int max = RingItemUtil.getMaxStack(stack, type, RingConfig.getDefaultMaxStack());
        RingData data = plugin.getRingManager().get(player);
        int already = data.countType(type);
        int canAdd = Math.max(0, max - already);

        if (canAdd <= 0) {
            player.sendMessage(ChatColor.RED + "该魂珠已达上限 " + max);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
            return;
        }

        int predictedGid = data.findSlotOfType(type);
        if (predictedGid < 0) predictedGid = data.findEmptySlot();

        int amount = Math.min(stack.getAmount(), canAdd);
        int added = data.addRings(type, amount, stack, max);
        if (added <= 0) {
            player.sendMessage(ChatColor.RED + "没有空的已解锁槽位");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
            return;
        }

        if (added >= stack.getAmount()) e.setCurrentItem(null);
        else {
            ItemStack copy = stack.clone();
            copy.setAmount(stack.getAmount() - added);
            e.setCurrentItem(copy);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.6f);
        player.sendMessage(ChatColor.GREEN + "已放入 " + added + " 个魂珠（当前 " + data.countType(type) + "/" + max + "）");

        if (predictedGid >= 0) {
            RingGUI.flash(player.getUniqueId(), predictedGid);
            plugin.getRingGUI().open(player, holder.getPage());
            final int backPage = holder.getPage();
            final java.util.UUID uuid = player.getUniqueId();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                RingGUI.clearFlash(uuid);
                if (!player.isOnline()) return;
                Inventory top = player.getOpenInventory().getTopInventory();
                if (top == null || !(top.getHolder() instanceof RingHolder)) return;
                RingHolder rh = (RingHolder) top.getHolder();
                if (rh.getOwner().equals(uuid) && rh.getPage() == backPage) {
                    plugin.getRingGUI().open(player, backPage);
                }
            }, 16L);
        } else {
            plugin.getRingGUI().open(player, holder.getPage());
        }
    }

    // ==================== 槽位解锁 ====================

    private void handleUnlockClick(Player player, InventoryClickEvent e, RingUnlockGUI.UnlockHolder holder) {
        e.setCancelled(true);
        int rawSlot = e.getRawSlot();
        if (rawSlot < 0 || rawSlot >= e.getInventory().getSize()) return;
        if (rawSlot == RingUnlockGUI.BTN_CANCEL) { plugin.getRingGUI().open(player, holder.backPage); return; }
        if (rawSlot == RingUnlockGUI.BTN_CONFIRM) doUnlock(player, holder);
    }

    private void doUnlock(Player player, RingUnlockGUI.UnlockHolder holder) {
        RingData data = plugin.getRingManager().get(player);
        int slotId = holder.slotId;
        if (data.isUnlocked(slotId)) { plugin.getRingGUI().open(player, holder.backPage); return; }

        RingSlotConfig.SlotUnlock cfg = RingSlotConfig.get(slotId);
        String perm = cfg == null ? "" : cfg.permission;
        int points = cfg == null ? 0 : cfg.costPoints;
        double vault = cfg == null ? 0 : cfg.costVault;
        List<String> rawItems = cfg == null ? null : cfg.items;
        List<RingCost.ItemReq> reqs = RingCost.parseItems(rawItems);

        String err = RingCost.check(player, perm, points, vault, reqs);
        if (err != null) {
            player.sendMessage(ChatColor.RED + err);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
            return;
        }
        RingCost.pay(player, perm, points, vault, reqs);

        data.unlockSlot(slotId);
        data.save();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        player.sendMessage(ChatColor.GREEN + "\u2714 已解锁槽位 " + RingGUI.tag(slotId));
        plugin.getRingGUI().open(player, holder.backPage);
    }

    // ==================== 页解锁 ====================

    private void handlePageUnlockClick(Player player, InventoryClickEvent e, RingPageUnlockGUI.PageUnlockHolder holder) {
        e.setCancelled(true);
        int rawSlot = e.getRawSlot();
        if (rawSlot < 0 || rawSlot >= e.getInventory().getSize()) return;
        if (rawSlot == RingPageUnlockGUI.BTN_CANCEL) { plugin.getRingGUI().open(player, holder.backPage); return; }
        if (rawSlot == RingPageUnlockGUI.BTN_CONFIRM) doPageUnlock(player, holder);
    }

    private void doPageUnlock(Player player, RingPageUnlockGUI.PageUnlockHolder holder) {
        RingData data = plugin.getRingManager().get(player);
        int targetPage = holder.targetPage;
        if (data.getUnlockedPages() > targetPage) {
            plugin.getRingGUI().open(player, targetPage); return;
        }

        RingConfig.PageUnlock cfg = RingConfig.getPageUnlock(targetPage);
        if (cfg == null) { player.sendMessage(ChatColor.RED + "该页没有解锁配置"); return; }

        List<RingCost.ItemReq> reqs = RingCost.parseItems(cfg.items);

        String err = RingCost.check(player, cfg.permission, cfg.costPoints, cfg.costVault, reqs);
        if (err != null) {
            player.sendMessage(ChatColor.RED + err);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
            return;
        }
        RingCost.pay(player, cfg.permission, cfg.costPoints, cfg.costVault, reqs);

        data.unlockPage(targetPage);
        data.save();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        player.sendMessage(ChatColor.GREEN + "\u2714 已解锁第 " + (targetPage + 1) + " 页");
        plugin.getRingGUI().open(player, targetPage);
    }
}