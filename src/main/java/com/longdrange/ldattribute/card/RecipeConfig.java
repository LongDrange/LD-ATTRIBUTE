package com.longdrange.ldattribute.card;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class RecipeConfig {

    public static class Recipe {
        public final String id;
        public final String display;
        public final String icon;
        public final Map<String, Integer> inputCards = new LinkedHashMap<>();
        public int costPoints = 0;
        public double costVault = 0;
        public List<String> costItems = new ArrayList<>();
        public String permission = "";
        public final Map<String, Integer> outputCards = new LinkedHashMap<>();
        public final Map<String, Integer> randomPool = new LinkedHashMap<>();
        public boolean keepLevel = false;
        public boolean requireMaxLevel = false;
        public boolean keepStar = false;
        public boolean requireMaxStar = false;
        public String broadcast = "";

        public Recipe(String id, String display, String icon) {
            this.id = id; this.display = display; this.icon = icon;
        }
    }

    private static final Map<String, Recipe> recipes = new LinkedHashMap<>();

    public static void load(LDAttribute plugin) {
        recipes.clear();
        File f = new File(plugin.getDataFolder(), "recipe.yml");
        if (!f.exists()) { try { plugin.saveResource("recipe.yml", false); } catch (Exception ignored) {} }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection sec = cfg.getConfigurationSection("Recipes");
        if (sec == null) { plugin.getLogger().info("recipe.yml 無 Recipes 區塊"); return; }

        for (String key : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) continue;
            Recipe r = new Recipe(key, s.getString("Display", key), s.getString("Icon", ""));
            ConfigurationSection input = s.getConfigurationSection("Input");
            if (input != null) for (String c : input.getStringList("Cards")) parse(c, r.inputCards);
            ConfigurationSection cost = s.getConfigurationSection("Cost");
            if (cost != null) {
                r.costPoints = cost.getInt("Points", 0);
                r.costVault = cost.getDouble("Vault", 0);
                r.costItems = cost.getStringList("Items");
                if (r.costItems == null) r.costItems = new ArrayList<>();
                r.permission = cost.getString("Permission", "");
            }
            ConfigurationSection output = s.getConfigurationSection("Output");
            if (output != null) {
                for (String c : output.getStringList("Cards")) parse(c, r.outputCards);
                for (String c : output.getStringList("Random")) parse(c, r.randomPool);
            }
            r.keepLevel = s.getBoolean("KeepLevel", false);
            r.keepStar = s.getBoolean("KeepStar", false);
            r.broadcast = s.getString("Broadcast", "");
            r.requireMaxLevel = s.getBoolean("RequireMaxLevel", false);
            r.requireMaxStar = s.getBoolean("RequireMaxStar", false);
            recipes.put(key, r);
        }
        plugin.getLogger().info("已載入 " + recipes.size() + " 個合成配方");
    }

    private static void parse(String entry, Map<String, Integer> map) {
        if (entry == null || entry.isEmpty()) return;
        String[] parts = entry.split(":");
        int n = 1;
        if (parts.length >= 2) { try { n = Integer.parseInt(parts[1]); } catch (Exception ignored) {} }
        map.put(parts[0], n);
    }

    public static Recipe get(String id) { return recipes.get(id); }
    public static Collection<Recipe> getAll() { return recipes.values(); }
}