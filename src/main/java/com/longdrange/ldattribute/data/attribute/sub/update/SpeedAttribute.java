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
 * 移動速度屬性
 * 效果：提高移動速度（原版基準 0.2）
 *
 * Lore 格式：移動速度: 0.1
 *   0.1 = 額外增加的速度（0.2 + 0.1 = 0.3）
 */
public class SpeedAttribute extends LDSubAttribute {

    public SpeedAttribute() {
        super("移動速度", 1, LDAttributeType.UPDATE);
    }

    @Override
    public void eventMethod(LDEventData data) {
        if (!(data instanceof LDUpdateEventData)) return;
        LDUpdateEventData updateData = (LDUpdateEventData) data;
        LivingEntity entity = updateData.getEntity();
        if (!(entity instanceof Player)) return;

        double bonus = getAttributes()[0];
        if (bonus <= 0) return;

        Player player = (Player) entity;
        float newSpeed = (float) (player.getWalkSpeed() + bonus);
        // 上限 1.0
        if (newSpeed > 1.0f) newSpeed = 1.0f;
        player.setWalkSpeed(newSpeed);
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
        return Collections.singletonList("移動速度");
    }
}