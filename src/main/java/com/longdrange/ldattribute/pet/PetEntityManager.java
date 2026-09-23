package com.longdrange.ldattribute.pet;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 宠物实体管理：生成、跟随、攻击 AI
 */
public class PetEntityManager {

    private static LDAttribute plugin;

    /** 玩家UUID → 宠物实体UUID */
    private static final Map<UUID, UUID> playerPet = new ConcurrentHashMap<>();
    /** 宠物实体UUID → 玩家UUID */
    private static final Map<UUID, UUID> petOwner = new ConcurrentHashMap<>();
    /** 宠物实体UUID → 出生位置 */
    private static final Map<UUID, Location> petSpawnLoc = new ConcurrentHashMap<>();

    public static void init(LDAttribute pl) {
        plugin = pl;

        // 每秒 AI 任务
        Bukkit.getScheduler().runTaskTimer(pl, () -> {
            for (Map.Entry<UUID, UUID> e : playerPet.entrySet()) {
                UUID playerId = e.getKey();
                UUID petId = e.getValue();
                Player player = Bukkit.getPlayer(playerId);
                if (player == null) continue;
                Entity petEnt = Bukkit.getEntity(petId);
                if (petEnt == null || !petEnt.isValid() || !(petEnt instanceof LivingEntity)) {
                    // 宠物死了/丢失，重生
                    tryRespawnPet(player);
                    continue;
                }
                try { handleAI(player, (LivingEntity) petEnt); } catch (Throwable ignored) {}
            }
        }, 20L, 20L);
    }

    /** 生成宠物实体 */
    public static void spawnPet(Player player, PetInstance pi) {
        removePet(player);
        if (pi == null) return;
        PetConfig.Pet def = pi.getDef();
        if (def == null) return;

        EntityType et;
        try { et = EntityType.valueOf(def.type); } catch (Exception e) { et = EntityType.WOLF; }

        Location loc = player.getLocation().clone().add(1, 0, 0);
        LivingEntity pet;
        try {
            Entity e = player.getWorld().spawnEntity(loc, et);
            if (!(e instanceof LivingEntity)) return;
            pet = (LivingEntity) e;
        } catch (Throwable t) {
            return;
        }

        // 设置
        try { pet.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(def.health); } catch (Throwable ignored) {}
        pet.setHealth(def.health);

        String displayName = ChatColor.translateAlternateColorCodes('&', def.name) + " §7(" + player.getName() + ")";
        pet.setCustomName(displayName);
        pet.setCustomNameVisible(true);
        try { pet.setGlowing(def.glowing); } catch (Throwable ignored) {}

        // 防止误伤
        if (pet instanceof Tameable) {
            ((Tameable) pet).setOwner(player);
        }
        if (pet instanceof Ageable && def.baby) {
            ((Ageable) pet).setBaby();
        }

        // 速度
        if (def.speed != 0) {
            try {
                double base = pet.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue();
                pet.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(base + def.speed);
            } catch (Throwable ignored) {}
        }

        playerPet.put(player.getUniqueId(), pet.getUniqueId());
        petOwner.put(pet.getUniqueId(), player.getUniqueId());
        petSpawnLoc.put(pet.getUniqueId(), loc);
    }

    /** 移除宠物实体 */
    public static void removePet(Player player) {
        UUID petId = playerPet.remove(player.getUniqueId());
        if (petId == null) return;
        petOwner.remove(petId);
        petSpawnLoc.remove(petId);
        Entity e = Bukkit.getEntity(petId);
        if (e != null) try { e.remove(); } catch (Throwable ignored) {}
    }

    private static void tryRespawnPet(Player player) {
        PetInstance pi = PetManager.getActivePet(player);
        if (pi == null) {
            removePet(player);
            return;
        }
        spawnPet(player, pi);
    }

    /** AI：跟随玩家 + 攻击附近敌人 */
    private static void handleAI(Player player, LivingEntity pet) {
        Location playerLoc = player.getLocation();
        Location petLoc = pet.getLocation();

        // 距离太远传送
        double dist2 = playerLoc.distanceSquared(petLoc);
        if (dist2 > 200) {  // 14格以上
            pet.teleport(playerLoc.clone().add(1, 0, 1));
            return;
        }

        // 找敌人：玩家的目标 / 最近怪
        LivingEntity target = findTarget(player, pet, 10);
        if (target != null) {
            if (pet instanceof org.bukkit.entity.Creature) ((org.bukkit.entity.Creature) pet).setTarget(target);
            // 近身攻击
            if (pet.getLocation().distanceSquared(target.getLocation()) < 4) {
                try {
                    PetConfig.Pet def = PetManager.getActivePet(player) != null
                            ? PetManager.getActivePet(player).getDef() : null;
                    double dmg = def != null ? def.damage : 5;
                    target.damage(dmg, pet);
                } catch (Throwable ignored) {}
            }
        } else {
            // 无目标，跟随玩家
            if (dist2 > 9) {  // 3格以上
                // 原版 AI 追击：设置 pet 走路
                // 用传送或原版路径，这里用原版
                if (pet instanceof org.bukkit.entity.Creature) ((org.bukkit.entity.Creature) pet).setTarget(null);
                // 简单方式：让宠物朝向玩家
                Vector dir = playerLoc.toVector().subtract(petLoc.toVector()).normalize().multiply(0.3);
                pet.setVelocity(dir);
            }
        }
    }

    private static LivingEntity findTarget(Player player, LivingEntity pet, double range) {
        // 优先玩家的目标
        // 找附近敌对生物
        World world = pet.getWorld();
        LivingEntity best = null;
        double bestDist = range * range;
        for (Entity e : world.getNearbyEntities(pet.getLocation(), range, range, range)) {
            if (!(e instanceof LivingEntity)) continue;
            if (e == player || e == pet) continue;
            if (e instanceof Player) continue;   // 不打玩家
            if (e instanceof Tameable && ((Tameable) e).getOwner() != null) {
                // 其他玩家宠物不打
                UUID owner = ((Tameable) e).getOwner().getUniqueId();
                if (owner.equals(player.getUniqueId())) continue;
                continue;
            }
            if (e instanceof Monster || e instanceof Slime || e instanceof Ghast) {
                double d = e.getLocation().distanceSquared(pet.getLocation());
                if (d < bestDist) { bestDist = d; best = (LivingEntity) e; }
            }
        }
        return best;
    }

    /** 宠物击杀怪物 → 给玩家宠物加经验 */
    public static UUID getOwner(UUID petEntityId) {
        return petOwner.get(petEntityId);
    }

    public static boolean isPet(UUID entityId) {
        return petOwner.containsKey(entityId);
    }

    /** 清理离线玩家的宠物 */
    public static void onPlayerQuit(Player player) {
        removePet(player);
    }
}