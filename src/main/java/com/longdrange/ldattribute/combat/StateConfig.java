package com.longdrange.ldattribute.combat;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class StateConfig {

    public static class State {
        public final String id;
        public final String name;
        public final String conditionType;
        public final double conditionValue;
        public final List<String> attributes;
        public final String message;
        public State(String id, String name, String type, double value, List<String> attrs, String msg) {
            this.id = id; this.name = name;
            this.conditionType = type; this.conditionValue = value;
            this.attributes = attrs; this.message = msg;
        }
    }

    private static final List<State> states = new ArrayList<>();

    public static void load(LDAttribute plugin) {
        states.clear();
        File f = com.longdrange.ldattribute.util.ConfigPaths.resolve(plugin, "state.yml");
        if (!f.exists()) { try { plugin.saveResource("state.yml", false); } catch (Exception ignored) {} }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection sec = cfg.getConfigurationSection("States");
        if (sec == null) { plugin.getLogger().info("state.yml 無 States 區塊"); return; }
        for (String key : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) continue;
            String name = org.bukkit.ChatColor.translateAlternateColorCodes((char) 38, s.getString("Name", key));
            String cond = s.getString("Condition", "");
            List<String> attrs = s.getStringList("Attributes");
            if (attrs == null) attrs = new ArrayList<>();
            String msg = org.bukkit.ChatColor.translateAlternateColorCodes((char)38, s.getString("Message", ""));
            String type = ""; double value = 0;
            if (cond.contains(":")) {
                type = cond.split(":")[0].toUpperCase();
                try { value = Double.parseDouble(cond.split(":")[1]); } catch (Exception ignored) {}
            }
            states.add(new State(key, name, type, value, attrs, msg));
        }
        plugin.getLogger().info("已載入 " + states.size() + " 個戰鬥狀態");
    }

    public static List<State> getAll() { return states; }
}