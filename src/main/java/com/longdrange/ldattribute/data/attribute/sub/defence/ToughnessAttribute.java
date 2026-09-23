package com.longdrange.ldattribute.data.attribute.sub.defence;

import com.longdrange.ldattribute.data.attribute.LDAttributeType;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import com.longdrange.ldattribute.data.eventdata.LDEventData;
import com.longdrange.ldattribute.data.eventdata.LDDamageEventData;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * 韌性屬性
 * 效果：減少「額外傷害加成」對自己造成的傷害
 *       主要用於減少暴擊倍率帶來的額外傷害
 *
 * 公式：額外傷害部分 = 原傷害 × (1 - 韌性 / (韌性 + 100))
 *
 * 簡化實作：直接當作第二層減傷
 *
 * Lore 格式：韌性: 50
 */
public class ToughnessAttribute extends LDSubAttribute {

    public ToughnessAttribute() {
        super("韌性", 1, LDAttributeType.DEFENSE);
    }

    @Override
    public void eventMethod(LDEventData data) {
        if (!(data instanceof LDDamageEventData)) return;
        LDDamageEventData damageData = (LDDamageEventData) data;
        double toughness = getAttributes()[0];
        if (toughness <= 0) return;

        // 減傷比例上限 80%
        double reduction = Math.min(0.8, toughness / (toughness + 100.0));
        double newDamage = damageData.getDamage() * (1 - reduction);
        damageData.setDamage(Math.max(0, newDamage));
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
        return getDf().format(getAttributes()[0]);
    }

    @Override
    public List<String> getPlaceholders() {
        return Collections.singletonList("韌性");
    }
}