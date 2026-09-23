package com.longdrange.ldattribute.data.attribute.sub.damage;

import com.longdrange.ldattribute.data.attribute.LDAttributeType;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import com.longdrange.ldattribute.data.eventdata.LDEventData;
import com.longdrange.ldattribute.data.eventdata.LDDamageEventData;
import com.longdrange.ldattribute.util.LanguageManager;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class PveDamageAttribute extends LDSubAttribute {
    public PveDamageAttribute() {
        super("PVE攻击力", 1, LDAttributeType.ATTACK);
    }

    @Override
    public void eventMethod(LDEventData data) {
        if (!(data instanceof LDDamageEventData)) return;
        LDDamageEventData dd = (LDDamageEventData) data;
        if (dd.getVictim() instanceof Player) return;
        double v = getAttributes()[0];
        if (v <= 0) return;
        dd.addDamage(v);
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
        } catch (NumberFormatException e) { return false; }
    }

    @Override public double getValue() { return getAttributes()[0]; }
    @Override public String getPlaceholder(Player player, String params) { return getDf().format(getAttributes()[0]); }
    @Override public List<String> getPlaceholders() { return Collections.singletonList("PVE攻击力"); }
}