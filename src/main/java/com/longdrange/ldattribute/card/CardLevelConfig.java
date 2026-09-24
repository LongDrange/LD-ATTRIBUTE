package com.longdrange.ldattribute.card;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class CardLevelConfig {

    public static class AttrConf {
        public final String name;
        public final double base;
        public final String mode;   // FIXED / PERCENT / MIXED
        public final double fixed;
        public final double growth;
        public final double max;    // -1 = 無上限
        public AttrConf(String name, double base, String mode, double fixed, double growth, double max) {
            this.name = name; this.base = base; this.mode = mode.toUpperCase();
            this.fixed = fixed; this.growth = growth; this.max = max;
        }
    }

    public static class CardLevel {
        public final String cardId;
        public final int maxLevel;
        public final int baseExp;
        public final double expGrowth;
        public final int cooldownSeconds;
        public final int dailyLimit;
        public final Map<String, AttrConf> attrs = new LinkedHashMap<>();
        public int maxStar = -1;   // -1 = 用全局值
        public int timeOnlineInterval = 0;
        public int timeOnlineExp = 0;
        public int timeOfflineInterval = 0;
        public int timeOfflineExp = 0;
        public int timeOfflineMaxSeconds = 86400;
        public CardLevel(String cardId, int maxLevel, int baseExp, double expGrowth,
                         int cooldownSeconds, int dailyLimit) {
            this.cardId = cardId; this.maxLevel = maxLevel; this.baseExp = baseExp;
            this.expGrowth = expGrowth; this.cooldownSeconds = cooldownSeconds;
            this.dailyLimit = dailyLimit;
        }
    }

    private static final Map<String, CardLevel> map = new HashMap<>();

    public static void load(LDAttribute plugin) {
        map.clear();
        File f = com.longdrange.ldattribute.util.ConfigPaths.resolve(plugin, "cardlevel.yml");
        if (!f.exists()) { try { plugin.saveResource("cardlevel.yml", false); } catch (Exception ignored) {} }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection sec = cfg.getConfigurationSection("Cards");
        if (sec == null) { plugin.getLogger().info("cardlevel.yml 無 Cards 區塊"); return; }

        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) continue;
            CardLevel cl = new CardLevel(id,
                    s.getInt("MaxLevel", -1),
                    s.getInt("BaseExp", 100),
                    s.getDouble("ExpGrowth", 1.5),
                    s.getInt("CooldownSeconds", 0),
                    s.getInt("DailyLimit", -1));
            ConfigurationSection attrs = s.getConfigurationSection("Attributes");
            if (attrs != null) {
                for (String an : attrs.getKeys(false)) {
                    ConfigurationSection a = attrs.getConfigurationSection(an);
                    if (a == null) continue;
                    cl.attrs.put(an, new AttrConf(an,
                            a.getDouble("Base", 0),
                            a.getString("Mode", "FIXED"),
                            a.getDouble("Fixed", 0),
                            a.getDouble("Growth", 0),
                            a.getDouble("Max", -1)));
                }
            }
            org.bukkit.configuration.ConfigurationSection timeSec = s.getConfigurationSection("TimeExp");
            if (timeSec != null) {
                org.bukkit.configuration.ConfigurationSection on = timeSec.getConfigurationSection("Online");
                if (on != null && on.getBoolean("Enabled", false)) {
                    cl.timeOnlineInterval = on.getInt("Interval", 60);
                    cl.timeOnlineExp = on.getInt("Exp", 1);
                }
                org.bukkit.configuration.ConfigurationSection off = timeSec.getConfigurationSection("Offline");
                if (off != null && off.getBoolean("Enabled", false)) {
                    cl.timeOfflineInterval = off.getInt("Interval", 600);
                    cl.timeOfflineExp = off.getInt("Exp", 1);
                    cl.timeOfflineMaxSeconds = off.getInt("MaxSeconds", 86400);
                }
            }            cl.maxStar = s.getInt("MaxStar", -1);
            map.put(id, cl);
        }
        plugin.getLogger().info("已載入 " + map.size() + " 張可升級卡片設定");
    }

    public static CardLevel get(String cardId) { return map.get(cardId); }
    public static boolean isUpgradable(String cardId) { return map.containsKey(cardId); }
}
