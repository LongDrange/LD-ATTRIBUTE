package com.longdrange.ldattribute.command;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

public class CommandConfig {

    private static String mainName = "ldc";
    private static List<String> mainAliases = new ArrayList<>();
    private static final Map<String, SubCmd> subs = new LinkedHashMap<>();

    public static class SubCmd {
        public final String name;
        public final List<String> aliases;
        public final String permission;
        public final String description;
        public SubCmd(String name, List<String> aliases, String permission, String description) {
            this.name = name; this.aliases = aliases;
            this.permission = permission; this.description = description;
        }
        public boolean matches(String s) {
            if (s.equalsIgnoreCase(name)) return true;
            for (String a : aliases) if (s.equalsIgnoreCase(a)) return true;
            return false;
        }
    }

    public static void load(LDAttribute plugin) {
        File f = com.longdrange.ldattribute.util.ConfigPaths.resolve(plugin, "command.yml");
        if (!f.exists()) {
            try { plugin.saveResource("command.yml", false); } catch (Exception ignored) {}
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

        mainName = cfg.getString("Main.name", "ldc");
        mainAliases = cfg.getStringList("Main.aliases");
        if (mainAliases == null) mainAliases = new ArrayList<>();

        subs.clear();
        ConfigurationSection sub = cfg.getConfigurationSection("Sub");
        if (sub != null) {
            for (String key : sub.getKeys(false)) {
                ConfigurationSection s = sub.getConfigurationSection(key);
                if (s == null) continue;
                String name = s.getString("name", key.toLowerCase());
                List<String> aliases = s.getStringList("aliases");
                if (aliases == null) aliases = new ArrayList<>();
                String perm = s.getString("permission", "");
                String desc = org.bukkit.ChatColor.translateAlternateColorCodes((char)38, s.getString("description", ""));
                subs.put(key, new SubCmd(name, aliases, perm, desc));
            }
        }
        plugin.getLogger().info("已載入 " + subs.size() + " 個子指令設定");
    }

    public static String getMainName() { return mainName; }
    public static List<String> getMainAliases() { return mainAliases; }
    public static SubCmd getSub(String key) { return subs.get(key); }
    public static Map<String, SubCmd> getAllSubs() { return subs; }

    public static String matchSubKey(String input) {
        for (Map.Entry<String, SubCmd> e : subs.entrySet())
            if (e.getValue().matches(input)) return e.getKey();
        return null;
    }

    public static void registerDynamic(LDAttribute plugin, CardCommand handler) {
        try {
            Field f = plugin.getServer().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            CommandMap map = (CommandMap) f.get(plugin.getServer());

            if (map.getCommand(mainName) != null) {
                plugin.getLogger().info("主指令 /" + mainName + " 已存在，跳過動態註冊");
                return;
            }

            Command cmd = new Command(mainName) {
                @Override
                public boolean execute(CommandSender sender, String label, String[] args) {
                    return handler.onCommand(sender, this, label, args);
                }
                @Override
                public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
                    return handler.onTabComplete(sender, this, alias, args);
                }
            };
            cmd.setDescription("LD-CardStats 卡片指令");
            if (!mainAliases.isEmpty()) cmd.setAliases(mainAliases);
            map.register("ldattribute", cmd);
            plugin.getLogger().info("已動態註冊 /" + mainName + " (別名: " + mainAliases + ")");
        } catch (Throwable t) {
            plugin.getLogger().warning("動態註冊主指令失敗：" + t.getMessage());
        }
    }
}
