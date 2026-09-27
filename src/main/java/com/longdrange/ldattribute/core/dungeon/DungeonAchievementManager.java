package com.longdrange.ldattribute.core.dungeon;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.card.CardData;
import com.longdrange.ldattribute.card.CardDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DungeonAchievementManager {

    private final LDAttribute plugin;
    private final File file;
    private org.bukkit.configuration.file.YamlConfiguration cfg;
    // playerUUID -> set of achievementId
    private final Map<UUID, Set<String>> unlocked = new ConcurrentHashMap<>();

    public DungeonAchievementManager(LDAttribute plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/dungeon_achievements.yml");
        load();
    }

    public void load() {
        unlocked.clear();
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        for (String uuidStr : cfg.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                List<String> list = cfg.getStringList(uuidStr);
                if (list != null) unlocked.put(uuid, new LinkedHashSet<>(list));
            } catch (Throwable ignored) {}
        }
    }

    public void save() {
        for (Map.Entry<UUID, Set<String>> e : unlocked.entrySet()) {
            cfg.set(e.getKey().toString(), new ArrayList<>(e.getValue()));
        }
        try { cfg.save(file); } catch (IOException e) {
            plugin.getLogger().warning("保存 dungeon_achievements.yml 失败: " + e.getMessage());
        }
    }

    public boolean has(UUID uuid, String achievementId) {
        Set<String> set = unlocked.get(uuid);
        return set != null && set.contains(achievementId);
    }

    public boolean unlock(Player p, String achievementId) {
        DungeonAchievementConfig.AchievementDef def = DungeonAchievementConfig.get(achievementId);
        if (def == null) return false;

        Set<String> set = unlocked.computeIfAbsent(p.getUniqueId(), k -> new LinkedHashSet<>());
        if (set.contains(achievementId)) return false;

        set.add(achievementId);
        save();

        // 发奖励
        p.sendMessage("");
        p.sendMessage("\u00a7a\u00a7l\u2714 \u6210\u5c31\u89e3\u9501\uff1a " + def.name);
        if (def.description != null && !def.description.isEmpty()) p.sendMessage("\u00a77  " + def.description);
        giveReward(p, def);

        return true;
    }

    private void giveReward(Player p, DungeonAchievementConfig.AchievementDef def) {
        if (def.rewardPoints > 0) {
            try {
                com.longdrange.ldattribute.points.PointAPI.addPlayerPoints(p.getName(), def.rewardPoints);
                p.sendMessage("\u00a77  - \u70b9\u5238: \u00a76" + def.rewardPoints);
            } catch (Throwable ignored) {}
        }
        if (def.rewardVault > 0) {
            try {
                org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
                        Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                if (rsp != null) rsp.getProvider().depositPlayer(p, def.rewardVault);
                p.sendMessage("\u00a77  - \u91d1\u5e01: \u00a76" + (long) def.rewardVault);
            } catch (Throwable ignored) {}
        }
        for (String s : def.rewardItems) {
            String[] parts = s.split(":");
            if (parts.length < 2) continue;
            Material mat = Material.getMaterial(parts[0].toUpperCase());
            if (mat == null) continue;
            int amt = 1;
            try { amt = Integer.parseInt(parts[1]); } catch (Throwable ignored) {}
            ItemStack item = new ItemStack(mat, Math.min(64, amt));
            try {
                com.longdrange.ldattribute.core.soulring.SoulRingData sr = plugin.getSoulRingManager().get(p);
                sr.deposit(item, item.getAmount());
                p.sendMessage("\u00a77  - \u7269\u54c1: \u00a7f" + mat.name() + " x" + amt);
            } catch (Throwable t) {
                p.getInventory().addItem(item);
            }
        }
        for (String s : def.rewardCards) {
            String[] parts = s.split(":");
            if (parts.length < 1) continue;
            String cardId = parts[0];
            int amt = 1;
            if (parts.length >= 2) { try { amt = Integer.parseInt(parts[1]); } catch (Throwable ignored) {} }
            CardData card = CardDataManager.getCard(cardId);
            if (card == null) continue;
            for (int i = 0; i < amt; i++) {
                ItemStack item = card.getItem();
                try {
                    com.longdrange.ldattribute.core.soulring.SoulRingData sr = plugin.getSoulRingManager().get(p);
                    sr.deposit(item, item.getAmount());
                } catch (Throwable t) {
                    p.getInventory().addItem(item);
                }
            }
            p.sendMessage("\u00a77  - \u5361\u7247: \u00a7f" + cardId + " x" + amt);
        }
        for (String cmd : def.rewardCommands) {
            try {
                String run = cmd.replace("%player%", p.getName());
                Bukkit.dispatchCommand(p, run.startsWith("/") ? run.substring(1) : run);
            } catch (Throwable ignored) {}
        }
    }

    public Set<String> getUnlocked(UUID uuid) {
        return unlocked.getOrDefault(uuid, new LinkedHashSet<>());
    }

    /** 通关时检查所有成就 */
    public void checkOnClear(DungeonInstance inst, DungeonConfig.DungeonDef def, long clearSec) {
        for (UUID u : inst.players) {
            Player p = Bukkit.getPlayer(u);
            if (p == null) continue;
            for (DungeonAchievementConfig.AchievementDef a : DungeonAchievementConfig.all()) {
                if (has(u, a.id)) continue;
                if (!a.dungeonId.isEmpty() && !a.dungeonId.equals(def.id)) continue;

                boolean unlock = false;
                switch (a.type) {
                    case FIRST_CLEAR:
                        unlock = true;
                        break;
                    case FAST_CLEAR:
                        if (a.seconds > 0 && clearSec <= a.seconds) unlock = true;
                        break;
                    case NO_DEATH:
                        if (!inst.playerDied) unlock = true;
                        break;
                    case SOLO_CLEAR:
                        if (inst.players.size() == 1) unlock = true;
                        break;
                    case FULL_TEAM:
                        if (inst.players.size() >= DungeonConfig.maxTeamSize) unlock = true;
                        break;
                    case CLEAR_COUNT:
                        if (a.count > 0) {
                            DungeonRankManager.RankEntry re = plugin.getDungeonRankManager().get(def.id);
                            int cnt = re.playerClears.getOrDefault(p.getName(), 0);
                            if (cnt >= a.count) unlock = true;
                        }
                        break;
                }
                if (unlock) unlock(p, a.id);
            }
        }
    }
}