package com.longdrange.ldattribute.data.attribute.sub.damage;

import com.longdrange.ldattribute.data.attribute.LDAttributeType;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import com.longdrange.ldattribute.data.eventdata.LDEventData;
import com.longdrange.ldattribute.data.eventdata.LDDamageEventData;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * 命中率屬性
 * 效果：攻擊時 N% 機率命中，否則傷害歸 0
 *
 * Lore 格式：命中率: 80
 *   80 = 命中機率（%）
 *
 * 注意：若沒此屬性，默認 100% 命中
 */
public class HitRateAttribute extends LDSubAttribute {

    public HitRateAttribute() {
        super("命中率", 1, LDAttributeType.ATTACK);
    }

    @Override
    public void eventMethod(LDEventData data) {
        if (!(data instanceof LDDamageEventData)) return;
        LDDamageEventData damageData = (LDDamageEventData) data;
        double chance = getAttributes()[0];
        if (chance <= 0 || chance >= 100) return;

        if (!probability(chance)) {
            // 未命中 → 傷害歸 0
            damageData.setDamage(0);
        }
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
        return Collections.singletonList("命中率");
    }
}