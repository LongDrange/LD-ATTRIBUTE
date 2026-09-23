package com.longdrange.ldattribute.spell;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.card.ManaManager;
import com.longdrange.ldattribute.card.StatsDataRead;
import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 法術核心
 */
public class SpellManager {

    private static LDAttribute plugin;

    // 冷卻：UUID → (spellId → 到期時間ms)
    private static final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public static void init(LDAttribute pl) {
        plugin = pl;
        cooldowns.clear();
    }

    public static void clear(UUID uuid) { cooldowns.remove(uuid); }

    // ==================== 釋放 ====================

    /** @return 錯誤訊息，成功回傳 null */
    public static String cast(Player player, SpellConfig.Spell sp) {
        return cast(player, sp, 1);
    }

    public static String cast(Player player, SpellConfig.Spell sp, int level) {
        if (player == null || sp == null) return "&c無效法術";

        // 冷卻
        long now = System.currentTimeMillis();
        Map<String, Long> cd = cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        Long expire = cd.get(sp.id);
        if (expire != null && expire > now) {
            long remain = (expire - now) / 1000 + 1;
            return "&c技能冷卻中，還需 &e" + remain + " &c秒";
        }

        // 法力
        if (sp.mana > 0) {
            if (!ManaManager.take(player, sp.mana)) {
                return "&c法力不足！需要 &e" + sp.mana + " &c點法力";
            }
        }

        // 設置冷卻
        if (sp.cooldown > 0) {
            cd.put(sp.id, now + sp.cooldown * 1000L);
        }

        // 讀取玩家屬性
        Map<String, Double> vars = collectVars(player, sp);
        // 法术等级系数
        if (level > 1) {
            double mult = Math.pow(sp.levelMultiplier, level - 1);
            vars.put("base", sp.base * mult);
            vars.put("level_mult", mult);
        } else {
            vars.put("level_mult", 1.0);
        }

        // 根據 target 分發
        try {
            switch (sp.target) {
                case "SELF":   castSelf(player, sp, vars); break;
                case "SINGLE": castSingle(player, sp, vars); break;
                case "AOE":    castAoe(player, sp, vars); break;
                case "BEAM":   castBeam(player, sp, vars); break;
                case "CONE":   castCone(player, sp, vars); break;
                default:       castSingle(player, sp, vars);
            }
        } catch (Throwable t) {
            return "&c施法失敗：" + t.getMessage();
        }

        // 原生效果（不依賴 MythicMobs）
        try { applyNativeEffect(player, sp); } catch (Throwable ignored) {}

        // 播放特效
        playEffects(player, sp);

        // MythicMobs 額外觸發
        if (sp.mythicSkill != null && !sp.mythicSkill.isEmpty()) {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "mm skills cast " + sp.mythicSkill + " " + player.getName());
            } catch (Throwable ignored) {}
        }

        return null;
    }

    // ==================== 目標分發 ====================

    private static void castSelf(Player p, SpellConfig.Spell sp, Map<String, Double> vars) {
        if (sp.healFormula != null && !sp.healFormula.isEmpty()) {
            double heal = FormulaEvaluator.eval(sp.healFormula, vars);
            if (heal > 0) {
                double max = p.getMaxHealth();
                p.setHealth(Math.min(max, p.getHealth() + heal));
            }
        }
        if (sp.damageFormula != null && !sp.damageFormula.isEmpty()) {
            // 自傷不常見，但允許
        }
    }

    private static void castSingle(Player p, SpellConfig.Spell sp, Map<String, Double> vars) {
        LivingEntity target = findTarget(p, sp.range);
        if (target == null) return;
        applyDamage(p, target, sp, vars);
    }

    private static void castAoe(Player p, SpellConfig.Spell sp, Map<String, Double> vars) {
        Location center = p.getLocation();
        for (Entity e : p.getWorld().getNearbyEntities(center, sp.radius, sp.radius, sp.radius)) {
            if (e instanceof LivingEntity && e != p) {
                if (e instanceof Player && ((Player) e).getGameMode() == GameMode.CREATIVE) continue;
                applyDamage(p, (LivingEntity) e, sp, vars);
            }
        }
    }

    private static void castBeam(Player p, SpellConfig.Spell sp, Map<String, Double> vars) {
        Vector dir = p.getLocation().getDirection().normalize();
        Location start = p.getEyeLocation();
        for (double d = 1; d <= sp.range; d += 0.5) {
            Location check = start.clone().add(dir.clone().multiply(d));
            for (Entity e : check.getWorld().getNearbyEntities(check, 0.8, 0.8, 0.8)) {
                if (e instanceof LivingEntity && e != p) {
                    applyDamage(p, (LivingEntity) e, sp, vars);
                    return;
                }
            }
        }
    }

    private static void castCone(Player p, SpellConfig.Spell sp, Map<String, Double> vars) {
        Vector dir = p.getLocation().getDirection().normalize();
        Location center = p.getLocation();
        for (Entity e : p.getWorld().getNearbyEntities(center, sp.range, sp.range, sp.range)) {
            if (!(e instanceof LivingEntity) || e == p) continue;
            if (e instanceof Player && ((Player) e).getGameMode() == GameMode.CREATIVE) continue;
            Vector toE = e.getLocation().toVector().subtract(center.toVector()).normalize();
            double dot = toE.dot(dir);
            if (dot >= 0.5) { // 約 60 度範圍
                applyDamage(p, (LivingEntity) e, sp, vars);
            }
        }
    }

    // ==================== 傷害應用 ====================

    private static void applyDamage(Player caster, LivingEntity target, SpellConfig.Spell sp, Map<String, Double> vars) {
        if (sp.damageFormula == null || sp.damageFormula.isEmpty()) return;
        double dmg = FormulaEvaluator.eval(sp.damageFormula, vars);
        if (dmg <= 0) return;

        // 暴擊判定（用 magic_crit 和 magic_crit_dmg）
        double critChance = vars.getOrDefault("magic_crit", 0.0);
        double critDmg = vars.getOrDefault("magic_crit_dmg", 150.0);
        if (critChance > 0 && Math.random() * 100 < critChance) {
            dmg = dmg * critDmg / 100.0;
        }

        // 法術穿透：減目標防禦（這裡用近似，只做額外傷害）
        double pen = vars.getOrDefault("magic_pen", 0.0);
        if (pen > 0) dmg = dmg * (1.0 + pen / 200.0);

        target.damage(dmg, caster);
    }

    // ==================== 輔助 ====================

    private static LivingEntity findTarget(Player p, double range) {
        Vector dir = p.getLocation().getDirection().normalize();
        Location start = p.getEyeLocation();
        for (double d = 1; d <= range; d += 0.5) {
            Location check = start.clone().add(dir.clone().multiply(d));
            for (Entity e : check.getWorld().getNearbyEntities(check, 0.6, 0.6, 0.6)) {
                if (e instanceof LivingEntity && e != p) {
                    if (e instanceof Player && ((Player) e).getGameMode() == GameMode.CREATIVE) continue;
                    return (LivingEntity) e;
                }
            }
        }
        return null;
    }

    private static Map<String, Double> collectVars(Player p, SpellConfig.Spell sp) {
        Map<String, Double> vars = new HashMap<>();
        vars.put("base", sp.base);
        vars.put("magic_damage", getAttr(p, "法术伤害"));
        vars.put("magic_pen", getAttr(p, "法术穿透"));
        vars.put("magic_crit", getAttr(p, "法术暴击几率"));
        vars.put("magic_crit_dmg", getAttr(p, "法术暴击倍率"));
        vars.put("magic_defense", getAttr(p, "法术防御"));
        vars.put("magic_lifesteal", getAttr(p, "法术吸血"));
        return vars;
    }

    private static double getAttr(Player p, String name) {
        try {
            LDAttributeData data = StatsDataRead.loadPlayerStats(p);
            if (data == null) return 0;
            for (LDSubAttribute a : data.getAttributeMap().values()) {
                if (a.getName().equals(name)) return a.getValue();
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    /**
     * 原生效果（不依赖 MythicMobs）
     * 格式：
     *   TELEPORT:<距离>                     向前传送 N 格
     *   POTION:<类型>:<持续tick>:<等级>      加药水效果
     *   SUMMON:<类型>:<数量>                召唤生物
     *   HEAL:<值>                           治疗
     *   BURN:<tick>                         点燃自己/目标
     *   PUSH:<力量>                         击退
     */
    private static void applyNativeEffect(Player caster, SpellConfig.Spell sp) {
        String eff = sp.nativeEffect;
        if (eff == null || eff.isEmpty()) return;
        String[] parts = eff.split(":");
        String type = parts[0].toUpperCase();

        switch (type) {
            case "TELEPORT": {
                double dist = parts.length >= 2 ? parseDouble(parts[1], 10) : 10;
                Location loc = caster.getLocation();
                Vector dir = loc.getDirection().normalize();
                Location target = loc.clone().add(dir.multiply(dist));
                // 保持 Y 不变，防止掉洞
                Location checkLoc = target.clone();
                while (checkLoc.getY() > 1 && checkLoc.getBlock().getType() == Material.AIR) {
                    checkLoc.subtract(0, 1, 0);
                }
                target.setY(checkLoc.getY() + 1);
                // 找安全落点
                for (int i = 0; i < 5; i++) {
                    if (target.getBlock().getType() == Material.AIR
                            && target.clone().add(0, 1, 0).getBlock().getType() == Material.AIR) break;
                    target.add(0, 1, 0);
                }
                // 粒子效果在原位置和新位置
                caster.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.3);
                caster.teleport(target);
                caster.getWorld().spawnParticle(Particle.PORTAL, target.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.3);
                break;
            }
            case "POTION": {
                if (parts.length < 4) return;
                PotionEffectType pt = PotionEffectType.getByName(parts[1].toUpperCase());
                if (pt == null) return;
                int duration = (int) parseDouble(parts[2], 60);
                int amp = (int) parseDouble(parts[3], 1) - 1;
                if (amp < 0) amp = 0;
                caster.addPotionEffect(new PotionEffect(pt, duration, amp, true, true), true);
                break;
            }
            case "SUMMON": {
                if (parts.length < 3) return;
                EntityType et = EntityType.valueOf(parts[1].toUpperCase());
                int count = (int) parseDouble(parts[2], 1);
                Location base = caster.getLocation();
                for (int i = 0; i < count; i++) {
                    Location spawn = base.clone().add(
                        (Math.random() - 0.5) * 2, 0, (Math.random() - 0.5) * 2);
                    org.bukkit.entity.Entity e = caster.getWorld().spawnEntity(spawn, et);
                    if (e instanceof Tameable) {
                        ((Tameable) e).setOwner(caster);
                    }
                    if (e instanceof LivingEntity) {
                        ((LivingEntity) e).setCustomName("§7" + caster.getName() + " 的僕從");
                    }
                }
                break;
            }
            case "HEAL": {
                double heal = parts.length >= 2 ? parseDouble(parts[1], 20) : 20;
                caster.setHealth(Math.min(caster.getMaxHealth(), caster.getHealth() + heal));
                break;
            }
            case "BURN": {
                int ticks = parts.length >= 2 ? (int) parseDouble(parts[1], 60) : 60;
                caster.setFireTicks(ticks);
                break;
            }
            case "PUSH": {
                double force = parts.length >= 2 ? parseDouble(parts[1], 1.5) : 1.5;
                Vector dir = caster.getLocation().getDirection().normalize();
                dir.setY(0.3);
                caster.setVelocity(dir.multiply(force));
                break;
            }
        }
    }

    private static double parseDouble(String s, double def) {
        try { return Double.parseDouble(s); } catch (Exception e) { return def; }
    }
    private static void playEffects(Player p, SpellConfig.Spell sp) {
        if (sp.effects == null) return;
        for (String eff : sp.effects) {
            try {
                String[] parts = eff.split(":");
                if (parts[0].equalsIgnoreCase("PARTICLE") && parts.length >= 3) {
                    String pname = parts[1].toUpperCase();
                    int count = Integer.parseInt(parts[2]);
                    Location loc = p.getLocation().add(0, 1, 0);
                    try {
                        Particle particle = Particle.valueOf(pname);
                        p.getWorld().spawnParticle(particle, loc, count, 1, 1, 1, 0.1);
                    } catch (Throwable ignored) {}
                } else if (parts[0].equalsIgnoreCase("SOUND") && parts.length >= 2) {
                    String sname = parts[1].toUpperCase();
                    try {
                        Sound sound = Sound.valueOf(sname);
                        p.getWorld().playSound(p.getLocation(), sound, 1f, 1f);
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
        }
    }

    // ==================== 冷卻查詢 ====================

    public static int getCooldownRemain(Player player, String spellId) {
        Map<String, Long> cd = cooldowns.get(player.getUniqueId());
        if (cd == null) return 0;
        Long expire = cd.get(spellId);
        if (expire == null || expire <= System.currentTimeMillis()) return 0;
        return (int) ((expire - System.currentTimeMillis()) / 1000) + 1;
    }
}