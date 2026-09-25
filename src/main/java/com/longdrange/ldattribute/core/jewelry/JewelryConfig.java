package com.longdrange.ldattribute.core.jewelry;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class JewelryConfig {

    /** 单个槽位定义 */
    public static class SlotDef {
        public final String id;            // 全局唯一，如 helmet
        public final String name;          // 显示名
        public final String loreKey;       // Lore 里"饰品槽位:"后写的内容
        public final int guiSlot;          // GUI 中第几格（0-44）
        public final String icon;          // 空槽材质
        public final String permission;
        public final int costPoints;
        public final double costVault;
        public final List<String> items;
        public final String pageId;        // 属于哪一页

        public SlotDef(String id, String name, String loreKey, int guiSlot, String icon,
                       String permission, int costPoints, double costVault,
                       List<String> items, String pageId) {
            this.id = id; this.name = name; this.loreKey = loreKey;
            this.guiSlot = guiSlot; this.icon = icon;
            this.permission = permission; this.costPoints = costPoints;
            this.costVault = costVault; this.items = items; this.pageId = pageId;
        }
    }

    /** 单页定义 */
    public static class PageDef {
        public final String id;
        public final String title;
        public final LinkedHashMap<String, SlotDef> slots = new LinkedHashMap<>();
        public PageDef(String id, String title) { this.id = id; this.title = title; }
    }

    private static String defaultPage = "main";
    private static final LinkedHashMap<String, PageDef> pages = new LinkedHashMap<>();
    private static final LinkedHashMap<String, SlotDef> allSlots = new LinkedHashMap<>();

    public static void load(LDAttribute plugin) {
        pages.clear();
        allSlots.clear();

        File f = new File(plugin.getDataFolder(), "jewelry.yml");
        if (!f.exists()) { try { plugin.saveResource("jewelry.yml", false); } catch (Throwable ignored) {} }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

        defaultPage = cfg.getString("DefaultPage", "main");

        ConfigurationSection psec = cfg.getConfigurationSection("Pages");
        if (psec == null) {
            plugin.getLogger().warning("[Jewelry] jewelry.yml 缺少 Pages 节点");
            return;
        }

        for (String pid : psec.getKeys(false)) {
            ConfigurationSection ps = psec.getConfigurationSection(pid);
            if (ps == null) continue;
            String title = ChatColor.translateAlternateColorCodes('&', ps.getString("Title", "饰品背包"));
            PageDef page = new PageDef(pid, title);

            ConfigurationSection ssec = ps.getConfigurationSection("Slots");
            if (ssec != null) {
                for (String sid : ssec.getKeys(false)) {
                    ConfigurationSection ss = ssec.getConfigurationSection(sid);
                    if (ss == null) continue;
                    String name = ChatColor.translateAlternateColorCodes('&', ss.getString("Name", sid));
                    String loreKey = ss.getString("LoreKey", name);
                    int guiSlot = ss.getInt("GuiSlot", 0);
                    String icon = ss.getString("Icon", "STONE");
                    String perm = ss.getString("Permission", "");
                    int cp = ss.getInt("CostPoints", 0);
                    double cv = ss.getDouble("CostVault", 0);
                    List<String> items = ss.getStringList("Items");
                    if (items == null) items = new ArrayList<>();

                    SlotDef def = new SlotDef(sid, name, loreKey, guiSlot, icon, perm, cp, cv, items, pid);
                    page.slots.put(sid, def);
                    allSlots.put(sid, def);
                }
            }
            pages.put(pid, page);
        }

        plugin.getLogger().info("[Jewelry] 已加载 " + pages.size() + " 页 / " + allSlots.size() + " 槽位");
    }

    public static String getDefaultPage() { return defaultPage; }
    public static PageDef getPage(String id) { return pages.get(id); }
    public static Collection<PageDef> allPages() { return pages.values(); }
    public static SlotDef getSlot(String id) { return allSlots.get(id); }
    public static Collection<SlotDef> allSlots() { return allSlots.values(); }

    public static SlotDef getSlotByGui(String pageId, int guiSlot) {
        PageDef p = pages.get(pageId);
        if (p == null) return null;
        for (SlotDef s : p.slots.values()) if (s.guiSlot == guiSlot) return s;
        return null;
    }

    /** 通过 Lore 里写的槽位名找槽位（支持 id 和 loreKey 两种写法） */
    public static SlotDef findByLoreKey(String key) {
        if (key == null) return null;
        for (SlotDef s : allSlots.values()) {
            if (key.equalsIgnoreCase(s.id)) return s;
            if (key.equals(s.loreKey)) return s;
        }
        return null;
    }
}