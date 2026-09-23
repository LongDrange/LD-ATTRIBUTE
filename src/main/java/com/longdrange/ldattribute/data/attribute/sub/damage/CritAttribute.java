package com.longdrange.ldattribute.data.attribute.sub.damage;

import com.longdrange.ldattribute.data.attribute.LDAttributeType;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import com.longdrange.ldattribute.data.eventdata.LDEventData;
import com.longdrange.ldattribute.data.eventdata.LDDamageEventData;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * 暴擊屬性
 * 效果：攻擊時有機率造成額外倍率傷害
 *
 * Lore 格式：暴擊: 30/2.0
 *   30  = 暴擊機率（%）
 *   2.0 = 暴擊倍率
 *
 * 例如：暴擊: 30/2.0 → 30% 機率造成 2 倍傷害
 */
public class CritAttribute extends LDSubAttribute {

    public CritAttribute() {
        super("暴擊", 2, LDAttributeType.ATTACK);
    }

    @Override
    public void eventMethod(LDEventData data) {
        if (!(data instanceof LDDamageEventData)) return;
        LDDamageEventData damageData = (LDDamageEventData) data;

        double chance = getAttributes()[0];
        double multiplier = getAttributes()[1];
        if (chance <= 0 || multiplier <= 1) return;

        // 機率判定
        if (probability(chance)) {
            double newDamage = damageData.getDamage() * multiplier;
            damageData.setDamage(newDamage);
        }
    }

    @Override
    public boolean loadAttribute(String str) {
        if (str == null) return false;
        if (!com.longdrange.ldattribute.util.LanguageManager.matches(str, getName())) return false;
        try {
            // 格式：暴擊: 30/2.0
            String nums = getNumber(str);
            // getNumber 只會抓第一段數字，這裡需要自己解析
            String[] parts = str.replaceAll("\u00a7+[a-z0-9]", "")
                                 .replaceAll("[^0-9./]", "")
                                 .split("/");
            if (parts.length < 2) return false;
            double chance = Double.parseDouble(parts[0]);
            double multiplier = Double.parseDouble(parts[1]);
            if (chance == 0 && multiplier == 0) return false;
            setAttributes(chance, multiplier);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public double getValue() {
        // 代表值用「機率 × 倍率」來排序
        return getAttributes()[0] * getAttributes()[1];
    }

    @Override
    public String getPlaceholder(Player player, String params) {
        if ("chance".equalsIgnoreCase(params)) {
            return getDf().format(getAttributes()[0]);
        }
        if ("multiplier".equalsIgnoreCase(params)) {
            return getDf().format(getAttributes()[1]);
        }
        return getDf().format(getAttributes()[0]) + "/" + getDf().format(getAttributes()[1]);
    }

    @Override
    public List<String> getPlaceholders() {
        return Collections.singletonList("暴擊");
    }
}