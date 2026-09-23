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
 * 破甲屬性
 * 效果：攻擊時 N% 機率觸發，觸發時削減目標 M% 防御力
 *
 * Lore 格式：
 *   破甲: 10/50   → 10% 機率，削減 50% 防御
 *   破甲: 10      → 10% 機率，完全無視防御（100%）
 */
public class ArmorPenAttribute extends LDSubAttribute {

    public ArmorPenAttribute() {
        super("破甲", 2, LDAttributeType.ATTACK);
    }

    @Override
    public void eventMethod(LDEventData data) {
        if (!(data instanceof LDDamageEventData)) return;
        LDDamageEventData damageData = (LDDamageEventData) data;
        double chance = getAttributes()[0];
        if (chance <= 0) return;

        if (probability(chance)) {
            double percent = getAttributes()[1];
            if (percent <= 0) percent = 100;
            damageData.setArmorPenPercent(percent);
        }
    }

    @Override
    public boolean loadAttribute(String str) {
        if (str == null) return false;
        if (!LanguageManager.matches(str, getName())) return false;
        try {
            String[] parts = str.replaceAll("\u00a7+[a-z0-9]", "")
                                 .replaceAll("[^0-9./]", "")
                                 .split("/");
            if (parts.length < 1) return false;
            double chance = Double.parseDouble(parts[0]);
            double percent = parts.length >= 2 ? Double.parseDouble(parts[1]) : 100;
            if (chance == 0) return false;
            setAttributes(chance, percent);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public double getValue() { return getAttributes()[0]; }

    @Override
    public String getPlaceholder(Player player, String params) {
        return getDf().format(getAttributes()[0]) + "%/" + getDf().format(getAttributes()[1]) + "%";
    }

    @Override
    public List<String> getPlaceholders() {
        return Collections.singletonList("破甲");
    }
}