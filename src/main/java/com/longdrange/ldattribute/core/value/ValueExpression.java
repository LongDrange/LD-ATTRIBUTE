package com.longdrange.ldattribute.core.value;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.entity.Player;

/**
 * 表达式 API（供条件/动作使用）
 *
 * 例：
 *   ValueExpression.look(player, "金币")         → 当前值
 *   ValueExpression.add(player, "金币", 100)     → 加
 *   ValueExpression.take(player, "金币", 100)    → 扣（不足则不扣，返回 false）
 *   ValueExpression.set(player, "金币", 500)     → 设
 *   ValueExpression.has(player, "金币", 1000)    → 是否 >= 1000
 */
public class ValueExpression {

    public static double look(Player player, String id) {
        if (player == null) return 0;
        try {
            return LDAttribute.getInstance().getValueManager().get(player).get(id);
        } catch (Throwable t) { return 0; }
    }

    public static void add(Player player, String id, double amount) {
        if (player == null || amount == 0) return;
        try {
            LDAttribute.getInstance().getValueManager().get(player).add(id, amount);
        } catch (Throwable ignored) {}
    }

    /** 扣除，不足则失败返回 false */
    public static boolean take(Player player, String id, double amount) {
        if (player == null || amount <= 0) return false;
        try {
            ValueData d = LDAttribute.getInstance().getValueManager().get(player);
            double cur = d.get(id);
            if (cur < amount) return false;
            d.take(id, amount);
            return true;
        } catch (Throwable t) { return false; }
    }

    public static void set(Player player, String id, double amount) {
        if (player == null) return;
        try {
            LDAttribute.getInstance().getValueManager().get(player).set(id, amount);
        } catch (Throwable ignored) {}
    }

    public static boolean has(Player player, String id, double need) {
        return look(player, id) >= need;
    }
}