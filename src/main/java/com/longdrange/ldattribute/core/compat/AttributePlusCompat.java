package com.longdrange.ldattribute.core.compat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * AttributePlus 对接（反射/PAPI 方式，未安装自动跳过）
 */
public class AttributePlusCompat {

    private static final String[] PAPI_PREFIXES = {
        "ap", "ap_attribute", "attributeplus", "attribute_plus", "ap_"
    };

    private static final List<String> ATTR_NAMES = Arrays.asList(
        "攻击力", "防御力", "生命上限", "暴击率", "暴击伤害",
        "命中率", "闪避率", "穿透", "吸血", "反伤",
        "移速", "攻速", "幸运", "经验加成", "真实伤害"
    );

    private static String cachedPrefix = null;

    public static List<String> getAPAttributes(Player player) {
        if (player == null) return Collections.emptyList();
        if (!isAPLoaded()) return Collections.emptyList();
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

    private static boolean isAPLoaded() {
        return Bukkit.getPluginManager().getPlugin("AttributePlus") != null
            || Bukkit.getPluginManager().getPlugin("Attribute-Plus") != null
            || Bukkit.getPluginManager().getPlugin("AP") != null;
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
        cachedPrefix = PAPI_PREFIXES[0];
        return cachedPrefix;
    }

    public static void resetCache() {
        cachedPrefix = null;
    }
}
