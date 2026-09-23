package com.longdrange.ldattribute.data.attribute.sub.damage;

import com.longdrange.ldattribute.data.attribute.LDAttributeType;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import com.longdrange.ldattribute.data.eventdata.LDEventData;
import com.longdrange.ldattribute.data.eventdata.LDDamageEventData;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * 真實傷害屬性
 * 效果：在最終傷害上額外增加（不被暴擊倍率放大）
 *
 * Lore 格式：真實傷害: 50
 */
public class RealAttribute extends LDSubAttribute {

    public RealAttribute() {
        super("真實傷害", 1, LDAttributeType.ATTACK);
    }

    @Override
    public void eventMethod(LDEventData data) {
        if (!(data instanceof LDDamageEventData)) return;
        LDDamageEventData damageData = (LDDamageEventData) data;
        double value = getAttributes()[0];
        if (value <= 0) return;
        damageData.addDamage(value);
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
        return Collections.singletonList("真實傷害");
    }
}