package com.longdrange.ldattribute.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 配置工具類
 * 負責載入與讀取 config.yml
 */
public class Config {

    private static FileConfiguration config;
    private final JavaPlugin plugin;

    public Config(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 載入配置（若檔案不存在會自動從 jar 內部釋放預設檔）
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    /**
     * 重新載入配置
     */
    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    public static FileConfiguration getConfig() {
        return config;
    }

    public static int getInt(String path, int def) {
        return config.getInt(path, def);
    }

    public static String getString(String path, String def) {
        return config.getString(path, def);
    }

    public static boolean getBoolean(String path, boolean def) {
        return config.getBoolean(path, def);
    }

    public static double getDouble(String path, double def) {
        return config.getDouble(path, def);
    }
}