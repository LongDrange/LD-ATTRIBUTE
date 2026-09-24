package com.longdrange.ldattribute.util;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import java.util.ArrayList;
import java.util.List;

public class ColorUtil {
    public static String c(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }
    public static List<String> c(List<String> list) {
        List<String> out = new ArrayList<>();
        if (list != null) for (String s : list) out.add(c(s));
        return out;
    }
    public static String name(ConfigurationSection s, String key, String def) {
        return c(s.getString(key, def));
    }
    public static List<String> lore(ConfigurationSection s, String key) {
        return c(s.getStringList(key));
    }
}