package com.longdrange.ldattribute.achievement;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class AchievementConfig {

    public enum Type {
        CARD_COUNT, LEVEL_UP, STAR_UP, MERGE, KILL,
        RUNE_EQUIP, RUNE_COLLECT, PET_COLLECT, POINTS
    }

    public static class Achievement {
        public String id;
        public String name;
        public String description;
        public Type type;
        public int target;
        public Material icon;
        public String category;
        public int rewardPoints;
        public List<String> rewardCards = new ArrayList<>();
        public List<String> rewardRunes = new ArrayList<>();
        public List<String> rewardCommands = new ArrayList<>();
    }

    private static final Map<String, Achievement> all = new LinkedHashMap<>();

    public static void load(LDAttribute plugin) {
        all.clear();
        File f = com.longdrange.ldattribute.util.ConfigPaths.resolve(plugin, "achievement.yml");
        if (!f.exists()) { try { plugin.saveResource("achievement.yml", false); } catch (Exception ignored) {} }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection sec = cfg.getConfigurationSection("Achievements");
        if (sec == null) { plugin.getLogger().info("achievement.yml 无 Achievements 区块"); return; }

        for (String key : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) continue;
            Achievement a = new Achievement();
            a.id = key;
            a.name = org.bukkit.ChatColor.translateAlternateColorCodes((char) 38, s.getString("Name", key));
            a.description = org.bukkit.ChatColor.translateAlternateColorCodes((char)38, s.getString("Description", ""));
            try { a.type = Type.valueOf(s.getString("Type", "CARD_COUNT").toUpperCase()); }
            catch (Exception e) { a.type = Type.CARD_COUNT; }
            a.target = s.getInt("Target", 1);
            String iconName = s.getString("Icon", "PAPER");
            Material m = Material.getMaterial(iconName.toUpperCase());
            a.icon = m != null ? m : Material.PAPER;
            a.category = s.getString("Category", "MISC");
            ConfigurationSection rw = s.getConfigurationSection("Reward");
            if (rw != null) {
                a.rewardPoints = rw.getInt("Points", 0);
                List<String> cards = rw.getStringList("Cards");
                if (cards != null) a.rewardCards.addAll(cards);
                List<String> runes = rw.getStringList("Runes");
                if (runes != null) a.rewardRunes.addAll(runes);
                List<String> cmds = rw.getStringList("Commands");
                if (cmds != null) a.rewardCommands.addAll(cmds);
            }
            all.put(key, a);
        }
        plugin.getLogger().info("已載入 " + all.size() + " 個成就");
    }

    public static Achievement get(String id) { return all.get(id); }
    public static Collection<Achievement> getAll() { return all.values(); }
}