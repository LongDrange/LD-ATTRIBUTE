package com.longdrange.ldattribute.core.dungeon;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class DungeonConfig {

    public static class SpawnDef {
        public final String world;
        public final double x, y, z;
        public final float yaw, pitch;
        public SpawnDef(String world, double x, double y, double z, float yaw, float pitch) {
            this.world = world; this.x = x; this.y = y; this.z = z;
            this.yaw = yaw; this.pitch = pitch;
        }
    }

    public static class EntryDef {
        public final int minLevel;
        public final int costPoints;
        public final double costVault;
        public final String keyMaterial;
        public final String keyName;
        public final List<String> keyLore;
        public EntryDef(int minLevel, int costPoints, double costVault,
                        String keyMaterial, String keyName, List<String> keyLore) {
            this.minLevel = minLevel; this.costPoints = costPoints; this.costVault = costVault;
            this.keyMaterial = keyMaterial; this.keyName = keyName; this.keyLore = keyLore;
        }
    }

    public static class MobSpawn {
        public final String mobId;
        public final int count;
        public final double x, y, z;
        public MobSpawn(String mobId, int count, double x, double y, double z) {
            this.mobId = mobId; this.count = count;
            this.x = x; this.y = y; this.z = z;
        }
    }

    public static class WaveDef {
        public final String name;
        public final String subtitle;
        public final int delay;
        public final List<MobSpawn> mobs;
        public final List<String> beforeMessages;
        public final List<String> beforeCommands;
        public final List<String> afterMessages;
        public final List<String> afterCommands;
        public final RewardDef reward;
        public WaveDef(String name, String subtitle, int delay, List<MobSpawn> mobs,
                       List<String> beforeMessages, List<String> beforeCommands,
                       List<String> afterMessages, List<String> afterCommands, RewardDef reward) {
            this.reward = reward;
            this.name = name; this.subtitle = subtitle; this.delay = delay; this.mobs = mobs;
            this.beforeMessages = beforeMessages; this.beforeCommands = beforeCommands;
            this.afterMessages = afterMessages; this.afterCommands = afterCommands;
        }
    }

    public static class RewardDef {
        public final int points;
        public final double vault;
        public final List<String> items;
        public final List<String> cards;
        public final boolean toSoulRing;
        public final boolean applyLuck;
        public final boolean applyRate;
        public final List<String> messages;
        public final List<String> commands;
        public RewardDef(int points, double vault, List<String> items, List<String> cards,
                         boolean toSoulRing, boolean applyLuck, boolean applyRate,
                         List<String> messages, List<String> commands) {
            this.points = points; this.vault = vault;
            this.items = items; this.cards = cards;
            this.toSoulRing = toSoulRing;
            this.applyLuck = applyLuck; this.applyRate = applyRate;
            this.messages = messages; this.commands = commands;
        }
    }

    public static class DungeonDef {
        public final String id;
        public final String name;
        public final String icon;
        public final int order;
        public final boolean enabled;
        public final String permission;
        public final File worldFolder;
        public final String worldName;
        public final SpawnDef spawn;
        public final SpawnDef exitSpawn;
        public final EntryDef entry;
        public final int timeLimit;
        public final int cooldown;
        public final List<String> intro;
        public final int introDelay;
        public final List<String> startCommands;
        public final List<String> startMessages;
        public final List<WaveDef> waves;
        public final RewardDef reward;
        public final List<String> winMessages;
        public final List<String> winCommands;
        public final List<String> failMessages;
        public final List<String> failCommands;

        public DungeonDef(String id, String name, String icon, int order, boolean enabled, String permission,
                          File worldFolder, String worldName,
                          SpawnDef spawn, SpawnDef exitSpawn, EntryDef entry,
                          int timeLimit, int cooldown, List<String> intro, int introDelay,
                          List<String> startCommands, List<String> startMessages,
                          List<WaveDef> waves, RewardDef reward,
                          List<String> winMessages, List<String> winCommands,
                          List<String> failMessages, List<String> failCommands) {
            this.id = id; this.name = name; this.icon = icon;
            this.order = order; this.enabled = enabled; this.permission = permission;
            this.worldFolder = worldFolder; this.worldName = worldName;
            this.spawn = spawn; this.exitSpawn = exitSpawn; this.entry = entry;
            this.timeLimit = timeLimit; this.cooldown = cooldown;
            this.intro = intro; this.introDelay = introDelay;
            this.startCommands = startCommands; this.startMessages = startMessages;
            this.waves = waves; this.reward = reward;
            this.winMessages = winMessages; this.winCommands = winCommands;
            this.failMessages = failMessages; this.failCommands = failCommands;
        }
    }

    public static String title = "&8&l✦ 副本列表";
    public static boolean protectBreak = true;
    public static boolean protectPlace = true;
    public static boolean protectDrop = true;
    public static boolean protectInteract = true;

    public static boolean commandBlockEnabled = true;
    public static String commandBlockMode = "WHITELIST";
    public static List<String> commandBlockList = new ArrayList<>();
    public static String commandBlockMsg = "§c副本中禁止使用此指令";
    public static final java.util.Map<String, String> messages = new java.util.HashMap<>();
    public static int maxTeamSize = 4;
    public static int inviteTimeoutSec = 30;

    private static final LinkedHashMap<String, DungeonDef> dungeons = new LinkedHashMap<>();

    public static void load(LDAttribute plugin) {
        dungeons.clear();

        File root = new File(plugin.getDataFolder(), "配置/副本");
        if (!root.exists()) root.mkdirs();

        File globalFile = new File(root, "config.yml");
        if (!globalFile.exists()) writeGlobalDefault(globalFile);
        YamlConfiguration gc = YamlConfiguration.loadConfiguration(globalFile);
        title = ChatColor.translateAlternateColorCodes((char)38, gc.getString("Title", title));
        protectBreak = gc.getBoolean("Protect.BreakBlock", true);
        protectPlace = gc.getBoolean("Protect.PlaceBlock", true);
        protectDrop = gc.getBoolean("Protect.DropItem", true);
        protectInteract = gc.getBoolean("Protect.Interact", true);

        commandBlockEnabled = gc.getBoolean("CommandBlock.Enabled", true);
        commandBlockMode = gc.getString("CommandBlock.Mode", "WHITELIST").toUpperCase();
        commandBlockList.clear();
        List<String> cmdList = gc.getStringList("CommandBlock.List");
        if (cmdList != null) {
            for (String s : cmdList) commandBlockList.add(s.toLowerCase().replace("/", ""));
        }
        commandBlockMsg = ChatColor.translateAlternateColorCodes((char)38,
                gc.getString("CommandBlock.Message", commandBlockMsg));

        // 系统消息
        messages.clear();
        ConfigurationSection msgSec = gc.getConfigurationSection("Messages");
        if (msgSec != null) {
            for (String key : msgSec.getKeys(false)) {
                String v = msgSec.getString(key);
                if (v != null) {
                    messages.put(key, ChatColor.translateAlternateColorCodes((char)38, v));
                }
            }
        }

        maxTeamSize = gc.getInt("Team.MaxSize", 4);
        inviteTimeoutSec = gc.getInt("Team.InviteTimeout", 30);

        File[] children = root.listFiles(File::isDirectory);
        if (children == null) children = new File[0];
        Arrays.sort(children, Comparator.comparing(File::getName));

        for (File dFolder : children) {
            File defFile = new File(dFolder, "內容配置.yml");
            if (!defFile.exists()) defFile = new File(dFolder, "内容配置.yml");
            if (!defFile.exists()) continue;
            try {
                DungeonDef d = parseDungeon(plugin, dFolder, defFile);
                if (d != null) dungeons.put(d.id, d);
            } catch (Throwable t) {
                plugin.getLogger().warning("[Dungeon] 加载 " + dFolder.getName() + " 失败: " + t.getMessage());
            }
        }

        List<DungeonDef> sorted = new ArrayList<>(dungeons.values());
        sorted.sort(Comparator.comparingInt(d -> d.order));
        dungeons.clear();
        for (DungeonDef d : sorted) dungeons.put(d.id, d);

        checkDuplicateWorlds(plugin);
        plugin.getLogger().info("[Dungeon] 已加载 " + dungeons.size() + " 个副本");
    }

    /** 检查是否有副本用了重复的世界名 */
    private static void checkDuplicateWorlds(LDAttribute plugin) {
        Map<String, String> worldToDungeon = new HashMap<>();
        for (DungeonDef d : dungeons.values()) {
            String existing = worldToDungeon.get(d.worldName);
            if (existing != null) {
                plugin.getLogger().warning("[Dungeon] 副本 " + d.id
                        + " 和 " + existing + " 用了同一个世界 " + d.worldName
                        + "，会互相干扰！请改成不同的 WorldName");
            } else {
                worldToDungeon.put(d.worldName, d.id);
            }
        }
    }

    private static void writeGlobalDefault(File f) {
        String c = "# 副本全局配置\n" +
                "Title: \"&8&l✦ 副本列表\"\n" +
                "\n" +
                "Protect:\n" +
                "  BreakBlock: true\n" +
                "  PlaceBlock: true\n" +
                "  DropItem: true\n" +
                "  Interact: true\n" +
                "\n" +
                "CommandBlock:\n" +
                "  Enabled: true\n" +
                "  Mode: WHITELIST\n" +
                "  Message: '&c副本中禁止使用此指令'\n" +
                "  List:\n" +
                "    - 'lddungeon'\n" +
                "    - 'dg'\n" +
                "    - 'fuben'\n" +
                "    - 'say'\n" +
                "    - 'help'\n" +
                "    - 'ldsr'\n" +
                "    - 'spawn'\n";
        try {
            java.nio.file.Files.write(f.toPath(), c.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Throwable ignored) {}
    }

    private static DungeonDef parseDungeon(LDAttribute plugin, File folder, File defFile) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(defFile);
        String id = folder.getName();
        String name = ChatColor.translateAlternateColorCodes((char)38, cfg.getString("Name", id));
        String icon = cfg.getString("Icon", "STONE");
        int order = cfg.getInt("Order", 999);
        boolean enabled = cfg.getBoolean("Enabled", true);
        String permission = cfg.getString("Permission", "");
        if (permission == null) permission = "";

        String worldName = cfg.getString("WorldName", "dungeon_" + id);
        File worldFolder = new File(folder, "world");

        SpawnDef spawn = parseSpawn(cfg.getConfigurationSection("Spawn"), worldName);
        SpawnDef exitSpawn = parseSpawn(cfg.getConfigurationSection("ExitSpawn"), worldName);

        ConfigurationSection entrySec = cfg.getConfigurationSection("Entry");
        int minLevel = 1;
        int costPoints = 0;
        double costVault = 0;
        String keyMat = "PAPER";
        String keyName = "";
        List<String> keyLore = new ArrayList<>();
        if (entrySec != null) {
            minLevel = entrySec.getInt("MinLevel", 1);
            ConfigurationSection costSec = entrySec.getConfigurationSection("Cost");
            if (costSec != null) {
                costPoints = costSec.getInt("Points", 0);
                costVault = costSec.getDouble("Vault", 0);
            }
            ConfigurationSection keySec = entrySec.getConfigurationSection("KeyItem");
            if (keySec != null) {
                keyMat = keySec.getString("Type", "PAPER");
                keyName = ChatColor.translateAlternateColorCodes((char)38, keySec.getString("Name", ""));
                List<String> kl = keySec.getStringList("Lore");
                if (kl != null) for (String s : kl) keyLore.add(ChatColor.translateAlternateColorCodes((char)38, s));
            }
        }
        EntryDef entry = new EntryDef(minLevel, costPoints, costVault, keyMat, keyName, keyLore);

        int timeLimit = cfg.getInt("TimeLimit", 600);
        int cooldown = cfg.getInt("Cooldown", 3600);
        List<String> intro = cfg.getStringList("Intro");
        if (intro == null) intro = new ArrayList<>();
        List<String> coloredIntro = new ArrayList<>();
        for (String s : intro) coloredIntro.add(ChatColor.translateAlternateColorCodes((char)38, s));
        int introDelay = cfg.getInt("IntroDelay", 10);

        List<String> startCommands = colorList(cfg.getStringList("StartCommands"));
        List<String> startMessages = colorList(cfg.getStringList("StartMessages"));

        List<WaveDef> waves = new ArrayList<>();
        List<Map<?, ?>> waveList = cfg.getMapList("Waves");
        for (Map<?, ?> wm : waveList) {
            String wname = ChatColor.translateAlternateColorCodes((char)38, String.valueOf(wm.get("Name")));
            Object subObj = wm.get("Subtitle");
            String wsub = subObj == null ? "" : ChatColor.translateAlternateColorCodes((char)38, String.valueOf(subObj));
            int wdelay = 3;
            try { wdelay = Integer.parseInt(String.valueOf(wm.get("Delay"))); } catch (Throwable ignored) {}

            List<String> wBeforeMsg = colorListFromObj(wm.get("BeforeMessages"));
            List<String> wBeforeCmd = rawListFromObj(wm.get("BeforeCommands"));
            List<String> wAfterMsg = colorListFromObj(wm.get("AfterMessages"));
            List<String> wAfterCmd = rawListFromObj(wm.get("AfterCommands"));

            List<MobSpawn> mobs = new ArrayList<>();
            Object mobsObj = wm.get("Mobs");
            if (mobsObj instanceof List) {
                for (Object mo : (List<?>) mobsObj) {
                    if (!(mo instanceof Map)) continue;
                    Map<?, ?> mm = (Map<?, ?>) mo;
                    String mid = String.valueOf(mm.get("Mob"));
                    int mcount = 1;
                    try { mcount = Integer.parseInt(String.valueOf(mm.get("Count"))); } catch (Throwable ignored) {}
                    double mx = toD(mm.get("X"), 0);
                    double my = toD(mm.get("Y"), 64);
                    double mz = toD(mm.get("Z"), 0);
                    mobs.add(new MobSpawn(mid, mcount, mx, my, mz));
                }
            }
            // 波次奖励
            ConfigurationSection wRw = null;
            try {
                Object wRewardObj = wm.get("Reward");
                if (wRewardObj instanceof Map) {
                    // 用临时 YamlConfiguration 解析
                    YamlConfiguration tmp = new YamlConfiguration();
                    for (Map.Entry<?, ?> e : ((Map<?, ?>) wRewardObj).entrySet()) {
                        tmp.set(String.valueOf(e.getKey()), e.getValue());
                    }
                    wRw = tmp;
                }
            } catch (Throwable ignored) {}
            RewardDef wReward = parseReward(wRw);
            waves.add(new WaveDef(wname, wsub, wdelay, mobs,
                    wBeforeMsg, wBeforeCmd, wAfterMsg, wAfterCmd, wReward));
        }

        RewardDef reward = parseReward(cfg.getConfigurationSection("Reward"));

        List<String> winMsg = colorList(cfg.getStringList("WinMessages"));
        List<String> winCmd = cfg.getStringList("WinCommands");
        if (winCmd == null) winCmd = new ArrayList<>();
        List<String> failMsg = colorList(cfg.getStringList("FailMessages"));
        List<String> failCmd = cfg.getStringList("FailCommands");
        if (failCmd == null) failCmd = new ArrayList<>();

        return new DungeonDef(id, name, icon, order, enabled, permission,
                worldFolder, worldName,
                spawn, exitSpawn, entry,
                timeLimit, cooldown, coloredIntro, introDelay,
                startCommands, startMessages,
                waves, reward,
                winMsg, winCmd, failMsg, failCmd);
    }

    private static List<String> colorListFromObj(Object o) {
        List<String> out = new ArrayList<>();
        if (!(o instanceof List)) return out;
        for (Object x : (List<?>) o) {
            out.add(ChatColor.translateAlternateColorCodes((char)38, String.valueOf(x)));
        }
        return out;
    }

    private static List<String> rawListFromObj(Object o) {
        List<String> out = new ArrayList<>();
        if (!(o instanceof List)) return out;
        for (Object x : (List<?>) o) out.add(String.valueOf(x));
        return out;
    }

    /** 读取系统消息（key 不存在用 default） */
    public static String msg(String key, String def, Object... args) {
        String v = messages.getOrDefault(key, def);
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                v = v.replace("{" + i + "}", String.valueOf(args[i]));
            }
        }
        return v;
    }

    private static RewardDef parseReward(ConfigurationSection rw) {
        int rPoints = 0;
        double rVault = 0;
        List<String> rItems = new ArrayList<>();
        List<String> rCards = new ArrayList<>();
        boolean toSR = true;
        boolean aLuck = true;
        boolean aRate = true;
        List<String> rMsg = new ArrayList<>();
        List<String> rCmd = new ArrayList<>();
        if (rw != null) {
            rPoints = rw.getInt("Points", 0);
            rVault = rw.getDouble("Vault", 0);
            List<String> ri = rw.getStringList("Items");
            if (ri != null) rItems.addAll(ri);
            List<String> rc = rw.getStringList("Cards");
            if (rc != null) rCards.addAll(rc);
            toSR = rw.getBoolean("ToSoulRing", true);
            aLuck = rw.getBoolean("ApplyLuck", true);
            aRate = rw.getBoolean("ApplyRate", true);
            rMsg = colorList(rw.getStringList("Messages"));
            rCmd = rw.getStringList("Commands");
            if (rCmd == null) rCmd = new ArrayList<>();
        }
        return new RewardDef(rPoints, rVault, rItems, rCards, toSR, aLuck, aRate, rMsg, rCmd);
    }

    private static SpawnDef parseSpawn(ConfigurationSection s, String defaultWorld) {
        if (s == null) return new SpawnDef(defaultWorld, 0, 64, 0, 0, 0);
        String w = s.getString("World", defaultWorld);
        double x = s.getDouble("X", 0);
        double y = s.getDouble("Y", 64);
        double z = s.getDouble("Z", 0);
        float yaw = (float) s.getDouble("Yaw", 0);
        float pitch = (float) s.getDouble("Pitch", 0);
        return new SpawnDef(w, x, y, z, yaw, pitch);
    }

    private static double toD(Object o, double def) {
        if (o == null) return def;
        try { return Double.parseDouble(String.valueOf(o)); } catch (Throwable t) { return def; }
    }

    private static List<String> colorList(List<String> list) {
        List<String> out = new ArrayList<>();
        if (list == null) return out;
        for (String s : list) out.add(ChatColor.translateAlternateColorCodes((char)38, s));
        return out;
    }

    public static DungeonDef get(String id) { return dungeons.get(id); }
    public static Collection<DungeonDef> all() { return dungeons.values(); }

    public static List<DungeonDef> visibleFor(org.bukkit.entity.Player player) {
        List<DungeonDef> out = new ArrayList<>();
        for (DungeonDef d : dungeons.values()) {
            if (!d.enabled) continue;
            if (!d.permission.isEmpty() && !player.hasPermission(d.permission)) continue;
            out.add(d);
        }
        return out;
    }
}
