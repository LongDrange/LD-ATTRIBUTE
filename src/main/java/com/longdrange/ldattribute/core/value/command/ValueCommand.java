package com.longdrange.ldattribute.core.value.command;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.value.ValueConfig;
import com.longdrange.ldattribute.core.value.ValueData;
import com.longdrange.ldattribute.core.value.ValuePAPI;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ValueCommand implements CommandExecutor, TabCompleter {

    private final LDAttribute plugin;
    public ValueCommand(LDAttribute plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String prefix = plugin.getCoreManager().prefix();
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help": sendHelp(sender); return true;
            case "list": {
                sender.sendMessage(ChatColor.GOLD + "==== 所有自定义值 ====");
                for (ValueConfig.ValueDef d : ValueConfig.all()) {
                    sender.sendMessage(ChatColor.YELLOW + "  " + d.id + ChatColor.GRAY
                            + " - " + d.name + ChatColor.GRAY
                            + " (" + (long) d.min + "~" + (long) d.max + ")");
                }
                sender.sendMessage(ChatColor.GRAY + "共 " + ValueConfig.all().size() + " 个");
                return true;
            }
            case "reload":
                if (!sender.hasPermission("ldattribute.value.admin")) {
                    sender.sendMessage(prefix + ChatColor.RED + "权限不足"); return true;
                }
                plugin.getValueManager().reload();
                sender.sendMessage(prefix + ChatColor.GREEN + "自定义值配置已重载");
                return true;
            case "add":
            case "take":
            case "set": {
                if (!sender.hasPermission("ldattribute.value.admin")) {
                    sender.sendMessage(prefix + ChatColor.RED + "权限不足"); return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(prefix + ChatColor.RED + "用法: /ldvalue " + sub + " <玩家> <值Id> <数值>");
                    return true;
                }
                Player t = plugin.getServer().getPlayerExact(args[1]);
                if (t == null) { sender.sendMessage(prefix + ChatColor.RED + "玩家不在线"); return true; }
                String id = args[2];
                if (!ValueConfig.exists(id)) {
                    sender.sendMessage(prefix + ChatColor.RED + "未知值Id: " + id + "（用 /ldvalue list 查看）");
                    return true;
                }
                double n;
                try { n = Double.parseDouble(args[3]); }
                catch (Exception ex) { sender.sendMessage(prefix + ChatColor.RED + "数值必须是数字"); return true; }

                ValueData d = plugin.getValueManager().get(t);
                switch (sub) {
                    case "add": d.add(id, n); break;
                    case "take": d.take(id, n); break;
                    case "set": d.set(id, n); break;
                }
                sender.sendMessage(prefix + ChatColor.GREEN + "已 " + sub + " " + t.getName()
                        + " 的 " + id + " " + n + "（当前 " + ValuePAPI.format(d.get(id)) + "）");
                return true;
            }
            case "info": {
                if (args.length == 1) {
                    // 查自己
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(prefix + ChatColor.RED + "用法: /ldvalue info <玩家> [值Id]");
                        return true;
                    }
                    dumpValue(sender, (Player) sender, null);
                    return true;
                }
                Player t = plugin.getServer().getPlayerExact(args[1]);
                if (t == null) { sender.sendMessage(prefix + ChatColor.RED + "玩家不在线"); return true; }
                String id = args.length >= 3 ? args[2] : null;
                if (id != null && !ValueConfig.exists(id)) {
                    sender.sendMessage(prefix + ChatColor.RED + "未知值Id: " + id); return true;
                }
                dumpValue(sender, t, id);
                return true;
            }
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void dumpValue(CommandSender sender, Player t, String id) {
        ValueData d = plugin.getValueManager().get(t);
        if (id != null) {
            ValueConfig.ValueDef def = ValueConfig.get(id);
            sender.sendMessage(ChatColor.GOLD + t.getName() + " 的 " + id + ": "
                    + ChatColor.WHITE + ValuePAPI.format(d.get(id))
                    + ChatColor.GRAY + " / " + (long) def.max);
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "==== " + t.getName() + " 的所有自定义值 ====");
        Map<String, Double> all = d.getAll();
        for (Map.Entry<String, Double> e : all.entrySet()) {
            ValueConfig.ValueDef def = ValueConfig.get(e.getKey());
            sender.sendMessage(ChatColor.YELLOW + "  " + e.getKey()
                    + ChatColor.GRAY + " (" + def.name + ChatColor.GRAY + "): "
                    + ChatColor.WHITE + ValuePAPI.format(e.getValue())
                    + ChatColor.GRAY + " / " + (long) def.max);
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "==== 自定义值 ====");
        sender.sendMessage(ChatColor.YELLOW + "/ldvalue list " + ChatColor.GRAY + "- 列出所有值Id");
        sender.sendMessage(ChatColor.YELLOW + "/ldvalue add <玩家> <值Id> <数值>");
        sender.sendMessage(ChatColor.YELLOW + "/ldvalue take <玩家> <值Id> <数值>");
        sender.sendMessage(ChatColor.YELLOW + "/ldvalue set <玩家> <值Id> <数值>");
        sender.sendMessage(ChatColor.YELLOW + "/ldvalue info [玩家] [值Id]");
        sender.sendMessage(ChatColor.YELLOW + "/ldvalue reload");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : Arrays.asList("help", "list", "reload", "add", "take", "set", "info")) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
        } else if (args.length == 2) {
            for (Player p : plugin.getServer().getOnlinePlayers())
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(p.getName());
        } else if (args.length == 3) {
            for (String id : ValueConfig.ids())
                if (id.toLowerCase().startsWith(args[2].toLowerCase())) out.add(id);
        }
        return out;
    }
}