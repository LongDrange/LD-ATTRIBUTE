package com.longdrange.ldattribute.core.autoattack.listener;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class AutoAttackListener implements Listener {

    private final LDAttribute plugin;
    public AutoAttackListener(LDAttribute plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        try { plugin.getCoreManager().getAutoAttackManager().unload(e.getPlayer()); }
        catch (Throwable ignored) {}
    }
}