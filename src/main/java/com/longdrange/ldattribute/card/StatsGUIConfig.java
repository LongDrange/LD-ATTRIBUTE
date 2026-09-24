package com.longdrange.ldattribute.card;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * 属性面板配置（SX-Stats 风格）
 */
public class StatsGUIConfig {

    public static class IconDef {
        public final Material material;
        public final String name;
        public final List<String> lore;
        public IconDef(Material m, String n, List<String> l) {
            this.material = m; this.name = n; this.lore = l;
        }
    }

    private static String title = "§8§l✦ 属性面板";
    private static int rows = 6;
    private static boolean hideZero = true;
    private static String[][] layout = new String[0][0];
    private static boolean dynamic = false;
    private static final List<String> order = new ArrayList<>();
    private static final Map<String, IconDef> icons = new LinkedHashMap<>();

    public static void load(LDAttribute plugin) {
        icons.clear();
        title = "§8§l✦ 属性面板";
        rows = 6;
        hideZero = true;
        layout = new String[0][0];

        File f = com.longdrange.ldattribute.util.ConfigPaths.resolve(plugin, "stats_gui.yml");
        if (!f.exists()) {
            try { plugin.saveResource("stats_gui.yml", false); } catch (Exception ignored) {}
        }
        if (!f.exists()) return;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        title = org.bukkit.ChatColor.translateAlternateColorCodes((char)38, cfg.getString("Title", title));
        title = org.bukkit.ChatColor.translateAlternateColorCodes('&', title);
        rows = Math.max(1, Math.min(6, cfg.getInt("Rows", 6)));
        hideZero = cfg.getBoolean("HideZero", true);
        dynamic = cfg.getBoolean("Dynamic", false);
        order.clear();
        List<String> ord = cfg.getStringList("Order");
        if (ord != null) order.addAll(ord);

        // 读取 Layout
        List<?> rawLayout = cfg.getList("Layout");
        if (rawLayout != null) {
            List<String[]> rowsList = new ArrayList<>();
            for (Object rowObj : rawLayout) {
                if (rowObj instanceof List) {
                    List<?> rowList = (List<?>) rowObj;
                    String[] row = new String[rowList.size()];
                    for (int i = 0; i < rowList.size(); i++) {
                        Object o = rowList.get(i);
                        row[i] = o == null ? "" : o.toString();
                    }
                    rowsList.add(row);
                }
            }
            layout = rowsList.toArray(new String[0][]);
        }

        // 读取 Icons
        ConfigurationSection sec = cfg.getConfigurationSection("Icons");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                ConfigurationSection s = sec.getConfigurationSection(key);
                if (s == null) continue;
                String matName = s.getString("Material", "PAPER");
                Material mat = Material.getMaterial(matName.toUpperCase());
                if (mat == null) mat = Material.PAPER;
                String name = org.bukkit.ChatColor.translateAlternateColorCodes((char) 38, s.getString("Name", "§b" + key));
                List<String> lore = new java.util.ArrayList<>(); for (String ls : s.getStringList("Lore")) lore.add(org.bukkit.ChatColor.translateAlternateColorCodes((char)38, ls));
                if (lore == null) lore = new ArrayList<>();
                icons.put(key, new IconDef(mat, name, lore));
            }
        }

        plugin.getLogger().info("已載入屬性面板配置 (" + icons.size() + " 個圖標)");
    }

    public static String getTitle() { return title; }
    public static int getRows() { return rows; }
    public static boolean isHideZero() { return hideZero; }
    public static String[][] getLayout() { return layout; }
    public static IconDef getIcon(String name) { return icons.get(name); }
    public static boolean isDynamic() { return dynamic; }
    public static List<String> getOrder() { return order; }
}