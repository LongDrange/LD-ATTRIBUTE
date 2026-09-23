package com.longdrange.ldattribute.data.attribute.sub.other;

import com.longdrange.ldattribute.data.attribute.LDAttributeType;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import com.longdrange.ldattribute.data.eventdata.LDEventData;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * 經驗加成屬性
 * 效果：玩家獲得經驗時，經驗值乘以 (1 + 加成%)
 *
 * 注意：這個屬性的 eventMethod 不會被 OnDamageListener 呼叫，
 *       而是由 OnExpListener 直接讀取數值來使用
 *
 * Lore 格式：經驗加成: 50
 *   50 = 額外加成 50%
 */
public class ExpAdditionAttribute extends LDSubAttribute {

    public ExpAdditionAttribute() {
        super("經驗加成", 1, LDAttributeType.OTHER);
    }

    @Override
    public void eventMethod(LDEventData data) {
        // 由 OnExpListener 直接讀取數值，這裡不做事
    }

    /**
     * 取得加成倍率（例如 50 → 1.5）
     */
    public double getMultiplier() {
        return 1.0 + getAttributes()[0] / 100.0;
    }

    @Override
    public boolean loadAttribute(String str) {
        if (str == null) return false;
        if (!com.longdrange.ldattribute.util.LanguageManager.matches(str, getName())) return false;
        try {
            double value = Double.parseDouble(getNumber(str));
            if (value == 0) return false;
            setAttributes(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public double getValue() {
        return getAttributes()[0];
    }

    @Override
    public String getPlaceholder(Player player, String params) {
        return getDf().format(getAttributes()[0]) + "%";
    }

    @Override
    public List<String> getPlaceholders() {
        return Collections.singletonList("經驗加成");
    }
}