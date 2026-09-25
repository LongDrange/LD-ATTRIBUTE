package com.longdrange.ldattribute.core.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.util.JoinConfig;
import com.longdrange.ldattribute.core.util.PapiUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public class CorePlayerListener implements Listener {

    private final LDAttribute plugin;
    public CorePlayerListener(LDAttribute plugin) { this.plugin = plugin; }

    /**
     * 高优先级拦截：静默原版 "xxx joined the game"
     * - setJoinMessage(null) → 控制台和聊天框都不显示
     * - 只有 core.yml 里 join.silent-vanilla = true 时才执行
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoinSilent(PlayerJoinEvent e) {
        FileConfiguration cfg = plugin.getCoreManager().getCoreConfig();
        if (cfg.getBoolean("join.silent-vanilla", false)) {
            e.setJoinMessage(null);
        }
    }

    /**
     * 高优先级拦截：静默原版 "xxx left the game"
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuitSilent(PlayerQuitEvent e) {
        FileConfiguration cfg = plugin.getCoreManager().getCoreConfig();
        if (cfg.getBoolean("join.silent-vanilla", false)) {
            e.setQuitMessage(null);
        }
    }

    /** 发送自定义欢迎消息（个人） */
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        final Player player = e.getPlayer();
        if (!JoinConfig.isEnabled()) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            boolean debug = plugin.getCoreManager().getCoreConfig().getBoolean("debug", false);

            if (JoinConfig.isPersonalEnabled()) {
                List<String> personal = JoinConfig.getPersonalMessage();
                for (String line : personal) {
                    if (line == null || line.isEmpty()) continue;
                    String parsed = PapiUtil.parse(player, line);
                    parsed = ChatColor.translateAlternateColorCodes('&', parsed);
                    if (debug) plugin.getLogger().info("[Debug/Join] " + parsed);
                    player.sendMessage(parsed);
                }
            }

            if (JoinConfig.isBroadcastEnabled()) {
                List<String> bc = JoinConfig.getBroadcast();
                for (String line : bc) {
                    if (line == null || line.isEmpty()) continue;
                    String parsed = PapiUtil.parse(player, line);
                    parsed = ChatColor.translateAlternateColorCodes('&', parsed);
                    Bukkit.broadcastMessage(parsed);
                }
            }
        }, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getModuleDataManager().unloadAll(e.getPlayer().getUniqueId(), true);
    }
}