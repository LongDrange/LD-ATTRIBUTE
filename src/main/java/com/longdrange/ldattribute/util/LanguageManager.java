package com.longdrange.ldattribute.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

/**
 * 語言管理器
 * 負責載入 lang/*.yml，讓屬性名支援多語言別名
 *
 * 用法：
 *   LanguageManager.matches(text, "攻擊力")  // 比對所有別名
 */
public class LanguageManager {

    /** key = 屬性內部名稱, value = 該屬性的所有別名（含本身） */
    private static final Map<String, List<String>> aliasMap = new HashMap<>();

    /** 當前語言代碼 */
    private static String currentLang = "zh_TW";

    /**
     * 由插件啟動時呼叫
     */
    public static void load(JavaPlugin plugin) {
        String lang = plugin.getConfig().getString("Language", "zh_TW");
        loadLang(plugin, lang);
    }

    /**
     * 載入指定語言檔案
     */
    public static void loadLang(JavaPlugin plugin, String lang) {
        aliasMap.clear();
        currentLang = lang;

        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) langDir.mkdirs();

        File langFile = new File(langDir, lang + ".yml");
        if (!langFile.exists()) {
            try {
                plugin.saveResource("lang/" + lang + ".yml", false);
            } catch (Exception e) {
                plugin.getLogger().warning("無法釋放語言檔案: " + lang + ".yml");
            }
        }

        if (!langFile.exists()) {
            plugin.getLogger().warning("語言檔案不存在: " + lang + ".yml");
            return;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(langFile);
        for (String key : cfg.getKeys(false)) {
            List<String> aliases = new ArrayList<>();
            aliases.add(key);  // 主要名稱也算一個別名
            List<String> extras = cfg.getStringList(key);
            if (extras != null) aliases.addAll(extras);
            aliasMap.put(key, aliases);
        }

        plugin.getLogger().info("已載入語言檔案: " + lang + " (" + aliasMap.size() + " 個屬性)");
    }

    /**
     * 取得某屬性的所有別名
     */
    public static List<String> getAliases(String primaryName) {
        return aliasMap.getOrDefault(primaryName, Collections.singletonList(primaryName));
    }

    /**
     * 判斷文字是否匹配某屬性（遍歷所有別名）
     */
    public static boolean matches(String text, String primaryName) {
        if (text == null) return false;
        for (String alias : getAliases(primaryName)) {
            if (text.contains(alias)) return true;
        }
        return false;
    }

    /**
     * 判斷文字是否以某屬性的別名為開頭
     * 用於 PAPI 解析（例如「攻擊力_chance」）
     * @return 匹配到的別名長度，沒有匹配回傳 -1
     */
    public static int matchPrefixLength(String text, String primaryName) {
        if (text == null) return -1;
        int best = -1;
        for (String alias : getAliases(primaryName)) {
            if (text.equalsIgnoreCase(alias)) return alias.length();
            if (text.toLowerCase().startsWith(alias.toLowerCase() + "_")) {
                if (alias.length() > best) best = alias.length();
            }
        }
        return best;
    }

    public static String getCurrentLang() { return currentLang; }
}