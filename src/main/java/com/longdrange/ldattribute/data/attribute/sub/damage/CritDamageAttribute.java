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
 * 暴擊傷害屬性
 * 效果：當暴擊觸發時，傷害 × (1 + N/100)
 *
 * Lore 格式：暴擊傷害: 50
 *   50 → 傷害 × 1.5
 *   100 → 傷害 × 2.0
 */
public class CritDamageAttribute extends LDSubAttribute {

    public CritDamageAttribute() {
        super("暴擊傷害", 1, LDAttributeType.ATTACK);
    }

    @Override
    public void eventMethod(LDEventData data) {
        if (!(data instanceof LDDamageEventData)) return;
        LDDamageEventData damageData = (LDDamageEventData) data;
        if (!damageData.isCritical()) return;  // 沒暴擊就不加成

        double bonus = getAttributes()[0];
        if (bonus <= 0) return;

        double newDamage = damageData.getDamage() * (1.0 + bonus / 100.0);
        damageData.setDamage(newDamage);
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
        return Collections.singletonList("暴擊傷害");
    }
}