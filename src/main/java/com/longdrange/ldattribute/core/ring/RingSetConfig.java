package com.longdrange.ldattribute.core.ring;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * 魂珠套装配置
 *  - 集合 = 多个类型 + 每个类型的数量需求
 *  - 满足 → 提供额外属性
 */
public class RingSetConfig {

    public static class SetDef {
        public final String id;
        public final String name;
        public final String description;
        public final Map<String, Integer> requirements = new LinkedHashMap<>();
        public final List<String> attributes = new ArrayList<>();
        public final String message;
        public SetDef(String id, String name, String description, String message) {
            this.id = id; this.name = name;
            this.description = description; this.message = message;
        }
    }

    private static final Map<String, SetDef> sets = new LinkedHashMap<>();

    public static void load(LDAttribute plugin) {
        sets.clear();
        File f = new File(plugin.getDataFolder(), "ring-sets.yml");
        if (!f.exists()) { try { plugin.saveResource("ring-sets.yml", false); } catch (Throwable ignored) {} }
        if (!f.exists()) { plugin.getLogger().info("[Ring] 无 ring-sets.yml，跳过套装加载"); return; }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection sec = cfg.getConfigurationSection("Sets");
        if (sec == null) { plugin.getLogger().info("[Ring] ring-sets.yml 无 Sets 区块"); return; }

        for (String key : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) continue;
            String name = color(s.getString("Name", key));
            String desc = color(s.getString("Description", ""));
            String msg = color(s.getString("Message", ""));
            SetDef def = new SetDef(key, name, desc, msg);

            ConfigurationSection req = s.getConfigurationSection("Requirements");
            if (req != null) {
                for (String k : req.getKeys(false)) {
                    def.requirements.put(k, req.getInt(k, 1));
                }
            }
            List<String> attrs = s.getStringList("Attributes");
            if (attrs != null) def.attributes.addAll(attrs);
            sets.put(key, def);
        }
        plugin.getLogger().info("[Ring] 已加载 " + sets.size() + " 个魂珠套装");
    }

    private static String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }

    public static Collection<SetDef> all() { return sets.values(); }
    public static SetDef get(String id) { return sets.get(id); }

    /**
     * 计算玩家当前触发的套装
     * @return key=套装id, value=已触发
     */
    public static Map<String, Boolean> evaluate(RingData data) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        if (data == null) return result;
        for (SetDef def : sets.values()) {
            boolean ok = true;
            for (Map.Entry<String, Integer> e : def.requirements.entrySet()) {
                if (data.countType(e.getKey()) < e.getValue()) { ok = false; break; }
            }
            result.put(def.id, ok);
        }
        return result;
    }

    /** 汇总玩家所有已触发套装的属性 */
    public static Map<String, Double> collectActiveAttributes(RingData data) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (data == null) return out;
        for (SetDef def : sets.values()) {
            boolean ok = true;
            for (Map.Entry<String, Integer> e : def.requirements.entrySet()) {
                if (data.countType(e.getKey()) < e.getValue()) { ok = false; break; }
            }
            if (!ok) continue;
            for (String line : def.attributes) {
                String plain = ChatColor.stripColor(line).trim();
                int idx = plain.indexOf(':');
                if (idx < 0) idx = plain.indexOf('\uFF1A');
                if (idx < 0) continue;
                String k = plain.substring(0, idx).trim();
                String v = plain.substring(idx + 1).trim();
                try {
                    double d = Double.parseDouble(v.replace("+", "").replace("%", "").trim());
                    out.merge(k, d, Double::sum);
                } catch (Exception ignored) {}
            }
        }
        return out;
    }
}