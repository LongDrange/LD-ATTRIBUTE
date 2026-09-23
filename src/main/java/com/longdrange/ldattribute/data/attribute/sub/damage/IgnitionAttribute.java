package com.longdrange.ldattribute.data.attribute.sub.damage;

import com.longdrange.ldattribute.data.attribute.LDAttributeType;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import com.longdrange.ldattribute.data.eventdata.LDEventData;
import com.longdrange.ldattribute.data.eventdata.LDDamageEventData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * 點燃屬性
 * 效果：攻擊時，讓目標著火 N 秒
 *
 * Lore 格式：點燃: 5
 *   5 = 著火秒數
 */
public class IgnitionAttribute extends LDSubAttribute {

    public IgnitionAttribute() {
        super("點燃", 1, LDAttributeType.ATTACK);
    }

    @Override
    public void eventMethod(LDEventData data) {
        if (!(data instanceof LDDamageEventData)) return;
        LDDamageEventData damageData = (LDDamageEventData) data;

        double seconds = getAttributes()[0];
        if (seconds <= 0) return;

        LivingEntity victim = damageData.getVictim();
        int ticks = (int) (seconds * 20);
        if (victim.getFireTicks() < ticks) {
            victim.setFireTicks(ticks);
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
        return getDf().format(getAttributes()[0]) + "s";
    }

    @Override
    public List<String> getPlaceholders() {
        return Collections.singletonList("點燃");
    }
}