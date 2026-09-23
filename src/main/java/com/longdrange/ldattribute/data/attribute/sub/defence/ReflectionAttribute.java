package com.longdrange.ldattribute.data.attribute.sub.defence;

import com.longdrange.ldattribute.data.attribute.LDAttributeType;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import com.longdrange.ldattribute.data.eventdata.LDEventData;
import com.longdrange.ldattribute.data.eventdata.LDDamageEventData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * 反射屬性
 * 效果：被攻擊時，將 N% 的傷害反彈給攻擊者
 *
 * Lore 格式：反射: 30
 *   30 = 反射百分比
 */
public class ReflectionAttribute extends LDSubAttribute {

    public ReflectionAttribute() {
        super("反射", 1, LDAttributeType.DEFENSE);
    }

    @Override
    public void eventMethod(LDEventData data) {
        if (!(data instanceof LDDamageEventData)) return;
        LDDamageEventData damageData = (LDDamageEventData) data;
        double percent = getAttributes()[0];
        if (percent <= 0) return;

        LivingEntity victim = damageData.getVictim();
        LivingEntity attacker = damageData.getAttacker();

        double reflectDamage = damageData.getDamage() * percent / 100.0;

        // 反伤倍率（受害者）
        try {
            if (damageData.getVictimData() != null) {
                com.longdrange.ldattribute.data.attribute.LDSubAttribute ratioAttr =
                        damageData.getVictimData().getSubAttribute("反伤倍率");
                if (ratioAttr != null && ratioAttr.getValue() > 0)
                    reflectDamage *= (1.0 + ratioAttr.getValue() / 100.0);
            }
        } catch (Throwable ignored) {}

        // 反伤抵抗（攻击者）
        try {
            if (damageData.getAttackerData() != null) {
                com.longdrange.ldattribute.data.attribute.LDSubAttribute resistAttr =
                        damageData.getAttackerData().getSubAttribute("反伤抵抗");
                if (resistAttr != null && resistAttr.getValue() > 0) {
                    double r = Math.min(resistAttr.getValue(), 100);
                    reflectDamage *= (1.0 - r / 100.0);
                }
            }
        } catch (Throwable ignored) {}

        if (reflectDamage <= 0) return;

        // 直接扣攻擊者血量（不觸發事件避免無限遞迴）
        double newHealth = attacker.getHealth() - reflectDamage;
        if (newHealth <= 0) {
            attacker.setHealth(0);
        } else {
            attacker.setHealth(newHealth);
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
        return getDf().format(getAttributes()[0]) + "%";
    }

    @Override
    public List<String> getPlaceholders() {
        return Collections.singletonList("反射");
    }
}