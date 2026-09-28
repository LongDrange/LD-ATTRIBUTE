package com.longdrange.ldattribute.core.relic;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * 遗物配置
 * 路径：plugins/LD-Attribute/配置/遗物/遗物表.yml
 */
public class RelicConfig {

    public static class SlotDef {
        public final String id;
        public final String name;
        public final String icon;
        public final String mainAttr;
        public final int guiSlot;   // GUI 里的格子位置
        public final int order;     // 排序
        public final int mainMin;
        public final int mainMax;
        public SlotDef(String id, String name, String icon, String mainAttr, int mainMin, int mainMax, int guiSlot, int order) {
            this.id = id; this.name = name; this.icon = icon;
            this.mainAttr = mainAttr; this.mainMin = mainMin; this.mainMax = mainMax;
            this.guiSlot = guiSlot; this.order = order;
        }
    }

    public static class SubEntry {
        public final String attr;
        public final int min;
        public final int max;
        public SubEntry(String attr, int min, int max) { this.attr = attr; this.min = min; this.max = max; }
    }

    public static class RelicDef {
        public final String id;
        public final String name;
        public final String slot;
        public final String icon;
        public final String set;
        public final int star;
        public final int mainMin;
        public final int mainMax;
        public final int subCount;
        public final List<SubEntry> subPool;
        public final List<String> subFixed;
        public RelicDef(String id, String name, String slot, String icon, String set, int star, int mainMin, int mainMax, int subCount, List<SubEntry> subPool, List<String> subFixed) {
            this.id = id; this.name = name; this.slot = slot; this.icon = icon;
            this.set = set; this.star = star; this.mainMin = mainMin; this.mainMax = mainMax;
            this.subCount = subCount; this.subPool = subPool; this.subFixed = subFixed;
        }
    }

    public static class SetDef {
        public final String id;
        public final String name;
        public final Map<Integer, List<String>> bonuses;
        public SetDef(String id, String name, Map<Integer, List<String>> bonuses) {
            this.id = id; this.name = name; this.bonuses = bonuses;
        }
    }

    private static final Map<String, SlotDef> slots = new LinkedHashMap<>();
    private static final Map<String, RelicDef> relics = new LinkedHashMap<>();
    private static final Map<String, SetDef> sets = new LinkedHashMap<>();
    private static final List<SubEntry> subPool = new ArrayList<>();

    public static void load(LDAttribute plugin) {
        slots.clear(); relics.clear(); sets.clear(); subPool.clear();

        File root = new File(plugin.getDataFolder(), "配置/遗物");
        if (!root.exists()) {
            root.mkdirs();
            try { plugin.saveResource("配置/遗物/遗物表.yml", false); } catch (Throwable ignored) {}
        }

        // 递归扫描所有 .yml
        List<File> files = new ArrayList<>();
        collectYml(root, files);
        for (File file : files) {
            try { parseFile(plugin, file); }
            catch (Throwable t) { plugin.getLogger().warning("[Relic] 加载 " + file.getName() + " 失败: " + t.getMessage()); }
        }

        plugin.getLogger().info("[Relic] 已加载 " + slots.size() + " 槽位 / " + relics.size() + " 遗物 / " + sets.size() + " 套装");
    }

    private static void collectYml(File dir, List<File> out) {
        File[] fs = dir.listFiles();
        if (fs == null) return;
        Arrays.sort(fs, Comparator.comparing(File::getName));
        for (File f : fs) {
            if (f.isDirectory()) collectYml(f, out);
            else if (f.getName().toLowerCase().endsWith(".yml")) out.add(f);
        }
    }

    /** 一个文件可以：
     *   1. 含 Slots/SubPool/Sets/Relics 段（传统格式）
     *   2. 顶层全是遗物 ID（每个 key 是一个遗物）
     *   两种可以混合。
     */
    private static void parseFile(LDAttribute plugin, File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // 1. Slots 段
        ConfigurationSection sl = cfg.getConfigurationSection("Slots");
        if (sl != null) {
            for (String id : sl.getKeys(false)) {
                ConfigurationSection s = sl.getConfigurationSection(id);
                if (s == null) continue;
                String name = ChatColor.translateAlternateColorCodes((char)38, s.getString("Name", id));
                String icon = s.getString("Icon", "STONE");
                String mainAttr = s.getString("MainAttr", "");
                int[] r = parseRange(s.getString("MainValue", "1~1"));
                if (r == null) r = new int[]{1, 1};
                int guiSlot = s.getInt("GuiSlot", -1);
                int order = s.getInt("Order", 999);
                slots.put(id, new SlotDef(id, name, icon, mainAttr, r[0], r[1], guiSlot, order));
            }
        }

        // 2. SubPool 段
        List<String> poolList = cfg.getStringList("SubPool");
        for (String line : poolList) {
            try {
                String[] p = line.split(":");
                if (p.length < 2) continue;
                int[] r = parseRange(p[1].trim());
                if (r == null) continue;
                subPool.add(new SubEntry(p[0].trim(), r[0], r[1]));
            } catch (Throwable ignored) {}
        }

        // 3. Relics 段
        ConfigurationSection rl = cfg.getConfigurationSection("Relics");
        if (rl != null) {
            for (String id : rl.getKeys(false)) {
                ConfigurationSection s = rl.getConfigurationSection(id);
                if (s != null) parseRelic(id, s);
            }
        }

        // 4. Sets 段
        ConfigurationSection st = cfg.getConfigurationSection("Sets");
        if (st != null) {
            for (String id : st.getKeys(false)) {
                ConfigurationSection s = st.getConfigurationSection(id);
                if (s == null) continue;
                String name = ChatColor.translateAlternateColorCodes((char)38, s.getString("Name", id));
                Map<Integer, List<String>> bonuses = new LinkedHashMap<>();
                ConfigurationSection bs = s.getConfigurationSection("Bonuses");
                if (bs != null) {
                    for (String k : bs.getKeys(false)) {
                        try {
                            int n = Integer.parseInt(k);
                            bonuses.put(n, bs.getStringList(k));
                        } catch (Throwable ignored) {}
                    }
                }
                sets.put(id, new SetDef(id, name, bonuses));
            }
        }

        // 5. 传统段没匹配的顶层 key（排除 Slots/Relics/Sets/SubPool）→ 当作遗物
        for (String id : cfg.getKeys(false)) {
            if (id.equals("Slots") || id.equals("SubPool") || id.equals("Sets") || id.equals("Relics")) continue;
            ConfigurationSection s = cfg.getConfigurationSection(id);
            if (s == null) continue;
            // 必须有 Slot 字段才当遗物
            if (s.contains("Slot")) parseRelic(id, s);
        }
    }

    private static void parseRelic(String id, ConfigurationSection s) {
        String name = ChatColor.translateAlternateColorCodes((char)38, s.getString("Name", id));
        String slot = s.getString("Slot", "");
        String icon = s.getString("Icon", "STONE");
        String set = s.getString("Set", "");
        int star = s.getInt("Star", 1);
        int[] r = parseRange(s.getString("MainValue", null));
        if (r == null) {
            SlotDef sd = slots.get(slot);
            r = new int[]{sd != null ? sd.mainMin : 1, sd != null ? sd.mainMax : 1};
        }
        int subCount = s.getInt("SubCount", 4);
        List<SubEntry> localPool = null;
        List<String> sp = s.getStringList("SubPool");
        if (sp != null && !sp.isEmpty()) {
            localPool = new ArrayList<>();
            for (String line : sp) {
                try {
                    String[] pp = line.split(":");
                    if (pp.length < 2) continue;
                    int[] pr = parseRange(pp[1].trim());
                    if (pr == null) continue;
                    localPool.add(new SubEntry(pp[0].trim(), pr[0], pr[1]));
                } catch (Throwable ignored) {}
            }
        }
        List<String> subFixed = s.getStringList("SubFixed");
        if (subFixed == null || subFixed.isEmpty()) subFixed = null;
        relics.put(id, new RelicDef(id, name, slot, icon, set, star, r[0], r[1], subCount, localPool, subFixed));
    }

    private static int[] parseRange(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            String[] p = s.split("~");
            if (p.length == 1) {
                int v = Integer.parseInt(p[0].trim());
                return new int[]{v, v};
            }
            return new int[]{Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim())};
        } catch (Throwable t) { return null; }
    }

    public static SlotDef getSlot(String id) { return slots.get(id); }
    public static Collection<SlotDef> allSlots() { return slots.values(); }
    public static RelicDef getRelic(String id) { return relics.get(id); }
    public static Collection<RelicDef> allRelics() { return relics.values(); }
    public static Set<String> relicIds() { return relics.keySet(); }
    public static SetDef getSet(String id) { return sets.get(id); }
    public static List<SubEntry> getSubPool() { return subPool; }

    /** 按 order 排序的槽位列表（GUI 用） */
    public static List<SlotDef> getOrderedSlots() {
        List<SlotDef> list = new ArrayList<>(slots.values());
        list.sort(Comparator.comparingInt(s -> s.order));
        return list;
    }

    public static Set<String> getAllSlotIds() { return slots.keySet(); }
}
