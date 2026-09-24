package com.longdrange.ldattribute.util;

import org.bukkit.command.CommandSender;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 管理员操作审计日志
 * 输出到 plugins/LD-Attribute/logs/admin.log
 */
public class AuditLog {
    private static File logFile;
    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void init(File dataFolder) {
        try {
            File dir = new File(dataFolder, "logs");
            if (!dir.exists()) dir.mkdirs();
            logFile = new File(dir, "admin.log");
        } catch (Throwable ignored) {}
    }

    public static void write(CommandSender sender, String action, String detail) {
        if (logFile == null || sender == null) return;
        String time = FMT.format(new Date());
        String who = sender.getName();
        String line = "[" + time + "] " + who + " | " + action + " | " + detail;
        try {
            Files.write(logFile.toPath(),
                    (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable ignored) {}
    }
}