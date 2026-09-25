package com.longdrange.ldattribute.core.ring.command;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.ring.RingData;
import com.longdrange.ldattribute.core.ring.RingStatsProvider;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class RingCommand implements CommandExecutor, TabCompleter {

    private final LDAttribute plugin;
    public RingCommand(LDAttribute plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String prefix = plugin.getCoreManager().prefix();
        if (args.length == 0) {
            if (!(sender instanceof Player)) { sender.sendMessage(prefix + ChatColor.RED + "只能由玩家执行"); return true; }
            plugin.getRingGUI().open((Player) sender, 0);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "help":
                sender.sendMessage(ChatColor.GOLD + "==== 魂珠空间 ====");
                sender.sendMessage(ChatColor.YELLOW + "/ldring " + ChatColor.GRAY + "- 打开");
                sender.sendMessage(ChatColor.YELLOW + "/ldring reload " + ChatColor.GRAY + "- 重载");
                sender.sendMessage(ChatColor.YELLOW + "/ldring unlock <玩家> <槽位> " + ChatColor.GRAY + "- 解锁");
                sender.sendMessage(ChatColor.YELLOW + "/ldring take <玩家> <类型> [数量] " + ChatColor.GRAY + "- 取出");
                sender.sendMessage(ChatColor.YELLOW + "/ldring attrs " + ChatColor.GRAY + "- 列出属性名");
                return true;
            case "reload":
                if (!sender.hasPermission("ldattribute.ring.admin")) { sender.sendMessage(prefix + ChatColor.RED + "权限不足"); return true; }
                plugin.getRingManager().reload();
                sender.sendMessage(prefix + ChatColor.GREEN + "魂珠配置已重载");
                return true;
            case "unlock": {
                if (!sender.hasPermission("ldattribute.ring.admin")) { sender.sendMessage(prefix + ChatColor.RED + "权限不足"); return true; }
                if (args.length < 3) { sender.sendMessage(prefix + ChatColor.RED + "用法: /ldring unlock <玩家> <槽位id>"); return true; }
                Player t = plugin.getServer().getPlayerExact(args[1]);
                if (t == null) { sender.sendMessage(prefix + ChatColor.RED + "玩家不在线"); return true; }
                int slot;
                try { slot = Integer.parseInt(args[2]); } catch (Exception ex) { sender.sendMessage(prefix + ChatColor.RED + "槽位id必须是数字"); return true; }
                RingData d = plugin.getRingManager().get(t);
                d.unlockSlot(slot); d.save();
                sender.sendMessage(prefix + ChatColor.GREEN + "已解锁 " + t.getName() + " 的槽位 #" + slot);
                return true;
            }
            case "take": {
                if (!sender.hasPermission("ldattribute.ring.admin")) { sender.sendMessage(prefix + ChatColor.RED + "权限不足"); return true; }
                if (args.length < 3) { sender.sendMessage(prefix + ChatColor.RED + "用法: /ldring take <玩家> <类型> [数量]"); return true; }
                Player t = plugin.getServer().getPlayerExact(args[1]);
                if (t == null) { sender.sendMessage(prefix + ChatColor.RED + "玩家不在线"); return true; }
                String type = args[2];
                int amount = Integer.MAX_VALUE;
                if (args.length >= 4) { try { amount = Math.max(1, Integer.parseInt(args[3])); } catch (Exception ignored) {} }
                RingData d = plugin.getRingManager().get(t);
                int sid = d.findSlotOfType(type);
                if (sid < 0) { sender.sendMessage(prefix + ChatColor.RED + "没有类型为 '" + type + "' 的魂珠"); return true; }
                RingData.Slot s = d.getSlot(sid);
                if (s == null) { sender.sendMessage(prefix + ChatColor.RED + "数据异常"); return true; }
                int take = Math.min(amount, s.count);
                ItemStack give = d.takeFromSlot(sid, take);
                if (give == null) { sender.sendMessage(prefix + ChatColor.RED + "取出失败"); return true; }
                t.getInventory().addItem(give);
                d.save();
                sender.sendMessage(prefix + ChatColor.GREEN + "已取出 " + take + " 个给 " + t.getName());
                return true;
            }
            case "attrs": {
                sender.sendMessage(ChatColor.GOLD + "==== 已注册属性 ====");
                List<String> names = RingStatsProvider.listAllRegistered();
                if (names.isEmpty()) { sender.sendMessage(ChatColor.RED + "读取失败"); return true; }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < names.size(); i++) {
                    if (i % 4 == 0) { if (i > 0) sender.sendMessage(sb.toString()); sb = new StringBuilder("  "); }
                    sb.append(ChatColor.GREEN).append(names.get(i)).append("    ");
                }
                if (sb.length() > 0) sender.sendMessage(sb.toString());
                sender.sendMessage(ChatColor.GRAY + "共 " + names.size() + " 个");
                return true;
            }
            default:
                sender.sendMessage(prefix + ChatColor.RED + "未知子命令，试试 /ldring help");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : Arrays.asList("help", "reload", "unlock", "take", "attrs"))
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
        } else if (args.length == 2) {
            for (Player p : plugin.getServer().getOnlinePlayers())
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(p.getName());
        }
        return out;
    }
}