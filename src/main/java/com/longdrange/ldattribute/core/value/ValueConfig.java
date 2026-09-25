package com.longdrange.ldattribute.core.value;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class ValueConfig {

    public static class ValueDef {
        public final String id;
        public final String name;
        public final String description;
        public final double defaultValue;
        public final double max;
        public final double min;
        // 自动恢复
        public final double regenAmount;
        public final int regenInterval;    // 秒
        public final double regenMax;

        public ValueDef(String id, String name, String description,
                        double def, double max, double min,
                        double regenAmount, int regenInterval, double regenMax) {
            this.id = id; this.name = name; this.description = description;
            this.defaultValue = def; this.max = max; this.min = min;
            this.regenAmount = regenAmount; this.regenInterval = regenInterval; this.regenMax = regenMax;
        }
        public boolean hasRegen() { return regenAmount > 0 && regenInterval > 0; }
    }

    private static final LinkedHashMap<String, ValueDef> values = new LinkedHashMap<>();

    public static void load(LDAttribute plugin) {
        values.clear();
        File f = new File(plugin.getDataFolder(), "value.yml");
        if (!f.exists()) {
            try { plugin.saveResource("value.yml", false); } catch (Throwable ignored) {}
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

        ConfigurationSection sec = cfg.getConfigurationSection("Values");
        if (sec == null) {
            plugin.getLogger().info("[Value] value.yml 缺少 Values 节点");
            return;
        }
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) continue;
            String name = ChatColor.translateAlternateColorCodes('&', s.getString("Name", id));
            String desc = ChatColor.translateAlternateColorCodes('&', s.getString("Description", ""));
            double def = s.getDouble("Default", 0);
            double max = s.getDouble("Max", 999999999);
            double min = s.getDouble("Min", 0);
            double regenAmount = 0;
            int regenInterval = 0;
            double regenMax = max;
            ConfigurationSection rg = s.getConfigurationSection("AutoRegen");
            if (rg != null) {
                regenAmount = rg.getDouble("Amount", 0);
                regenInterval = rg.getInt("Interval", 0);
                regenMax = rg.getDouble("Max", max);
            }
            values.put(id, new ValueDef(id, name, desc, def, max, min,
                    regenAmount, regenInterval, regenMax));
        }
        plugin.getLogger().info("[Value] 已加载 " + values.size() + " 个自定义值");
    }

    public static ValueDef get(String id) { return values.get(id); }
    public static Collection<ValueDef> all() { return values.values(); }
    public static Set<String> ids() { return values.keySet(); }
    public static boolean exists(String id) { return values.containsKey(id); }
}