package com.longdrange.ldattribute.card;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class TempBuffConfig {

    public static class Buff {
        public final String id;
        public final String event;
        public final double chance;
        public final int duration;
        public final int cooldown;
        public final String message;
        public final List<String> effects;
        public final int threshold;
        public final int streak;
        public Buff(String id, String event, double chance, int duration, int cooldown,
                    String message, List<String> effects, int threshold, int streak) {
            this.id = id; this.event = event; this.chance = chance;
            this.duration = duration; this.cooldown = cooldown;
            this.message = message; this.effects = effects;
            this.threshold = threshold; this.streak = streak;
        }
    }

    private static final Map<String, Buff> buffs = new LinkedHashMap<>();

    public static void load(LDAttribute plugin) {
        buffs.clear();
        File f = com.longdrange.ldattribute.util.ConfigPaths.resolve(plugin, "tempbuff.yml");
        if (!f.exists()) { try { plugin.saveResource("tempbuff.yml", false); } catch (Exception ignored) {} }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection sec = cfg.getConfigurationSection("Buffs");
        if (sec == null) { plugin.getLogger().info("tempbuff.yml 無 Buffs 區塊"); return; }
        for (String key : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) continue;
            ConfigurationSection trig = s.getConfigurationSection("Trigger");
            if (trig == null) continue;
            String event = trig.getString("Event", "PLAYER_ATTACK");
            double chance = trig.getDouble("Chance", 1.0);
            int duration = s.getInt("Duration", 5);
            int cooldown = s.getInt("Cooldown", 0);
            String message = org.bukkit.ChatColor.translateAlternateColorCodes((char)38, s.getString("Message", ""));
            List<String> effects = s.getStringList("Effects");
            if (effects == null) effects = new ArrayList<>();
            int threshold = trig.getInt("Threshold", 0);
            int streak = trig.getInt("Streak", 0);
            buffs.put(key, new Buff(key, event, chance, duration, cooldown, message, effects, threshold, streak));
        }
        plugin.getLogger().info("已載入 " + buffs.size() + " 個限時 Buff");
    }

    public static Collection<Buff> getAll() { return buffs.values(); }
    public static Buff get(String id) { return buffs.get(id); }
}
