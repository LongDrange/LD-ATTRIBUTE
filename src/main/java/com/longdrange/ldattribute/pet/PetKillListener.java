package com.longdrange.ldattribute.pet;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * 宠物/玩家击杀怪物 → 给主人宠物加经验
 */
public class PetKillListener implements Listener {

    private final LDAttribute plugin;

    public PetKillListener(LDAttribute plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        Player killer = dead.getKiller();
        if (killer == null) return;
        givePetExp(killer);
    }

    private void givePetExp(Player player) {
        PetInstance active = PetManager.getActivePet(player);
        if (active == null) return;
        int[] pos = PetData.findPetPos(player.getUniqueId(), active.petId);
        if (pos == null) return;
        int expGain = PetConfig.getExpPerKill();
        PetManager.addExp(player, pos[0], pos[1], expGain);
    }
}