package com.longdrange.ldattribute.core.talent.command;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.talent.TalentConfig;
import com.longdrange.ldattribute.core.talent.TalentData;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TalentCommand implements CommandExecutor, TabCompleter {

    private final LDAttribute plugin;
    public TalentCommand(LDAttribute plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String prefix = plugin.getCoreManager().prefix();

        if (args.length == 0) {
            if (!(sender instanceof Player)) { sender.sendMessage(prefix + ChatColor.RED + "只能由玩家执行"); return true; }
            plugin.getTalentGUI().open((Player) sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        // /ldtalent <页id>
        TalentConfig.PageDef page = TalentConfig.getPage(sub);
        if (page != null) {
            if (!(sender instanceof Player)) { sender.sendMessage(prefix + ChatColor.RED + "只能由玩家执行"); return true; }
            plugin.getTalentGUI().open((Player) sender, page.id);
            return true;
        }

        switch (sub) {
            case "help":
                sender.sendMessage(ChatColor.GOLD + "==== 天赋 ====");
                sender.sendMessage(ChatColor.YELLOW + "/ldtalent " + ChatColor.GRAY + "- 打开天赋");
                sender.sendMessage(ChatColor.YELLOW + "/ldtalent <页id> " + ChatColor.GRAY + "- 打开指定页");
                sender.sendMessage(ChatColor.YELLOW + "/ldtalent list " + ChatColor.GRAY + "- 列出所有页");
                sender.sendMessage(ChatColor.YELLOW + "/ldtalent reload " + ChatColor.GRAY + "- 重载配置");
                sender.sendMessage(ChatColor.YELLOW + "/ldtalent points <玩家> <页> add/take/set <数量> " + ChatColor.GRAY + "- 管理点数");
                sender.sendMessage(ChatColor.YELLOW + "/ldtalent reset <玩家> [页] " + ChatColor.GRAY + "- 重置天赋");
                return true;
            case "list":
                sender.sendMessage(ChatColor.GOLD + "==== 所有天赋页 ====");
                for (TalentConfig.PageDef p : TalentConfig.allPages())
                    sender.sendMessage(ChatColor.YELLOW + "  " + p.id + ChatColor.GRAY + " - "
                            + ChatColor.stripColor(p.name) + " (" + p.talents.size() + " 天赋)");
                return true;
            case "reload":
                if (!sender.hasPermission("ldattribute.talent.admin")) { sender.sendMessage(prefix + ChatColor.RED + "权限不足"); return true; }
                plugin.getTalentManager().reload();
                sender.sendMessage(prefix + ChatColor.GREEN + "天赋配置已重载");
                return true;
            case "points": {
                if (!sender.hasPermission("ldattribute.talent.admin")) { sender.sendMessage(prefix + ChatColor.RED + "权限不足"); return true; }
                if (args.length < 5) { sender.sendMessage(prefix + ChatColor.RED + "用法: /ldtalent points <玩家> <页> add/take/set <数量>"); return true; }
                Player t = plugin.getServer().getPlayerExact(args[1]);
                if (t == null) { sender.sendMessage(prefix + ChatColor.RED + "玩家不在线"); return true; }
                String pid = args[2];
                if (TalentConfig.getPage(pid) == null) { sender.sendMessage(prefix + ChatColor.RED + "未知页: " + pid); return true; }
                String op = args[3].toLowerCase();
                int n;
                try { n = Integer.parseInt(args[4]); } catch (Exception e) { sender.sendMessage(prefix + ChatColor.RED + "数量必须是数字"); return true; }
                TalentData d = plugin.getTalentManager().get(t);
                switch (op) {
                    case "add": d.addPoints(pid, n); break;
                    case "take": d.takePoints(pid, n); break;
                    case "set": d.setPoints(pid, n); break;
                    default: sender.sendMessage(prefix + ChatColor.RED + "未知操作: " + op); return true;
                }
                d.save();
                sender.sendMessage(prefix + ChatColor.GREEN + "已对 " + t.getName() + " 的 " + pid + " 页执行 " + op + " " + n
                        + "（当前 " + d.getPoints(pid) + "）");
                return true;
            }
            case "reset": {
                if (!sender.hasPermission("ldattribute.talent.admin")) { sender.sendMessage(prefix + ChatColor.RED + "权限不足"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + ChatColor.RED + "用法: /ldtalent reset <玩家> [页]"); return true; }
                Player t = plugin.getServer().getPlayerExact(args[1]);
                if (t == null) { sender.sendMessage(prefix + ChatColor.RED + "玩家不在线"); return true; }
                TalentData d = plugin.getTalentManager().get(t);
                if (args.length >= 3) {
                    String pid = args[2];
                    TalentConfig.PageDef p = TalentConfig.getPage(pid);
                    if (p == null) { sender.sendMessage(prefix + ChatColor.RED + "未知页: " + pid); return true; }
                    for (TalentConfig.TalentDef td : p.talents.values()) d.setLevel(td.id, 0);
                } else {
                    for (TalentConfig.TalentDef td : TalentConfig.allTalents()) d.setLevel(td.id, 0);
                }
                d.save();
                sender.sendMessage(prefix + ChatColor.GREEN + "已重置 " + t.getName() + " 的天赋");
                return true;
            }
            default:
                sender.sendMessage(prefix + ChatColor.RED + "未知参数，试试 /ldtalent help");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            List<String> base = new ArrayList<>(Arrays.asList("help", "list", "reload", "points", "reset"));
            for (TalentConfig.PageDef p : TalentConfig.allPages()) base.add(p.id);
            for (String s : base) if (s.startsWith(args[0].toLowerCase())) out.add(s);
        } else if (args.length == 2) {
            for (Player p : plugin.getServer().getOnlinePlayers())
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(p.getName());
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("points") || args[0].equalsIgnoreCase("reset"))) {
            for (TalentConfig.PageDef p : TalentConfig.allPages())
                if (p.id.startsWith(args[2].toLowerCase())) out.add(p.id);
        } else if (args.length == 4 && args[0].equalsIgnoreCase("points")) {
            for (String s : Arrays.asList("add", "take", "set"))
                if (s.startsWith(args[3].toLowerCase())) out.add(s);
        }
        return out;
    }
}