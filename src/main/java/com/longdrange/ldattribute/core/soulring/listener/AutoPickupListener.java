package com.longdrange.ldattribute.core.soulring.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.soulring.SoulRingConfig;
import com.longdrange.ldattribute.core.soulring.SoulRingData;
import com.longdrange.ldattribute.core.soulring.rate.RateManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AutoPickupListener implements Listener {

    private final LDAttribute plugin;
    public AutoPickupListener(LDAttribute plugin) { this.plugin = plugin; }

    // ==================== 怪物掉落 ====================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent e) {
        if (!SoulRingConfig.isAutoPickupEnabled()) return;
        if (!SoulRingConfig.isAutoPickupMobDrops()) return;

        LivingEntity entity = e.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        SoulRingData data = plugin.getSoulRingManager().get(killer);
        if (!data.isAutoPickupOn()) return;

        List<ItemStack> drops = e.getDrops();
        if (drops.isEmpty()) return;

        double rate = RateManager.getRate(killer);

        long totalPicked = 0;
        List<ItemStack> leftover = new ArrayList<>();

        for (ItemStack drop : drops) {
            if (drop == null) continue;
            if (SoulRingConfig.isFiltered(drop)) {
                leftover.add(drop);
                continue;
            }
            // ★ 应用倍率
            int newAmount = (int) Math.max(1, Math.round(drop.getAmount() * rate));
            ItemStack boosted = drop.clone();
            boosted.setAmount(newAmount);
            long added = data.deposit(boosted, boosted.getAmount());
            if (added >= boosted.getAmount()) {
                totalPicked += added;
            } else if (added > 0) {
                totalPicked += added;
                ItemStack copy = boosted.clone();
                copy.setAmount((int) (boosted.getAmount() - added));
                leftover.add(copy);
            } else {
                leftover.add(drop);
            }
        }

        drops.clear();
        drops.addAll(leftover);

        if (totalPicked > 0 && SoulRingConfig.isAutoPickupMessage()) {
            String rateText = rate > 1 ? ChatColor.GOLD + " x" + rate : "";
            killer.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.LIGHT_PURPLE + "灵魂空间"
                    + ChatColor.DARK_GRAY + "] " + ChatColor.GREEN + "自动拾取 " + ChatColor.WHITE
                    + totalPicked + ChatColor.GREEN + " 个物品" + rateText);
        }
    }

    // ==================== 挖方块掉落 ====================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (!SoulRingConfig.isAutoPickupEnabled()) return;
        if (!SoulRingConfig.isAutoPickupBlockDrops()) return;

        Player player = e.getPlayer();
        if (player == null) return;
        if (player.getGameMode() != null
                && player.getGameMode().name().equals("CREATIVE")) return;

        SoulRingData data = plugin.getSoulRingManager().get(player);
        if (!data.isAutoPickupOn()) return;

        Collection<ItemStack> drops;
        try {
            drops = e.getBlock().getDrops(player.getInventory().getItemInMainHand());
        } catch (Throwable t) { return; }
        if (drops.isEmpty()) return;

        double rate = RateManager.getRate(player);

        long totalPicked = 0;
        List<ItemStack> leftover = new ArrayList<>();

        for (ItemStack drop : drops) {
            if (drop == null) continue;
            if (SoulRingConfig.isFiltered(drop)) {
                leftover.add(drop);
                continue;
            }
            int newAmount = (int) Math.max(1, Math.round(drop.getAmount() * rate));
            ItemStack boosted = drop.clone();
            boosted.setAmount(newAmount);
            long added = data.deposit(boosted, boosted.getAmount());
            if (added >= boosted.getAmount()) {
                totalPicked += added;
            } else if (added > 0) {
                totalPicked += added;
                ItemStack copy = boosted.clone();
                copy.setAmount((int) (boosted.getAmount() - added));
                leftover.add(copy);
            } else {
                leftover.add(drop);
            }
        }

        if (totalPicked > 0) {
            e.setCancelled(true);
            try { e.getBlock().setType(org.bukkit.Material.AIR); } catch (Throwable ignored) {}
            for (ItemStack l : leftover) {
                e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), l);
            }
            if (SoulRingConfig.isAutoPickupMessage()) {
                String rateText = rate > 1 ? ChatColor.GOLD + " x" + rate : "";
                player.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.LIGHT_PURPLE + "灵魂空间"
                        + ChatColor.DARK_GRAY + "] " + ChatColor.GREEN + "自动拾取 " + ChatColor.WHITE
                        + totalPicked + ChatColor.GREEN + " 个物品" + rateText);
            }
        }
    }
}