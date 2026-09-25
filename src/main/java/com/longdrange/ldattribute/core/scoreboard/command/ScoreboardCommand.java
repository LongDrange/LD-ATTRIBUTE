package com.longdrange.ldattribute.core.scoreboard.command;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.scoreboard.ScoreboardConfig;
import com.longdrange.ldattribute.core.scoreboard.ScoreboardManager;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScoreboardCommand implements CommandExecutor, TabCompleter {

    private final LDAttribute plugin;
    public ScoreboardCommand(LDAttribute plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String prefix = plugin.getCoreManager().prefix();

        if (args.length == 0) {
            if (!(sender instanceof Player)) { sender.sendMessage(prefix + ChatColor.RED + "只能由玩家执行"); return true; }
            Player p = (Player) sender;
            boolean nowVisible = plugin.getScoreboardManager().toggle(p);
            p.sendMessage(prefix + (nowVisible ? ChatColor.GREEN + "侧边栏已显示" : ChatColor.YELLOW + "侧边栏已隐藏"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "toggle": {
                if (!(sender instanceof Player)) { sender.sendMessage(prefix + ChatColor.RED + "只能由玩家执行"); return true; }
                Player p = (Player) sender;
                boolean nowVisible = plugin.getScoreboardManager().toggle(p);
                p.sendMessage(prefix + (nowVisible ? ChatColor.GREEN + "侧边栏已显示" : ChatColor.YELLOW + "侧边栏已隐藏"));
                return true;
            }
            case "reload": {
                if (!sender.hasPermission("ldattribute.scoreboard.admin")) {
                    sender.sendMessage(prefix + ChatColor.RED + "权限不足"); return true;
                }
                plugin.getScoreboardManager().reload();
                sender.sendMessage(prefix + ChatColor.GREEN + "记分板配置已重载");
                return true;
            }
            case "list": {
                sender.sendMessage(ChatColor.GOLD + "==== 所有记分板 ====");
                for (ScoreboardConfig.BoardDef def : ScoreboardConfig.allBoards()) {
                    sender.sendMessage(ChatColor.YELLOW + "  " + def.id
                            + ChatColor.GRAY + " | 条件: " + def.condition
                            + ChatColor.GRAY + " | 行数: " + def.lines.size());
                }
                return true;
            }
            case "help": {
                sender.sendMessage(ChatColor.GOLD + "==== 侧边栏 ====");
                sender.sendMessage(ChatColor.YELLOW + "/ldsb " + ChatColor.GRAY + "- 切换显示");
                sender.sendMessage(ChatColor.YELLOW + "/ldsb toggle " + ChatColor.GRAY + "- 切换显示");
                sender.sendMessage(ChatColor.YELLOW + "/ldsb list " + ChatColor.GRAY + "- 列出所有记分板");
                sender.sendMessage(ChatColor.YELLOW + "/ldsb reload " + ChatColor.GRAY + "- 重载配置");
                return true;
            }
            default:
                sender.sendMessage(prefix + ChatColor.RED + "未知子命令，试试 /ldsb help");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : Arrays.asList("toggle", "reload", "list", "help")) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
        }
        return out;
    }
}