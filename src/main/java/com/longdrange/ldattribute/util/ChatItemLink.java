package com.longdrange.ldattribute.util;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 聊天栏物品链接：
 *   - 悬停显示物品信息（SHOW_TEXT 方式，跨版本）
 *   - 点击执行指令（一般是 /ldc giveme ...）
 */
public class ChatItemLink {

    public static void send(Player viewer, String prefix, ItemStack item,
                            String clickCommand, String hint) {
        TextComponent msg = new TextComponent(prefix);
        if (item != null && item.getType() != Material.AIR) {
            TextComponent link = new TextComponent(hint == null ? " §e§l[点击]" : hint);

            StringBuilder sb = new StringBuilder();
            sb.append("§7物品: §f").append(item.getType().name());
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                sb.append("\n§7名称: §f").append(meta.getDisplayName());
            }
            sb.append("\n§7数量: §f").append(item.getAmount());
            sb.append("\n§7点击执行: §a").append(clickCommand);

            link.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(sb.toString()).create()));
            link.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND, clickCommand));
            msg.addExtra(link);
        }
        viewer.spigot().sendMessage(msg);
    }
}