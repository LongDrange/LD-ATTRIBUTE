package com.longdrange.ldattribute.core.dungeon;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class DungeonInstance {

    public enum State { STARTING, RUNNING, FINISHED, FAILED }

    public final String dungeonId;
    public State state = State.STARTING;
    public int currentWave = -1;      // -1 = 未开始
    public long startTime;
    public long endTime;
    public BukkitTask introTask;
    public BukkitTask waveTask;
    public BukkitTask timeoutTask;
    public BukkitTask scoreboardTask;
    public org.bukkit.scoreboard.Scoreboard scoreboard;

    public final Set<UUID> players = new LinkedHashSet<>();
    public final Map<UUID, Location> previousLocations = new HashMap<>();
    public final Set<UUID> spawnedMobs = new HashSet<>();   // 存 entity UUID
    public boolean playerDied = false;   // 是否有人死亡
    public String worldName;   // 动态分配的世界名

    public DungeonInstance(String dungeonId) {
        this.dungeonId = dungeonId;
        this.startTime = System.currentTimeMillis();
    }

    public void addPlayer(Player p, Location prev) {
        players.add(p.getUniqueId());
        previousLocations.put(p.getUniqueId(), prev);
    }

    public void removePlayer(UUID uuid) {
        players.remove(uuid);
    }

    public boolean isEmpty() { return players.isEmpty(); }

    public long remainingSeconds() {
        if (state == State.FINISHED || state == State.FAILED) return 0;
        long elapsed = (System.currentTimeMillis() - startTime) / 1000L;
        // time limit 从 config 里取，调用方保证传入
        return elapsed;
    }
}
