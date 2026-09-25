package com.longdrange.ldattribute.core.compat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * SX-Attribute 对接（反射/PAPI 方式，未安装自动跳过）
 *
 * 通过 PlaceholderAPI 读取 SX-Attribute 的属性值。
 * 常见占位符前缀: sx_attribute / sxattr / sx / sx-attribute
 */
public class SXAttributeCompat {

    private static final String[] PAPI_PREFIXES = {
        "sx_attribute", "sxattr", "sx_attribute_", "sx-attribute", "sx"
    };

    private static final List<String> ATTR_NAMES = Arrays.asList(
        "攻击力", "防御力", "生命上限", "暴击率", "暴击伤害",
        "命中率", "闪避率", "穿透", "吸血", "反伤",
        "移速", "攻速", "幸运", "经验加成"
    );

    private static String cachedPrefix = null;

    public static List<String> getSXAttributes(Player player) {
        if (player == null) return Collections.emptyList();
        if (!isSXLoaded()) return Collections.emptyList();
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return Collections.emptyList();

        List<String> out = new ArrayList<>();
        String prefix = detectPapiPrefix(player);
        if (prefix == null) return out;

        for (String name : ATTR_NAMES) {
            try {
                String ph = "%" + prefix + "_" + name + "%";
                String val = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, ph);
                if (val != null && !val.isEmpty()
                        && !val.equals(ph)
                        && !val.equals("0") && !val.equals("0.0")
                        && !val.equalsIgnoreCase("null")) {
                    out.add(name + ": +" + val);
                }
            } catch (Throwable ignored) {}
        }
        return out;
    }

    private static boolean isSXLoaded() {
        return Bukkit.getPluginManager().getPlugin("SX-Attribute") != null
            || Bukkit.getPluginManager().getPlugin("SXAttribute") != null
            || Bukkit.getPluginManager().getPlugin("SX-Attributes") != null;
    }

    private static String detectPapiPrefix(Player player) {
        if (cachedPrefix != null) return cachedPrefix;
        for (String prefix : PAPI_PREFIXES) {
            try {
                String test = "%" + prefix + "_攻击力%";
                String result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, test);
                if (result != null && !result.equals(test)) {
                    cachedPrefix = prefix;
                    return prefix;
                }
            } catch (Throwable ignored) {}
        }
        // 都探测失败时用第一个（用户装了 SX 之后可能就对了）
        cachedPrefix = PAPI_PREFIXES[0];
        return cachedPrefix;
    }

    /** 供 reload 时清空缓存 */
    public static void resetCache() {
        cachedPrefix = null;
    }
}
