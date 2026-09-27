package com.longdrange.ldattribute.core.soulring;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.entity.Player;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 灵魂空间操作日志（按玩家分文件）
 * 输出到 plugins/LD-Attribute/logs/soulring/玩家名.log
 * 每个文件首行记录玩家 UUID
 */
public class SoulRingLog {

    private static File dir;
    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> initialized = new HashSet<>();

    public static void init(LDAttribute plugin) {
        try {
            dir = new File(plugin.getDataFolder(), "logs/soulring");
            if (!dir.exists()) dir.mkdirs();
        } catch (Throwable ignored) {}
    }

    /**
     * 写入日志
     * @param player 玩家
     * @param action 动作（Deposit / Withdraw / Delete / Decompose / ModeChange）
     * @param detail 详情
     */
    public static void write(Player player, String action, String detail) {
        if (dir == null || player == null) return;

        String name = player.getName();
        UUID uuid = player.getUniqueId();
        File file = new File(dir, name + ".log");

        // 首次写入：加头部（UUID + 创建时间）
        boolean isNew = !file.exists() || !initialized.contains(name);
        if (isNew) {
            try {
                if (!file.exists()) {
                    String header = "# Player: " + name + "\n"
                            + "# UUID: " + uuid.toString() + "\n"
                            + "# Created: " + FMT.format(new Date()) + "\n\n";
                    Files.write(file.toPath(), header.getBytes(StandardCharsets.UTF_8),
                            StandardOpenOption.CREATE);
                }
                initialized.add(name);
            } catch (Throwable ignored) {}
        }

        // 追加日志行
        String time = FMT.format(new Date());
        String line = "[" + time + "] " + action + " | " + detail;
        try {
            Files.write(file.toPath(),
                    (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable ignored) {}
    }

    /** 根据玩家名或 UUID 查找日志文件 */
    public static File findLogFile(String key) {
        if (dir == null || key == null || key.isEmpty()) return null;
        File direct = new File(dir, key + ".log");
        if (direct.exists()) return direct;

        // 按 UUID 查
        UUID uuid = null;
        try { uuid = UUID.fromString(key); } catch (Throwable ignored) {}
        if (uuid != null) {
            File[] files = dir.listFiles((d, n) -> n.endsWith(".log"));
            if (files != null) {
                String uuidStr = uuid.toString();
                for (File f : files) {
                    try {
                        List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
                        for (String l : lines) {
                            if (l.startsWith("# UUID: ") && l.substring(8).trim().equalsIgnoreCase(uuidStr)) {
                                return f;
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        }
        return null;
    }

    /** 读取最后 N 行 */
    public static List<String> tail(File file, int n) {
        List<String> out = new ArrayList<>();
        if (file == null || !file.exists()) return out;
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            int from = Math.max(0, lines.size() - n);
            for (int i = from; i < lines.size(); i++) out.add(lines.get(i));
        } catch (Throwable ignored) {}
        return out;
    }

    public static File getDir() { return dir; }
}