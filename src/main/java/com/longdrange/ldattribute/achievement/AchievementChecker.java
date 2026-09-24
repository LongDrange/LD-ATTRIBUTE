package com.longdrange.ldattribute.achievement;

import org.bukkit.entity.Player;

import java.util.UUID;

public class AchievementChecker {

    public static void checkSnapshot(Player player, AchievementConfig.Type type) {
        UUID uuid = player.getUniqueId();
        for (AchievementConfig.Achievement a : AchievementConfig.getAll()) {
            if (a.type != type) continue;
            int before = AchievementData.getProgress(uuid, a.id);
            int current = snapshotValue(player, type);
            AchievementData.setProgress(uuid, a.id, current);
            if (before < a.target && current >= a.target) {
                player.sendMessage("§8[§6成就§8] §a✓ 完成 §e" + a.name + " §7- " + a.description);
                try { com.longdrange.ldattribute.achievement.AchievementManager.claim(player, a.id); } catch (Throwable ignored) {}
            }
        }
    }

    public static void addProgress(Player player, AchievementConfig.Type type, int delta) {
        UUID uuid = player.getUniqueId();
        for (AchievementConfig.Achievement a : AchievementConfig.getAll()) {
            if (a.type != type) continue;
            int before = AchievementData.getProgress(uuid, a.id);
            int after = AchievementData.addProgress(uuid, a.id, delta);
            if (before < a.target && after >= a.target) {
                player.sendMessage("§8[§6成就§8] §a✓ 完成 §e" + a.name + " §7- " + a.description);
                try { player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f); } catch (Throwable ignored) {}
                try { com.longdrange.ldattribute.achievement.AchievementManager.claim(player, a.id); } catch (Throwable ignored) {}
            }
        }
    }

    private static int snapshotValue(Player player, AchievementConfig.Type type) {
        switch (type) {
            case CARD_COUNT:
                try { return com.longdrange.ldattribute.card.PlayerData.getOwnedCardIds(player.getUniqueId()).size(); }
                catch (Throwable t) { return 0; }
            case RUNE_COLLECT:
                try { return com.longdrange.ldattribute.rune.RuneData.count(player.getUniqueId()); }
                catch (Throwable t) { return 0; }
            case PET_COLLECT:
                try { return com.longdrange.ldattribute.pet.PetData.getAllPets(player.getUniqueId()).size(); }
                catch (Throwable t) { return 0; }
            case POINTS:
                try { return com.longdrange.ldattribute.points.PointAPI.getPlayerPoints(player.getName()); }
                catch (Throwable t) { return 0; }
            default:
                return 0;
        }
    }
}
