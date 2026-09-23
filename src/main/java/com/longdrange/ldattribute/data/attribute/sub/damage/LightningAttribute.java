package com.longdrange.ldattribute.data.attribute.sub.damage;

import com.longdrange.ldattribute.data.attribute.LDAttributeType;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import com.longdrange.ldattribute.data.eventdata.LDEventData;
import com.longdrange.ldattribute.data.eventdata.LDDamageEventData;
import com.longdrange.ldattribute.util.LanguageManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * 閃電屬性
 * 效果：攻擊時 N% 機率觸發，觸發時召喚視覺閃電並造成 M 點額外傷害
 *
 * Lore 格式：
 *   閃電: 20      → 20% 機率觸發（純特效）
 *   閃電: 20/50   → 20% 機率觸發 + 50 點額外傷害
 */
public class LightningAttribute extends LDSubAttribute {

    public LightningAttribute() {
        super("閃電", 2, LDAttributeType.ATTACK);
    }

    @Override
    public void eventMethod(LDEventData data) {
        if (!(data instanceof LDDamageEventData)) return;
        LDDamageEventData damageData = (LDDamageEventData) data;

        double chance = getAttributes()[0];
        double damage = getAttributes()[1];
        if (chance <= 0) return;

        if (probability(chance)) {
            LivingEntity victim = damageData.getVictim();
            victim.getWorld().strikeLightningEffect(victim.getLocation());
            if (damage > 0) {
                damageData.addDamage(damage);
            }
        }
    }

    @Override
    public boolean loadAttribute(String str) {
        if (str == null) return false;
        if (!LanguageManager.matches(str, getName())) return false;
        try {
            String[] parts = str.replaceAll("\u00a7+[a-z0-9]", "")
                                 .replaceAll("[^0-9./]", "")
                                 .split("/");
            if (parts.length < 1) return false;
            double chance = Double.parseDouble(parts[0]);
            double damage = parts.length >= 2 ? Double.parseDouble(parts[1]) : 0;
            if (chance == 0 && damage == 0) return false;
            setAttributes(chance, damage);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public double getValue() { return getAttributes()[0]; }

    @Override
    public String getPlaceholder(Player player, String params) {
        return getDf().format(getAttributes()[0]) + "%/" + getDf().format(getAttributes()[1]);
    }

    @Override
    public List<String> getPlaceholders() {
        return Collections.singletonList("閃電");
    }
}