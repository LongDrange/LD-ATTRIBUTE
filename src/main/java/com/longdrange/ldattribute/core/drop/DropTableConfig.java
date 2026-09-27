package com.longdrange.ldattribute.core.drop;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * 掉落增强配置
 * 路径：plugins/LD-Attribute/配置/掉落/掉落表.yml
 */
public class DropTableConfig {

    public static class Entry {
        public String id;              // 物品定义
        public double chance = 0;      // 基础几率 0~1
        public int amountMin = 1;
        public int amountMax = 1;
        public boolean useDropRate = true;
        public boolean broadcast = false;
        public String permission = "";
        public int dailyLimit = 0;     // 0 = 无限
        public String target = "killer";  // killer / nearby:N / world / server
    }

    /** 怪物内名 → 掉落列表 */
    private static final Map<String, List<Entry>> tables = new LinkedHashMap<>();

    public static void load(LDAttribute plugin) {
        tables.clear();
        File file = new File(plugin.getDataFolder(), "配置/掉落/掉落表.yml");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                plugin.saveResource("配置/掉落/掉落表.yml", false);
            } catch (Throwable ignored) {}
        }
        if (!file.exists()) {
            plugin.getLogger().info("[Drop] 无 掉落表.yml，跳过");
            return;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        int total = 0;
        for (String key : cfg.getKeys(false)) {
            List<Map<?, ?>> raw = cfg.getMapList(key);
            if (raw == null || raw.isEmpty()) continue;
            List<Entry> list = new ArrayList<>();
            for (Map<?, ?> m : raw) {
                try {
                    Entry e = new Entry();
                    e.id = String.valueOf(m.get("id"));
                    Object ch = m.get("chance");
                    e.chance = ch == null ? 0 : Double.parseDouble(String.valueOf(ch));
                    if (e.chance < 0) e.chance = 0;
                    if (e.chance > 1) e.chance = 1;
                    Object am = m.get("amount");
                    if (am != null) {
                        String s = String.valueOf(am);
                        if (s.contains("~")) {
                            String[] p = s.split("~");
                            e.amountMin = Integer.parseInt(p[0].trim());
                            e.amountMax = Integer.parseInt(p[1].trim());
                        } else {
                            e.amountMin = e.amountMax = Integer.parseInt(s.trim());
                        }
                    }
                    Object udr = m.get("useDropRate");
                    e.useDropRate = udr == null || Boolean.parseBoolean(String.valueOf(udr));
                    Object bc = m.get("broadcast");
                    e.broadcast = bc != null && Boolean.parseBoolean(String.valueOf(bc));
                    Object perm = m.get("permission");
                    e.permission = perm == null ? "" : String.valueOf(perm);
                    Object dl = m.get("dailyLimit");
                    e.dailyLimit = dl == null ? 0 : Integer.parseInt(String.valueOf(dl));
                    Object tg = m.get("target");
                    if (tg != null) e.target = String.valueOf(tg).toLowerCase();
                    if (e.id != null && !e.id.isEmpty() && e.chance > 0) list.add(e);
                } catch (Throwable t) {
                    plugin.getLogger().warning("[Drop] 解析 " + key + " 条目失败: " + t.getMessage());
                }
            }
            if (!list.isEmpty()) {
                tables.put(key, list);
                total += list.size();
            }
        }
        plugin.getLogger().info("[Drop] 已加载 " + tables.size() + " 个怪物掉落表 / " + total + " 条掉落");
    }

    /** 拿某怪物的掉落列表（含通用表） */
    public static List<Entry> getDrops(String mobId) {
        List<Entry> out = new ArrayList<>();
        List<Entry> specific = tables.get(mobId);
        if (specific != null) out.addAll(specific);
        List<Entry> generic = tables.get("*");
        if (generic != null) out.addAll(generic);
        return out;
    }

    public static boolean hasTable(String mobId) {
        return tables.containsKey(mobId) || tables.containsKey("*");
    }

    public static Set<String> allMobIds() { return tables.keySet(); }
}
