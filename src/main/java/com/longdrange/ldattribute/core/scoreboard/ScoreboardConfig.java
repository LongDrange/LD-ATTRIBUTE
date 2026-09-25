package com.longdrange.ldattribute.core.scoreboard;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class ScoreboardConfig {

    public static class BoardDef {
        public final String id;
        public final String condition;
        public final String title;
        public final List<String> lines;
        public BoardDef(String id, String condition, String title, List<String> lines) {
            this.id = id; this.condition = condition; this.title = title; this.lines = lines;
        }
    }

    private static boolean enabled = true;
    private static int updateInterval = 20;
    private static boolean allowToggle = true;
    private static String defaultBoard = "main";
    private static final LinkedHashMap<String, BoardDef> boards = new LinkedHashMap<>();

    // header/footer
    private static boolean hfEnabled = true;
    private static List<String> header = new ArrayList<>();
    private static List<String> footer = new ArrayList<>();

    public static void load(LDAttribute plugin) {
        boards.clear();
        header.clear();
        footer.clear();

        File f = new File(plugin.getDataFolder(), "scoreboard.yml");
        if (!f.exists()) {
            try { plugin.saveResource("scoreboard.yml", false); } catch (Throwable ignored) {}
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

        enabled = cfg.getBoolean("enabled", true);
        updateInterval = Math.max(1, cfg.getInt("update-interval", 20));
        allowToggle = cfg.getBoolean("allow-toggle", true);
        defaultBoard = cfg.getString("default", "main");

        ConfigurationSection bs = cfg.getConfigurationSection("Boards");
        if (bs != null) {
            for (String id : bs.getKeys(false)) {
                ConfigurationSection s = bs.getConfigurationSection(id);
                if (s == null) continue;
                String condition = s.getString("condition", "true");
                String title = color(s.getString("title", "&6&l✦ " + id));
                List<String> lines = new ArrayList<>();
                for (String line : s.getStringList("lines")) lines.add(color(line));
                boards.put(id, new BoardDef(id, condition, title, lines));
            }
        }

        ConfigurationSection hf = cfg.getConfigurationSection("header-footer");
        if (hf != null) {
            hfEnabled = hf.getBoolean("enabled", true);
            for (String s : hf.getStringList("header")) header.add(color(s));
            for (String s : hf.getStringList("footer")) footer.add(color(s));
        }

        plugin.getLogger().info("[Scoreboard] 已加载 " + boards.size() + " 套记分板 / header-footer: " + hfEnabled);
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    public static boolean isEnabled() { return enabled; }
    public static int getUpdateInterval() { return updateInterval; }
    public static boolean isAllowToggle() { return allowToggle; }
    public static String getDefaultBoard() { return defaultBoard; }
    public static BoardDef getBoard(String id) { return boards.get(id); }
    public static Collection<BoardDef> allBoards() { return boards.values(); }
    public static boolean isHfEnabled() { return hfEnabled; }
    public static List<String> getHeader() { return header; }
    public static List<String> getFooter() { return footer; }
}