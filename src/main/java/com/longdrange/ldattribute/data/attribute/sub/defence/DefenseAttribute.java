package com.longdrange.ldattribute.data.attribute.sub.defence;

import com.longdrange.ldattribute.data.attribute.LDAttributeType;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import com.longdrange.ldattribute.data.eventdata.LDEventData;
import com.longdrange.ldattribute.data.eventdata.LDDamageEventData;
import com.longdrange.ldattribute.util.LanguageManager;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * 防御力屬性
 * 效果：被攻擊時按公式減傷，並支援破甲穿透
 *
 * 公式：實際傷害 = 傷害 × (1 - 防御力 / (防御力 + 100))
 *
 * 破甲：若攻擊者有破甲，防御力 × (1 - 穿透/100)
 */
public class DefenseAttribute extends LDSubAttribute {

    public DefenseAttribute() {
        super("防禦力", 1, LDAttributeType.DEFENSE);
    }

    @Override
    public void eventMethod(LDEventData data) {
        if (!(data instanceof LDDamageEventData)) return;
        LDDamageEventData damageData = (LDDamageEventData) data;
        double defense = getAttributes()[0];
        if (defense <= 0) return;

        // 防御加成（受害者）
        try {
            if (damageData.getVictimData() != null) {
                com.longdrange.ldattribute.data.attribute.LDSubAttribute ampAttr =
                        damageData.getVictimData().getSubAttribute("防御加成");
                if (ampAttr != null && ampAttr.getValue() > 0)
                    defense *= (1.0 + ampAttr.getValue() / 100.0);
            }
        } catch (Throwable ignored) {}

        // 破甲穿透
        double pen = damageData.getArmorPenPercent();
        if (pen > 0) {
            defense = defense * (1.0 - pen / 100.0);
            if (defense < 0) defense = 0;
        }

        double reduction = defense / (defense + 100.0);
        double newDamage = damageData.getDamage() * (1 - reduction);
        damageData.setDamage(Math.max(0, newDamage));
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
        return getDf().format(getAttributes()[0]);
    }

    @Override
    public List<String> getPlaceholders() {
        return Collections.singletonList("防禦力");
    }
}