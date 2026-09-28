package com.longdrange.ldattribute.core.autoattack.command;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.autoattack.AutoAttackConfig;
import com.longdrange.ldattribute.core.autoattack.AutoAttackManager;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AutoAttackCommand implements CommandExecutor, TabCompleter {

    private final LDAttribute plugin;
    public AutoAttackCommand(LDAttribute plugin) { this.plugin = plugin; }

    private static final String PREFIX = ChatColor.DARK_GRAY + "[" + ChatColor.RED + "杀戮" + ChatColor.DARK_GRAY + "] " + ChatColor.RESET;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) { sender.sendMessage(PREFIX + ChatColor.RED + "只能玩家用"); return true; }
            Player p = (Player) sender;
            AutoAttackManager mgr = plugin.getCoreManager().getAutoAttackManager();
            boolean on = mgr.toggle(p);
            p.sendMessage(PREFIX + (on ? ChatColor.GREEN + "⚔ 杀戮已开启" : ChatColor.RED + "⚔ 杀戮已关闭"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "on":
            case "off": {
                if (!(sender instanceof Player)) { sender.sendMessage(PREFIX + ChatColor.RED + "只能玩家用"); return true; }
                Player p = (Player) sender;
                AutoAttackManager mgr = plugin.getCoreManager().getAutoAttackManager();
                mgr.setEnabled(p, args[0].equalsIgnoreCase("on"));
                return true;
            }
            case "info": {
                if (!(sender instanceof Player)) { sender.sendMessage(PREFIX + ChatColor.RED + "只能玩家用"); return true; }
                Player p = (Player) sender;
                AutoAttackConfig.Profile prof = AutoAttackConfig.resolve(p);
                AutoAttackManager mgr = plugin.getCoreManager().getAutoAttackManager();
                sender.sendMessage(ChatColor.GOLD + "==== 杀戮信息 ====");
                sender.sendMessage(ChatColor.YELLOW + "当前状态: " + (mgr.isEnabled(p) ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭"));
                sender.sendMessage(ChatColor.YELLOW + "攻击范围: " + ChatColor.WHITE + prof.range);
                sender.sendMessage(ChatColor.YELLOW + "攻击间隔: " + ChatColor.WHITE + prof.interval + " tick");
                sender.sendMessage(ChatColor.YELLOW + "目标数: " + ChatColor.WHITE + (prof.maxTargets <= 0 ? "全部" : prof.maxTargets));
                sender.sendMessage(ChatColor.YELLOW + "防击退: " + ChatColor.WHITE + (prof.antiKnockback ? "开" : "关"));
                sender.sendMessage(ChatColor.YELLOW + "自动转向: " + ChatColor.WHITE + (prof.autoRotate ? "开" : "关"));
                return true;
            }
            case "reload": {
                if (!sender.hasPermission("ldattribute.autoattack.admin")) { sender.sendMessage(PREFIX + ChatColor.RED + "权限不足"); return true; }
                plugin.getCoreManager().getAutoAttackManager().reload();
                sender.sendMessage(PREFIX + ChatColor.GREEN + "配置已重载");
                return true;
            }
            case "set": {
                if (!sender.hasPermission("ldattribute.autoattack.admin")) { sender.sendMessage(PREFIX + ChatColor.RED + "权限不足"); return true; }
                if (args.length < 4) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "用法: /laa set <玩家> <范围|间隔|目标数|防击退> <值>");
                    return true;
                }
                String name = args[1];
                String key = args[2].toLowerCase();
                String val = args[3];
                AutoAttackConfig.Profile p = AutoAttackConfig.players.get(name.toLowerCase());
                if (p == null) {
                    p = AutoAttackConfig.defaultProfile.copy();
                    AutoAttackConfig.players.put(name.toLowerCase(), p);
                    if (p.displayName == null || p.displayName.isEmpty()) p.displayName = name;
                    else p.displayName = name;
                }
                try {
                    switch (key) {
                        case "range": case "范围": p.range = Double.parseDouble(val); break;
                        case "interval": case "间隔": p.interval = Integer.parseInt(val); break;
                        case "targets": case "目标数": p.maxTargets = Integer.parseInt(val); break;
                        case "antiknockback": case "防击退": p.antiKnockback = Boolean.parseBoolean(val); break;
                        case "autorotate": case "转向": p.autoRotate = Boolean.parseBoolean(val); break;
                        default:
                            sender.sendMessage(PREFIX + ChatColor.RED + "未知参数: " + key);
                            return true;
                    }
                } catch (Throwable t) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "数值无效: " + val);
                    return true;
                }
                AutoAttackConfig.savePlayers();
                sender.sendMessage(PREFIX + ChatColor.GREEN + "已设置 " + name + " 的 " + key + " = " + val);
                return true;
            }
            case "clear": {
                if (!sender.hasPermission("ldattribute.autoattack.admin")) { sender.sendMessage(PREFIX + ChatColor.RED + "权限不足"); return true; }
                if (args.length < 2) { sender.sendMessage(PREFIX + ChatColor.RED + "用法: /laa clear <玩家>"); return true; }
                String name = args[1];
                if (AutoAttackConfig.players.remove(name.toLowerCase()) == null) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "该玩家没有独立配置");
                    return true;
                }
                AutoAttackConfig.savePlayers();
                sender.sendMessage(PREFIX + ChatColor.GREEN + "已清除 " + name + " 的独立配置");
                return true;
            }
            case "list": {
                if (!sender.hasPermission("ldattribute.autoattack.admin")) { sender.sendMessage(PREFIX + ChatColor.RED + "权限不足"); return true; }
                sender.sendMessage(ChatColor.GOLD + "==== 独立配置玩家 ====");
                if (AutoAttackConfig.players.isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "（无）");
                    return true;
                }
                for (String n : AutoAttackConfig.players.keySet()) {
                    AutoAttackConfig.Profile p = AutoAttackConfig.players.get(n);
                    sender.sendMessage(ChatColor.YELLOW + (p.displayName != null && !p.displayName.isEmpty() ? p.displayName : n) + ChatColor.GRAY + " - 范围" + p.range + " 间隔" + p.interval + " 目标" + p.maxTargets);
                }
                return true;
            }
            case "help":
            default:
                sender.sendMessage(ChatColor.GOLD + "==== 杀戮系统 ====");
                sender.sendMessage(ChatColor.YELLOW + "/laa " + ChatColor.GRAY + "- 切换开关");
                sender.sendMessage(ChatColor.YELLOW + "/laa on|off " + ChatColor.GRAY + "- 强制开关");
                sender.sendMessage(ChatColor.YELLOW + "/laa info " + ChatColor.GRAY + "- 查看配置");
                sender.sendMessage(ChatColor.YELLOW + "/laa reload " + ChatColor.GRAY + "- 重载（管理员）");
                sender.sendMessage(ChatColor.YELLOW + "/laa set <玩家> <参数> <值> " + ChatColor.GRAY + "- 独立配置（管理员）");
                sender.sendMessage(ChatColor.YELLOW + "/laa clear <玩家> " + ChatColor.GRAY + "- 清除独立配置（管理员）");
                sender.sendMessage(ChatColor.YELLOW + "/laa list " + ChatColor.GRAY + "- 列出独立配置");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : Arrays.asList("on", "off", "info", "reload", "set", "clear", "list", "help"))
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("clear")) {
                for (Player p : plugin.getServer().getOnlinePlayers())
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(p.getName());
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("set")) {
                for (String s : Arrays.asList("range", "interval", "targets", "antiknockback", "autorotate"))
                    if (s.startsWith(args[2].toLowerCase())) out.add(s);
            }
        }
        return out;
    }
}
