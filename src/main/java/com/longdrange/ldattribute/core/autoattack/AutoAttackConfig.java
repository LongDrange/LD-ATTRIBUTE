package com.longdrange.ldattribute.core.autoattack;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * 杀戮系统配置
 * 路径：
 *   配置/杀戮/config.yml  （Default + Groups）
 *   配置/杀戮/玩家.yml    （Players 独立配置，命令管理）
 */
public class AutoAttackConfig {

    public static class Profile {
        public String displayName = "";
        public boolean enabled = false;
        public double range = 4.0;
        public int interval = 10;
        public int maxTargets = 1;
        public boolean antiKnockback = false;
        public boolean autoRotate = true;
        public boolean animation = true;
        public boolean useEvent = true;
        public String msgOn = "&a⚔ 杀戮已开启";
        public String msgOff = "&c⚔ 杀戮已关闭";
        public List<String> onEnableCmds = new ArrayList<>();
        public List<String> onDisableCmds = new ArrayList<>();

        public Profile copy() {
            Profile p = new Profile();
            p.displayName = displayName;
            p.enabled = enabled; p.range = range; p.interval = interval;
            p.maxTargets = maxTargets; p.antiKnockback = antiKnockback;
            p.autoRotate = autoRotate; p.animation = animation;
            p.useEvent = useEvent; p.msgOn = msgOn; p.msgOff = msgOff;
            p.onEnableCmds = new ArrayList<>(onEnableCmds);
            p.onDisableCmds = new ArrayList<>(onDisableCmds);
            return p;
        }
    }

    public static class Group {
        public String id;
        public String permission = "";
        public Profile profile = new Profile();
    }

    public static Profile defaultProfile = new Profile();
    public static final Map<String, Group> groups = new LinkedHashMap<>();
    public static final Map<String, Profile> players = new LinkedHashMap<>();  // key 可为玩家名或 UUID

    // 玩家配置文件的引用（保存时用）
    private static File playersFile;
    private static LDAttribute pluginRef;

    public static void load(LDAttribute plugin) {
        pluginRef = plugin;
        defaultProfile = new Profile();
        groups.clear();
        players.clear();

        // 1. config.yml（Default + Groups）
        File cfgFile = new File(plugin.getDataFolder(), "配置/杀戮/config.yml");
        if (!cfgFile.exists()) {
            try {
                cfgFile.getParentFile().mkdirs();
                plugin.saveResource("配置/杀戮/config.yml", false);
            } catch (Throwable ignored) {}
        }
        if (cfgFile.exists()) {
            try {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(cfgFile);
                ConfigurationSection def = cfg.getConfigurationSection("Default");
                if (def != null) applySection(defaultProfile, def);

                ConfigurationSection gs = cfg.getConfigurationSection("Groups");
                if (gs != null) {
                    for (String gid : gs.getKeys(false)) {
                        ConfigurationSection sec = gs.getConfigurationSection(gid);
                        if (sec == null) continue;
                        Group g = new Group();
                        g.id = gid;
                        g.permission = sec.getString("Permission", "");
                        g.profile = defaultProfile.copy();
                        // 用 Default 作为基底，覆盖 Groups 里的字段
                        copyFromSection(g.profile, sec);
                        groups.put(gid, g);
                    }
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("[AutoAttack] 加载 config.yml 失败: " + t.getMessage());
            }
        }

        // 2. 玩家.yml（Players 独立配置）
        playersFile = new File(plugin.getDataFolder(), "配置/杀戮/玩家.yml");
        if (playersFile.exists()) {
            try {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(playersFile);
                ConfigurationSection ps = cfg.getConfigurationSection("Players");
                if (ps != null) {
                    for (String key : ps.getKeys(false)) {
                        ConfigurationSection sec = ps.getConfigurationSection(key);
                        if (sec == null) continue;
                        Profile p = defaultProfile.copy();
                        applySection(p, sec);
                        if (p.displayName == null || p.displayName.isEmpty()) p.displayName = key;
                        players.put(key.toLowerCase(), p);
                    }
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("[AutoAttack] 加载 玩家.yml 失败: " + t.getMessage());
            }
        }

        plugin.getLogger().info("[AutoAttack] 已加载 " + groups.size() + " 权限组 / " + players.size() + " 独立玩家");
    }

    private static void applySection(Profile p, ConfigurationSection s) {
        if (s.contains("DisplayName")) p.displayName = s.getString("DisplayName");
        if (s.contains("Enabled")) p.enabled = s.getBoolean("Enabled");
        if (s.contains("Range")) p.range = s.getDouble("Range");
        if (s.contains("Interval")) p.interval = s.getInt("Interval");
        if (s.contains("MaxTargets")) p.maxTargets = s.getInt("MaxTargets");
        if (s.contains("AntiKnockback")) p.antiKnockback = s.getBoolean("AntiKnockback");
        if (s.contains("AutoRotate")) p.autoRotate = s.getBoolean("AutoRotate");
        if (s.contains("Animation")) p.animation = s.getBoolean("Animation");
        if (s.contains("UseEvent")) p.useEvent = s.getBoolean("UseEvent");
        if (s.contains("MessageOnEnable")) p.msgOn = s.getString("MessageOnEnable");
        if (s.contains("MessageOnDisable")) p.msgOff = s.getString("MessageOnDisable");
        List<String> e1 = s.getStringList("OnEnableCommands"); if (e1 != null && !e1.isEmpty()) p.onEnableCmds = new ArrayList<>(e1);
        List<String> e2 = s.getStringList("OnDisableCommands"); if (e2 != null && !e2.isEmpty()) p.onDisableCmds = new ArrayList<>(e2);
    }

    /** 只覆盖 Groups 里显式写了的字段（不覆盖没写的） */
    private static void copyFromSection(Profile p, ConfigurationSection s) {
        if (s.contains("Range")) p.range = s.getDouble("Range");
        if (s.contains("Interval")) p.interval = s.getInt("Interval");
        if (s.contains("MaxTargets")) p.maxTargets = s.getInt("MaxTargets");
        if (s.contains("AntiKnockback")) p.antiKnockback = s.getBoolean("AntiKnockback");
        if (s.contains("AutoRotate")) p.autoRotate = s.getBoolean("AutoRotate");
        if (s.contains("Animation")) p.animation = s.getBoolean("Animation");
        if (s.contains("UseEvent")) p.useEvent = s.getBoolean("UseEvent");
        if (s.contains("MessageOnEnable")) p.msgOn = s.getString("MessageOnEnable");
        if (s.contains("MessageOnDisable")) p.msgOff = s.getString("MessageOnDisable");
    }

    /**
     * 拿玩家的最终生效配置
     * 优先级：Players > Groups（多组取范围最大的）> Default
     */
    public static Profile resolve(org.bukkit.entity.Player player) {
        if (player == null) return defaultProfile.copy();

        // 1. 独立玩家
        Profile p = players.get(player.getName().toLowerCase());
        if (p == null) p = players.get(player.getUniqueId().toString().toLowerCase());
        if (p != null) return p.copy();

        // 2. 权限组（多组取"范围最大"的）
        Group best = null;
        for (Group g : groups.values()) {
            if (g.permission == null || g.permission.isEmpty()) continue;
            if (!player.hasPermission(g.permission)) continue;
            if (best == null || g.profile.range > best.profile.range) best = g;
        }
        if (best != null) return best.profile.copy();

        // 3. Default
        return defaultProfile.copy();
    }

    /** 保存玩家独立配置到 玩家.yml */
    public static void savePlayers() {
        if (playersFile == null || pluginRef == null) return;
        try {
            YamlConfiguration cfg = new YamlConfiguration();
            for (Map.Entry<String, Profile> e : players.entrySet()) {
                String prefix = "Players." + e.getKey().toLowerCase() + ".";
                if (e.getValue().displayName != null && !e.getValue().displayName.isEmpty()) cfg.set(prefix + "DisplayName", e.getValue().displayName);
                Profile p = e.getValue();
                cfg.set(prefix + "Range", p.range);
                cfg.set(prefix + "Interval", p.interval);
                cfg.set(prefix + "MaxTargets", p.maxTargets);
                cfg.set(prefix + "AntiKnockback", p.antiKnockback);
                cfg.set(prefix + "AutoRotate", p.autoRotate);
                cfg.set(prefix + "Animation", p.animation);
                cfg.set(prefix + "UseEvent", p.useEvent);
                if (p.msgOn != null) cfg.set(prefix + "MessageOnEnable", p.msgOn);
                if (p.msgOff != null) cfg.set(prefix + "MessageOnDisable", p.msgOff);
                if (p.onEnableCmds != null && !p.onEnableCmds.isEmpty()) cfg.set(prefix + "OnEnableCommands", p.onEnableCmds);
                if (p.onDisableCmds != null && !p.onDisableCmds.isEmpty()) cfg.set(prefix + "OnDisableCommands", p.onDisableCmds);
            }
            cfg.save(playersFile);
        } catch (Throwable t) {
            pluginRef.getLogger().warning("[AutoAttack] 保存 玩家.yml 失败: " + t.getMessage());
        }
    }
}
