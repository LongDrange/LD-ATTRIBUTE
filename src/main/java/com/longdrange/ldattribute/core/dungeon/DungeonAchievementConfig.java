package com.longdrange.ldattribute.core.dungeon;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class DungeonAchievementConfig {

    public enum Type {
        FIRST_CLEAR, FAST_CLEAR, NO_DEATH, SOLO_CLEAR, FULL_TEAM, CLEAR_COUNT
    }

    public static class AchievementDef {
        public final String id;
        public final String name;
        public final String icon;
        public final String description;
        public final Type type;
        public final String dungeonId;
        public final int seconds;       // FAST_CLEAR 用
        public final int count;         // CLEAR_COUNT 用
        public final int rewardPoints;
        public final double rewardVault;
        public final List<String> rewardItems;
        public final List<String> rewardCards;
        public final List<String> rewardCommands;

        public AchievementDef(String id, String name, String icon, String description,
                              Type type, String dungeonId, int seconds, int count,
                              int rewardPoints, double rewardVault,
                              List<String> rewardItems, List<String> rewardCards, List<String> rewardCommands) {
            this.id = id; this.name = name; this.icon = icon; this.description = description;
            this.type = type; this.dungeonId = dungeonId; this.seconds = seconds; this.count = count;
            this.rewardPoints = rewardPoints; this.rewardVault = rewardVault;
            this.rewardItems = rewardItems; this.rewardCards = rewardCards; this.rewardCommands = rewardCommands;
        }
    }

    private static final LinkedHashMap<String, AchievementDef> achievements = new LinkedHashMap<>();

    public static void load(LDAttribute plugin) {
        achievements.clear();

        File root = new File(plugin.getDataFolder(), "配置/副本");
        File f = new File(root, "成就.yml");
        if (!f.exists()) {
            // 创建默认
            try {
                f.getParentFile().mkdirs();
                writeDefault(f);
            } catch (Throwable ignored) {}
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection sec = cfg.getConfigurationSection("Achievements");
        if (sec == null) {
            plugin.getLogger().info("[DungeonAchievement] 无成就配置");
            return;
        }
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) continue;
            try {
                String name = ChatColor.translateAlternateColorCodes((char)38, s.getString("Name", id));
                String icon = s.getString("Icon", "STONE");
                String desc = ChatColor.translateAlternateColorCodes((char)38, s.getString("Description", ""));
                String typeStr = s.getString("Type", "FIRST_CLEAR").toUpperCase();
                Type type;
                try { type = Type.valueOf(typeStr); } catch (Throwable t) { type = Type.FIRST_CLEAR; }
                String dungeonId = s.getString("Dungeon", "");
                int seconds = s.getInt("Seconds", 0);
                int count = s.getInt("Count", 0);

                ConfigurationSection rw = s.getConfigurationSection("Reward");
                int rp = 0;
                double rv = 0;
                List<String> rItems = new ArrayList<>();
                List<String> rCards = new ArrayList<>();
                List<String> rCmds = new ArrayList<>();
                if (rw != null) {
                    rp = rw.getInt("Points", 0);
                    rv = rw.getDouble("Vault", 0);
                    List<String> ri = rw.getStringList("Items");
                    if (ri != null) rItems.addAll(ri);
                    List<String> rc = rw.getStringList("Cards");
                    if (rc != null) rCards.addAll(rc);
                    List<String> rcmd = rw.getStringList("Commands");
                    if (rcmd != null) rCmds.addAll(rcmd);
                }

                achievements.put(id, new AchievementDef(id, name, icon, desc, type, dungeonId,
                        seconds, count, rp, rv, rItems, rCards, rCmds));
            } catch (Throwable t) {
                plugin.getLogger().warning("[DungeonAchievement] 加载 " + id + " 失败: " + t.getMessage());
            }
        }
        plugin.getLogger().info("[DungeonAchievement] 已加载 " + achievements.size() + " 个成就");
    }

    private static void writeDefault(File f) {
        String c = "# 副本成就\n" +
                "Achievements:\n" +
                "  first_cave:\n" +
                "    Name: \"&6初次探索\"\n" +
                "    Icon: \"STONE\"\n" +
                "    Description: \"首次通关幽暗洞穴\"\n" +
                "    Type: FIRST_CLEAR\n" +
                "    Dungeon: \"幽暗洞穴\"\n" +
                "    Reward:\n" +
                "      Points: 5000\n" +
                "      Items:\n" +
                "        - \"DIAMOND:10\"\n" +
                "\n" +
                "  speed_cave:\n" +
                "    Name: \"&b风驰电掣\"\n" +
                "    Icon: \"FEATHER\"\n" +
                "    Description: \"300 秒内通关幽暗洞穴\"\n" +
                "    Type: FAST_CLEAR\n" +
                "    Dungeon: \"幽暗洞穴\"\n" +
                "    Seconds: 300\n" +
                "    Reward:\n" +
                "      Points: 10000\n" +
                "\n" +
                "  no_death_cave:\n" +
                "    Name: \"&a毫发无伤\"\n" +
                "    Icon: \"GOLDEN_APPLE\"\n" +
                "    Description: \"全程无人死亡通关幽暗洞穴\"\n" +
                "    Type: NO_DEATH\n" +
                "    Dungeon: \"幽暗洞穴\"\n" +
                "    Reward:\n" +
                "      Points: 20000\n" +
                "      Cards:\n" +
                "        - \"孙逊:1\"\n" +
                "\n" +
                "  solo_cave:\n" +
                "    Name: \"&e孤胆英雄\"\n" +
                "    Icon: \"IRON_SWORD\"\n" +
                "    Description: \"单人通关幽暗洞穴\"\n" +
                "    Type: SOLO_CLEAR\n" +
                "    Dungeon: \"幽暗洞穴\"\n" +
                "    Reward:\n" +
                "      Points: 8000\n" +
                "\n" +
                "  clear_10_cave:\n" +
                "    Name: \"&d副本达人\"\n" +
                "    Icon: \"DIAMOND\"\n" +
                "    Description: \"累计通关幽暗洞穴 10 次\"\n" +
                "    Type: CLEAR_COUNT\n" +
                "    Dungeon: \"幽暗洞穴\"\n" +
                "    Count: 10\n" +
                "    Reward:\n" +
                "      Points: 50000\n";
        try {
            java.nio.file.Files.write(f.toPath(), c.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Throwable ignored) {}
    }

    public static AchievementDef get(String id) { return achievements.get(id); }
    public static Collection<AchievementDef> all() { return achievements.values(); }
}