package com.longdrange.ldattribute.data.eventdata;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * 傷害事件資料
 * 攻擊時建立，攜帶攻擊者、受害者、傷害值等資訊
 */
public class LDDamageEventData extends LDEventData {

    private final LivingEntity attacker;
    private final LivingEntity victim;
    private final EntityDamageEvent.DamageCause cause;
    private final EntityDamageByEntityEvent event;
    private double damage;

    /** 是否已觸發暴擊（由暴擊機率屬性設定） */
    private boolean critical = false;

    /** 攻击者/受害者属性数据（用于反伤、防御加成读取） */
    private com.longdrange.ldattribute.data.attribute.LDAttributeData attackerData;
    private com.longdrange.ldattribute.data.attribute.LDAttributeData victimData;

    /** 破甲穿透百分比（由破甲屬性設定，0 = 無破甲） */
    private double armorPenPercent = 0.0;

    public LDDamageEventData(LivingEntity attacker,
                             LivingEntity victim,
                             EntityDamageEvent.DamageCause cause,
                             double damage,
                             EntityDamageByEntityEvent event) {
        super(attacker);
        this.attacker = attacker;
        this.victim = victim;
        this.cause = cause;
        this.damage = damage;
        this.event = event;
    }

    public LivingEntity getAttacker() { return attacker; }
    public LivingEntity getVictim() { return victim; }
    public EntityDamageEvent.DamageCause getCause() { return cause; }
    public EntityDamageByEntityEvent getEvent() { return event; }
    public double getDamage() { return damage; }
    public void setDamage(double damage) { this.damage = damage; }
    public void addDamage(double amount) { this.damage += amount; }

    public boolean isCritical() { return critical; }
    public void setCritical(boolean critical) { this.critical = critical; }

    public com.longdrange.ldattribute.data.attribute.LDAttributeData getAttackerData() { return attackerData; }
    public void setAttackerData(com.longdrange.ldattribute.data.attribute.LDAttributeData d) { this.attackerData = d; }
    public com.longdrange.ldattribute.data.attribute.LDAttributeData getVictimData() { return victimData; }
    public void setVictimData(com.longdrange.ldattribute.data.attribute.LDAttributeData d) { this.victimData = d; }

    public double getArmorPenPercent() { return armorPenPercent; }
    public void setArmorPenPercent(double armorPenPercent) { this.armorPenPercent = armorPenPercent; }
}