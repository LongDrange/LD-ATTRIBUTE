package com.longdrange.ldattribute.data.attribute.sub.update;

import com.longdrange.ldattribute.data.attribute.LDAttributeType;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import com.longdrange.ldattribute.data.eventdata.LDEventData;
import com.longdrange.ldattribute.data.eventdata.LDUpdateEventData;
import com.longdrange.ldattribute.util.LanguageManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * 速度百分比屬性
 * 效果：以百分比方式提高移動速度（基準 0.2）
 *
 * Lore 格式：速度百分比: 3
 *   3  → 速度 +3%（實際 +0.006）
 *   10 → 速度 +10%（實際 +0.02）
 */
public class SpeedPercentAttribute extends LDSubAttribute {

    public SpeedPercentAttribute() {
        super("速度百分比", 1, LDAttributeType.UPDATE);
    }

    @Override
    public void eventMethod(LDEventData data) {
        if (!(data instanceof LDUpdateEventData)) return;
        LDUpdateEventData updateData = (LDUpdateEventData) data;
        LivingEntity entity = updateData.getEntity();
        if (!(entity instanceof Player)) return;

        double percent = getAttributes()[0];
        if (percent <= 0) return;

        Player player = (Player) entity;
        float bonus = (float) (0.2 * percent / 100.0);
        float newSpeed = player.getWalkSpeed() + bonus;
        if (newSpeed > 1.0f) newSpeed = 1.0f;
        player.setWalkSpeed(newSpeed);
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
        return Collections.singletonList("速度百分比");
    }
}