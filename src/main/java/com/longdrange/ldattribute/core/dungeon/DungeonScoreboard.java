package com.longdrange.ldattribute.core.dungeon;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DungeonScoreboard {

    public static void start(LDAttribute plugin, DungeonInstance inst, DungeonConfig.DungeonDef def) {
        if (inst.scoreboard != null) return;

        // 延迟 20 tick 后启动（等传送完成）
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (inst.state != DungeonInstance.State.RUNNING) return;

            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective obj = board.registerNewObjective("ld_dungeon", "dummy");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            obj.setDisplayName(def.name);

            inst.scoreboard = board;

            // 应用给每个玩家
            for (UUID u : inst.players) {
                Player p = Bukkit.getPlayer(u);
                if (p != null) {
                    try { p.setScoreboard(board); } catch (Throwable ignored) {}
                }
            }

            // 每秒更新
            inst.scoreboardTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (inst.state != DungeonInstance.State.RUNNING) {
                        try { cancel(); } catch (Throwable ignored) {}
                        return;
                    }
                    update(board, obj, inst, def);
                }
            }.runTaskTimer(plugin, 0L, 20L);
        }, 20L);
    }

    private static void update(Scoreboard board, Objective obj, DungeonInstance inst,
                                DungeonConfig.DungeonDef def) {
        // 清空
        for (String entry : new ArrayList<>(board.getEntries())) {
            board.resetScores(entry);
        }

        int totalWaves = def.waves.size();
        int curWave = Math.max(1, Math.min(inst.currentWave + 1, totalWaves));
        long elapsed = (System.currentTimeMillis() - inst.startTime) / 1000L;
        long remain = Math.max(0, def.timeLimit - elapsed);
        String timeStr = String.format("%02d:%02d", remain / 60, remain % 60);

        int teamSize = inst.players.size();
        int maxTeam = DungeonConfig.maxTeamSize;

        List<String> lines = new ArrayList<>();
        lines.add(DungeonConfig.msg("SbLine", "\u00a77\u00a7m--------------------"));
        lines.add(DungeonConfig.msg("SbDungeon", "\u00a7f\u526f\u672c: \u00a7e{0}", stripColor(def.name)));
        lines.add(DungeonConfig.msg("SbWave", "\u00a7f\u6ce2\u6b21: \u00a7e{0}\u00a77/\u00a7e{1}", curWave, totalWaves));
        lines.add(DungeonConfig.msg("SbTime", "\u00a7f\u65f6\u95f4: \u00a7c{0}", timeStr));
        lines.add(DungeonConfig.msg("SbTeam", "\u00a7f\u961f\u4f0d: \u00a7a{0}\u00a77/\u00a7a{1}", teamSize, maxTeam));
        lines.add(DungeonConfig.msg("SbLine", "\u00a77\u00a7m--------------------"));
        lines.add(DungeonConfig.msg("SbStatus", "\u00a7e\u526f\u672c\u8fdb\u884c\u4e2d..."));

        int score = lines.size();
        for (String line : lines) {
            obj.getScore(line).setScore(score--);
        }

        // 每秒重新 setScoreboard（防止被内置侧边栏覆盖）
        for (UUID u : inst.players) {
            Player p = Bukkit.getPlayer(u);
            if (p == null) continue;
            try {
                if (p.getScoreboard() != board) {
                    p.setScoreboard(board);
                }
            } catch (Throwable ignored) {}
        }
    }

    private static String stripColor(String s) {
        return org.bukkit.ChatColor.stripColor(s);
    }

    public static void stop(DungeonInstance inst) {
        if (inst == null) return;

        if (inst.scoreboardTask != null) {
            try { inst.scoreboardTask.cancel(); } catch (Throwable ignored) {}
            inst.scoreboardTask = null;
        }

        for (UUID u : new ArrayList<>(inst.players)) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                try { p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard()); } catch (Throwable ignored) {}
            }
        }
        inst.scoreboard = null;
    }
}
