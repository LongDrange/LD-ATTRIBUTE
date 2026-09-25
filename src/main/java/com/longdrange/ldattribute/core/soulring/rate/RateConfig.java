package com.longdrange.ldattribute.core.soulring.rate;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class RateConfig {

    public static class Rule {
        public final String id;
        public final String name;
        public final double value;
        public final String permission;
        public final boolean stack;

        public Rule(String id, String name, double value, String permission, boolean stack) {
            this.id = id; this.name = name; this.value = value;
            this.permission = permission; this.stack = stack;
        }
    }

    private static double defaultRate = 1.0;
    private static boolean ignoreOp = false;

    // ===== 幸运加成 =====
    private static boolean luckEnabled = true;
    private static String luckMode = "ADD";
    private static double luckDivisor = 100.0;
    private static String luckAttributeName = "幸运";
    /** 基准幸运：幸运 ≤ BaseLuck 时不算加成 */
    private static double baseLuck = 0;

    private static final LinkedHashMap<String, Rule> rules = new LinkedHashMap<>();

    public static void load(LDAttribute plugin) {
        rules.clear();
        File f = new File(plugin.getDataFolder(), "rates.yml");
        if (!f.exists()) { try { plugin.saveResource("rates.yml", false); } catch (Throwable ignored) {} }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

        ConfigurationSection st = cfg.getConfigurationSection("Settings");
        if (st != null) {
            defaultRate = st.getDouble("Default", 1.0);
            ignoreOp = st.getBoolean("IgnoreOp", false);

            ConfigurationSection luck = st.getConfigurationSection("Luck");
            if (luck != null) {
                luckEnabled = luck.getBoolean("Enabled", true);
                luckMode = luck.getString("Mode", "ADD").toUpperCase();
                luckDivisor = luck.getDouble("Divisor", 100.0);
                luckAttributeName = luck.getString("AttributeName", "幸运");
                baseLuck = luck.getDouble("BaseLuck", 0);
            }
        }

        ConfigurationSection rs = cfg.getConfigurationSection("Rules");
        if (rs != null) {
            for (String id : rs.getKeys(false)) {
                ConfigurationSection s = rs.getConfigurationSection(id);
                if (s == null) continue;
                String name = ChatColor.translateAlternateColorCodes('&', s.getString("Name", id));
                double v = s.getDouble("Value", 1.0);
                String perm = s.getString("Permission", "");
                boolean stack = s.getBoolean("Stack", false);
                rules.put(id, new Rule(id, name, v, perm, stack));
            }
        }
        plugin.getLogger().info("[Rate] 已加载 " + rules.size() + " 条倍率规则"
                + "（默认 " + defaultRate + ", IgnoreOp=" + ignoreOp
                + ", 幸运=" + (luckEnabled ? luckMode + "/" + luckDivisor + "/Base" + baseLuck : "关闭") + "）");
    }

    public static double getDefaultRate() { return defaultRate; }
    public static boolean isIgnoreOp() { return ignoreOp; }
    public static boolean isLuckEnabled() { return luckEnabled; }
    public static String getLuckMode() { return luckMode; }
    public static double getLuckDivisor() { return luckDivisor; }
    public static String getLuckAttributeName() { return luckAttributeName; }
    public static double getBaseLuck() { return baseLuck; }
    public static Collection<Rule> allRules() { return rules.values(); }
    public static Rule getRule(String id) { return rules.get(id); }
}