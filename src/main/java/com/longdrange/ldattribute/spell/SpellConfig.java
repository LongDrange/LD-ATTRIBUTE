package com.longdrange.ldattribute.spell;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class SpellConfig {

    public static class Spell {
        public final String id;
        public final String name;
        public final int mana;
        public final int cooldown;
        public final String target;     // SELF / SINGLE / AOE / BEAM / CONE
        public final double range;
        public final double radius;
        public final double base;
        public final String damageFormula;
        public final String healFormula;
        public final List<String> effects;
        public final String mythicSkill;
        public final String nativeEffect;
        public final double levelMultiplier;
        public final int maxLevel;

        public Spell(String id, String name, int mana, int cooldown, String target,
                     double range, double radius, double base,
                     String damageFormula, String healFormula,
                     List<String> effects, String mythicSkill, String nativeEffect,
                     double levelMultiplier, int maxLevel) {
            this.id = id; this.name = name; this.mana = mana; this.cooldown = cooldown;
            this.target = target; this.range = range; this.radius = radius; this.base = base;
            this.damageFormula = damageFormula; this.healFormula = healFormula;
            this.effects = effects; this.mythicSkill = mythicSkill;
            this.nativeEffect = nativeEffect;
            this.levelMultiplier = levelMultiplier;
            this.maxLevel = maxLevel;
        }
    }

    private static final Map<String, Spell> spells = new LinkedHashMap<>();

    public static void load(LDAttribute plugin) {
        spells.clear();
        File f = com.longdrange.ldattribute.util.ConfigPaths.resolve(plugin, "spells.yml");
        if (!f.exists()) { try { plugin.saveResource("spells.yml", false); } catch (Exception ignored) {} }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection sec = cfg.getConfigurationSection("Spells");
        if (sec == null) { plugin.getLogger().info("spells.yml 無 Spells 區塊"); return; }

        for (String key : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) continue;
            Spell sp = new Spell(
                key,
                color(s.getString("Name", key)),
                s.getInt("Mana", 0),
                s.getInt("Cooldown", 0),
                s.getString("Target", "SINGLE").toUpperCase(),
                s.getDouble("Range", 10),
                s.getDouble("Radius", 3),
                s.getDouble("Base", 0),
                s.getString("DamageFormula", ""),
                s.getString("HealFormula", ""),
                s.getStringList("Effects"),
                s.getString("MythicSkill", ""),
                s.getString("NativeEffect", ""),
                s.getDouble("LevelMultiplier", 1.15),
                s.getInt("MaxLevel", 5)
            );
            spells.put(key, sp);
        }
        plugin.getLogger().info("已載入 " + spells.size() + " 個法術");
    }

    private static String color(String s) {
        return org.bukkit.ChatColor.translateAlternateColorCodes((char) 38, s);
    }

    public static Spell get(String id) { return spells.get(id); }
    public static Collection<Spell> getAll() { return spells.values(); }
    public static Set<String> getIds() { return spells.keySet(); }
}