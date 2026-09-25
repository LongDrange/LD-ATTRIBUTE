package com.longdrange.ldattribute.core.scoreboard;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.util.PapiUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScoreboardManager {

    private final LDAttribute plugin;
    private BukkitTask task;

    private final Set<UUID> hidden = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> using = new ConcurrentHashMap<>();

    /** NMS 首次失败只报一次错 */
    private static final AtomicBoolean HF_ERROR_LOGGED = new AtomicBoolean(false);

    private static String nmsVersion = null;

    public ScoreboardManager(LDAttribute plugin) { this.plugin = plugin; }

    public void start() {
        if (!ScoreboardConfig.isEnabled()) {
            plugin.getLogger().info("[Scoreboard] 已禁用");
            return;
        }
        long period = ScoreboardConfig.getUpdateInterval();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, period);
        for (Player p : Bukkit.getOnlinePlayers()) apply(p);
    }

    public void stop() {
        if (task != null) task.cancel();
        for (Player p : Bukkit.getOnlinePlayers()) {
            try { p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard()); } catch (Throwable ignored) {}
        }
    }

    public void reload() {
        stop();
        hidden.clear();
        using.clear();
        HF_ERROR_LOGGED.set(false);
        ScoreboardConfig.load(plugin);
        start();
    }

    public void apply(Player player) {
        if (player == null || !player.isOnline()) return;
        if (hidden.contains(player.getUniqueId())) {
            try { player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard()); } catch (Throwable ignored) {}
            return;
        }
        ScoreboardConfig.BoardDef def = pickBoard(player);
        if (def == null) return;
        render(player, def);
        using.put(player.getUniqueId(), def.id);
    }

    private void tick() {
        if (!ScoreboardConfig.isEnabled()) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == null || !p.isOnline()) continue;
            apply(p);
            applyHf(p);
        }
    }

    private ScoreboardConfig.BoardDef pickBoard(Player player) {
        for (ScoreboardConfig.BoardDef def : ScoreboardConfig.allBoards()) {
            if (matchesCondition(player, def.condition)) return def;
        }
        return ScoreboardConfig.getBoard(ScoreboardConfig.getDefaultBoard());
    }

    private boolean matchesCondition(Player player, String condition) {
        if (condition == null || condition.isEmpty() || condition.equalsIgnoreCase("true")) return true;
        String c = condition.trim();
        if (c.startsWith("permission:")) {
            String perm = c.substring("permission:".length()).trim();
            return player.hasPermission(perm);
        }
        if (c.equalsIgnoreCase("op")) return player.isOp();
        return true;
    }

    private void render(Player player, ScoreboardConfig.BoardDef def) {
        try {
            org.bukkit.scoreboard.Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective obj = sb.registerNewObjective("ldsb", "dummy");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);

            String title = PapiUtil.parse(player, def.title);
            title = ChatColor.translateAlternateColorCodes('&', title);
            if (title.length() > 32) title = title.substring(0, 32);
            obj.setDisplayName(title);

            List<String> lines = def.lines;
            int count = Math.min(lines.size(), 15);

            for (int i = 0; i < count; i++) {
                String raw = lines.get(i);
                String parsed = PapiUtil.parse(player, raw);
                parsed = ChatColor.translateAlternateColorCodes('&', parsed);

                Team team = sb.registerNewTeam("ld_line_" + i);
                String entry = ChatColor.values()[i].toString() + ChatColor.RESET;
                team.addEntry(entry);

                if (parsed.length() <= 16) {
                    team.setPrefix(parsed);
                } else {
                    String pfx = parsed.substring(0, 16);
                    String sfx = parsed.length() > 32 ? parsed.substring(16, 32) : parsed.substring(16);
                    team.setPrefix(pfx);
                    team.setSuffix(sfx);
                }

                obj.getScore(entry).setScore(count - i);
            }

            player.setScoreboard(sb);
        } catch (Throwable t) {
            plugin.getLogger().warning("[Scoreboard] 渲染失败: " + t.getMessage());
        }
    }

    private void applyHf(Player player) {
        if (!ScoreboardConfig.isHfEnabled()) return;
        try {
            List<String> hLines = ScoreboardConfig.getHeader();
            List<String> fLines = ScoreboardConfig.getFooter();

            StringBuilder h = new StringBuilder();
            for (int i = 0; i < hLines.size(); i++) {
                if (i > 0) h.append("\n");
                h.append(PapiUtil.parse(player, hLines.get(i)));
            }
            StringBuilder f = new StringBuilder();
            for (int i = 0; i < fLines.size(); i++) {
                if (i > 0) f.append("\n");
                f.append(PapiUtil.parse(player, fLines.get(i)));
            }

            net.md_5.bungee.api.chat.BaseComponent[] header =
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(h.toString());
            net.md_5.bungee.api.chat.BaseComponent[] footer =
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(f.toString());

            player.setPlayerListHeaderFooter(header, footer);
        } catch (Throwable t) {
            if (HF_ERROR_LOGGED.compareAndSet(false, true)) {
                plugin.getLogger().warning("[Scoreboard] header/footer 失败: " + t.getClass().getSimpleName() + " - " + t.getMessage());
            }
        }
    }

    private static String getNmsVersion
() {
        if (nmsVersion == null) {
            try {
                String pkg = Bukkit.getServer().getClass().getPackage().getName();
                nmsVersion = pkg.substring(pkg.lastIndexOf('.') + 1);
            } catch (Throwable t) {
                nmsVersion = "v1_12_R1";
            }
        }
        return nmsVersion;
    }

    /**
     * 1.12 NMS：PacketPlayOutPlayerListHeaderFooter 无参构造 + 字段 a(header) b(footer)
     * 返回 true 表示发送成功
     */
    private static boolean sendTabPacket(Player player, String header, String footer) {
        // 已报过错就不再试（避免刷屏）
        if (HF_ERROR_LOGGED.get()) return false;

        try {
            String v = getNmsVersion();

            Class<?> chatSerializer = Class.forName("net.minecraft.server." + v + ".IChatBaseComponent$ChatSerializer");
            Class<?> packetClass = Class.forName("net.minecraft.server." + v + ".PacketPlayOutPlayerListHeaderFooter");
            Class<?> packetBase = Class.forName("net.minecraft.server." + v + ".Packet");

            Object headerComp = chatSerializer.getMethod("a", String.class).invoke(null, toJson(header));
            Object footerComp = chatSerializer.getMethod("a", String.class).invoke(null, toJson(footer));

            // 无参构造
            Object packet = packetClass.getConstructor().newInstance();

            // 尝试设置字段（1.12 用的是 a 和 b）
            try { packetClass.getField("a").set(packet, headerComp); } catch (Throwable ignored) {}
            try { packetClass.getField("b").set(packet, footerComp); } catch (Throwable ignored) {}

            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object conn = handle.getClass().getField("playerConnection").get(handle);
            conn.getClass().getMethod("sendPacket", packetBase).invoke(conn, packet);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String toJson(String s) {
        if (s == null) s = "";
        StringBuilder sb = new StringBuilder("{\"text\":\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) sb.append(' ');
                    else sb.append(c);
            }
        }
        sb.append("\"}");
        return sb.toString();
    }

    public boolean toggle(Player player) {
        if (!ScoreboardConfig.isAllowToggle()) return false;
        UUID id = player.getUniqueId();
        if (hidden.contains(id)) {
            hidden.remove(id);
            apply(player);
            return true;
        } else {
            hidden.add(id);
            try { player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard()); } catch (Throwable ignored) {}
            return false;
        }
    }

    public boolean isHidden(Player player) {
        return hidden.contains(player.getUniqueId());
    }

    public void onQuit(Player player) {
        if (player == null) return;
        hidden.remove(player.getUniqueId());
        using.remove(player.getUniqueId());
    }
}