package com.longdrange.ldattribute.core.autoattack;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class AntiCheatConfig {

    public static boolean enabled = true;
    public static int maxAPS = 20;
    public static int warnTimes = 3;
    public static long resetAfterMs = 60000L;
    public static String exemptPerm = "ldattribute.anticheat.exempt";
    public static String warnMsg = "&c⚠ 攻速异常 %cur%/%max% (警告 %n%/%total%)";
    public static String kickMsg = "&c检测到使用外挂（攻速异常），已踢出服务器";
    public static boolean broadcast = true;
    public static String broadcastMsg = "&c%player% 因攻速异常被踢出服务器";

    public static void load(LDAttribute plugin) {
        File f = new File(plugin.getDataFolder(), "配置/杀戮/反作弊.yml");
        if (!f.exists()) {
            try {
                f.getParentFile().mkdirs();
                plugin.saveResource("配置/杀戮/反作弊.yml", false);
            } catch (Throwable ignored) {}
        }
        if (!f.exists()) return;
        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            enabled = cfg.getBoolean("Enabled", true);
            maxAPS = cfg.getInt("MaxAPS", 20);
            warnTimes = Math.max(1, cfg.getInt("WarnTimes", 3));
            resetAfterMs = cfg.getLong("ResetAfterMs", 60000L);
            exemptPerm = cfg.getString("ExemptPermission", "ldattribute.anticheat.exempt");
            warnMsg = cfg.getString("WarnMessage", warnMsg);
            kickMsg = cfg.getString("KickMessage", kickMsg);
            broadcast = cfg.getBoolean("Broadcast", true);
            broadcastMsg = cfg.getString("BroadcastMessage", broadcastMsg);
        } catch (Throwable t) {
            plugin.getLogger().warning("[AntiCheat] 加载失败: " + t.getMessage());
        }
        plugin.getLogger().info("[AntiCheat] " + (enabled ? "已启用 (MaxAPS=" + maxAPS + ", WarnTimes=" + warnTimes + ")" : "已停用"));
    }
}