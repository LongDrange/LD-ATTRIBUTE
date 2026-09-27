package com.longdrange.ldattribute.core.dungeon.command;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.dungeon.DungeonConfig;
import com.longdrange.ldattribute.core.dungeon.DungeonTeam;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class DungeonCommand implements CommandExecutor, TabCompleter {

    private final LDAttribute plugin;
    public DungeonCommand(LDAttribute plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                plugin.getDungeonGUI().open((Player) sender);
            } else {
                sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令");
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("list")) {
            sender.sendMessage(ChatColor.GOLD + "===== 副本列表 =====");
            for (DungeonConfig.DungeonDef d : DungeonConfig.all()) {
                sender.sendMessage(ChatColor.YELLOW + "- " + ChatColor.WHITE + d.id
                        + ChatColor.GRAY + " (" + ChatColor.stripColor(d.name) + ChatColor.GRAY + ")");
            }
            return true;
        }

        if (sub.equals("team")) return handleTeam(sender, args);
        if (sub.equals("rank") || sub.equals("ranking")) {
            if (!(sender instanceof Player)) { sender.sendMessage("\u00a7c只有玩家能用"); return true; }
            plugin.getDungeonRankGUI().open((Player) sender);
            return true;
        }

        if (sub.equals("achieve") || sub.equals("achievement")) {
            if (!(sender instanceof Player)) { sender.sendMessage("\u00a7c只有玩家能用"); return true; }
            plugin.getDungeonAchievementGUI().open((Player) sender);
            return true;
        }

        if (sub.equals("leave") || sub.equals("quit")) {
            if (!(sender instanceof Player)) { sender.sendMessage("\u00a7c只有玩家能用"); return true; }
            plugin.getDungeonManager().leaveDungeon((Player) sender);
            return true;
        }

        if (sub.equals("reload")) {
            if (!sender.hasPermission("ldattribute.dungeon.admin") && !sender.isOp()) {
                sender.sendMessage("\u00a7c无权限"); return true;
            }
            plugin.getDungeonManager().reload();
            sender.sendMessage("\u00a7a已重载副本配置");
            return true;
        }

        if (sub.equals("join")) {
            if (!(sender instanceof Player)) { sender.sendMessage("\u00a7c只有玩家能用"); return true; }
            if (args.length < 2) { sender.sendMessage("\u00a7e用法: /lddungeon join <副本ID>"); return true; }
            DungeonConfig.DungeonDef def = DungeonConfig.get(args[1]);
            if (def == null) { sender.sendMessage("\u00a7c副本不存在: " + args[1]); return true; }
            plugin.getDungeonManager().startDungeon((Player) sender, def);
            return true;
        }

        sender.sendMessage("\u00a7e用法: /lddungeon [list|join <id>|leave|reload|team ...]");
        return true;
    }

    private boolean handleTeam(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7c只有玩家能用"); return true;
        }
        Player p = (Player) sender;
        plugin.getDungeonTeamGUI().openMain(p);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : new String[]{"list", "join", "leave", "reload", "team", "rank"}) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            for (DungeonConfig.DungeonDef d : DungeonConfig.all()) {
                if (d.id.toLowerCase().startsWith(args[1].toLowerCase())) out.add(d.id);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("team")) {
            for (String s : new String[]{"create", "invite", "accept", "leave", "kick", "list"}) {
                if (s.startsWith(args[1].toLowerCase())) out.add(s);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("team")
                && (args[1].equalsIgnoreCase("invite") || args[1].equalsIgnoreCase("kick"))) {
            for (Player pl : Bukkit.getOnlinePlayers()) {
                if (pl.getName().toLowerCase().startsWith(args[2].toLowerCase())) out.add(pl.getName());
            }
        }
        return out;
    }
}
