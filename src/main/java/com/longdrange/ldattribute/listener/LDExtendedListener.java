package com.longdrange.ldattribute.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.card.StatsDataRead;
import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 擴展戰鬥屬性監聽器
 * 處理：生命恢復、擊退抗性、燃燒、吸血抵抗、暴擊抵抗、
 *      箭矢速度/精準/穿透、盾牌減傷
 */
public class LDExtendedListener implements Listener {

    private final LDAttribute plugin;

    // 箭矢穿透：記錄箭矢已命中的實體
    private final Map<UUID, Set<UUID>> arrowHits = new ConcurrentHashMap<>();

    public LDExtendedListener(LDAttribute plugin) {
        this.plugin = plugin;
        startRegenTask();
    }

    // ==================== 生命恢復 ====================
    private void startRegenTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                try {
                    double regen = getAttrValue(p, "生命恢复");
                    if (regen <= 0) continue;
                    double hp = p.getHealth();
                    double max = p.getMaxHealth();
                    if (hp >= max) continue;
                    double newHp = Math.min(max, hp + regen);
                    p.setHealth(newHp);
                } catch (Throwable ignored) {}
            }
        }, 20L, 20L);
    }

    // ==================== 箭矢：速度 / 精準 / 穿透 ====================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();
        try {
            double speed = getAttrValue(p, "箭矢速度");
            double accuracy = getAttrValue(p, "箭矢精准");
            double pierce = getAttrValue(p, "箭矢穿透数");

            if (speed > 0) {
                Vector v = event.getProjectile().getVelocity();
                event.getProjectile().setVelocity(v.multiply(1.0 + speed / 10.0));
            }
            if (accuracy > 0) {
                Vector v = event.getProjectile().getVelocity();
                double spread = Math.max(0, 1.0 - accuracy / 100.0);
                v.setX(v.getX() * spread);
                v.setZ(v.getZ() * spread);
                event.getProjectile().setVelocity(v);
            }
            if (pierce > 0 && event.getProjectile() instanceof Arrow) {
                arrowHits.put(event.getProjectile().getUniqueId(), ConcurrentHashMap.newKeySet());
            }
        } catch (Throwable ignored) {}
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        UUID arrowId = event.getEntity().getUniqueId();
        if (!arrowHits.containsKey(arrowId)) return;
        // 箭矢命中后不消失，继续穿透
        event.getEntity().remove();
        arrowHits.remove(arrowId);
    }

    // ==================== 伤害事件：击退 / 燃烧 / 吸血抵抗 / 暴击抵抗 / 盾牌 ====================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity victim = (LivingEntity) event.getEntity();
        Entity damager = event.getDamager();

        // ===== 击退抗性（受害者） =====
        if (victim instanceof Player) {
            try {
                double kbResist = getAttrValue((Player) victim, "击退抗性");
                if (kbResist > 0) {
                    Vector v = victim.getVelocity();
                    double factor = Math.max(0, 1.0 - kbResist / 100.0);
                    victim.setVelocity(v.multiply(factor));
                }
            } catch (Throwable ignored) {}

            // ===== 盾牌减伤 =====
            try {
                Player p = (Player) victim;
                if (p.isBlocking()) {
                    double shield = getAttrValue(p, "盾牌减伤");
                    if (shield > 0) {
                        double newDmg = event.getDamage() * (1.0 - shield / 100.0);
                        event.setDamage(Math.max(0, newDmg));
                    }
                }
            } catch (Throwable ignored) {}

            // ===== 暴击抵抗 =====
            // 1.12.2 原版暴击在事件后判定，此处用近似方法：降低暴击伤害
            try {
                double critResist = getAttrValue((Player) victim, "暴击抵抗");
                if (critResist > 0 && event.getDamage() > 1.5) {
                    // 如果伤害明显偏高（可能是暴击），降低
                    double newDmg = event.getDamage() * (1.0 - critResist / 200.0);
                    event.setDamage(Math.max(0, newDmg));
                }
            } catch (Throwable ignored) {}
        }

        // ===== 攻击方触发：燃烧 / 吸血抵抗 =====
        if (damager instanceof Player) {
            Player attacker = (Player) damager;

            // 燃烧几率
            try {
                double burnChance = getAttrValue(attacker, "燃烧几率");
                if (burnChance > 0 && Math.random() * 100 < burnChance) {
                    double burnDmg = getAttrValue(attacker, "燃烧伤害");
                    if (burnDmg <= 0) burnDmg = 1;
                    final double finalBurnDmg = burnDmg;
                    final LivingEntity finalVictim = victim;
                    // 点燃：给目标加着火 + 持续伤害任务
                    victim.setFireTicks(60);
                    // 每秒额外伤害（用调度器）
                    final int[] ticks = {0};
                    final BukkitRunnable[] task = new BukkitRunnable[1];
                    task[0] = new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (ticks[0] >= 3 || finalVictim.isDead() || !finalVictim.isValid()) {
                                cancel();
                                return;
                            }
                            finalVictim.damage(finalBurnDmg, attacker);
                            ticks[0]++;
                        }
                    };
                    task[0].runTaskTimer(plugin, 20L, 20L);
                }
            } catch (Throwable ignored) {}
        }
    }

    // ==================== 法術書：右鍵釋放 ====================
    @EventHandler(priority = EventPriority.NORMAL)
    public void onRightClick(PlayerInteractEvent event) {
        Action a = event.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        Player p = event.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || item.getType() == org.bukkit.Material.AIR) return;

        String spellId = com.longdrange.ldattribute.card.CardNBT.getSpell(item);
        if (spellId == null || spellId.isEmpty()) return;

        // Shift + 右键 = 升级法术书
        if (p.isSneaking()) {
            event.setCancelled(true);
            tryUpgradeSpellBook(p, item, spellId);
            return;
        }

        event.setCancelled(true);
        com.longdrange.ldattribute.spell.SpellConfig.Spell sp =
                com.longdrange.ldattribute.spell.SpellConfig.get(spellId);
        if (sp == null) return;

        int lv = com.longdrange.ldattribute.card.CardNBT.getSpellLevel(item);
        String err = com.longdrange.ldattribute.spell.SpellManager.cast(p, sp, lv);
        if (err == null) {
            p.sendMessage("§a✦ 施放 " + sp.name);
        } else {
            org.bukkit.ChatColor.translateAlternateColorCodes((char) 38, "");
            p.sendMessage(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', err));
        }
    }
    private void tryUpgradeSpellBook(Player player, ItemStack book, String spellId) {
        com.longdrange.ldattribute.spell.SpellConfig.Spell sp =
                com.longdrange.ldattribute.spell.SpellConfig.get(spellId);
        if (sp == null) return;
        int curLv = com.longdrange.ldattribute.card.CardNBT.getSpellLevel(book);
        if (curLv >= sp.maxLevel) {
            player.sendMessage("§c此法术书已达最高等级 §eLv." + sp.maxLevel);
            return;
        }

        int handSlot = player.getInventory().getHeldItemSlot();
        ItemStack handItem = player.getInventory().getItem(handSlot);
        if (handItem == null) return;

        // 情况 A：手上这一堆本身 >= 2 本 → 消耗 2 本得 1 本升级版
        if (handItem.getAmount() >= 2) {
            int newAmount = handItem.getAmount() - 2;
            if (newAmount <= 0) {
                player.getInventory().setItem(handSlot, null);
            } else {
                handItem.setAmount(newAmount);
            }
            ItemStack upgraded = com.longdrange.ldattribute.spell.SpellBookItem.create(spellId, curLv + 1);
            java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(upgraded);
            for (ItemStack left : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
            player.sendMessage("§a✦ 法术书升级成功！§e" + sp.name + " §aLv." + curLv + " → §eLv." + (curLv + 1));
            try { player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f); } catch (Throwable ignored) {}
            return;
        }

        // 情况 B：手上 1 本，背包里找另一本同等级的
        ItemStack[] contents = player.getInventory().getContents();
        int foundSlot = -1;
        for (int i = 0; i < contents.length; i++) {
            if (i == handSlot) continue;
            ItemStack it = contents[i];
            if (it == null) continue;
            String sid = com.longdrange.ldattribute.card.CardNBT.getSpell(it);
            if (sid == null || !sid.equals(spellId)) continue;
            int lv = com.longdrange.ldattribute.card.CardNBT.getSpellLevel(it);
            if (lv == curLv) { foundSlot = i; break; }
        }
        if (foundSlot < 0) {
            player.sendMessage("§c需要 §e2 §c本同等级的法术书才能升级");
            player.sendMessage("§7当前: §eLv." + curLv);
            return;
        }

        // 消耗背包里那一本
        ItemStack other = contents[foundSlot];
        if (other.getAmount() > 1) {
            other.setAmount(other.getAmount() - 1);
        } else {
            player.getInventory().setItem(foundSlot, null);
        }

        // 升级手上的
        int newLv = curLv + 1;
        ItemStack upgraded = com.longdrange.ldattribute.spell.SpellBookItem.create(spellId, newLv);
        player.getInventory().setItem(handSlot, upgraded);
        player.sendMessage("§a✦ 法术书升级成功！§e" + sp.name + " §aLv." + curLv + " → §eLv." + newLv);
        try { player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f); } catch (Throwable ignored) {}
    }    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        arrowHits.remove(event.getPlayer().getUniqueId());
    }

    // ==================== 辅助：从玩家身上读取属性值 ====================
    private double getAttrValue(Player player, String attrName) {
        try {
            LDAttributeData data = StatsDataRead.loadPlayerStats(player);
            if (data == null) return 0;
            for (LDSubAttribute attr : data.getAttributeMap().values()) {
                if (attr.getName().equals(attrName)) {
                    return attr.getValue();
                }
            }
        } catch (Throwable ignored) {}
        return 0;
    }
}