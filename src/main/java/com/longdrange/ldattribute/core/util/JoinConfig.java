package com.longdrange.ldattribute.core.util;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class JoinConfig {

    private static List<String> broadcast = new ArrayList<>();
    private static List<String> personalMessage = new ArrayList<>();
    private static boolean enabled = true;
    private static boolean broadcastEnabled = false;
    private static boolean personalEnabled = true;

    public static void load(LDAttribute plugin) {
        FileConfiguration cfg = plugin.getCoreManager().getCoreConfig();
        enabled = cfg.getBoolean("join.enabled", true);
        broadcastEnabled = cfg.getBoolean("join.broadcast-enabled", false);
        personalEnabled = cfg.getBoolean("join.personal-enabled", true);
        broadcast = cfg.getStringList("join.broadcast");
        personalMessage = cfg.getStringList("join.personal");
        if (broadcast == null) broadcast = new ArrayList<>();
        if (personalMessage == null) personalMessage = new ArrayList<>();
    }

    public static boolean isEnabled() { return enabled; }
    public static boolean isBroadcastEnabled() { return broadcastEnabled; }
    public static boolean isPersonalEnabled() { return personalEnabled; }

    /** 返回原始字符串（不翻译颜色，不解析 PAPI） */
    public static List<String> getBroadcast() { return new ArrayList<>(broadcast); }
    public static List<String> getPersonalMessage() { return new ArrayList<>(personalMessage); }
}