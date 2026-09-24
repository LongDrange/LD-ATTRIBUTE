package com.longdrange.ldattribute.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import com.longdrange.ldattribute.data.attribute.LDAttributeType;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import com.longdrange.ldattribute.data.eventdata.LDDamageEventData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 傷害監聽器
 * 攻擊時觸發攻擊者的 ATTACK 屬性、受害者的 DEFENSE 屬性
 */
public class OnDamageListener implements Listener {

    private final LDAttribute plugin;

    public OnDamageListener(LDAttribute plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity attacker = (LivingEntity) event.getDamager();
        LivingEntity victim = (LivingEntity) event.getEntity();

        LDAttributeData attackerData = loadEntityData(attacker);
        LDAttributeData victimData = loadEntityData(victim);

        LDDamageEventData damageData = new LDDamageEventData(
                attacker, victim, event.getCause(), event.getDamage(), event);
        damageData.setAttackerData(attackerData);
        damageData.setVictimData(victimData);

        for (LDSubAttribute attr : attackerData.getAttributeMap().values()) {
            if (attr.containsType(LDAttributeType.ATTACK)) {
                attr.eventMethod(damageData);
            }
        }

        for (LDSubAttribute attr : victimData.getAttributeMap().values()) {
            if (attr.containsType(LDAttributeType.DEFENSE)) {
                attr.eventMethod(damageData);
            }
        }

        // ===== 符文扩展属性结算 =====

        // 1. 伤害加成（最终伤害 × %）
        try {
            double amp = getAttrValue(attackerData, "伤害加成");
            if (amp > 0) damageData.setDamage(damageData.getDamage() * (1.0 + amp / 100.0));
        } catch (Throwable ignored) {}

        // 2. 附加伤害（固定加值）
        try {
            double extra = getAttrValue(attackerData, "附加伤害");
            if (extra > 0) damageData.addDamage(extra);
        } catch (Throwable ignored) {}

        // 3. 暴伤抵抗（暴击时扣 %）
        try {
            if (damageData.isCritical()) {
                double critResist = getAttrValue(victimData, "暴伤抵抗");
                if (critResist > 0) {
                    damageData.setDamage(damageData.getDamage() * (1.0 - critResist / 100.0));
                }
            }
        } catch (Throwable ignored) {}

        // 4. 吸血（几率 + 倍率）
        try {
            if (attacker instanceof Player) {
                double chance = getAttrValue(attackerData, "吸血几率");
                if (chance > 0 && Math.random() * 100 < chance) {
                    double ratio = getAttrValue(attackerData, "吸血倍率");
                    if (ratio <= 0) ratio = 100;
                    double heal = Math.max(0, damageData.getDamage()) * ratio / 100.0;
                    Player ap = (Player) attacker;
                    if (heal > 0 && ap.getHealth() < ap.getMaxHealth()) {
                        ap.setHealth(Math.min(ap.getMaxHealth(), ap.getHealth() + heal));
                    }
                }
            }
        } catch (Throwable ignored) {}
        // ===== 元素克制 =====
        try {
            String atkElement = com.longdrange.ldattribute.combat.ElementHelper.getEntityElement(attacker);
            String defElement = com.longdrange.ldattribute.combat.ElementHelper.getEntityElement(victim);
            if (!atkElement.isEmpty() && !defElement.isEmpty()) {
                double mult = com.longdrange.ldattribute.combat.ElementConfig.getMultiplier(atkElement, defElement);
                if (mult != 1.0) {
                    damageData.setDamage(damageData.getDamage() * mult);
                    if (attacker instanceof Player) {
                        if (mult > 1.0) {
                            ((Player) attacker).sendMessage("§a✦ 元素克制！§7(§c" +
                                    com.longdrange.ldattribute.combat.ElementConfig.getDisplayName(atkElement) +
                                    " §7克 §b" +
                                    com.longdrange.ldattribute.combat.ElementConfig.getDisplayName(defElement) +
                                    "§7)");
                        } else {
                            ((Player) attacker).sendMessage("§c✘ 被元素克制... §7(§c" +
                                    com.longdrange.ldattribute.combat.ElementConfig.getDisplayName(atkElement) +
                                    " §7被 §b" +
                                    com.longdrange.ldattribute.combat.ElementConfig.getDisplayName(defElement) +
                                    "§7)");
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        // ===== 触发元素反应 =====
        try {
            String atkEl = com.longdrange.ldattribute.combat.ElementHelper.getEntityElement(attacker);
            String defEl = com.longdrange.ldattribute.combat.ElementHelper.getEntityElement(victim);
            if (!atkEl.isEmpty() && !defEl.isEmpty()) {
                com.longdrange.ldattribute.combat.ElementConfig.Reaction rxn =
                        com.longdrange.ldattribute.combat.ElementConfig.getReaction(atkEl, defEl);
                if (rxn != null) {
                    // 伤害倍率
                    if (rxn.damageMultiplier != 1.0) {
                        damageData.setDamage(damageData.getDamage() * rxn.damageMultiplier);
                    }
                    // 效果
                    if ("BURN".equals(rxn.effect)) {
                        victim.setFireTicks(60);
                    } else if ("SLOW".equals(rxn.effect)) {
                        victim.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.SLOW, 60, 1));
                    } else if ("WEAK".equals(rxn.effect)) {
                        victim.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.WEAKNESS, 60, 0));
                    } else if ("HEAL".equals(rxn.effect)) {
                        if (attacker instanceof Player) {
                            Player ap = (Player) attacker;
                            double heal = Math.max(0, damageData.getDamage()) * 0.5;
                            ap.setHealth(Math.min(ap.getMaxHealth(), ap.getHealth() + heal));
                        }
                    }
                    // 消息
                    if (attacker instanceof Player && rxn.message != null && !rxn.message.isEmpty()) {
                        ((Player) attacker).sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes(
                                (char) 38, rxn.message));
                    }
                    // 粒子特效
                    try {
                        victim.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_NORMAL,
                                victim.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
        // ===== 触发组合效果 =====
        try {
            if (attacker instanceof Player) {
                Player ap = (Player) attacker;
                for (com.longdrange.ldattribute.card.ComboData.Combo cb :
                        com.longdrange.ldattribute.card.ComboData.getAll()) {
                    if (!com.longdrange.ldattribute.card.ComboData.isActive(ap, cb.id)) continue;
                    String ef = cb.effect;
                    double ev = cb.effectValue;
                    if ("LIGHTNING_ON_HIT".equals(ef)) {
                        victim.getWorld().strikeLightningEffect(victim.getLocation());
                        damageData.addDamage(ev);
                    } else if ("FIRE_ON_HIT".equals(ef)) {
                        victim.setFireTicks(60);
                    } else if ("LIFESTEAL_ON_HIT".equals(ef)) {
                        double heal = Math.max(0, damageData.getDamage()) * ev / 100.0;
                        ap.setHealth(Math.min(ap.getMaxHealth(), ap.getHealth() + heal));
                    } else if ("THORNS".equals(ef)) {
                        if (victim instanceof LivingEntity) {
                            double reflect = damageData.getDamage() * ev / 100.0;
                            double nh = victim.getHealth() - reflect;
                            victim.setHealth(nh > 0 ? nh : 0);
                        }
                    }
                }
            }
            if (victim instanceof Player) {
                Player vp = (Player) victim;
                for (com.longdrange.ldattribute.card.ComboData.Combo cb :
                        com.longdrange.ldattribute.card.ComboData.getAll()) {
                    if (!com.longdrange.ldattribute.card.ComboData.isActive(vp, cb.id)) continue;
                    if ("DAMAGE_REDUCE".equals(cb.effect)) {
                        damageData.setDamage(damageData.getDamage() * (1.0 - cb.effectValue / 100.0));
                    }
                }
            }
        } catch (Throwable ignored) {}
        // ===== 战斗日志 =====
        try {
            double finalDmg = Math.max(0, damageData.getDamage());
            String elemInfo = "";
            String atkE = com.longdrange.ldattribute.combat.ElementHelper.getEntityElement(attacker);
            String defE = com.longdrange.ldattribute.combat.ElementHelper.getEntityElement(victim);
            if (!atkE.isEmpty() && !defE.isEmpty()) {
                double m = com.longdrange.ldattribute.combat.ElementConfig.getMultiplier(atkE, defE);
                if (m > 1.0) elemInfo = com.longdrange.ldattribute.combat.ElementConfig.getDisplayName(atkE) + "克" + com.longdrange.ldattribute.combat.ElementConfig.getDisplayName(defE);
                else if (m < 1.0) elemInfo = "被" + com.longdrange.ldattribute.combat.ElementConfig.getDisplayName(defE) + "克";
            }
            com.longdrange.ldattribute.combat.CombatLog.record(
                    attacker, victim, finalDmg, damageData.isCritical(), elemInfo, "");
        } catch (Throwable ignored) {}

        event.setDamage(Math.max(0, damageData.getDamage()));
    }

    /** 从属性数据按名字读值 */
    private double getAttrValue(LDAttributeData data, String name) {
        if (data == null) return 0;
        try {
            for (LDSubAttribute a : data.getAttributeMap().values()) {
                if (a.getName().equals(name)) return a.getValue();
            }
        } catch (Throwable ignored) {}
        return 0;
    }
    private LDAttributeData loadEntityData(LivingEntity entity) {
        LDAttributeData data = new LDAttributeData();

        if (entity instanceof Player) {
            Player player = (Player) entity;
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            data.add(plugin.getManager().getItemData(player, null, mainHand));
            ItemStack offHand = player.getInventory().getItemInOffHand();
            data.add(plugin.getManager().getItemData(player, null, offHand));
        } else {
            if (entity.getEquipment() != null) {
                ItemStack hand = entity.getEquipment().getItemInMainHand();
                data.add(plugin.getManager().getItemData(entity, null, hand));
            }
        }

        // 加上 API 附加資料（TcCardStats 等插件存入的卡片屬性）
        LDAttributeData apiData = plugin.getApi().getAPIStats(entity.getUniqueId());
        if (apiData != null) {
            data.add(apiData);
        }

        return data;
    }
}