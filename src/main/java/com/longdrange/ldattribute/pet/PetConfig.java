package com.longdrange.ldattribute.pet;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class PetConfig {

    public static class Evolution {
        public String to;
        public int level;
        public List<String> materials = new ArrayList<>();
    }

    public static class Pet {
        public String id, name, type, rarity;
        public boolean baby, glowing;
        public double health, damage, speed;
        public List<String> attributes = new ArrayList<>();
        public int maxLevel, baseExp;
        public double expGrowth;
        public Evolution evolution;
    }

    // ==================== Settings ====================
    private static int defaultSlots = 3;
    private static int maxPages = 3;
    private static int expPerKill = 5;
    private static int expPerFeed = 50;
    private static double levelGrowth = 0.05;
    private static final LinkedHashMap<String, Integer> permissionSlots = new LinkedHashMap<>();

    private static final Map<String, Pet> pets = new LinkedHashMap<>();

    public static void load(LDAttribute plugin) {
        pets.clear();
        permissionSlots.clear();

        File f = com.longdrange.ldattribute.util.ConfigPaths.resolve(plugin, "pet.yml");
        if (!f.exists()) { try { plugin.saveResource("pet.yml", false); } catch (Exception ignored) {} }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

        // Settings
        ConfigurationSection set = cfg.getConfigurationSection("Settings");
        if (set != null) {
            defaultSlots = set.getInt("DefaultSlots", 3);
            maxPages = set.getInt("MaxPages", 3);
            expPerKill = set.getInt("ExpPerKill", 5);
            expPerFeed = set.getInt("ExpPerFeed", 50);
            levelGrowth = set.getDouble("LevelGrowth", 0.05);
            ConfigurationSection ps = set.getConfigurationSection("PermissionSlots");
            if (ps != null) {
                for (String k : ps.getKeys(false)) {
                    permissionSlots.put(k, ps.getInt(k));
                }
            }
        }

        // Pets
        ConfigurationSection sec = cfg.getConfigurationSection("Pets");
        if (sec == null) { plugin.getLogger().info("pet.yml 無 Pets 區塊"); return; }
        for (String key : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) continue;
            Pet p = new Pet();
            p.id = key;
            p.name = org.bukkit.ChatColor.translateAlternateColorCodes((char) 38, s.getString("Name", key));
            p.type = s.getString("Type", "WOLF").toUpperCase();
            p.rarity = s.getString("Rarity", "COMMON").toUpperCase();
            p.baby = s.getBoolean("Baby", false);
            p.glowing = s.getBoolean("Glowing", true);
            p.health = s.getDouble("Health", 100);
            p.damage = s.getDouble("Damage", 10);
            p.speed = s.getDouble("Speed", 0.2);
            List<String> attrs = s.getStringList("Attributes");
            if (attrs != null) p.attributes.addAll(attrs);
            p.maxLevel = s.getInt("MaxLevel", 20);
            p.baseExp = s.getInt("BaseExp", 100);
            p.expGrowth = s.getDouble("ExpGrowth", 1.3);

            ConfigurationSection evo = s.getConfigurationSection("Evolution");
            if (evo != null) {
                Evolution e = new Evolution();
                e.to = evo.getString("To", "");
                e.level = evo.getInt("Level", 20);
                List<String> mats = evo.getStringList("Materials");
                if (mats != null) e.materials.addAll(mats);
                if (e.to != null && !e.to.isEmpty()) p.evolution = e;
            }

            pets.put(key, p);
        }
        plugin.getLogger().info("已載入 " + pets.size() + " 個寵物");
    }

    // ==================== Getter ====================
    public static int getDefaultSlots() { return defaultSlots; }
    public static int getMaxPages() { return maxPages; }
    public static int getExpPerKill() { return expPerKill; }
    public static int getExpPerFeed() { return expPerFeed; }
    public static double getLevelGrowth() { return levelGrowth; }
    public static Map<String, Integer> getPermissionSlots() { return permissionSlots; }

    public static Pet get(String id) { return pets.get(id); }
    public static Collection<Pet> getAll() { return pets.values(); }
    public static Set<String> getIds() { return pets.keySet(); }

    /** 取得某等级的所需经验 */
    public static int getRequiredExp(String petId, int level) {
        Pet p = pets.get(petId);
        if (p == null) return Integer.MAX_VALUE;
        double req = p.baseExp * Math.pow(p.expGrowth, level - 1);
        return (int) Math.round(req);
    }
}