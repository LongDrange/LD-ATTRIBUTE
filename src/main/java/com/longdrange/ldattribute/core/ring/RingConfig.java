package com.longdrange.ldattribute.core.ring;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class RingConfig {

    private static String title = "&8&l\u2726 魂珠空间 &7- 第 %page%/%max_page% 页";
    private static int slotsPerPage = 36;
    private static int defaultPages = 1;
    private static int defaultMaxStack = 64;
    private static final Map<String, Integer> typeLimits = new HashMap<>();
    private static final Map<String, String> attributeMapping = new HashMap<>();

    public static class PageUnlock {
        public final int page;
        public final String permission;
        public final int costPoints;
        public final double costVault;
        public final List<String> items;
        public PageUnlock(int page, String permission, int costPoints, double costVault, List<String> items) {
            this.page = page; this.permission = permission;
            this.costPoints = costPoints; this.costVault = costVault; this.items = items;
        }
    }
    private static final Map<Integer, PageUnlock> pageUnlocks = new HashMap<>();

    public static void load(LDAttribute plugin) {
        typeLimits.clear();
        pageUnlocks.clear();
        attributeMapping.clear();

        File f = new File(plugin.getDataFolder(), "rings.yml");
        if (!f.exists()) { try { plugin.saveResource("rings.yml", false); } catch (Throwable ignored) {} }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

        ConfigurationSection st = cfg.getConfigurationSection("Settings");
        if (st != null) {
            title = color(st.getString("Title", title));
            slotsPerPage = Math.max(1, st.getInt("SlotsPerPage", 36));
            defaultPages = Math.max(1, st.getInt("DefaultPages", 1));
            defaultMaxStack = Math.max(1, st.getInt("DefaultMaxStack", 64));
        }

        ConfigurationSection lim = cfg.getConfigurationSection("TypeLimits");
        if (lim != null) {
            for (String k : lim.getKeys(false)) {
                int v = lim.getInt(k, -1);
                if (v > 0) typeLimits.put(k, v);
            }
        }

        ConfigurationSection pu = cfg.getConfigurationSection("PageUnlock");
        if (pu != null) {
            for (String k : pu.getKeys(false)) {
                try {
                    int pg = Integer.parseInt(k);
                    ConfigurationSection s = pu.getConfigurationSection(k);
                    if (s == null) continue;
                    pageUnlocks.put(pg, new PageUnlock(pg,
                            s.getString("Permission", ""),
                            s.getInt("CostPoints", 0),
                            s.getDouble("CostVault", 0),
                            s.getStringList("Items") == null ? new ArrayList<String>() : s.getStringList("Items")));
                } catch (Exception ignored) {}
            }
        }

        // 属性映射：别名 → 插件真实属性名
        ConfigurationSection am = cfg.getConfigurationSection("AttributeMapping");
        if (am != null) {
            for (String k : am.getKeys(false)) {
                String v = am.getString(k);
                if (v != null && !v.isEmpty()) attributeMapping.put(k, v);
            }
        }

        plugin.getLogger().info("[Ring] 魂珠配置已加载（" + typeLimits.size() + " 独立上限, "
                + pageUnlocks.size() + " 页解锁, " + attributeMapping.size() + " 属性映射）");

        // SX-Attribute 检测
        if (plugin.getServer().getPluginManager().getPlugin("SX-Attribute") != null) {
            plugin.getLogger().info("[Ring] 检测到 SX-Attribute");
            plugin.getLogger().info("[Ring]   可在 SX 属性公式里用 %ldring_total_<属性>% 引用魂珠属性");
        }
    }

    private static String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }

    public static String getTitle() { return title; }
    public static int getSlotsPerPage() { return slotsPerPage; }
    public static int getDefaultPages() { return defaultPages; }
    public static int getDefaultMaxStack() { return defaultMaxStack; }
    public static Integer getTypeLimit(String ringType) { return ringType == null ? null : typeLimits.get(ringType); }
    public static PageUnlock getPageUnlock(int page) { return pageUnlocks.get(page); }
    public static String mapAttribute(String name) {
        if (name == null) return null;
        String v = attributeMapping.get(name);
        return v == null ? name : v;
    }
    public static Map<String, String> getAttributeMapping() { return attributeMapping; }
}