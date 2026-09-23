package com.longdrange.ldattribute.util;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;

public class BackupManager {

    private static LDAttribute plugin;
    private static File backupDir;
    private static int maxBackups = 24;
    private static int intervalMinutes = 60;

    public static void init(LDAttribute pl) {
        plugin = pl;
        backupDir = new File(pl.getDataFolder(), "backups");
        if (!backupDir.exists()) backupDir.mkdirs();

        try {
            maxBackups = pl.getConfig().getInt("Backup.MaxBackups", 24);
            intervalMinutes = pl.getConfig().getInt("Backup.IntervalMinutes", 60);
        } catch (Throwable ignored) {}
        if (maxBackups < 1) maxBackups = 24;
        if (intervalMinutes < 5) intervalMinutes = 60;

        long intervalTicks = 20L * 60L * intervalMinutes;
        Bukkit.getScheduler().runTaskTimerAsynchronously(pl, () -> {
            try {
                runBackup();
            } catch (Throwable t) {
                pl.getLogger().warning("自动备份失败: " + t.getMessage());
            }
        }, intervalTicks, intervalTicks);

        pl.getLogger().info("已启用自动备份（每 " + intervalMinutes + " 分钟，保留 " + maxBackups + " 份）");
    }

    public static File runBackup() {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File target = new File(backupDir, ts);
        target.mkdirs();

        int count = 0;
        File dataFolder = plugin.getDataFolder();

        // 1. 根目录所有 *.yml 和 *.dat（跳过 backups 目录本身）
        File[] rootFiles = dataFolder.listFiles();
        if (rootFiles != null) {
            for (File f : rootFiles) {
                if (f.isDirectory()) continue;
                if (!f.getName().endsWith(".yml") && !f.getName().endsWith(".dat")) continue;
                if (copy(f, new File(target, f.getName()))) count++;
            }
        }

        // 2. data/ 目录下所有文件
        File dataDir = new File(dataFolder, "data");
        if (dataDir.exists() && dataDir.isDirectory()) {
            File dataTarget = new File(target, "data");
            dataTarget.mkdirs();
            File[] dataFiles = dataDir.listFiles();
            if (dataFiles != null) {
                for (File f : dataFiles) {
                    if (!f.isFile()) continue;
                    if (copy(f, new File(dataTarget, f.getName()))) count++;
                }
            }
        }

        // 3. 清理旧备份
        cleanup();

        plugin.getLogger().info("已备份 " + count + " 个文件 -> backups/" + ts);
        return target;
    }

    private static boolean copy(File src, File dst) {
        try {
            Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static void cleanup() {
        File[] backups = backupDir.listFiles(File::isDirectory);
        if (backups == null) return;
        if (backups.length <= maxBackups) return;
        Arrays.sort(backups, Comparator.comparing(File::getName));
        int toDelete = backups.length - maxBackups;
        for (int i = 0; i < toDelete; i++) {
            deleteDir(backups[i]);
        }
        plugin.getLogger().info("已清理 " + toDelete + " 份旧备份");
    }

    private static void deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDir(f);
                else f.delete();
            }
        }
        dir.delete();
    }

    public static File getBackupDir() { return backupDir; }
    public static int getMaxBackups() { return maxBackups; }
    public static int getIntervalMinutes() { return intervalMinutes; }
}