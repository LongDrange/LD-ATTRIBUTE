package com.longdrange.ldattribute.data.attribute.sub.other;

import com.longdrange.ldattribute.data.attribute.LDAttributeType;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import com.longdrange.ldattribute.data.eventdata.LDEventData;
import com.longdrange.ldattribute.data.eventdata.LDDamageEventData;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * 事件訊息屬性
 * 效果：攻擊時，發送一條訊息給攻擊者顯示造成的傷害
 *
 * Lore 格式：事件訊息: 1
 *   1 = 啟用（0 = 停用）
 *
 * 訊息格式：&e你對 &c{目標} &e造成了 &c{傷害} &e點傷害！
 */
public class EventMessageAttribute extends LDSubAttribute {

    public EventMessageAttribute() {
        super("事件訊息", 1, LDAttributeType.ATTACK);
    }

    @Override
    public void eventMethod(LDEventData data) {
        if (!(data instanceof LDDamageEventData)) return;
        LDDamageEventData damageData = (LDDamageEventData) data;
        if (getAttributes()[0] <= 0) return;

        LivingEntity attacker = damageData.getAttacker();
        LivingEntity victim = damageData.getVictim();
        if (!(attacker instanceof Player)) return;

        Player player = (Player) attacker;
        String victimName = victim.getCustomName() != null
                ? victim.getCustomName()
                : victim.getType().name();
        double damage = damageData.getDamage();

        String msg = "&e你對 &c" + victimName + " &e造成了 &c"
                + getDf().format(damage) + " &e點傷害！";
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }

    @Override
    public boolean loadAttribute(String str) {
        if (str == null) return false;
        if (!com.longdrange.ldattribute.util.LanguageManager.matches(str, getName())) return false;
        try {
            double value = Double.parseDouble(getNumber(str));
            setAttributes(value);
            return value != 0;
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
        return getAttributes()[0] > 0 ? "啟用" : "停用";
    }

    @Override
    public List<String> getPlaceholders() {
        return Collections.singletonList("事件訊息");
    }
}