package com.longdrange.ldattribute.core.soulring;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class SoulRingConfig {

    public enum MatchMode {
        CONTAINS,   // 包含
        EXACT,      // 完全匹配
        REGEX       // 正则
    }

    public static class CategoryDef {
        public final String id;
        public final String name;
        public final String icon;
        public final boolean isBlock;
        public final boolean isEdible;
        public final boolean matchAll;
        public final List<String> materialContains;
        public final List<String> loreContains;
        public final List<String> nameContains;

        public CategoryDef(String id, String name, String icon,
                           boolean isBlock, boolean isEdible, boolean matchAll,
                           List<String> materialContains,
                           List<String> loreContains,
                           List<String> nameContains) {
            this.id = id; this.name = name; this.icon = icon;
            this.isBlock = isBlock; this.isEdible = isEdible; this.matchAll = matchAll;
            this.materialContains = materialContains;
            this.loreContains = loreContains;
            this.nameContains = nameContains;
        }

        public boolean hasRules() {
            return isBlock || isEdible
                || !materialContains.isEmpty()
                || !loreContains.isEmpty()
                || !nameContains.isEmpty();
        }
    }

    private static String title = "&8&l\u2726 灵魂空间";
    private static int slotsPerPage = 45;
    private static long maxStack = -1;
    private static final List<CategoryDef> categories = new ArrayList<>();

    // ==================== AutoPickup 配置 ====================
    private static boolean autoPickupEnabled = false;
    private static boolean autoPickupPlayerToggle = true;
    private static boolean autoPickupDefaultOn = true;
    private static boolean autoPickupMobDrops = true;
    private static boolean autoPickupBlockDrops = true;
    private static boolean autoPickupMessage = false;
    // ===== 过滤（白名单 + 黑名单）=====
    private static MatchMode whitelistMode = MatchMode.CONTAINS;
    private static MatchMode blacklistMode = MatchMode.CONTAINS;
    private static final List<String> wlMaterial = new ArrayList<>();
    private static final List<String> wlLore = new ArrayList<>();
    private static final List<String> wlName = new ArrayList<>();
    private static final List<String> blMaterial = new ArrayList<>();
    private static final List<String> blLore = new ArrayList<>();
    private static final List<String> blName = new ArrayList<>();

    public static void load(LDAttribute plugin) {
        categories.clear();
        wlMaterial.clear(); wlLore.clear(); wlName.clear();
        blMaterial.clear(); blLore.clear(); blName.clear();

        File f = new File(plugin.getDataFolder(), "soulring.yml");
        if (!f.exists()) { try { plugin.saveResource("soulring.yml", false); } catch (Throwable ignored) {} }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

        title = ChatColor.translateAlternateColorCodes('&', cfg.getString("Title", title));
        slotsPerPage = Math.max(1, Math.min(45, cfg.getInt("SlotsPerPage", 45)));
        maxStack = cfg.getLong("MaxStack", -1);

        // ===== AutoPickup =====
        ConfigurationSection ap = cfg.getConfigurationSection("AutoPickup");
        if (ap != null) {
            autoPickupEnabled = ap.getBoolean("Enabled", false);
            autoPickupPlayerToggle = ap.getBoolean("PlayerToggle", true);
            autoPickupDefaultOn = ap.getBoolean("DefaultOn", true);
            autoPickupMobDrops = ap.getBoolean("PickupMobDrops", true);
            autoPickupBlockDrops = ap.getBoolean("PickupBlockDrops", true);
            autoPickupMessage = ap.getBoolean("MessageOnPickup", false);

ConfigurationSection filter = ap.getConfigurationSection("Filter");
            if (filter != null) {
                ConfigurationSection wl = filter.getConfigurationSection("Whitelist");
                if (wl != null) {
                    whitelistMode = parseMode(wl.getString("MatchMode", "CONTAINS"));
                    wlMaterial.addAll(upperList(wl.getStringList("Material")));
                    wlLore.addAll(wl.getStringList("Lore"));
                    wlName.addAll(wl.getStringList("Name"));
                }
                ConfigurationSection bl = filter.getConfigurationSection("Blacklist");
                if (bl != null) {
                    blacklistMode = parseMode(bl.getString("MatchMode", "CONTAINS"));
                    blMaterial.addAll(upperList(bl.getStringList("Material")));
                    blLore.addAll(bl.getStringList("Lore"));
                    blName.addAll(bl.getStringList("Name"));
                }
            }
        }

        // ===== 分类 =====
        ConfigurationSection cs = cfg.getConfigurationSection("Categories");
        if (cs != null) {
            for (String id : cs.getKeys(false)) {
                ConfigurationSection s = cs.getConfigurationSection(id);
                if (s == null) continue;
                String name = ChatColor.translateAlternateColorCodes('&', s.getString("name", id));
                String icon = s.getString("icon", "CHEST");
                boolean isBlock = s.getBoolean("is-block", false);
                boolean isEdible = s.getBoolean("is-edible", false);
                boolean matchAll = s.getBoolean("match-all", false);
                List<String> mc = s.getStringList("material-contains");
                List<String> lc = s.getStringList("lore-contains");
                List<String> nc = s.getStringList("name-contains");
                if (mc == null) mc = new ArrayList<>();
                if (lc == null) lc = new ArrayList<>();
                if (nc == null) nc = new ArrayList<>();
                List<String> mcU = new ArrayList<>();
                for (String x : mc) mcU.add(x.toUpperCase());
                categories.add(new CategoryDef(id, name, icon, isBlock, isEdible, matchAll, mcU, lc, nc));
            }
        }
        if (categories.isEmpty()) {
            categories.add(new CategoryDef("all", "&f全部", "CHEST",
                    false, false, false,
                    new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>()));
        }

        plugin.getLogger().info("[SoulRing] 配置已加载（" + categories.size() + " 分类，AutoPickup: "
                + (autoPickupEnabled ? "启用" : "禁用") + "）");
    }

    public static String getTitle() { return title; }
    public static int getSlotsPerPage() { return slotsPerPage; }
    public static long getMaxStack() { return maxStack; }

    public static boolean isAutoPickupEnabled() { return autoPickupEnabled; }
    public static boolean isAutoPickupPlayerToggle() { return autoPickupPlayerToggle; }
    public static boolean isAutoPickupDefaultOn() { return autoPickupDefaultOn; }
    public static boolean isAutoPickupMobDrops() { return autoPickupMobDrops; }
    public static boolean isAutoPickupBlockDrops() { return autoPickupBlockDrops; }
    public static boolean isAutoPickupMessage() { return autoPickupMessage; }

    /** 判断物品是否应该被过滤（不拾取） */
    /** 判断物品是否应该被过滤（不拾取/不存入） */
    public static boolean isFiltered(org.bukkit.inventory.ItemStack stack) {
        if (stack == null) return true;

        String mat = stack.getType().name().toUpperCase();
        String display = "";
        List<String> loreLines = new ArrayList<>();

        if (stack.hasItemMeta()) {
            org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
            if (meta.hasDisplayName()) display = ChatColor.stripColor(meta.getDisplayName());
            if (meta.hasLore()) {
                for (String l : meta.getLore()) loreLines.add(ChatColor.stripColor(l));
            }
        }

        // 1. 白名单优先：匹配则允许
        if (matchEntry(whitelistMode, mat, display, loreLines, wlMaterial, wlLore, wlName)) {
            return false;
        }

        // 2. 黑名单：匹配则拒绝
        if (matchEntry(blacklistMode, mat, display, loreLines, blMaterial, blLore, blName)) {
            return true;
        }

        return false;
    }

    /** 匹配材质/名字/Lore */
    private static boolean matchEntry(MatchMode mode,
                                       String mat, String display, List<String> loreLines,
                                       List<String> matList, List<String> loreList, List<String> nameList) {
        // 材质
        for (String k : matList) {
            if (matchOne(mode, mat, k)) return true;
        }
        // 名字
        for (String k : nameList) {
            if (matchOne(mode, display, k)) return true;
        }
        // Lore
        for (String line : loreLines) {
            for (String k : loreList) {
                if (matchOne(mode, line, k)) return true;
            }
        }
        return false;
    }

    /** 单条匹配 */
    private static boolean matchOne(MatchMode mode, String text, String pattern) {
        if (text == null || pattern == null) return false;
        try {
            switch (mode) {
                case EXACT:
                    return text.equalsIgnoreCase(pattern);
                case REGEX:
                    return text.matches(pattern);
                case CONTAINS:
                default:
                    return text.toLowerCase().contains(pattern.toLowerCase());
            }
        } catch (Throwable t) {
            return false;
        }
    }

    private static MatchMode parseMode(String s) {
        if (s == null) return MatchMode.CONTAINS;
        s = s.toUpperCase();
        try { return MatchMode.valueOf(s); } catch (Throwable t) { return MatchMode.CONTAINS; }
    }

    private static List<String> upperList(List<String> list) {
        List<String> out = new ArrayList<>();
        if (list == null) return out;
        for (String s : list) {
            if (s != null && !s.isEmpty()) out.add(s.toUpperCase());
        }
        return out;
    }

    public static List<CategoryDef> getCategories() { return categories; }
    public static int getCategoryCount() { return categories.size(); }
    public static CategoryDef getCategory(int index) {
        if (categories.isEmpty()) return null;
        return categories.get(Math.floorMod(index, categories.size()));
    }
}
