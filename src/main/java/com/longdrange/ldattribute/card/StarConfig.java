package com.longdrange.ldattribute.card;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class StarConfig {

    public static class Cost {
        public final int star;
        public final int points;
        public final double vault;
        public final List<String> items;
        public final double successRate;
        public Cost(int star, int points, double vault, List<String> items, double successRate) {
            this.star = star; this.points = points; this.vault = vault; this.items = items;
            this.successRate = successRate;
        }
    }

    private static double multiplier = 0.10;
    private static int maxStar = 5;
    private static final Map<Integer, Cost> costs = new HashMap<>();

    public static void load(LDAttribute plugin) {
        costs.clear();
        File f = new File(plugin.getDataFolder(), "star.yml");
        if (!f.exists()) { try { plugin.saveResource("star.yml", false); } catch (Exception ignored) {} }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        multiplier = cfg.getDouble("Multiplier", 0.10);
        maxStar = cfg.getInt("MaxStar", 5);

        ConfigurationSection sec = cfg.getConfigurationSection("Costs");
        if (sec != null) {
            for (String k : sec.getKeys(false)) {
                int star;
                try { star = Integer.parseInt(k); } catch (Exception e) { continue; }
                ConfigurationSection s = sec.getConfigurationSection(k);
                if (s == null) continue;
                int points = s.getInt("Points", 0);
                double vault = s.getDouble("Vault", 0);
                List<String> items = s.getStringList("Items");
                if (items == null) items = new ArrayList<>();
                double successRate = s.getDouble("SuccessRate", 1.0);
                costs.put(star, new Cost(star, points, vault, items, successRate));
            }
        }
        plugin.getLogger().info("已載入 " + costs.size() + " 個升星配置 (最大 " + maxStar + " 星)");
    }

    public static double getMultiplier() { return multiplier; }
    public static int getMaxStar() { return maxStar; }
    public static Cost getCost(int star) { return costs.get(star); }

    /** 取得指定卡的最高星級：先看卡的 MaxStar，其次全局 */
    public static int getMaxStarFor(String cardId) {
        CardLevelConfig.CardLevel cl = CardLevelConfig.get(cardId);
        if (cl != null && cl.maxStar > 0) return cl.maxStar;
        return maxStar;
    }
}
