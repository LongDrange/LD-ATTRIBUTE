package com.longdrange.ldattribute.core.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.util.PapiUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * 头顶名字前缀/后缀（team prefix/suffix）
 * 通过 core.yml 的 nametag 节点配置，支持 PAPI 变量
 */
public class NameTagListener implements Listener {

    private final LDAttribute plugin;
    private Team team;

    public NameTagListener(LDAttribute plugin) { this.plugin = plugin; }

    private void ensureTeam() {
        if (team != null) return;
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        team = sb.getTeam("ld_nametag");
        if (team == null) {
            team = sb.registerNewTeam("ld_nametag");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        FileConfiguration cfg = plugin.getCoreManager().getCoreConfig();
        if (!cfg.getBoolean("nametag.enabled", false)) return;

        final Player player = e.getPlayer();
        // 延迟 10 tick，等 PAPI 变量全部就绪
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            apply(player);
        }, 10L);
    }

    public void apply(Player player) {
        try {
            ensureTeam();
            if (team == null) return;

            FileConfiguration cfg = plugin.getCoreManager().getCoreConfig();
            String prefix = cfg.getString("nametag.prefix", "");
            String suffix = cfg.getString("nametag.suffix", "");

            prefix = ChatColor.translateAlternateColorCodes('&', prefix);
            suffix = ChatColor.translateAlternateColorCodes('&', suffix);

            prefix = PapiUtil.parse(player, prefix);
            suffix = PapiUtil.parse(player, suffix);

            // 限制 16 字符
            if (prefix.length() > 16) prefix = prefix.substring(0, 16);
            if (suffix.length() > 16) suffix = suffix.substring(0, 16);

            team.setPrefix(prefix);
            team.setSuffix(suffix);

            // 确保玩家在此 team
            if (!team.hasEntry(player.getName())) {
                team.addEntry(player.getName());
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[NameTag] 更新失败: " + t.getMessage());
        }
    }
}