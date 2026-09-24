package com.longdrange.ldattribute.util;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * 可点击列表通用模板
 *   - 每行形如：<prefix> §a§l[hint]
 *   - 悬停显示物品信息
 *   - 点击执行命令
 */
public class ClickableList {

    public static class Entry {
        public final String prefix;      // 行首文本（含颜色，如 "§e孙逊 §7"）
        public final ItemStack item;     // 悬停物品（可为 null）
        public final String clickCmd;    // 点击执行的命令（如 "/ldc giveme LongBing 孙逊 1"）
        public final String hint;        // 行尾提示（如 "§a§l[领取]"）
        public Entry(String prefix, ItemStack item, String clickCmd, String hint) {
            this.prefix = prefix;
            this.item = item;
            this.clickCmd = clickCmd;
            this.hint = hint;
        }
    }

    /** 发一行 */
    public static void sendEntry(CommandSender sender, Entry e) {
        if (!(sender instanceof Player)) {
            // 控制台不支持 hover/click，退化为纯文本
            sender.sendMessage(e.prefix + " " + stripColor(e.hint) + " §7(" + e.clickCmd + ")");
            return;
        }

        TextComponent root = new TextComponent(e.prefix);
        TextComponent link = new TextComponent(e.hint == null ? " §a§l[点击]" : " " + e.hint);

        StringBuilder sb = new StringBuilder();
        if (e.item != null && e.item.getType() != Material.AIR) {
            sb.append("§7物品: §f").append(e.item.getType().name());
            ItemMeta m = e.item.getItemMeta();
            if (m != null && m.hasDisplayName()) sb.append("\n§7名称: §f").append(m.getDisplayName());
            sb.append("\n§7数量: §f").append(e.item.getAmount());
        }
        sb.append("\n§7点击执行: §a").append(e.clickCmd);

        link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(sb.toString()).create()));
        link.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, e.clickCmd));
        root.addExtra(link);
        ((Player) sender).spigot().sendMessage(root);
    }

    /** 发整段列表（带标题） */
    public static void sendList(CommandSender sender, String title, List<Entry> entries) {
        if (title != null && !title.isEmpty()) sender.sendMessage(title);
        for (Entry e : entries) sendEntry(sender, e);
    }

    private static String stripColor(String s) {
        return s == null ? "" : s.replaceAll("§[0-9a-fk-or]", "");
    }
}
