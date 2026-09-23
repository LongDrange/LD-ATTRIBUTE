package com.longdrange.ldattribute.data.attribute.sub.damage;

import com.longdrange.ldattribute.data.attribute.LDAttributeType;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import com.longdrange.ldattribute.data.eventdata.LDEventData;
import com.longdrange.ldattribute.data.eventdata.LDDamageEventData;
import com.longdrange.ldattribute.util.LanguageManager;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * 暴擊機率屬性
 * 效果：攻擊時 N% 機率觸發暴擊
 *
 * Lore 格式：暴擊機率: 5
 *   5 = 暴擊機率（%）
 */
public class CritChanceAttribute extends LDSubAttribute {

    public CritChanceAttribute() {
        super("暴擊機率", 1, LDAttributeType.ATTACK);
    }

    @Override
    public void eventMethod(LDEventData data) {
        if (!(data instanceof LDDamageEventData)) return;
        LDDamageEventData damageData = (LDDamageEventData) data;
        double chance = getAttributes()[0];
        if (chance <= 0) return;

        // 受害者暴击抵抗（乘法削弱）
        double resist = 0;
        try {
            if (damageData.getVictimData() != null) {
                LDSubAttribute r = damageData.getVictimData().getSubAttribute("暴击抵抗");
                if (r != null) resist = r.getValue();
            }
        } catch (Throwable ignored) {}
        double actualChance = chance * (1.0 - Math.min(resist, 100) / 100.0);
        if (actualChance <= 0) return;

        if (probability(actualChance)) {
            damageData.setCritical(true);
        }
    }

    @Override
    public boolean loadAttribute(String str) {
        if (str == null) return false;
        if (!LanguageManager.matches(str, getName())) return false;
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
    public double getValue() { return getAttributes()[0]; }

    @Override
    public String getPlaceholder(Player player, String params) {
        return getDf().format(getAttributes()[0]) + "%";
    }

    @Override
    public List<String> getPlaceholders() {
        return Collections.singletonList("暴擊機率");
    }
}