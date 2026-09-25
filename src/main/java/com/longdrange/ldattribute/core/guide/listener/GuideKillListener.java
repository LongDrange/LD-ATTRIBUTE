package com.longdrange.ldattribute.core.guide.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.guide.GuideConfig;
import com.longdrange.ldattribute.core.guide.GuideData;
import com.longdrange.ldattribute.core.guide.MythicMobsHelper;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Random;

public class GuideKillListener implements Listener {

    private final LDAttribute plugin;
    private static final Random RANDOM = new Random();

    public GuideKillListener(LDAttribute plugin) { this.plugin = plugin; }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        LivingEntity entity = e.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        String mobId = MythicMobsHelper.getMobId(entity);
        if (mobId == null) return;

        GuideConfig.MonsterDef def = GuideConfig.get(mobId);
        if (def == null) return;

        GuideData data = plugin.getGuideManager().get(killer);
        if (data.isUnlocked(mobId)) return;

        // ===== 1) 概率直接解锁 =====
        if (def.unlockChance > 0 && RANDOM.nextDouble() < def.unlockChance) {
            unlockGuide(killer, def, data, "幸运");
            return;
        }

        // ===== 2) 击杀累计 =====
        int cur = data.addKills(mobId, 1);
        if (cur >= def.requiredKills) {
            unlockGuide(killer, def, data, "达成");
        } else {
            // 每 5 杀提示进度（避免刷屏）
            if (cur % 5 == 0 || cur == def.requiredKills - 1) {
                killer.sendMessage(ChatColor.GRAY + "[" + ChatColor.stripColor(def.name)
                        + "] 已击杀 " + cur + "/" + def.requiredKills);
            }
        }
    }

    static void unlockGuide(Player player, GuideConfig.MonsterDef def, GuideData data, String reason) {
        data.setUnlocked(def.id, true);
        player.sendMessage(ChatColor.GREEN + "\u2714 解锁图鉴: " + def.name
                + ChatColor.GRAY + " (" + reason + ")");
        try { player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f); } catch (Throwable ignored) {}
        try { com.longdrange.ldattribute.card.StatsDataRead.updatePlayer(player); } catch (Throwable ignored) {}
    }
}