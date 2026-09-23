package com.longdrange.ldattribute.card;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class CollectionConfig {
    public static class Reward {
        public final String id;
        public final List<String> cards;
        public final List<String> commands;
        public Reward(String id, List<String> cards, List<String> commands) {
            this.id = id; this.cards = cards; this.commands = commands;
        }
    }

    private static final List<Reward> rewards = new ArrayList<>();

    public static void load(LDAttribute plugin) {
        rewards.clear();
        File f = new File(plugin.getDataFolder(), "collection.yml");
        if (!f.exists()) {
            try { plugin.saveResource("collection.yml", false); } catch (Exception ignored) {}
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection sec = cfg.getConfigurationSection("Rewards");
        if (sec == null) { plugin.getLogger().info("collection.yml 無 Rewards 區塊"); return; }
        for (String key : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) continue;
            List<String> cards = s.getStringList("Cards");
            List<String> commands = s.getStringList("Commands");
            if (cards == null) cards = new ArrayList<>();
            if (commands == null) commands = new ArrayList<>();
            rewards.add(new Reward(key, cards, commands));
        }
        plugin.getLogger().info("已載入 " + rewards.size() + " 個集齊獎勵");
    }

    public static List<Reward> getAll() { return rewards; }
}
