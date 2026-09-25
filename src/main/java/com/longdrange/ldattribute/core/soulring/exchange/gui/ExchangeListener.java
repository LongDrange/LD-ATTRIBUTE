package com.longdrange.ldattribute.core.soulring.exchange.gui;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.soulring.exchange.ExchangeConfig;
import com.longdrange.ldattribute.core.soulring.exchange.ExchangeManager;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;

public class ExchangeListener implements Listener {

    private final LDAttribute plugin;
    public ExchangeListener(LDAttribute plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!(e.getInventory().getHolder() instanceof ExchangeHolder)) return;
        Player player = (Player) e.getWhoClicked();
        ExchangeHolder holder = (ExchangeHolder) e.getInventory().getHolder();

        e.setCancelled(true);
        int rawSlot = e.getRawSlot();
        if (rawSlot < 0 || rawSlot >= e.getInventory().getSize()) return;

        // 返回按钮
        if (rawSlot == ExchangeGUI.BTN_BACK) {
            plugin.getSoulRingGUI().open(player);
            return;
        }

        // 物品格
        if (rawSlot < 45) {
            ExchangeConfig.Exchange ex = ExchangeConfig.getBySlot(holder.getPageId(), rawSlot);
            if (ex == null) return;
            handleExchange(player, holder, ex, e);
        }
    }

    private void handleExchange(Player player, ExchangeHolder holder,
                                ExchangeConfig.Exchange ex, InventoryClickEvent e) {
        int times = e.isShiftClick() ? 0 : 1;
        ExchangeManager.Result r = ExchangeManager.exchange(plugin, player, ex, times);

        if (!r.success) {
            player.sendMessage(ChatColor.RED + "" + r.message);
            try { player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f); } catch (Throwable ignored) {}
            return;
        }

        player.sendMessage(ChatColor.GREEN + "\u2714 " + ex.name + " §7- " + r.message);
        try { player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f); } catch (Throwable ignored) {}

        plugin.getExchangeGUI().refresh(player, holder);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof ExchangeHolder) {
            e.setCancelled(true);
        }
    }
}