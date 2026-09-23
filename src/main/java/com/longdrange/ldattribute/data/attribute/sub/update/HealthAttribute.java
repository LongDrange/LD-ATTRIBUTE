package com.longdrange.ldattribute.data.attribute.sub.update;

import com.longdrange.ldattribute.data.attribute.LDAttributeType;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import com.longdrange.ldattribute.data.eventdata.LDEventData;
import com.longdrange.ldattribute.data.eventdata.LDUpdateEventData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * 生命上限屬性
 * 效果：提高最大血量
 *
 * Lore 格式：生命上限: 20
 *   20 = 額外增加的血量
 */
public class HealthAttribute extends LDSubAttribute {

    public HealthAttribute() {
        super("生命上限", 1, LDAttributeType.UPDATE);
    }

    @Override
    public void eventMethod(LDEventData data) {
        if (!(data instanceof LDUpdateEventData)) return;
        LDUpdateEventData updateData = (LDUpdateEventData) data;
        LivingEntity entity = updateData.getEntity();
        double bonus = getAttributes()[0];
        if (bonus <= 0) return;

        double newMax = entity.getMaxHealth() + bonus;
        entity.setMaxHealth(newMax);
        entity.setHealth(Math.min(entity.getHealth(), newMax));
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
        return Collections.singletonList("生命上限");
    }
}