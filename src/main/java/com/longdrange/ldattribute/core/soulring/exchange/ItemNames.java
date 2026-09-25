package com.longdrange.ldattribute.core.soulring.exchange;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * 拿物品的中文显示名
 *  - 有自定义 DisplayName → 用 DisplayName
 *  - 否则反射 NMS 的 ItemStack.getName()（返回本地化中文）
 *  - 失败退回材质名美化
 */
public class ItemNames {

    private static String nmsVersion = null;
    private static boolean nmsAvailable = true;

    private static String getNmsVersion() {
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

    /** 拿物品显示名（本地化） */
    public static String getDisplayName(ItemStack item) {
        if (item == null) return "未知物品";

        // 1. 自定义 DisplayName
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }

        // 2. NMS 反射拿本地化名
        if (nmsAvailable) {
            try {
                String v = getNmsVersion();
                Class<?> craftStack = Class.forName("org.bukkit.craftbukkit." + v + ".inventory.CraftItemStack");
                Object nms = craftStack.getMethod("asNMSCopy", ItemStack.class).invoke(null, item);
                if (nms != null) {
                    Method getName = nms.getClass().getMethod("getName");
                    Object name = getName.invoke(nms);
                    if (name != null) {
                        String s = name.toString();
                        if (!s.isEmpty()) return s;
                    }
                }
            } catch (Throwable t) {
                nmsAvailable = false;
            }
        }

        // 3. fallback：材质名美化
        return fallback(item.getType());
    }

    public static String getDisplayName(Material mat, short data) {
        if (mat == null) return "未知";
        try {
            ItemStack stack = new ItemStack(mat, 1, data);
            return getDisplayName(stack);
        } catch (Throwable t) {
            return fallback(mat);
        }
    }

    public static String fallback(Material mat) {
        if (mat == null) return "未知";
        String s = mat.name();
        String[] parts = s.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    /** 去掉颜色码 */
    public static String strip(String s) {
        return s == null ? "" : ChatColor.stripColor(s);
    }
}