package com.longdrange.ldattribute.core.guide.command;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.guide.*;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuideCommand implements CommandExecutor, TabCompleter {

    private final LDAttribute plugin;
    public GuideCommand(LDAttribute plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String prefix = plugin.getCoreManager().prefix();
        if (args.length == 0) {
            if (!(sender instanceof Player)) { sender.sendMessage(prefix + ChatColor.RED + "只能由玩家执行"); return true; }
            plugin.getGuideGUI().open((Player) sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help":
                sender.sendMessage(ChatColor.GOLD + "==== 怪物图鉴 ====");
                sender.sendMessage(ChatColor.YELLOW + "/ldguide " + ChatColor.GRAY + "- 打开图鉴");
                sender.sendMessage(ChatColor.YELLOW + "/ldguide unlock <玩家> <怪物id> " + ChatColor.GRAY + "- 强制解锁");
                sender.sendMessage(ChatColor.YELLOW + "/ldguide addkills <玩家> <怪物id> <数量> " + ChatColor.GRAY + "- 加击杀");
                sender.sendMessage(ChatColor.YELLOW + "/ldguide reload " + ChatColor.GRAY + "- 重载");
                return true;
            case "reload":
                if (!sender.hasPermission("ldattribute.guide.admin")) { sender.sendMessage(prefix + ChatColor.RED + "权限不足"); return true; }
                plugin.getGuideManager().reload();
                sender.sendMessage(prefix + ChatColor.GREEN + "图鉴配置已重载");
                return true;
            case "unlock": {
                if (!sender.hasPermission("ldattribute.guide.admin")) { sender.sendMessage(prefix + ChatColor.RED + "权限不足"); return true; }
                if (args.length < 3) { sender.sendMessage(prefix + ChatColor.RED + "用法: /ldguide unlock <玩家> <怪物id>"); return true; }
                Player t = plugin.getServer().getPlayerExact(args[1]);
                if (t == null) { sender.sendMessage(prefix + ChatColor.RED + "玩家不在线"); return true; }
                GuideConfig.MonsterDef def = GuideConfig.findByNameOrId(args[2]);
                if (def == null) { sender.sendMessage(prefix + ChatColor.RED + "未知怪物id: " + args[2]); return true; }
                GuideData d = plugin.getGuideManager().get(t);
                d.setUnlocked(def.id, true);
                try { com.longdrange.ldattribute.card.StatsDataRead.updatePlayer(t); } catch (Throwable ignored) {}
                sender.sendMessage(prefix + ChatColor.GREEN + "已解锁 " + t.getName() + " 的 " + def.name);
                return true;
            }
            case "addkills": {
                if (!sender.hasPermission("ldattribute.guide.admin")) { sender.sendMessage(prefix + ChatColor.RED + "权限不足"); return true; }
                if (args.length < 4) { sender.sendMessage(prefix + ChatColor.RED + "用法: /ldguide addkills <玩家> <怪物id> <数量>"); return true; }
                Player t = plugin.getServer().getPlayerExact(args[1]);
                if (t == null) { sender.sendMessage(prefix + ChatColor.RED + "玩家不在线"); return true; }
                GuideConfig.MonsterDef def = GuideConfig.findByNameOrId(args[2]);
                if (def == null) { sender.sendMessage(prefix + ChatColor.RED + "未知怪物id: " + args[2]); return true; }
                int n;
                try { n = Integer.parseInt(args[3]); } catch (Exception ex) { sender.sendMessage(prefix + ChatColor.RED + "数量必须是数字"); return true; }
                GuideData d = plugin.getGuideManager().get(t);
                int cur = d.addKills(def.id, n);
                if (cur >= def.requiredKills && !d.isUnlocked(def.id)) {
                    d.setUnlocked(def.id, true);
                    try { com.longdrange.ldattribute.card.StatsDataRead.updatePlayer(t); } catch (Throwable ignored) {}
                    sender.sendMessage(prefix + ChatColor.GREEN + "击杀已达 " + cur + "，" + def.name + " 已解锁");
                } else {
                    sender.sendMessage(prefix + ChatColor.GREEN + "击杀数更新: " + cur + "/" + def.requiredKills);
                }
                return true;
            }
            default:
                sender.sendMessage(prefix + ChatColor.RED + "未知子命令");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : Arrays.asList("help", "reload", "unlock", "addkills")) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
        } else if (args.length == 2) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(p.getName());
            }
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("unlock") || args[0].equalsIgnoreCase("addkills"))) {
            for (GuideConfig.MonsterDef d : GuideConfig.allMonsters()) {
                if (d.id.toLowerCase().startsWith(args[2].toLowerCase())) out.add(d.id);
            }
        }
        return out;
    }
}