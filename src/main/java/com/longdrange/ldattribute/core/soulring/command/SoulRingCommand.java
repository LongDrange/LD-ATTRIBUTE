package com.longdrange.ldattribute.core.soulring.command;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.soulring.SoulRingConfig;
import com.longdrange.ldattribute.core.soulring.SoulRingData;
import com.longdrange.ldattribute.core.soulring.rate.RateManager;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SoulRingCommand implements CommandExecutor, TabCompleter {

    private final LDAttribute plugin;
    public SoulRingCommand(LDAttribute plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String prefix = plugin.getCoreManager().prefix();

        if (args.length == 0) {
            if (!(sender instanceof Player)) { sender.sendMessage(prefix + ChatColor.RED + "只能由玩家执行"); return true; }
            plugin.getSoulRingGUI().open((Player) sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "exchange":
            case "ex": {
                if (!(sender instanceof Player)) { sender.sendMessage(prefix + ChatColor.RED + "只能由玩家执行"); return true; }
                if (args.length >= 2) {
                    plugin.getExchangeGUI().open((Player) sender, args[1]);
                } else {
                    plugin.getExchangeGUI().openDefault((Player) sender);
                }
                return true;
            }
                        case "search": {
                if (!(sender instanceof Player)) { sender.sendMessage(prefix + ChatColor.RED + "只能由玩家执行"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix + ChatColor.RED + "用法: /ldsr search <关键字>"); return true; }
                plugin.getSoulRingGUI().open((Player) sender, 0, args[1]);
                return true;
            }
            case "autopickup":
            case "ap": {
                if (!(sender instanceof Player)) { sender.sendMessage(prefix + ChatColor.RED + "只能由玩家执行"); return true; }
                if (!SoulRingConfig.isAutoPickupEnabled()) { sender.sendMessage(prefix + ChatColor.RED + "服务器未启用自动拾取"); return true; }
                if (!SoulRingConfig.isAutoPickupPlayerToggle()) { sender.sendMessage(prefix + ChatColor.RED + "服务器不允许玩家切换自动拾取"); return true; }
                Player p = (Player) sender;
                SoulRingData data = plugin.getSoulRingManager().get(p);

                boolean newState;
                if (args.length >= 2) {
                    String on = args[1].toLowerCase();
                    if (on.equals("on") || on.equals("true") || on.equals("1")) newState = true;
                    else if (on.equals("off") || on.equals("false") || on.equals("0")) newState = false;
                    else { sender.sendMessage(prefix + ChatColor.RED + "用法: /ldsr autopickup [on|off]"); return true; }
                    data.setAutoPickupState(newState ? 1 : 0);
                } else {
                    newState = !data.isAutoPickupOn();
                    data.setAutoPickupState(newState ? 1 : 0);
                }
                data.save();
                sender.sendMessage(prefix + ChatColor.GREEN + "自动拾取: "
                        + (newState ? ChatColor.GREEN + "已开启" : ChatColor.RED + "已关闭"));
                return true;
            }
            case "rate":
            case "rates": {
                if (args.length == 1) {
                    // 查看自己倍率
                    if (!(sender instanceof Player)) { sender.sendMessage(prefix + ChatColor.RED + "只能由玩家执行"); return true; }
                    Player p = (Player) sender;
                    double r = RateManager.getRate(p);
                    sender.sendMessage(prefix + ChatColor.GREEN + "你当前倍率: " + ChatColor.YELLOW + "x" + r);
                    List<String> sources = RateManager.getActiveSources(p);
                    if (sources.isEmpty()) {
                        sender.sendMessage(ChatColor.GRAY + "  (无生效来源，使用默认倍率)");
                    } else {
                        sender.sendMessage(ChatColor.GRAY + "  生效来源:");
                        for (String s : sources) sender.sendMessage(ChatColor.GRAY + "    " + s);
                    }
                    return true;
                }
                String op = args[1].toLowerCase();
                if (op.equals("add") || op.equals("give")) {
                    if (!sender.hasPermission("ldattribute.soulring.admin")) { sender.sendMessage(prefix + ChatColor.RED + "权限不足"); return true; }
                    if (args.length < 5) {
                        sender.sendMessage(prefix + ChatColor.RED + "用法: /ldsr rate add <玩家> <倍率> <时间>");
                        sender.sendMessage(prefix + ChatColor.RED + "  时间格式: 1d 2h 30m 60s（可组合）");
                        return true;
                    }
                    Player t = plugin.getServer().getPlayerExact(args[2]);
                    if (t == null) { sender.sendMessage(prefix + ChatColor.RED + "玩家不在线"); return true; }
                    double val;
                    try { val = Double.parseDouble(args[3]); } catch (Exception e) { sender.sendMessage(prefix + ChatColor.RED + "倍率必须是数字"); return true; }
                    long sec = RateManager.parseTime(args[4]);
                    if (sec < 0) { sender.sendMessage(prefix + ChatColor.RED + "时间格式错，例: 1d 2h 30m 60s"); return true; }

                    String rateId = "temp_" + System.currentTimeMillis();
                    plugin.getRateManager().get(t).addTempRateTime(rateId, val, sec);
                    plugin.getRateManager().get(t).save();

                    sender.sendMessage(prefix + ChatColor.GREEN + "已给 " + t.getName()
                            + " 添加限时倍率 x" + val + " 时长 " + RateManager.formatTime(sec));
                    t.sendMessage(prefix + ChatColor.YELLOW + "你获得了限时倍率 x" + val
                            + " 时长 " + RateManager.formatTime(sec));
                    return true;
                }
                if (op.equals("clear")) {
                    if (!sender.hasPermission("ldattribute.soulring.admin")) { sender.sendMessage(prefix + ChatColor.RED + "权限不足"); return true; }
                    if (args.length < 3) { sender.sendMessage(prefix + ChatColor.RED + "用法: /ldsr rate clear <玩家>"); return true; }
                    Player t = plugin.getServer().getPlayerExact(args[2]);
                    if (t == null) { sender.sendMessage(prefix + ChatColor.RED + "玩家不在线"); return true; }
                    // 清所有限时倍率
                    com.longdrange.ldattribute.core.soulring.rate.RateData data =
                            plugin.getRateManager().get(t);
                    for (String id : new ArrayList<String>()) { }
                    // 遍历清空
                    java.util.List<com.longdrange.ldattribute.core.soulring.rate.RateData.TempRate> list =
                            data.getActiveTempRates();
                    for (com.longdrange.ldattribute.core.soulring.rate.RateData.TempRate tr : list) {
                        data.removeTempRate(tr.id);
                    }
                    data.save();
                    sender.sendMessage(prefix + ChatColor.GREEN + "已清空 " + t.getName() + " 的所有限时倍率");
                    return true;
                }
                sender.sendMessage(prefix + ChatColor.RED + "用法: /ldsr rate [add|clear] ...");
                return true;
            }
            case "reload": {
                if (!sender.hasPermission("ldattribute.soulring.admin")) { sender.sendMessage(prefix + ChatColor.RED + "权限不足"); return true; }
                plugin.getSoulRingManager().reload();
                plugin.getRateManager().reload();
                sender.sendMessage(prefix + ChatColor.GREEN + "灵魂空间 & 倍率配置已重载");
                return true;
            }
            case "help":
            default: {
                sender.sendMessage(ChatColor.GOLD + "==== 灵魂空间 ====");
                sender.sendMessage(ChatColor.YELLOW + "/ldsr " + ChatColor.GRAY + "- 打开灵魂空间");
                sender.sendMessage(ChatColor.YELLOW + "/ldsr search <关键字> " + ChatColor.GRAY + "- 搜索");
                sender.sendMessage(ChatColor.YELLOW + "/ldsr exchange " + ChatColor.GRAY + "- 打开灵魂兑换");
                sender.sendMessage(ChatColor.YELLOW + "/ldsr autopickup [on|off] " + ChatColor.GRAY + "- 自动拾取");
                sender.sendMessage(ChatColor.YELLOW + "/ldsr rate " + ChatColor.GRAY + "- 查看倍率");
                sender.sendMessage(ChatColor.YELLOW + "/ldsr rate add <玩家> <倍率> <时间> " + ChatColor.GRAY + "- 给限时倍率");
                sender.sendMessage(ChatColor.YELLOW + "/ldsr rate clear <玩家> " + ChatColor.GRAY + "- 清空限时倍率");
                sender.sendMessage(ChatColor.YELLOW + "/ldsr reload " + ChatColor.GRAY + "- 重载配置");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : Arrays.asList("search", "exchange", "autopickup", "rate", "reload", "help")) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("autopickup")) {
            for (String s : Arrays.asList("on", "off"))
                if (s.startsWith(args[1].toLowerCase())) out.add(s);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("rate")) {
            for (String s : Arrays.asList("add", "clear"))
                if (s.startsWith(args[1].toLowerCase())) out.add(s);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("rate")) {
            for (Player p : plugin.getServer().getOnlinePlayers())
                if (p.getName().toLowerCase().startsWith(args[2].toLowerCase())) out.add(p.getName());
        } else if (args.length == 5 && args[0].equalsIgnoreCase("rate") && args[1].equalsIgnoreCase("add")) {
            for (String s : Arrays.asList("1h", "1d", "7d", "30m"))
                if (s.startsWith(args[4].toLowerCase())) out.add(s);
        }
        return out;
    }
}