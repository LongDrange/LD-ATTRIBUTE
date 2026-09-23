package com.longdrange.ldattribute.combat;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class ElementConfig {

    private static final Map<String, List<String>> beats = new HashMap<>();
    private static final Map<String, String> displayNames = new HashMap<>();
    private static double strongMultiplier = 1.5;
    private static double weakMultiplier = 0.75;

    public static void load(LDAttribute plugin) {
        beats.clear();
        displayNames.clear();
        File f = new File(plugin.getDataFolder(), "element.yml");
        if (!f.exists()) { try { plugin.saveResource("element.yml", false); } catch (Exception ignored) {} }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

        strongMultiplier = cfg.getDouble("StrongMultiplier", 1.5);
        weakMultiplier = cfg.getDouble("WeakMultiplier", 0.75);

        ConfigurationSection sec = cfg.getConfigurationSection("Elements");
        if (sec == null) { plugin.getLogger().info("element.yml 無 Elements 區塊"); return; }
        for (String key : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) continue;
            List<String> b = s.getStringList("Beats");
            if (b == null) b = new ArrayList<>();
            beats.put(key, b);
            displayNames.put(key, s.getString("Name", key));
        }
        plugin.getLogger().info("已載入 " + beats.size() + " 個元素");
    }

    public static double getMultiplier(String attackerElement, String victimElement) {
        if (attackerElement == null || victimElement == null) return 1.0;
        List<String> b = beats.get(attackerElement);
        if (b != null && b.contains(victimElement)) return strongMultiplier;
        List<String> vb = beats.get(victimElement);
        if (vb != null && vb.contains(attackerElement)) return weakMultiplier;
        return 1.0;
    }

    public static String getDisplayName(String element) {
        return displayNames.getOrDefault(element, element);
    }

    public static Set<String> getAllElements() { return beats.keySet(); }
}