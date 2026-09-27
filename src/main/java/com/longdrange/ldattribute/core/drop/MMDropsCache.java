package com.longdrange.ldattribute.core.drop;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * 读取 MythicMobs 的 Mobs/*.yml + DropTables/*.yml
 * 缓存每个怪物的掉落列表（内联 + 表引用）
 *
 * MM 格式：
 *   Drops:
 *   - gold_nugget 2 0.5          # 内联：物品 数量 几率
 *   - 371:0 32-64 1              # 数量支持范围
 *   - SkeletonKingDrops          # 引用掉落表（单个词、首字母大写）
 *   - exp 100                    # 特殊：经验
 *
 * DropTables 格式：
 *   SkeletonKingDrops:
 *     Drops:
 *     - KingsCrown 1 0.01
 *     - 371:0 32-64 1
 */
public class MMDropsCache {

    public static class Entry {
        public String item;      // 物品（数字ID 或 名字 或 RING:CARD: 等）
        public int amountMin = 1;
        public int amountMax = 1;
        public double chance = 1.0;
        public boolean isExp = false;
        public boolean isSpecial = false;
        public String raw;
    }

    private static final Map<String, List<Entry>> mobDrops = new LinkedHashMap<>();
    private static final Map<String, List<Entry>> tables = new LinkedHashMap<>();
    private static boolean loaded = false;

    public static void load(LDAttribute plugin) {
        mobDrops.clear();
        tables.clear();

        File mmRoot = new File(plugin.getDataFolder().getParentFile(), "MythicMobs");
        if (!mmRoot.exists()) {
            plugin.getLogger().info("[MMDrops] 找不到 MythicMobs 目录，跳过");
            return;
        }

        // 1. 先读 DropTables
        File dtDir = new File(mmRoot, "DropTables");
        if (dtDir.exists() && dtDir.isDirectory()) {
            File[] files = dtDir.listFiles((d, n) -> n.endsWith(".yml"));
            if (files != null) {
                for (File file : files) parseTableFile(file);
            }
        }

        // 2. 读 Mobs
        File mobsDir = new File(mmRoot, "Mobs");
        if (mobsDir.exists() && mobsDir.isDirectory()) {
            File[] files = mobsDir.listFiles((d, n) -> n.endsWith(".yml"));
            if (files != null) {
                for (File file : files) parseMobsFile(file);
            }
        }

        loaded = true;
        int total = 0;
        for (List<Entry> l : mobDrops.values()) total += l.size();
        plugin.getLogger().info("[MMDrops] 已读取 " + mobDrops.size() + " 个 MM 怪物 / " + tables.size() + " 个掉落表 / " + total + " 条内联掉落");
    }

    private static void parseTableFile(File file) {
        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            for (String tableId : cfg.getKeys(false)) {
                ConfigurationSection sec = cfg.getConfigurationSection(tableId);
                if (sec == null) continue;
                List<Entry> entries = new ArrayList<>();
                List<String> drops = sec.getStringList("Drops");
                if (drops != null) {
                    for (String line : drops) {
                        Entry e = parseDropLine(line);
                        if (e != null) entries.add(e);
                    }
                }
                if (!entries.isEmpty()) tables.put(tableId, entries);
            }
        } catch (Throwable ignored) {}
    }

    private static void parseMobsFile(File file) {
        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            for (String mobId : cfg.getKeys(false)) {
                ConfigurationSection sec = cfg.getConfigurationSection(mobId);
                if (sec == null) continue;
                List<String> drops = sec.getStringList("Drops");
                if (drops == null || drops.isEmpty()) continue;
                List<Entry> entries = new ArrayList<>();
                for (String line : drops) {
                    Entry e = parseDropLine(line);
                    if (e == null) continue;
                    // 是引用掉落表？展开
                    if (e.isSpecial && tables.containsKey(e.raw)) {
                        entries.addAll(tables.get(e.raw));
                    } else {
                        entries.add(e);
                    }
                }
                if (!entries.isEmpty()) mobDrops.put(mobId, entries);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * 解析一行掉落：
     *   "gold_nugget 2 0.5"     → 物品 gold_nugget 数量 2 几率 0.5
     *   "371:0 32-64 1"         → 物品 371:0 数量 32~64 几率 1.0
     *   "SkeletonKingDrops"     → 引用表（特殊）
     *   "exp 100"               → 特殊经验
     */
    private static Entry parseDropLine(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        line = line.trim();

        // 跳过注释
        if (line.startsWith("#")) return null;

        String[] parts = line.split("\\s+");
        if (parts.length == 0) return null;

        Entry e = new Entry();
        e.raw = line;

        // 单个词 → 可能是掉落表引用
        if (parts.length == 1) {
            e.item = parts[0];
            e.isSpecial = true;
            return e;
        }

        e.item = parts[0];

        // 特物品名（exp / heroesexp / mythicdrop 等）
        String lower = e.item.toLowerCase();
        if (lower.equals("exp") || lower.equals("experience") || lower.equals("heroesexp")) {
            e.isExp = true;
            try { e.amountMin = e.amountMax = Integer.parseInt(parts[1]); } catch (Throwable ignored) {}
            return e;
        }

        // 数量
        if (parts.length >= 2) {
            String am = parts[1];
            try {
                if (am.contains("-")) {
                    String[] p = am.split("-");
                    e.amountMin = Integer.parseInt(p[0].trim());
                    e.amountMax = Integer.parseInt(p[1].trim());
                } else {
                    e.amountMin = e.amountMax = Integer.parseInt(am.trim());
                }
            } catch (Throwable ignored) {}
        }

        // 几率
        if (parts.length >= 3) {
            try { e.chance = Double.parseDouble(parts[2].trim()); } catch (Throwable ignored) {}
        } else {
            e.chance = 1.0;
        }
        if (e.chance < 0) e.chance = 0;
        if (e.chance > 1) e.chance = 1;

        return e;
    }

    /** 拿某怪物的 MM 掉落（已展开表引用） */
    public static List<Entry> get(String mobId) {
        List<Entry> list = mobDrops.get(mobId);
        return list == null ? Collections.emptyList() : list;
    }

    public static boolean has(String mobId) {
        return mobDrops.containsKey(mobId);
    }

    public static boolean isLoaded() { return loaded; }

    public static Set<String> allMobIds() { return mobDrops.keySet(); }
}