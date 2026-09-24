package com.longdrange.ldattribute.util;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

/**
 * 配置路径解析器
 * 新路径：plugins/LD-Attribute/配置/<分类>/<文件名>
 * 旧路径：plugins/LD-Attribute/<文件名>（兼容，首次会自动迁移）
 */
public class ConfigPaths {

    private static final String ROOT = "配置";

    public static File resolve(JavaPlugin plugin, String fileName) {
        String sub = getSubDir(fileName);
        File newFile = new File(plugin.getDataFolder(), ROOT + "/" + sub + "/" + fileName);

        // 1. 新路径存在 → 直接用
        if (newFile.exists()) return newFile;

        // 2. 旧路径存在 → 迁移
        File oldFile = new File(plugin.getDataFolder(), fileName);
        if (oldFile.exists()) {
            try {
                File parent = newFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                if (oldFile.renameTo(newFile)) return newFile;
            } catch (Throwable ignored) {}
            return oldFile;
        }

        // 3. 都没有 → 从 jar 释放到根目录
        try { plugin.saveResource(fileName, false); } catch (Throwable ignored) {}

        // 4. 尝试迁移到新路径
        if (oldFile.exists()) {
            try {
                File parent = newFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                if (oldFile.renameTo(newFile)) return newFile;
            } catch (Throwable ignored) {}
            return oldFile;
        }

        // 5. 兜底
        if (newFile.exists()) return newFile;
        return oldFile;
    }

    public static String getSubDir(String fileName) {
        switch (fileName) {
            case "item.yml":
            case "cardlevel.yml":
            case "star.yml":
            case "decompose.yml":
            case "suit.yml":
            case "combo.yml":
            case "resonance.yml":
            case "bond.yml":
            case "collection.yml":
            case "recipe.yml":
                return "卡片";
            case "element.yml":
            case "state.yml":
            case "tempbuff.yml":
                return "战斗";
            case "pet.yml":
            case "pet_equipment.yml":
                return "宠物";
            case "rune.yml":
            case "spells.yml":
                return "符文";
            case "achievement.yml":
            case "gacha.yml":
            case "mm.yml":
                return "玩法";
            case "ui.yml":
            case "stats_gui.yml":
            case "page.yml":
                return "界面";
            case "config.yml":
            case "command.yml":
                return "系统";
            default:
                return "其他";
        }
    }
}