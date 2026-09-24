package com.longdrange.ldattribute.combat;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class ElementConfig {

    private static final Map<String, List<String>> beats = new HashMap<>();
    private static final Map<String, String> displayNames = new HashMap<>();
    public static class Reaction {
        public String name;
        public String element1, element2;
        public double damageMultiplier = 1.0;
        public String effect = "";
        public String message = "";
    }

    private static final java.util.List<Reaction> reactions = new java.util.ArrayList<>();
    private static double strongMultiplier = 1.5;
    private static double weakMultiplier = 0.75;

    public static void load(LDAttribute plugin) {
        beats.clear();
        displayNames.clear();
        File f = com.longdrange.ldattribute.util.ConfigPaths.resolve(plugin, "element.yml");
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
            displayNames.put(key, org.bukkit.ChatColor.translateAlternateColorCodes((char) 38, s.getString("Name", key)));
        }
        reactions.clear();
        ConfigurationSection rsec = cfg.getConfigurationSection("Reactions");
        if (rsec != null) {
            for (String key : rsec.getKeys(false)) {
                ConfigurationSection s = rsec.getConfigurationSection(key);
                if (s == null) continue;
                Reaction r = new Reaction();
                r.name = org.bukkit.ChatColor.translateAlternateColorCodes((char) 38, s.getString("Name", key));
                java.util.List<String> els = s.getStringList("Elements");
                if (els == null || els.size() < 2) continue;
                r.element1 = els.get(0).toUpperCase();
                r.element2 = els.get(1).toUpperCase();
                r.damageMultiplier = s.getDouble("DamageMultiplier", 1.0);
                r.effect = s.getString("Effect", "").toUpperCase();
                r.message = org.bukkit.ChatColor.translateAlternateColorCodes((char)38, s.getString("Message", ""));
                reactions.add(r);
            }
        }
        plugin.getLogger().info("已載入 " + beats.size() + " 個元素, " + reactions.size() + " 個反應");
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

    /** 匹配元素反应（顺序敏感） */
    public static Reaction getReaction(String atkElement, String defElement) {
        if (atkElement == null || defElement == null) return null;
        String a = atkElement.toUpperCase();
        String d = defElement.toUpperCase();
        for (Reaction r : reactions) {
            if (r.element1.equals(a) && r.element2.equals(d)) return r;
        }
        return null;
    }
}