package com.longdrange.ldattribute.card;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class DecomposeConfig {

    public static class Entry {
        public int points = 0;
        public double vault = 0;
        public List<String> items = new ArrayList<>();
        public Map<String, Integer> random = new LinkedHashMap<>();
        public boolean disabled = false;
    }

    private static Entry def = new Entry();
    private static final Map<String, Entry> cards = new HashMap<>();

    public static void load(LDAttribute plugin) {
        cards.clear();
        def = new Entry();
        File f = new File(plugin.getDataFolder(), "decompose.yml");
        if (!f.exists()) { try { plugin.saveResource("decompose.yml", false); } catch (Exception ignored) {} }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

        ConfigurationSection defSec = cfg.getConfigurationSection("Default");
        if (defSec != null) def = parse(defSec);

        ConfigurationSection cardSec = cfg.getConfigurationSection("Cards");
        if (cardSec != null) {
            for (String id : cardSec.getKeys(false)) {
                ConfigurationSection s = cardSec.getConfigurationSection(id);
                if (s == null) continue;
                cards.put(id, parse(s));
            }
        }
        plugin.getLogger().info("已載入 " + cards.size() + " 張卡的分解配置");
    }

    private static Entry parse(ConfigurationSection s) {
        Entry e = new Entry();
        e.points = s.getInt("Points", 0);
        e.vault = s.getDouble("Vault", 0);
        e.items = s.getStringList("Items");
        if (e.items == null) e.items = new ArrayList<>();
        e.disabled = s.getBoolean("Disabled", false);
        for (String r : s.getStringList("Random")) {
            String[] p = r.split(":");
            if (p.length >= 2) {
                try { e.random.put(p[0], Integer.parseInt(p[1])); } catch (Exception ignored) {}
            }
        }
        return e;
    }

    /** 取得某张卡的分解配置（沒配置用 Default） */
    public static Entry get(String cardId) {
        Entry e = cards.get(cardId);
        if (e == null) return def;
        return e;
    }
}
