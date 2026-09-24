package com.longdrange.ldattribute.gacha;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class GachaConfig {

    public static class PoolEntry {
        public String kind;         // "card" / "rune"
        public String id;
        public int weight;
        public String raw;          // 原始字符串
    }

    public static class Gacha {
        public String id;
        public String name;
        public String description;
        public Material icon;
        public int costPoints;
        public double costVault;
        public List<String> costItems = new ArrayList<>();
        public List<PoolEntry> pool = new ArrayList<>();
        public int totalWeight = 0;
        public boolean pityEnabled;
        public int pityCount;
        public String pityGuarantee;
    }

    private static final Map<String, Gacha> all = new LinkedHashMap<>();

    public static void load(LDAttribute plugin) {
        all.clear();
        File f = com.longdrange.ldattribute.util.ConfigPaths.resolve(plugin, "gacha.yml");
        if (!f.exists()) { try { plugin.saveResource("gacha.yml", false); } catch (Exception ignored) {} }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection sec = cfg.getConfigurationSection("Gachas");
        if (sec == null) { plugin.getLogger().info("gacha.yml 无 Gachas 区块"); return; }

        for (String key : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) continue;
            Gacha g = new Gacha();
            g.id = key;
            g.name = org.bukkit.ChatColor.translateAlternateColorCodes((char) 38, s.getString("Name", key));
            g.description = org.bukkit.ChatColor.translateAlternateColorCodes((char)38, s.getString("Description", ""));
            String iconName = s.getString("Icon", "CHEST");
            Material m = Material.getMaterial(iconName.toUpperCase());
            g.icon = m != null ? m : Material.CHEST;

            ConfigurationSection c = s.getConfigurationSection("Cost");
            if (c != null) {
                g.costPoints = c.getInt("Points", 0);
                g.costVault = c.getDouble("Vault", 0);
                List<String> items = c.getStringList("Items");
                if (items != null) g.costItems.addAll(items);
            }

            List<String> pool = s.getStringList("Pools");
            if (pool != null) {
                for (String raw : pool) {
                    PoolEntry pe = parsePool(raw);
                    if (pe != null) {
                        g.pool.add(pe);
                        g.totalWeight += pe.weight;
                    }
                }
            }

            ConfigurationSection p = s.getConfigurationSection("Pity");
            if (p != null) {
                g.pityEnabled = p.getBoolean("Enabled", false);
                g.pityCount = p.getInt("Count", 50);
                g.pityGuarantee = p.getString("Guarantee", "");
            }
            all.put(key, g);
        }
        plugin.getLogger().info("已載入 " + all.size() + " 個抽獎卡池");
    }

    /** 解析 "卡片ID:权重" 或 "rune:符文ID:权重" */
    private static PoolEntry parsePool(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String[] parts = raw.split(":");
        if (parts.length < 2) return null;
        PoolEntry pe = new PoolEntry();
        pe.raw = raw;
        if (parts[0].equalsIgnoreCase("rune") && parts.length >= 3) {
            pe.kind = "rune";
            pe.id = parts[1];
            try { pe.weight = Integer.parseInt(parts[2]); } catch (Exception e) { return null; }
        } else {
            pe.kind = "card";
            pe.id = parts[0];
            try { pe.weight = Integer.parseInt(parts[1]); } catch (Exception e) { return null; }
        }
        return pe;
    }

    public static Gacha get(String id) { return all.get(id); }
    public static Collection<Gacha> getAll() { return all.values(); }
    public static Gacha getFirst() { return all.isEmpty() ? null : all.values().iterator().next(); }
}