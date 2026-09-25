package com.longdrange.ldattribute.core.jewelry.command;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.jewelry.JewelryConfig;
import com.longdrange.ldattribute.core.jewelry.JewelryData;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class JewelryCommand implements CommandExecutor, TabCompleter {

    private final LDAttribute plugin;
    public JewelryCommand(LDAttribute plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String prefix = plugin.getCoreManager().prefix();

        if (args.length == 0) {
            if (!(sender instanceof Player)) { sender.sendMessage(prefix + ChatColor.RED + "只能由玩家执行"); return true; }
            plugin.getJewelryGUI().open((Player) sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        // /ldsp <页id>
        JewelryConfig.PageDef page = JewelryConfig.getPage(sub);
        if (page != null) {
            if (!(sender instanceof Player)) { sender.sendMessage(prefix + ChatColor.RED + "只能由玩家执行"); return true; }
            plugin.getJewelryGUI().open((Player) sender, page.id);
            return true;
        }

        switch (sub) {
            case "help":
                sender.sendMessage(ChatColor.GOLD + "==== 饰品背包 ====");
                sender.sendMessage(ChatColor.YELLOW + "/ldsp " + ChatColor.GRAY + "- 打开饰品背包");
                sender.sendMessage(ChatColor.YELLOW + "/ldsp <页id> " + ChatColor.GRAY + "- 打开指定页");
                sender.sendMessage(ChatColor.YELLOW + "/ldsp list " + ChatColor.GRAY + "- 列出所有页");
                sender.sendMessage(ChatColor.YELLOW + "/ldsp reload " + ChatColor.GRAY + "- 重载配置");
                sender.sendMessage(ChatColor.YELLOW + "/ldsp remove <玩家> <槽位id> " + ChatColor.GRAY + "- 强制移除");
                return true;
            case "list":
                sender.sendMessage(ChatColor.GOLD + "==== 所有饰品页 ====");
                for (JewelryConfig.PageDef p : JewelryConfig.allPages()) {
                    sender.sendMessage(ChatColor.YELLOW + "  " + p.id + ChatColor.GRAY + " - "
                            + ChatColor.stripColor(p.title) + ChatColor.GRAY + " (" + p.slots.size() + " 槽位)");
                }
                return true;
            case "reload":
                if (!sender.hasPermission("ldattribute.jewelry.admin")) { sender.sendMessage(prefix + ChatColor.RED + "权限不足"); return true; }
                plugin.getJewelryManager().reload();
                sender.sendMessage(prefix + ChatColor.GREEN + "饰品配置已重载");
                return true;
            case "remove":
                if (!sender.hasPermission("ldattribute.jewelry.admin")) { sender.sendMessage(prefix + ChatColor.RED + "权限不足"); return true; }
                if (args.length < 3) { sender.sendMessage(prefix + ChatColor.RED + "用法: /ldsp remove <玩家> <槽位id>"); return true; }
                Player t = plugin.getServer().getPlayerExact(args[1]);
                if (t == null) { sender.sendMessage(prefix + ChatColor.RED + "玩家不在线"); return true; }
                JewelryData d = plugin.getJewelryManager().get(t);
                ItemStack removed = d.get(args[2]);
                if (removed == null) { sender.sendMessage(prefix + ChatColor.RED + "该槽位没有饰品"); return true; }
                d.set(args[2], null);
                d.save();
                t.getInventory().addItem(removed);
                sender.sendMessage(prefix + ChatColor.GREEN + "已移除 " + t.getName() + " 的 " + args[2]);
                return true;
            default:
                sender.sendMessage(prefix + ChatColor.RED + "未知参数，试试 /ldsp help");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            List<String> base = new ArrayList<>(Arrays.asList("help", "list", "reload", "remove"));
            for (JewelryConfig.PageDef p : JewelryConfig.allPages()) base.add(p.id);
            for (String s : base) if (s.startsWith(args[0].toLowerCase())) out.add(s);
        } else if (args.length == 2) {
            for (Player p : plugin.getServer().getOnlinePlayers())
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(p.getName());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("remove")) {
            for (JewelryConfig.SlotDef sd : JewelryConfig.allSlots())
                if (sd.id.startsWith(args[2].toLowerCase())) out.add(sd.id);
        }
        return out;
    }
}