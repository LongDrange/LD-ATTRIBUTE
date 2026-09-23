package com.longdrange.ldattribute.command;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.data.attribute.LDAttributeManager;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * LD-Attribute 主指令
 */
public class LDCommand implements CommandExecutor, TabCompleter {

    private final LDAttribute plugin;

    public LDCommand(LDAttribute plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help":
                sendHelp(sender);
                return true;
            case "list":
                if (!sender.hasPermission("ldattribute.command.list")) {
                    sender.sendMessage(msg("&c你沒有權限！"));
                    return true;
                }
                sendList(sender);
                return true;
            case "info":
                if (!sender.hasPermission("ldattribute.command.info")) {
                    sender.sendMessage(msg("&c你沒有權限！"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(msg("&c用法：/lda info <屬性名>"));
                    return true;
                }
                sendInfo(sender, args[1]);
                return true;
            case "reload":
                if (!sender.hasPermission("ldattribute.command.reload")) {
                    sender.sendMessage(msg("&c你沒有權限！"));
                    return true;
                }
                plugin.reload();
                sender.sendMessage(msg("&a配置已重新載入！"));
                return true;
            case "version":
                sender.sendMessage(msg("&bLD-Attribute &e" + plugin.getDescription().getVersion()));
                return true;
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(msg("&8&m----------&r &bLD-Attribute &8&m----------"));
        sender.sendMessage(msg("&e/lda help &7- 顯示幫助"));
        sender.sendMessage(msg("&e/lda list &7- 列出所有屬性"));
        sender.sendMessage(msg("&e/lda info <屬性> &7- 查看屬性資訊"));
        sender.sendMessage(msg("&e/lda reload &7- 重新載入配置"));
        sender.sendMessage(msg("&e/lda version &7- 顯示版本"));
        sender.sendMessage(msg("&8&m----------------------------------"));
    }

    private void sendList(CommandSender sender) {
        sender.sendMessage(msg("&8&m----------&r &b已註冊屬性 &8&m----------"));
        for (LDSubAttribute attr : LDAttributeManager.getAttributeMap().values()) {
            sender.sendMessage(msg("&e" + attr.getName()
                    + " &7[優先級 " + attr.getPriority()
                    + ", 類型 " + Arrays.toString(attr.getType()) + "]"));
        }
        sender.sendMessage(msg("&8&m----------------------------------"));
    }

    private void sendInfo(CommandSender sender, String name) {
        LDSubAttribute attr = null;
        for (LDSubAttribute a : LDAttributeManager.getAttributeMap().values()) {
            if (a.getName().equalsIgnoreCase(name)) {
                attr = a;
                break;
            }
        }
        if (attr == null) {
            sender.sendMessage(msg("&c找不到屬性：&e" + name));
            return;
        }
        sender.sendMessage(msg("&8&m----------&r &b" + attr.getName() + " &8&m----------"));
        sender.sendMessage(msg("&7優先級：&e" + attr.getPriority()));
        sender.sendMessage(msg("&7類型：&e" + Arrays.toString(attr.getType())));
        sender.sendMessage(msg("&7數值：&e" + Arrays.toString(attr.getAttributes())));
        sender.sendMessage(msg("&7佔位符：&e" + attr.getPlaceholders()));
        sender.sendMessage(msg("&8&m----------------------------------"));
    }

    private String msg(String s) {
        String prefix = plugin.getConfigUtil().getConfig()
                .getString("Message.Prefix", "&8[&bLD-Attribute&8] &r");
        return ChatColor.translateAlternateColorCodes('&', prefix + s);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            for (String s : Arrays.asList("help", "list", "info", "reload", "version")) {
                if (s.startsWith(args[0].toLowerCase())) result.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            for (LDSubAttribute attr : LDAttributeManager.getAttributeMap().values()) {
                if (attr.getName().startsWith(args[1])) result.add(attr.getName());
            }
        }
        return result;
    }
}