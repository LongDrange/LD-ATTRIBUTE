package com.longdrange.ldattribute.data.attribute.sub.other;

import com.longdrange.ldattribute.data.attribute.LDAttributeType;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import com.longdrange.ldattribute.data.eventdata.LDEventData;
import com.longdrange.ldattribute.util.LanguageManager;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class ArrowPierceAttribute extends LDSubAttribute {
    public ArrowPierceAttribute() {
        super("箭矢穿透数", 1, LDAttributeType.OTHER);
    }

    @Override
    public void eventMethod(LDEventData data) {
        // 純數值存儲
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
    @Override public List<String> getPlaceholders() { return Collections.singletonList("箭矢穿透数"); }
}