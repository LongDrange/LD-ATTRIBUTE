package com.longdrange.ldattribute.points;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 點券指令
 * /ldpoints see/add/take/set/top/reload
 */
public class PointCommand implements CommandExecutor, TabCompleter {

    private final LDAttribute plugin;

    public PointCommand(LDAttribute plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("see")) { onSee(sender, args); return true; }
        if (sub.equals("add")) { onAdd(sender, args); return true; }
        if (sub.equals("take")) { onTake(sender, args); return true; }
        if (sub.equals("set")) { onSet(sender, args); return true; }
        if (sub.equals("top")) { onTop(sender, args); return true; }
        if (sub.equals("reload")) {
            if (!sender.hasPermission("ldattribute.points.admin")) {
                sender.sendMessage(Message.get("Admin.NoPermission"));
                return true;
            }
            PointData.loadData(plugin);
            sender.sendMessage(Message.get("Admin.PluginReload"));
            return true;
        }

        sendHelp(sender, label);
        return true;
    }

    // ==================== 子指令 ====================

    private void onSee(CommandSender sender, String[] args) {
        String playerName = sender.getName();
        if (args.length >= 2 && sender.hasPermission("ldattribute.points.admin")) {
            playerName = args[1];
        }
        sender.sendMessage(Message.get("Points.Player.See",
                playerName, PointData.getPlayerPoints(playerName)));
    }

    private void onAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ldattribute.points.admin")) {
            sender.sendMessage(Message.get("Admin.NoPermission"));
            return;
        }
        if (args.length < 3 || !isNumber(args[2])) {
            sender.sendMessage(Message.get("Admin.NoFormat"));
            return;
        }
        String playerName = args[1];
        int value = Integer.parseInt(args[2]);
        int points = PointData.getPlayerPoints(playerName);
        int newPoints = points + value;
        PointData.setPlayerPoints(playerName, newPoints);
        sender.sendMessage(Message.get("Points.Admin.Add", playerName, value, newPoints));
        if (Bukkit.getOfflinePlayer(playerName).isOnline()) {
            Bukkit.getPlayer(playerName).sendMessage(
                    Message.get("Points.Player.Add", value, newPoints));
        }
    }

    private void onTake(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ldattribute.points.admin")) {
            sender.sendMessage(Message.get("Admin.NoPermission"));
            return;
        }
        if (args.length < 3 || !isNumber(args[2])) {
            sender.sendMessage(Message.get("Admin.NoFormat"));
            return;
        }
        String playerName = args[1];
        int value = Integer.parseInt(args[2]);
        int points = PointData.getPlayerPoints(playerName);
        int newPoints = points - value;
        PointData.setPlayerPoints(playerName, newPoints);
        sender.sendMessage(Message.get("Points.Admin.Take", playerName, value, newPoints));
        if (Bukkit.getOfflinePlayer(playerName).isOnline()) {
            Bukkit.getPlayer(playerName).sendMessage(
                    Message.get("Points.Player.Take", value, newPoints));
        }
    }

    private void onSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ldattribute.points.admin")) {
            sender.sendMessage(Message.get("Admin.NoPermission"));
            return;
        }
        if (args.length < 3 || !isNumber(args[2])) {
            sender.sendMessage(Message.get("Admin.NoFormat"));
            return;
        }
        String playerName = args[1];
        int value = Integer.parseInt(args[2]);
        PointData.setPlayerPoints(playerName, value);
        sender.sendMessage(Message.get("Points.Admin.Set", playerName, value));
    }

    private void onTop(CommandSender sender, String[] args) {
        int page = 1;
        if (args.length >= 2 && isNumber(args[1])) {
            page = Integer.parseInt(args[1]);
        }
        PointData.sendPointsTop(sender, page);
    }

    // ==================== 輔助 ====================

    private boolean isNumber(String s) {
        return Pattern.compile("[0-9]+").matcher(s).matches();
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes((char)38,
                "&6==========[&b LD-Points &6]=========="));
        sender.sendMessage(ChatColor.translateAlternateColorCodes((char)38,
                "&7/" + label + " see [玩家] &6-&3 查看點券"));
        if (sender.hasPermission("ldattribute.points.admin")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes((char)38,
                    "&7/" + label + " add <玩家> <點數> &6-&3 增加點券"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes((char)38,
                    "&7/" + label + " take <玩家> <點數> &6-&3 減少點券"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes((char)38,
                    "&7/" + label + " set <玩家> <點數> &6-&3 設定點券"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes((char)38,
                    "&7/" + label + " reload &6-&3 重新載入"));
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes((char)38,
                "&7/" + label + " top [頁數] &6-&3 點券排行榜"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            for (String s : Arrays.asList("see", "add", "take", "set", "top", "reload")) {
                if (s.startsWith(args[0].toLowerCase())) result.add(s);
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("take")
                    || args[0].equalsIgnoreCase("set")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().startsWith(args[1])) result.add(p.getName());
                }
            }
        }
        return result;
    }
}
