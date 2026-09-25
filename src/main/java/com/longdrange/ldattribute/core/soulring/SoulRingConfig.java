package com.longdrange.ldattribute.core.soulring;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class SoulRingConfig {

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
    private static final Set<String> pickupMaterialBlacklist = new HashSet<>();
    private static final List<String> pickupLoreBlacklist = new ArrayList<>();
    private static final List<String> pickupNameBlacklist = new ArrayList<>();

    public static void load(LDAttribute plugin) {
        categories.clear();
        pickupMaterialBlacklist.clear();
        pickupLoreBlacklist.clear();
        pickupNameBlacklist.clear();

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
                for (String s : filter.getStringList("MaterialBlacklist")) {
                    if (s != null && !s.isEmpty()) pickupMaterialBlacklist.add(s.toUpperCase());
                }
                for (String s : filter.getStringList("LoreBlacklist")) {
                    if (s != null && !s.isEmpty()) pickupLoreBlacklist.add(s);
                }
                for (String s : filter.getStringList("NameBlacklist")) {
                    if (s != null && !s.isEmpty()) pickupNameBlacklist.add(s);
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
    public static boolean isFiltered(org.bukkit.inventory.ItemStack stack) {
        if (stack == null) return true;
        if (pickupMaterialBlacklist.contains(stack.getType().name().toUpperCase())) return true;

        if (stack.hasItemMeta()) {
            org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
            if (meta.hasDisplayName() && !pickupNameBlacklist.isEmpty()) {
                String dn = ChatColor.stripColor(meta.getDisplayName());
                for (String k : pickupNameBlacklist) if (dn.contains(k)) return true;
            }
            if (meta.hasLore() && !pickupLoreBlacklist.isEmpty()) {
                for (String line : meta.getLore()) {
                    String plain = ChatColor.stripColor(line);
                    for (String k : pickupLoreBlacklist) if (plain.contains(k)) return true;
                }
            }
        }
        return false;
    }

    public static List<CategoryDef> getCategories() { return categories; }
    public static int getCategoryCount() { return categories.size(); }
    public static CategoryDef getCategory(int index) {
        if (categories.isEmpty()) return null;
        return categories.get(Math.floorMod(index, categories.size()));
    }
}