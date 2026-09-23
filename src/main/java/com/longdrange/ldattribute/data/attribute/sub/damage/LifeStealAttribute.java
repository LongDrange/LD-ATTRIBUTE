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
 * 吸血屬性
 * 效果：攻擊時，將造成傷害的 N% 轉換為自己的血量
 *
 * Lore 格式：吸血: 10
 *   10 = 吸血百分比（造成 100 傷害就回 10 血）
 */
public class LifeStealAttribute extends LDSubAttribute {

    public LifeStealAttribute() {
        super("吸血", 1, LDAttributeType.ATTACK);
    }

    @Override
    public void eventMethod(LDEventData data) {
        if (!(data instanceof LDDamageEventData)) return;
        LDDamageEventData damageData = (LDDamageEventData) data;

        double percent = getAttributes()[0];
        if (percent <= 0) return;

        double heal = damageData.getDamage() * percent / 100.0;
        if (heal <= 0) return;

        LivingEntity attacker = damageData.getAttacker();
        if (attacker.getHealth() >= attacker.getMaxHealth()) return;

        double newHealth = Math.min(attacker.getMaxHealth(), attacker.getHealth() + heal);
        attacker.setHealth(newHealth);
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
        return getDf().format(getAttributes()[0]) + "%";
    }

    @Override
    public List<String> getPlaceholders() {
        return Collections.singletonList("吸血");
    }
}