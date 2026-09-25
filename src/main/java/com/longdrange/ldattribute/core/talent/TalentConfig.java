package com.longdrange.ldattribute.core.talent;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class TalentConfig {

    public static class TalentDef {
        public final String id;
        public final String name;
        public final String icon;
        public final int guiSlot;
        public final int maxLevel;
        public final int pointsPerLevel;
        public final List<String> attributes;   // 每级属性
        public final List<String> requires;     // 前置天赋 id 列表
        public final String permission;
        public final String pageId;
        public TalentDef(String id, String name, String icon, int guiSlot,
                         int maxLevel, int pointsPerLevel, List<String> attributes,
                         List<String> requires, String permission, String pageId) {
            this.id = id; this.name = name; this.icon = icon;
            this.guiSlot = guiSlot; this.maxLevel = maxLevel;
            this.pointsPerLevel = pointsPerLevel;
            this.attributes = attributes; this.requires = requires;
            this.permission = permission; this.pageId = pageId;
        }
    }

    public static class PageDef {
        public final String id;
        public final String name;
        public final LinkedHashMap<String, TalentDef> talents = new LinkedHashMap<>();
        public PageDef(String id, String name) { this.id = id; this.name = name; }
    }

    private static String defaultPage = "combat";
    private static final LinkedHashMap<String, PageDef> pages = new LinkedHashMap<>();
    private static final LinkedHashMap<String, TalentDef> allTalents = new LinkedHashMap<>();

    public static void load(LDAttribute plugin) {
        pages.clear();
        allTalents.clear();
        File f = new File(plugin.getDataFolder(), "talent.yml");
        if (!f.exists()) { try { plugin.saveResource("talent.yml", false); } catch (Throwable ignored) {} }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

        defaultPage = cfg.getString("DefaultPage", "combat");

        ConfigurationSection psec = cfg.getConfigurationSection("Pages");
        if (psec == null) {
            plugin.getLogger().warning("[Talent] talent.yml 缺少 Pages");
            return;
        }

        for (String pid : psec.getKeys(false)) {
            ConfigurationSection ps = psec.getConfigurationSection(pid);
            if (ps == null) continue;
            String pname = ChatColor.translateAlternateColorCodes('&', ps.getString("Name", pid));
            PageDef page = new PageDef(pid, pname);

            ConfigurationSection tsec = ps.getConfigurationSection("Talents");
            if (tsec != null) {
                for (String tid : tsec.getKeys(false)) {
                    ConfigurationSection ts = tsec.getConfigurationSection(tid);
                    if (ts == null) continue;
                    String name = ChatColor.translateAlternateColorCodes('&', ts.getString("Name", tid));
                    String icon = ts.getString("Icon", "PAPER");
                    int guiSlot = ts.getInt("GuiSlot", 0);
                    int maxLevel = Math.max(1, ts.getInt("MaxLevel", 10));
                    int ppl = Math.max(1, ts.getInt("PointsPerLevel", 1));
                    List<String> attrs = ts.getStringList("Attribute");
                    if (attrs == null) attrs = new ArrayList<>();
                    List<String> req = ts.getStringList("Requires");
                    if (req == null) req = new ArrayList<>();
                    String perm = ts.getString("Permission", "");

                    TalentDef def = new TalentDef(tid, name, icon, guiSlot, maxLevel, ppl, attrs, req, perm, pid);
                    page.talents.put(tid, def);
                    allTalents.put(tid, def);
                }
            }
            pages.put(pid, page);
        }

        plugin.getLogger().info("[Talent] 已加载 " + pages.size() + " 页 / " + allTalents.size() + " 天赋");
    }

    public static String getDefaultPage() { return defaultPage; }
    public static PageDef getPage(String id) { return pages.get(id); }
    public static Collection<PageDef> allPages() { return pages.values(); }
    public static TalentDef get(String id) { return allTalents.get(id); }
    public static Collection<TalentDef> allTalents() { return allTalents.values(); }
    public static TalentDef getByGui(String pageId, int guiSlot) {
        PageDef p = pages.get(pageId);
        if (p == null) return null;
        for (TalentDef t : p.talents.values()) if (t.guiSlot == guiSlot) return t;
        return null;
    }
}