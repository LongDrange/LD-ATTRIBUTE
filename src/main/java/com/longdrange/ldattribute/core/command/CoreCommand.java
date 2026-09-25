package com.longdrange.ldattribute.core.command;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.CoreManager;
import com.longdrange.ldattribute.core.data.ModuleDataManager;
import com.longdrange.ldattribute.core.storage.StorageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class CoreCommand implements CommandExecutor, TabCompleter {

    private final LDAttribute plugin;
    public CoreCommand(LDAttribute plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        CoreManager core = plugin.getCoreManager();
        String prefix = core.prefix();

        if (args.length == 0) {
            sender.sendMessage(prefix + "\u00A77LD-Core \u00A7bv1.0");
            sender.sendMessage(prefix + "\u00A77/ldcore reload \u00A7f- 重载 core.yml 和存储");
            sender.sendMessage(prefix + "\u00A77/ldcore storage \u00A7f- 查看当前存储方式");
            sender.sendMessage(prefix + "\u00A77/ldcore save [玩家] \u00A7f- 立即保存");
            sender.sendMessage(prefix + "\u00A77/ldcore modules <玩家> \u00A7f- 列出玩家已加载模块");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload": {
                if (!sender.hasPermission("ldattribute.core.admin")) {
                    sender.sendMessage(prefix + core.msg("no-permission"));
                    return true;
                }
                try {
                    core.reload();
                    sender.sendMessage(prefix + core.msg("reload"));
                } catch (Exception e) {
                    sender.sendMessage(prefix + "\u00A7c重载失败: " + e.getMessage());
                    e.printStackTrace();
                }
                return true;
            }
            case "storage": {
                StorageManager sm = core.getStorageManager();
                sender.sendMessage(prefix + core.msg("storage-current",
                        "%type%", sm == null ? "N/A" : sm.getType().name()));
                return true;
            }
            case "save": {
                if (!sender.hasPermission("ldattribute.core.admin")) {
                    sender.sendMessage(prefix + core.msg("no-permission"));
                    return true;
                }
                ModuleDataManager dm = core.getModuleDataManager();
                if (args.length >= 2) {
                    Player target = plugin.getServer().getPlayerExact(args[1]);
                    if (target == null) {
                        sender.sendMessage(prefix + "\u00A7c玩家不在线: " + args[1]);
                        return true;
                    }
                    dm.savePlayer(target.getUniqueId());
                    sender.sendMessage(prefix + "\u00A7a已保存 " + target.getName() + " 的数据");
                } else {
                    dm.saveAll();
                    sender.sendMessage(prefix + "\u00A7a已保存所有缓存数据");
                }
                return true;
            }
            case "modules": {
                if (!sender.hasPermission("ldattribute.core.admin")) {
                    sender.sendMessage(prefix + core.msg("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(prefix + "\u00A7c用法: /ldcore modules <玩家>");
                    return true;
                }
                Player target = plugin.getServer().getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(prefix + "\u00A7c玩家不在线: " + args[1]);
                    return true;
                }
                Set<String> mods = core.getModuleDataManager().listModules(target.getUniqueId());
                sender.sendMessage(prefix + "\u00A77玩家 \u00A7b" + target.getName()
                        + " \u00A77已加载模块: \u00A7f" + (mods.isEmpty() ? "(无)" : mods));
                return true;
            }
            default:
                sender.sendMessage(prefix + "\u00A7c未知子命令");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                       String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : Arrays.asList("reload", "storage", "save", "modules")) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
        } else if (args.length == 2
                && (args[0].equalsIgnoreCase("save") || args[0].equalsIgnoreCase("modules"))) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    out.add(p.getName());
                }
            }
        }
        return out;
    }
}