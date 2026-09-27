package com.longdrange.ldattribute.core.guide.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.guide.GuideScoreConfig;
import com.longdrange.ldattribute.core.guide.GuideScoreManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * 聊天前缀：自动在聊天前加当前称号前缀
 * 前缀来自 配置/图鉴/称号.yml 里每个称号的 Prefix 字段
 */
public class GuideChatListener implements Listener {

    private final LDAttribute plugin;
    public GuideChatListener(LDAttribute plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if (player == null) return;

        try {
            GuideScoreManager sm = plugin.getCoreManager().getGuideScoreManager();
            if (sm == null) return;
            GuideScoreConfig.TitleDef t = sm.getCurrentTitle(player);
            if (t == null) return;
            String prefix = t.prefix;
            if (prefix == null || prefix.isEmpty()) return;

            // 先剥掉 ChatColor 用 & 的写法（如果配置是 &a，要再翻译一次）
            prefix = ChatColor.translateAlternateColorCodes((char)38, prefix);

            String format = e.getFormat();
            // 在 {player} 或 %1$s 前插入前缀
            if (format.contains("{player}")) {
                e.setFormat(format.replace("{player}", prefix + player.getName()));
            } else if (format.contains("%1$s")) {
                e.setFormat(format.replace("%1$s", prefix + "%1$s"));
            }
        } catch (Throwable ignored) {}
    }
}