package com.longdrange.ldattribute.data.attribute.sub.other;

import com.longdrange.ldattribute.data.attribute.LDAttributeType;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import com.longdrange.ldattribute.data.eventdata.LDEventData;
import com.longdrange.ldattribute.util.LanguageManager;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * 爆率属性
 *  - 影响掉落概率（额外掉落份数）
 *  - 值格式：百分数（如 50 表示 +50% 爆率）
 *  - 效果：每个掉落物品，额外掉落「爆率/100」份（小数部分按概率）
 *
 * 用法：装备/魂珠 Lore 写「爆率: +50」
 */
public class DropRateAttribute extends LDSubAttribute {

    public DropRateAttribute() {
        super("爆率", 1, LDAttributeType.OTHER);
    }

    @Override
    public void eventMethod(LDEventData data) {
        // 纯数值存储，由掉落监听器读取
    }

    @Override
    public boolean loadAttribute(String str) {
        if (str == null) return false;
        // 支持多种别名
        if (!LanguageManager.matches(str, getName())
                && !LanguageManager.matches(str, "掉落几率")
                && !LanguageManager.matches(str, "掉宝率")
                && !LanguageManager.matches(str, "掉落率")) return false;
        try {
            double value = Double.parseDouble(getNumber(str));
            if (value == 0) return false;
            setAttributes(value);
            return true;
        } catch (NumberFormatException e) { return false; }
    }

    @Override
    public double getValue() { return getAttributes()[0]; }

    @Override
    public String getPlaceholder(Player player, String params) {
        return getDf().format(getAttributes()[0]) + "%";
    }

    @Override
    public List<String> getPlaceholders() {
        return Arrays.asList("爆率", "掉落几率", "掉宝率", "掉落率");
    }
}