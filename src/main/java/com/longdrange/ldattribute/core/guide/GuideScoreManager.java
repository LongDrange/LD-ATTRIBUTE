package com.longdrange.ldattribute.core.guide;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图鉴收藏分/称号管理器
 */
public class GuideScoreManager {

    private final LDAttribute plugin;
    // playerUUID -> 上次已知称号ID（用于检测升级）
    private final Map<UUID, String> lastTitle = new ConcurrentHashMap<>();

    public GuideScoreManager(LDAttribute plugin) { this.plugin = plugin; }

    public void reload() {
        lastTitle.clear();
        GuideScoreConfig.load(plugin);
    }

    /**
     * 计算玩家收藏分：
     *   = Σ 已解锁图鉴的 Score + Σ 集齐分组的 SetScore
     */
    public int getScore(Player player) {
        if (player == null) return 0;
        GuideData data = plugin.getGuideManager().get(player);
        int total = 0;

        // 1. 已解锁图鉴的分数
        for (GuideConfig.MonsterDef def : GuideConfig.allMonsters()) {
            if (!data.isUnlocked(def.id)) continue;
            int sc = def.score;
            if (sc <= 0) sc = GuideScoreConfig.scorePerUnlock;
            total += sc;
        }

        // 2. 集齐分组的分数
        for (GuideConfig.GroupDef g : GuideConfig.allGroups()) {
            List<GuideConfig.MonsterDef> list = GuideConfig.byGroup(g.id);
            if (list.isEmpty()) continue;
            boolean allUnlocked = true;
            for (GuideConfig.MonsterDef d : list) {
                if (!data.isUnlocked(d.id)) { allUnlocked = false; break; }
            }
            if (allUnlocked) {
                int sc = g.setScore;
                if (sc <= 0) sc = GuideScoreConfig.scorePerSet;
                total += sc;
            }
        }

        return total;
    }

    /** 获取玩家当前应得的称号（可能 null） */
    public GuideScoreConfig.TitleDef getCurrentTitle(Player player) {
        return GuideScoreConfig.getTitleByScore(getScore(player));
    }

    /**
     * 检查并触发称号变更（玩家解锁图鉴后调用）
     * 返回是否升级
     */
    public boolean checkAndUpdate(Player player) {
        if (player == null) return false;
        GuideScoreConfig.TitleDef t = getCurrentTitle(player);
        String newId = (t == null) ? null : t.id;
        String oldId = lastTitle.get(player.getUniqueId());

        if (Objects.equals(newId, oldId)) return false;

        lastTitle.put(player.getUniqueId(), newId);

        if (t != null) {
            // 发消息
            player.sendMessage("");
            player.sendMessage(ChatColor.GREEN + "§l✦ 获得称号：" + t.name);
            if (!t.attribute.isEmpty()) {
                for (String a : t.attribute) {
                    player.sendMessage("  " + ChatColor.GRAY + "· " + ChatColor.WHITE + a);
                }
            }
            // 执行命令
            for (String cmd : t.commands) {
                try {
                    String run = cmd.replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), run.startsWith("/") ? run.substring(1) : run);
                } catch (Throwable ignored) {}
            }
            try { player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f); } catch (Throwable ignored) {}
        }
        return true;
    }

    /** 返回玩家当前称号的所有属性行（用于 StatsProvider） */
    public List<String> getTitleAttributes(Player player) {
        List<String> out = new ArrayList<>();
        GuideScoreConfig.TitleDef t = getCurrentTitle(player);
        if (t != null) out.addAll(t.attribute);
        return out;
    }

    /** 返回分组集齐的属性行 */
    public List<String> getSetAttributes(Player player) {
        List<String> out = new ArrayList<>();
        if (player == null) return out;
        GuideData data = plugin.getGuideManager().get(player);
        for (GuideConfig.GroupDef g : GuideConfig.allGroups()) {
            List<GuideConfig.MonsterDef> list = GuideConfig.byGroup(g.id);
            if (list.isEmpty()) continue;
            boolean allUnlocked = true;
            for (GuideConfig.MonsterDef d : list) {
                if (!data.isUnlocked(d.id)) { allUnlocked = false; break; }
            }
            if (allUnlocked) {
                out.addAll(g.setAttribute);
            }
        }
        return out;
    }

    /** 主动刷新某玩家的称号缓存（图鉴解锁后调用） */
    public void refresh(Player player) {
        if (player != null) {
            checkAndUpdate(player);
        }
    }
}