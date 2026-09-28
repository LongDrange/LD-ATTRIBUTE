package com.longdrange.ldattribute.core.relic.command;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.relic.*;
import com.longdrange.ldattribute.core.relic.gui.RelicGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RelicCommand implements CommandExecutor, TabCompleter {

    private final LDAttribute plugin;
    public RelicCommand(LDAttribute plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String prefix = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "遗物" + ChatColor.DARK_GRAY + "] " + ChatColor.RESET;
        if (args.length == 0) {
            if (!(sender instanceof Player)) { sender.sendMessage(prefix + ChatColor.RED + "只能玩家用"); return true; }
            new RelicGUI(plugin).open((Player) sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "help":
                sender.sendMessage(ChatColor.GOLD + "==== 遗物系统 ====");
                sender.sendMessage(ChatColor.YELLOW + "/ldrelic " + ChatColor.GRAY + "- 打开遗物界面");
                sender.sendMessage(ChatColor.YELLOW + "/ldrelic list " + ChatColor.GRAY + "- 列出所有遗物ID");
                sender.sendMessage(ChatColor.YELLOW + "/ldrelic give <玩家> <ID> [数量] " + ChatColor.GRAY + "- 给遗物");
                sender.sendMessage(ChatColor.YELLOW + "/ldrelic reload " + ChatColor.GRAY + "- 重载配置");
                return true;
            case "reload":
                if (!sender.hasPermission("ldattribute.relic.admin")) { sender.sendMessage(prefix + ChatColor.RED + "权限不足"); return true; }
                plugin.getCoreManager().getRelicManager().reload();
                sender.sendMessage(prefix + ChatColor.GREEN + "遗物配置已重载");
                return true;
            case "list":
                sender.sendMessage(ChatColor.GOLD + "==== 已配置遗物 ====");
                if (RelicConfig.relicIds().isEmpty()) {
                    sender.sendMessage(ChatColor.RED + "无");
                    return true;
                }
                for (String id : RelicConfig.relicIds()) {
                    RelicConfig.RelicDef def = RelicConfig.getRelic(id);
                    sender.sendMessage(ChatColor.GREEN + "  " + id + ChatColor.GRAY + " - " + def.name
                            + ChatColor.DARK_GRAY + " [" + def.slot + "]");
                }
                sender.sendMessage(ChatColor.GRAY + "共 " + RelicConfig.relicIds().size() + " 个");
                return true;
            case "give": {
                if (!sender.hasPermission("ldattribute.relic.admin")) { sender.sendMessage(prefix + ChatColor.RED + "权限不足"); return true; }
                if (args.length < 3) { sender.sendMessage(prefix + ChatColor.RED + "用法: /ldrelic give <玩家> <ID> [数量]"); return true; }
                Player t = plugin.getServer().getPlayerExact(args[1]);
                if (t == null) { sender.sendMessage(prefix + ChatColor.RED + "玩家不在线"); return true; }
                String rid = args[2];
                if (RelicConfig.getRelic(rid) == null) {
                    sender.sendMessage(prefix + ChatColor.RED + "未知遗物ID: " + rid);
                    return true;
                }
                int amt = 1;
                if (args.length >= 4) { try { amt = Math.max(1, Integer.parseInt(args[3])); } catch (Throwable ignored) {} }
                for (int i = 0; i < amt; i++) {
                    RelicData.RelicInstance ins = RelicItem.roll(rid);
                    if (ins == null) continue;
                    ItemStack it = RelicItem.toItem(ins);
                    if (it == null) continue;
                    t.getInventory().addItem(it);
                }
                sender.sendMessage(prefix + ChatColor.GREEN + "已给予 " + t.getName() + " " + rid + " x" + amt);
                return true;
            }
            default:
                sender.sendMessage(prefix + ChatColor.RED + "未知子命令，试试 /ldrelic help");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : Arrays.asList("help", "list", "give", "reload"))
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) {
                for (Player p : plugin.getServer().getOnlinePlayers())
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(p.getName());
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                for (String id : RelicConfig.relicIds())
                    if (id.toLowerCase().startsWith(args[2].toLowerCase())) out.add(id);
            }
        }
        return out;
    }
}