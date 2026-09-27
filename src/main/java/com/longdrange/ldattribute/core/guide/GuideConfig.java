package com.longdrange.ldattribute.core.guide;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class GuideConfig {

    public static class MonsterDef {
        public final String id;
        public final String name;
        public final String groupId;
        public final String icon;
        public final int requiredKills;
        public final double unlockChance;
        public final List<String> lore;
        public final List<String> attribute;
        public final int rewardPoints;
        public final double rewardVault;
        public final List<String> rewardItems;
        public final List<String> rewardCommands;
        public final int score;
        public MonsterDef(String id, String name, String groupId, String icon,
                          int requiredKills, double unlockChance,
                          List<String> lore, List<String> attribute,
                          int rewardPoints, double rewardVault,
                          List<String> rewardItems, List<String> rewardCommands, int score) {
            this.score = score;
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
        public final int order;
        public final boolean enabled;
        public final String permission;
        public final int setScore;
        public final List<String> setAttribute;

        public GroupDef(String id, String name, String icon, int order, boolean enabled, String permission, int setScore, List<String> setAttribute) {
            this.setScore = setScore;
            this.setAttribute = setAttribute;
            this.id = id; this.name = name; this.icon = icon;
            this.order = order; this.enabled = enabled; this.permission = permission;
        }

        /** 玩家能否看到这个分类 */
        public boolean visibleTo(Player player) {
            if (!enabled) return false;
            if (permission == null || permission.isEmpty()) return true;
            if (player == null) return false;
            return player.hasPermission(permission);
        }
    }

    private static String title = "&8&l✦ 怪物图鉴";
    private static final LinkedHashMap<String, GroupDef> groups = new LinkedHashMap<>();
    private static final LinkedHashMap<String, MonsterDef> monsters = new LinkedHashMap<>();

    public static void load(LDAttribute plugin) {
        groups.clear();
        monsters.clear();

        File folderRoot = new File(plugin.getDataFolder(), "配置/图鉴");
        File legacyFile = new File(plugin.getDataFolder(), "guide.yml");

        if (folderRoot.exists() && folderRoot.isDirectory()) {
            loadFromFolder(plugin, folderRoot);
        } else if (legacyFile.exists()) {
            loadLegacy(plugin, legacyFile);
        } else {
            try { plugin.saveResource("guide.yml", false); } catch (Throwable ignored) {}
            if (legacyFile.exists()) loadLegacy(plugin, legacyFile);
        }

        sortGroupsByOrder();

        plugin.getLogger().info("[Guide] 已加载 " + groups.size() + " 组 / " + monsters.size() + " 只怪物");
    }

    private static void sortGroupsByOrder() {
        List<GroupDef> sorted = new ArrayList<>(groups.values());
        sorted.sort(Comparator.comparingInt(g -> g.order));
        LinkedHashMap<String, GroupDef> newMap = new LinkedHashMap<>();
        for (GroupDef g : sorted) newMap.put(g.id, g);
        groups.clear();
        groups.putAll(newMap);
    }

    private static void loadFromFolder(LDAttribute plugin, File folderRoot) {
        File[] children = folderRoot.listFiles(File::isDirectory);
        if (children == null) return;
        Arrays.sort(children, Comparator.comparing(File::getName));

        for (File groupFolder : children) {
            String groupId = groupFolder.getName();

            File pageFile = new File(groupFolder, "_page.yml");
            if (!pageFile.exists()) pageFile = new File(groupFolder, "_page.yaml");

            String groupName = groupId;
            String groupIcon = "CHEST";
            int setScore = 0;
            List<String> setAttr = new ArrayList<>();
            int order = 999;
            boolean enabled = true;
            String permission = "";

            if (pageFile.exists()) {
                YamlConfiguration pc = YamlConfiguration.loadConfiguration(pageFile);
                groupName = ChatColor.translateAlternateColorCodes((char)38, pc.getString("Name", groupId));
                groupIcon = pc.getString("Icon", "CHEST");
                setScore = pc.getInt("SetScore", 0);
                List<String> saList = pc.getStringList("SetAttribute");
                if (saList != null) setAttr = saList;
                order = pc.getInt("Order", 999);
                enabled = pc.getBoolean("Enabled", true);
                permission = pc.getString("Permission", "");
                if (permission == null) permission = "";
            }

            groups.put(groupId, new GroupDef(groupId, groupName, groupIcon, order, enabled, permission, setScore, setAttr));

            File[] files = groupFolder.listFiles(f -> f.isFile()
                    && (f.getName().endsWith(".yml") || f.getName().endsWith(".yaml"))
                    && !f.getName().startsWith("_"));
            if (files == null) continue;
            Arrays.sort(files, Comparator.comparing(File::getName));

            for (File file : files) {
                try {
                    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                    ConfigurationSection msec = cfg.getConfigurationSection("Monsters");
                    if (msec != null) {
                        for (String mid : msec.getKeys(false)) {
                            ConfigurationSection ms = msec.getConfigurationSection(mid);
                            if (ms == null) continue;
                            MonsterDef def = parseMonster(mid, ms, groupId);
                            if (def != null) monsters.put(mid, def);
                        }
                    } else {
                        for (String mid : cfg.getKeys(false)) {
                            if (mid.startsWith("_") || mid.equals("Title") || mid.equals("Name") || mid.equals("Icon")) continue;
                            ConfigurationSection ms = cfg.getConfigurationSection(mid);
                            if (ms == null) continue;
                            MonsterDef def = parseMonster(mid, ms, groupId);
                            if (def != null) monsters.put(mid, def);
                        }
                    }
                } catch (Throwable t) {
                    plugin.getLogger().warning("[Guide] 加载 " + file.getName() + " 失败: " + t.getMessage());
                }
            }
        }
    }

    private static MonsterDef parseMonster(String mid, ConfigurationSection ms, String groupId) {
        try {
            String name = ChatColor.translateAlternateColorCodes((char)38, ms.getString("Name", mid));
            String group = ms.getString("Group", groupId);
            String icon = ms.getString("Icon", "SKELETON_SKULL");
            int req = Math.max(1, ms.getInt("RequiredKills", 1));
            int score = ms.getInt("Score", 1);
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
            return new MonsterDef(mid, name, group, icon, req, chance, lore, attrs, pts, vault, rItems, rCmds, score);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void loadLegacy(LDAttribute plugin, File f) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        title = ChatColor.translateAlternateColorCodes((char)38, cfg.getString("Title", title));

        ConfigurationSection gsec = cfg.getConfigurationSection("Groups");
        if (gsec != null) {
            for (String gid : gsec.getKeys(false)) {
                ConfigurationSection gs = gsec.getConfigurationSection(gid);
                if (gs == null) continue;
                String name = ChatColor.translateAlternateColorCodes((char)38, gs.getString("Name", gid));
                String icon = gs.getString("Icon", "CHEST");
                groups.put(gid, new GroupDef(gid, name, icon, 999, true, "", 0, new ArrayList<String>()));
            }
        }

        ConfigurationSection msec = cfg.getConfigurationSection("Monsters");
        if (msec != null) {
            for (String mid : msec.getKeys(false)) {
                ConfigurationSection ms = msec.getConfigurationSection(mid);
                if (ms == null) continue;
                MonsterDef def = parseMonster(mid, ms, ms.getString("Group", ""));
                if (def != null) monsters.put(mid, def);
            }
        }
    }

    private static List<String> colorList(List<String> list) {
        List<String> out = new ArrayList<>();
        if (list == null) return out;
        for (String s : list) out.add(ChatColor.translateAlternateColorCodes((char)38, s));
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

    /** 玩家可见的分类（已过滤开关 + 权限，按 Order 排序）*/
    public static List<GroupDef> visibleGroups(Player player) {
        List<GroupDef> out = new ArrayList<>();
        for (GroupDef g : groups.values()) {
            if (g.visibleTo(player)) out.add(g);
        }
        return out;
    }

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
