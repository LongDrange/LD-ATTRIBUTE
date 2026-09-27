package com.longdrange.ldattribute.core.guide;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * 图鉴收藏分/称号配置
 * 路径：plugins/LD-Attribute/配置/图鉴/称号.yml
 */
public class GuideScoreConfig {

    public static class TitleDef {
        public final String id;
        public final String name;            // 带色称号名
        public final String plainName;       // 剥色后
        public final int score;              // 需要的收藏分
        public final String icon;            // GUI 图标
        public final List<String> attribute; // 称号属性
        public final String prefix;          // 聊天前缀（带色）
        public final List<String> commands;  // 获得时执行的命令

        public TitleDef(String id, String name, String plainName, int score, String icon,
                        List<String> attribute, String prefix, List<String> commands) {
            this.id = id; this.name = name; this.plainName = plainName; this.score = score;
            this.icon = icon; this.attribute = attribute; this.prefix = prefix;
            this.commands = commands;
        }
    }

    /** 每解锁 1 只图鉴默认加多少分（若怪物配置里没写 Score） */
    public static int scorePerUnlock = 1;
    /** 集齐一组额外加多少分（若分组配置里没写 SetScore） */
    public static int scorePerSet = 10;

    private static final LinkedHashMap<String, TitleDef> titles = new LinkedHashMap<>();

    public static void load(LDAttribute plugin) {
        titles.clear();
        File f = new File(plugin.getDataFolder(), "配置/图鉴/称号.yml");
        if (!f.exists()) {
            try {
                f.getParentFile().mkdirs();
                writeDefault(f);
            } catch (Throwable ignored) {}
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

        scorePerUnlock = cfg.getInt("ScorePerUnlock", 1);
        scorePerSet = cfg.getInt("ScorePerSet", 10);

        ConfigurationSection sec = cfg.getConfigurationSection("Titles");
        if (sec == null) {
            plugin.getLogger().info("[GuideScore] 无称号配置");
            return;
        }
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) continue;
            try {
                String name = ChatColor.translateAlternateColorCodes((char)38, s.getString("Name", id));
                String plainName = ChatColor.stripColor(name);
                int score = s.getInt("Score", 0);
                String icon = s.getString("Icon", "PAPER");
                List<String> attrs = s.getStringList("Attribute");
                if (attrs == null) attrs = new ArrayList<>();
                String prefix = ChatColor.translateAlternateColorCodes((char)38, s.getString("Prefix", ""));
                List<String> cmds = s.getStringList("Commands");
                if (cmds == null) cmds = new ArrayList<>();
                titles.put(id, new TitleDef(id, name, plainName, score, icon, attrs, prefix, cmds));
            } catch (Throwable t) {
                plugin.getLogger().warning("[GuideScore] 加载称号 " + id + " 失败: " + t.getMessage());
            }
        }

        // 按分数升序排序
        List<TitleDef> sorted = new ArrayList<>(titles.values());
        sorted.sort(Comparator.comparingInt(t -> t.score));
        titles.clear();
        for (TitleDef t : sorted) titles.put(t.id, t);

        plugin.getLogger().info("[GuideScore] 已加载 " + titles.size() + " 个称号");
    }

    public static Collection<TitleDef> allTitles() { return titles.values(); }
    public static TitleDef getTitle(String id) { return titles.get(id); }

    /** 根据收藏分返回应得的最高称号（可能 null） */
    public static TitleDef getTitleByScore(int score) {
        TitleDef best = null;
        for (TitleDef t : titles.values()) {
            if (t.score <= score) best = t;
        }
        return best;
    }

    private static void writeDefault(File f) {
        String c = "# ============================================================\n" +
                "# 图鉴收藏分/称号配置\n" +
                "# ============================================================\n" +
                "# ScorePerUnlock: 每解锁 1 只图鉴默认加多少分（怪物配置里没写 Score 时用这个）\n" +
                "# ScorePerSet:    集齐一组额外加多少分（分组配置里没写 SetScore 时用这个）\n" +
                "#\n" +
                "# 称号：\n" +
                "#   Name:      称号显示名（带色）\n" +
                "#   Score:     达到多少收藏分解锁\n" +
                "#   Icon:      图标（GUI 显示）\n" +
                "#   Prefix:    聊天前缀（带色，可空）\n" +
                "#   Attribute: 永久属性加成\n" +
                "#   Commands:  获得时执行的命令（%player% = 玩家名）\n" +
                "# ============================================================\n" +
                "\n" +
                "ScorePerUnlock: 1\n" +
                "ScorePerSet: 10\n" +
                "\n" +
                "Titles:\n" +
                "  novice:\n" +
                "    Name: \"&7新手收藏家\"\n" +
                "    Score: 5\n" +
                "    Icon: \"PAPER\"\n" +
                "    Prefix: \"&7[新手] \"\n" +
                "    Attribute:\n" +
                "      - \"攻击力: +10\"\n" +
                "    Commands: []\n" +
                "\n" +
                "  expert:\n" +
                "    Name: \"&e资深收藏家\"\n" +
                "    Score: 20\n" +
                "    Icon: \"GOLD_INGOT\"\n" +
                "    Prefix: \"&e[资深] \"\n" +
                "    Attribute:\n" +
                "      - \"攻击力: +30\"\n" +
                "      - \"生命上限: +200\"\n" +
                "    Commands: []\n" +
                "\n" +
                "  master:\n" +
                "    Name: \"&d收藏大师\"\n" +
                "    Score: 50\n" +
                "    Icon: \"DIAMOND\"\n" +
                "    Prefix: \"&d[大师] \"\n" +
                "    Attribute:\n" +
                "      - \"攻击力: +100\"\n" +
                "      - \"生命上限: +500\"\n" +
                "      - \"暴击机率: +5\"\n" +
                "    Commands:\n" +
                "      - \"say %player% 获得了收藏大师称号！\"\n";
        try {
            java.nio.file.Files.write(f.toPath(), c.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Throwable ignored) {}
    }
}