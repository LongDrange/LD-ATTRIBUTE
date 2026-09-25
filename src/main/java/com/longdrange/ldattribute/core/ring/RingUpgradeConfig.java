package com.longdrange.ldattribute.core.ring;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class RingUpgradeConfig {

    public static class Cost {
        public int points;
        public double vault;
        public List<String> items = new ArrayList<>();
        public boolean isEmpty() { return points <= 0 && vault <= 0 && items.isEmpty(); }
    }

    public static class TypeUpgrade {
        public final String type;
        public int maxLevel;
        public double bonusPerLevel;
        public final Map<Integer, Cost> costs = new HashMap<>();
        public TypeUpgrade(String type) { this.type = type; }
    }

    private static int defaultMaxLevel = 10;
    private static double defaultBonusPerLevel = 0.1;
    private static final Map<String, TypeUpgrade> types = new LinkedHashMap<>();
    private static final Cost defaultCost = new Cost();

    public static void load(LDAttribute plugin) {
        types.clear();
        defaultCost.points = 0;
        defaultCost.vault = 0;
        defaultCost.items.clear();

        File f = new File(plugin.getDataFolder(), "ring-upgrade.yml");
        if (!f.exists()) { try { plugin.saveResource("ring-upgrade.yml", false); } catch (Throwable ignored) {} }
        if (!f.exists()) { plugin.getLogger().info("[Ring] 无 ring-upgrade.yml，跳过升级配置"); return; }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection st = cfg.getConfigurationSection("Settings");
        if (st != null) {
            defaultMaxLevel = Math.max(1, st.getInt("DefaultMaxLevel", 10));
            defaultBonusPerLevel = st.getDouble("DefaultBonusPerLevel", 0.1);
        }

        ConfigurationSection dc = cfg.getConfigurationSection("DefaultCost");
        if (dc != null) {
            Cost c = parseCost(dc);
            defaultCost.points = c.points;
            defaultCost.vault = c.vault;
            defaultCost.items.addAll(c.items);
        }

        ConfigurationSection ts = cfg.getConfigurationSection("Types");
        if (ts != null) {
            for (String key : ts.getKeys(false)) {
                ConfigurationSection s = ts.getConfigurationSection(key);
                if (s == null) continue;
                TypeUpgrade tu = new TypeUpgrade(key);
                tu.maxLevel = Math.max(1, s.getInt("MaxLevel", defaultMaxLevel));
                tu.bonusPerLevel = s.getDouble("BonusPerLevel", defaultBonusPerLevel);

                ConfigurationSection cs = s.getConfigurationSection("Costs");
                if (cs != null) {
                    for (String lk : cs.getKeys(false)) {
                        try {
                            int lv = Integer.parseInt(lk);
                            ConfigurationSection csec = cs.getConfigurationSection(lk);
                            if (csec == null) continue;
                            tu.costs.put(lv, parseCost(csec));
                        } catch (Exception ignored) {}
                    }
                }
                types.put(key, tu);
            }
        }
        plugin.getLogger().info("[Ring] 已加载升级配置（" + types.size() + " 类型）");
    }

    private static Cost parseCost(ConfigurationSection s) {
        Cost c = new Cost();
        c.points = s.getInt("CostPoints", 0);
        c.vault = s.getDouble("CostVault", 0);
        List<String> items = s.getStringList("Items");
        if (items != null) c.items.addAll(items);
        return c;
    }

    public static int getMaxLevel(String type) {
        TypeUpgrade tu = types.get(type);
        return tu == null ? defaultMaxLevel : tu.maxLevel;
    }

    public static double getBonusPerLevel(String type) {
        TypeUpgrade tu = types.get(type);
        return tu == null ? defaultBonusPerLevel : tu.bonusPerLevel;
    }

    /** 目标等级的成本；null = 无法升级到该等级 */
    public static Cost getCost(String type, int targetLevel) {
        TypeUpgrade tu = types.get(type);
        if (tu != null) {
            Cost c = tu.costs.get(targetLevel);
            if (c != null) return c;
        }
        // 没配置类型专属成本 → 用默认成本
        if (!defaultCost.isEmpty()) return defaultCost;
        return null;
    }
}