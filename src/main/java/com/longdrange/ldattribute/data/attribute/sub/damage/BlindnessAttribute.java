package com.longdrange.ldattribute.data.attribute.sub.damage;

import com.longdrange.ldattribute.data.attribute.LDAttributeType;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import com.longdrange.ldattribute.data.eventdata.LDEventData;
import com.longdrange.ldattribute.data.eventdata.LDDamageEventData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.List;

/**
 * 失明屬性
 * 效果：攻擊時讓目標失明
 *
 * Lore 格式：失明: 5/1
 */
public class BlindnessAttribute extends LDSubAttribute {

    public BlindnessAttribute() {
        super("失明", 2, LDAttributeType.ATTACK);
    }

    @Override
    public void eventMethod(LDEventData data) {
        if (!(data instanceof LDDamageEventData)) return;
        LDDamageEventData damageData = (LDDamageEventData) data;

        double seconds = getAttributes()[0];
        double level = getAttributes()[1];
        if (seconds <= 0) return;

        LivingEntity victim = damageData.getVictim();
        int duration = (int) (seconds * 20);
        int amplifier = (int) Math.max(0, level - 1);

        victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, amplifier), true);
    }

    @Override
    public boolean loadAttribute(String str) {
        if (str == null) return false;
        if (!com.longdrange.ldattribute.util.LanguageManager.matches(str, getName())) return false;
        try {
            String[] parts = str.replaceAll("\u00a7+[a-z0-9]", "")
                                 .replaceAll("[^0-9./]", "")
                                 .split("/");
            if (parts.length < 2) return false;
            double seconds = Double.parseDouble(parts[0]);
            double level = Double.parseDouble(parts[1]);
            if (seconds == 0) return false;
            setAttributes(seconds, level);
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
        return Collections.singletonList("失明");
    }
}