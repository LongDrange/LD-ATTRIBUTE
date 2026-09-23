package com.longdrange.ldattribute.data.attribute.sub.defence;

import com.longdrange.ldattribute.data.attribute.LDAttributeType;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import com.longdrange.ldattribute.data.eventdata.LDEventData;
import com.longdrange.ldattribute.data.eventdata.LDDamageEventData;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * 格擋屬性
 * 效果：被攻擊時 N% 機率減免 M% 傷害
 *
 * Lore 格式：格擋: 30/50
 *   30 = 觸發機率（%）
 *   50 = 減傷比例（%）
 */
public class BlockAttribute extends LDSubAttribute {

    public BlockAttribute() {
        super("格擋", 2, LDAttributeType.DEFENSE);
    }

    @Override
    public void eventMethod(LDEventData data) {
        if (!(data instanceof LDDamageEventData)) return;
        LDDamageEventData damageData = (LDDamageEventData) data;
        double chance = getAttributes()[0];
        double reduction = getAttributes()[1];
        if (chance <= 0 || reduction <= 0) return;

        if (probability(chance)) {
            double newDamage = damageData.getDamage() * (1 - reduction / 100.0);
            damageData.setDamage(Math.max(0, newDamage));
        }
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
            double chance = Double.parseDouble(parts[0]);
            double reduction = Double.parseDouble(parts[1]);
            if (chance == 0) return false;
            setAttributes(chance, reduction);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public double getValue() {
        return getAttributes()[0] * getAttributes()[1] / 100.0;
    }

    @Override
    public String getPlaceholder(Player player, String params) {
        return getDf().format(getAttributes()[0]) + "%/" + getDf().format(getAttributes()[1]) + "%";
    }

    @Override
    public List<String> getPlaceholders() {
        return Collections.singletonList("格擋");
    }
}