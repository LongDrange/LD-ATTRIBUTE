package com.longdrange.ldattribute.core.guide;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class GuideConfig {

    public static class MonsterDef {
        public final String id;
        public final String name;
        public final String groupId;
        public final String icon;
        public final int requiredKills;
        public final double unlockChance;   // 击杀时直接解锁概率，0-1
        public final List<String> lore;
        public final List<String> attribute;
        public final int rewardPoints;
        public final double rewardVault;
        public final List<String> rewardItems;
        public final List<String> rewardCommands;
        public MonsterDef(String id, String name, String groupId, String icon,
                          int requiredKills, double unlockChance,
                          List<String> lore, List<String> attribute,
                          int rewardPoints, double rewardVault,
                          List<String> rewardItems, List<String> rewardCommands) {
            this.id = id; this.name = name; this.groupId = groupId; this.icon = icon;
            this.requiredKills = requiredKills; this.unlockChance = unlockChance;
            this.lore = lore; this.attribute = attribute;
            this.rewardPoints = rewardPoints; this.rewardVault = rewardVault;
            this.rewardItems = rewardItems; this.rewardCommands = rewardCommands;
        }
    }

    public static class GroupDef {
        public final String id;
        public final String name;
        public final String icon;
        public GroupDef(String id, String name, String icon) {
            this.id = id; this.name = name; this.icon = icon;
        }
    }

    private static String title = "&8&l✦ 怪物图鉴";
    private static final LinkedHashMap<String, GroupDef> groups = new LinkedHashMap<>();
    private static final LinkedHashMap<String, MonsterDef> monsters = new LinkedHashMap<>();

    public static void load(LDAttribute plugin) {
        groups.clear();
        monsters.clear();

        File f = new File(plugin.getDataFolder(), "guide.yml");
        if (!f.exists()) {
            try { plugin.saveResource("guide.yml", false); } catch (Throwable ignored) {}
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

        title = ChatColor.translateAlternateColorCodes('&', cfg.getString("Title", title));

        ConfigurationSection gsec = cfg.getConfigurationSection("Groups");
        if (gsec != null) {
            for (String gid : gsec.getKeys(false)) {
                ConfigurationSection gs = gsec.getConfigurationSection(gid);
                if (gs == null) continue;
                String name = ChatColor.translateAlternateColorCodes('&', gs.getString("Name", gid));
                String icon = gs.getString("Icon", "CHEST");
                groups.put(gid, new GroupDef(gid, name, icon));
            }
        }

        ConfigurationSection msec = cfg.getConfigurationSection("Monsters");
        if (msec != null) {
            for (String mid : msec.getKeys(false)) {
                ConfigurationSection ms = msec.getConfigurationSection(mid);
                if (ms == null) continue;
                String name = ChatColor.translateAlternateColorCodes('&', ms.getString("Name", mid));
                String group = ms.getString("Group", "");
                String icon = ms.getString("Icon", "SKELETON_SKULL");
                int req = Math.max(1, ms.getInt("RequiredKills", 1));
                double chance = ms.getDouble("UnlockChance", 0.0);
                if (chance < 0) chance = 0;
                if (chance > 1) chance = 1;
                List<String> lore = colorList(ms.getStringList("Lore"));
                List<String> attrs = ms.getStringList("Attribute");
                if (attrs == null) attrs = new ArrayList<>();

                int pts = 0;
                double vault = 0;
                List<String> rItems = new ArrayList<>();
                List<String> rCmds = new ArrayList<>();
                ConfigurationSection rw = ms.getConfigurationSection("Reward");
                if (rw != null) {
                    pts = rw.getInt("Points", 0);
                    vault = rw.getDouble("Vault", 0);
                    List<String> ri = rw.getStringList("Items");
                    if (ri != null) rItems.addAll(ri);
                    List<String> rc = rw.getStringList("Commands");
                    if (rc != null) rCmds.addAll(rc);
                }

                monsters.put(mid, new MonsterDef(mid, name, group, icon, req, chance,
                        lore, attrs, pts, vault, rItems, rCmds));
            }
        }

        plugin.getLogger().info("[Guide] 已加载 " + groups.size() + " 组 / " + monsters.size() + " 只怪物");
    }

    private static List<String> colorList(List<String> list) {
        List<String> out = new ArrayList<>();
        if (list == null) return out;
        for (String s : list) out.add(ChatColor.translateAlternateColorCodes('&', s));
        return out;
    }

    public static String getTitle() { return title; }
    public static GroupDef getGroup(String id) { return groups.get(id); }
    public static Collection<GroupDef> allGroups() { return groups.values(); }
    public static MonsterDef get(String id) { return monsters.get(id); }
    public static Collection<MonsterDef> allMonsters() { return monsters.values(); }
    public static List<MonsterDef> byGroup(String groupId) {
        List<MonsterDef> out = new ArrayList<>();
        for (MonsterDef d : monsters.values()) {
            if (groupId == null || groupId.isEmpty() || groupId.equals(d.groupId)) out.add(d);
        }
        return out;
    }

    /**
     * 按 id 或显示名查找怪物（用于解锁石）
     * 优先 id，其次匹配显示名（剥色后）
     */
    public static MonsterDef findByNameOrId(String key) {
        if (key == null || key.isEmpty()) return null;
        MonsterDef byId = monsters.get(key);
        if (byId != null) return byId;
        String stripped = ChatColor.stripColor(key).trim();
        for (MonsterDef d : monsters.values()) {
            String namePlain = ChatColor.stripColor(d.name).trim();
            if (namePlain.equals(stripped)) return d;
        }
        return null;
    }
}