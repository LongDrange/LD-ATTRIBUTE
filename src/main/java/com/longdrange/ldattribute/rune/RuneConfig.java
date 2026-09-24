package com.longdrange.ldattribute.rune;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class RuneConfig {

    public static class Socket {
        public String id, name;
        public List<String> allowedTypes = new ArrayList<>();
        public int costPoints;
        public double costVault;
        public List<String> costItems = new ArrayList<>();
    }

    public static class Rune {
        public String id, name, type;
        public String upgradeTo = "";
        public int costPoints = 0;
        public java.util.List<String> costItems = new java.util.ArrayList<>();
        public Material material;
        public int level;
        public List<String> attributes = new ArrayList<>();
    }

    private static final Map<String, Socket> sockets = new LinkedHashMap<>();
    private static final Map<String, Rune> runes = new LinkedHashMap<>();
    private static final Map<String, List<String>> cardSockets = new LinkedHashMap<>();
    private static List<String> defaultSockets = new ArrayList<>();

    public static void load(LDAttribute plugin) {
        sockets.clear();
        runes.clear();
        cardSockets.clear();
        defaultSockets = new ArrayList<>();

        File f = com.longdrange.ldattribute.util.ConfigPaths.resolve(plugin, "rune.yml");
        if (!f.exists()) { try { plugin.saveResource("rune.yml", false); } catch (Exception ignored) {} }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

        ConfigurationSection sec = cfg.getConfigurationSection("Sockets");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                ConfigurationSection s = sec.getConfigurationSection(key);
                if (s == null) continue;
                Socket sk = new Socket();
                sk.id = key;
                sk.name = org.bukkit.ChatColor.translateAlternateColorCodes((char)38, s.getString("Name", key));
                List<String> types = s.getStringList("AllowedTypes");
                if (types != null) sk.allowedTypes.addAll(types);
                ConfigurationSection uc = s.getConfigurationSection("UnlockCost");
                if (uc != null) {
                    sk.costPoints = uc.getInt("Points", 0);
                    sk.costVault = uc.getDouble("Vault", 0);
                    List<String> items = uc.getStringList("Items");
                    if (items != null) sk.costItems.addAll(items);
                }
                sockets.put(key, sk);
            }
        }

        ConfigurationSection csSec = cfg.getConfigurationSection("CardSockets");
        if (csSec != null) {
            for (String key : csSec.getKeys(false)) {
                List<String> list = csSec.getStringList(key);
                if (list == null) continue;
                if (key.equals("default")) {
                    defaultSockets = list;
                } else {
                    cardSockets.put(key, list);
                }
            }
        }

        ConfigurationSection rs = cfg.getConfigurationSection("Runes");
        if (rs != null) {
            for (String key : rs.getKeys(false)) {
                ConfigurationSection s = rs.getConfigurationSection(key);
                if (s == null) continue;
                Rune r = new Rune();
                r.id = key;
                r.name = org.bukkit.ChatColor.translateAlternateColorCodes((char)38, s.getString("Name", key));
                r.type = s.getString("Type", "UTILITY").toUpperCase();
                String matName = s.getString("Material", "REDSTONE");
                Material m = Material.getMaterial(matName.toUpperCase());
                r.material = m != null ? m : Material.REDSTONE;
                r.level = s.getInt("Level", 1);
                r.upgradeTo = s.getString("UpgradeTo", "");
                ConfigurationSection rec = s.getConfigurationSection("Recipe");
                if (rec != null) {
                    r.costPoints = rec.getInt("Points", 0);
                    java.util.List<String> items = rec.getStringList("Items");
                    if (items != null) r.costItems.addAll(items);
                }
                List<String> attrs = s.getStringList("Attributes");
                if (attrs != null) r.attributes.addAll(attrs);
                runes.put(key, r);
            }
        }

        plugin.getLogger().info("已載入 " + sockets.size() + " 個孔位類型, " + runes.size() + " 個符文");
    }

    public static List<String> getCardSockets(String cardId) {
        if (cardSockets.containsKey(cardId)) return cardSockets.get(cardId);
        return defaultSockets;
    }

    public static Socket getSocket(String id) { return sockets.get(id); }
    public static Rune getRune(String id) { return runes.get(id); }
    public static Collection<Rune> getAllRunes() { return runes.values(); }
    public static Collection<Socket> getAllSockets() { return sockets.values(); }

    public static boolean canPlace(String socketId, String runeId) {
        Socket sk = sockets.get(socketId);
        Rune r = runes.get(runeId);
        if (sk == null || r == null) return false;
        return sk.allowedTypes.contains(r.type);
    }
}
