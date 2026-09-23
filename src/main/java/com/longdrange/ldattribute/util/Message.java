package com.longdrange.ldattribute.util;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 多語言訊息管理
 * 載入 message/{lang}.yml，支援 {0} {1} 參數替換
 */
public class Message {

    private static YamlConfiguration messages;
    private static String prefix = "";
    private static String currentLang = "zh_TW";

    public static void load(JavaPlugin plugin, String lang) {
        currentLang = lang;

        File msgDir = new File(plugin.getDataFolder(), "message");
        if (!msgDir.exists()) msgDir.mkdirs();

        File langFile = new File(msgDir, lang + ".yml");
        if (!langFile.exists()) {
            try {
                plugin.saveResource("message/" + lang + ".yml", false);
            } catch (Exception e) {
                plugin.getLogger().warning("無法釋放語言檔案: message/" + lang + ".yml");
            }
        }

        if (!langFile.exists()) {
            plugin.getLogger().warning("語言檔案不存在: " + lang + ".yml");
            messages = new YamlConfiguration();
            prefix = "";
            return;
        }

        messages = YamlConfiguration.loadConfiguration(langFile);
        prefix = ChatColor.translateAlternateColorCodes((char)38, messages.getString("Prefix", ""));
        plugin.getLogger().info("已載入語言檔案: " + lang);
    }

    /**
     * 取得訊息（含 {0} {1} 參數替換）
     */
    public static String get(String path, Object... args) {
        if (messages == null) return "";
        String msg = messages.getString(path, "&c缺失訊息：" + path);
        msg = ChatColor.translateAlternateColorCodes((char)38, msg);
        for (int i = 0; i < args.length; i++) {
            msg = msg.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return prefix + msg;
    }

    /**
     * 取得訊息列表
     */
    public static List<String> getList(String path, Object... args) {
        List<String> result = new ArrayList<>();
        if (messages == null) return result;
        List<String> raw = messages.getStringList(path);
        if (raw == null) return result;
        for (String line : raw) {
            line = ChatColor.translateAlternateColorCodes((char)38, line);
            for (int i = 0; i < args.length; i++) {
                line = line.replace("{" + i + "}", String.valueOf(args[i]));
            }
            result.add(line);
        }
        return result;
    }

    /** 同 get，但 key 不存在返回 null（不会显示「缺失訊息」）*/
    public static String getOrNull(String path, Object... args) {
        if (messages == null) return null;
        String msg = messages.getString(path, null);
        if (msg == null) return null;
        msg = ChatColor.translateAlternateColorCodes((char)38, msg);
        for (int i = 0; i < args.length; i++) {
            msg = msg.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return prefix + msg;
    }
    public static String getCurrentLang() {
        return currentLang;
    }
}
