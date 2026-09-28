package com.longdrange.ldattribute.util;

import java.text.DecimalFormat;

/**
 * 数字缩写工具
 *   1000      → 1千
 *   15000     → 1.5万
 *   12345678  → 1234.5万  (或 1.23亿，看配置)
 *   1.5e9     → 15亿
 *
 * 支持配置：
 *   - 启用/禁用
 *   - 每级单位阈值（默认：万=1e4, 亿=1e8, 兆=1e12）
 *   - 保留小数位
 */
public class NumberAbbrev {

    /** 是否启用缩写 */
    private static boolean enabled = true;
    /** 万阈值 */
    private static double wan = 1_0000;
    /** 亿阈值 */
    private static double yi = 1_0000_0000;
    /** 兆阈值 */
    private static double zhao = 1_0000_0000_0000L;
    /** 保留小数位 */
    private static int decimals = 2;

    private static final DecimalFormat DF;

    static {
        StringBuilder sb = new StringBuilder("0");
        for (int i = 0; i < decimals; i++) {
            if (i == 0) sb.append(".");
            sb.append("#");
        }
        DF = new DecimalFormat(sb.toString());
    }

    public static void setEnabled(boolean b) { enabled = b; }
    public static void setDecimals(int d) {
        if (d < 0) d = 0;
        if (d > 4) d = 4;
        decimals = d;
    }
    public static void setThresholds(double wanV, double yiV, double zhaoV) {
        wan = wanV;
        yi = yiV;
        zhao = zhaoV;
    }

    /**
     * 格式化数字
     * 小于「万」的直接显示（保留原始整数/小数）
     */
    public static String format(double v) {
        if (!enabled) return plain(v);

        double abs = Math.abs(v);
        if (abs < wan) return plain(v);

        if (abs >= zhao) {
            return round(v / zhao) + "兆";
        } else if (abs >= yi) {
            return round(v / yi) + "亿";
        } else {
            return round(v / wan) + "万";
        }
    }

    /** 简写整型 */
    public static String format(long v) {
        return format((double) v);
    }

    /** 不缩写，仅做基础格式化 */
    private static String plain(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return String.valueOf((long) v);
        }
        return DF.format(v);
    }

    /** 保留小数位 */
    private static String round(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            // 整数值（如 2.0万）→ 显示 2万 而不是 2.00万
            return String.valueOf((long) v);
        }
        return DF.format(v);
    }
}