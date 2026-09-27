package com.longdrange.ldattribute.core.dungeon;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DungeonRankManager {

    public static class RankEntry {
        public String dungeonId;
        public long fastestTime = Long.MAX_VALUE;   // 秒
        public String fastestPlayer = "";
        public long fastestDate = 0;
        public int totalClears = 0;
        public long totalPoints = 0;
        public Map<String, Integer> playerClears = new LinkedHashMap<>();
    }

    private final LDAttribute plugin;
    private final File file;
    private YamlConfiguration cfg;
    private final Map<String, RankEntry> ranks = new LinkedHashMap<>();

    public DungeonRankManager(LDAttribute plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/dungeon_ranks.yml");
        load();
    }

    public void load() {
        ranks.clear();
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = cfg.getConfigurationSection("Dungeons");
        if (sec != null) {
            for (String did : sec.getKeys(false)) {
                ConfigurationSection s = sec.getConfigurationSection(did);
                if (s == null) continue;
                RankEntry e = new RankEntry();
                e.dungeonId = did;
                e.fastestTime = s.getLong("FastestTime", Long.MAX_VALUE);
                e.fastestPlayer = s.getString("FastestPlayer", "");
                e.fastestDate = s.getLong("FastestDate", 0);
                e.totalClears = s.getInt("TotalClears", 0);
                e.totalPoints = s.getLong("TotalPoints", 0);
                ConfigurationSection psec = s.getConfigurationSection("PlayerClears");
                if (psec != null) {
                    for (String pn : psec.getKeys(false)) {
                        e.playerClears.put(pn, psec.getInt(pn, 0));
                    }
                }
                ranks.put(did, e);
            }
        }
    }

    public void save() {
        for (Map.Entry<String, RankEntry> en : ranks.entrySet()) {
            String did = en.getKey();
            RankEntry e = en.getValue();
            String base = "Dungeons." + did + ".";
            cfg.set(base + "FastestTime", e.fastestTime == Long.MAX_VALUE ? -1L : e.fastestTime);
            cfg.set(base + "FastestPlayer", e.fastestPlayer);
            cfg.set(base + "FastestDate", e.fastestDate);
            cfg.set(base + "TotalClears", e.totalClears);
            cfg.set(base + "TotalPoints", e.totalPoints);
            cfg.set(base + "PlayerClears", null);
            for (Map.Entry<String, Integer> p : e.playerClears.entrySet()) {
                cfg.set(base + "PlayerClears." + p.getKey(), p.getValue());
            }
        }
        try { cfg.save(file); } catch (IOException e) {
            plugin.getLogger().warning("保存 dungeon_ranks.yml 失败: " + e.getMessage());
        }
    }

    public RankEntry get(String dungeonId) {
        return ranks.computeIfAbsent(dungeonId, k -> {
            RankEntry e = new RankEntry();
            e.dungeonId = k;
            return e;
        });
    }

    /** 记录一次通关 */
    public void recordClear(String dungeonId, String playerName, long timeSec, long points) {
        RankEntry e = get(dungeonId);
        e.totalClears++;
        e.totalPoints += points;

        if (timeSec < e.fastestTime) {
            e.fastestTime = timeSec;
            e.fastestPlayer = playerName;
            e.fastestDate = System.currentTimeMillis();
        }

        e.playerClears.merge(playerName, 1, Integer::sum);
    }

    /** 玩家通关次数排行（降序） */
    public List<Map.Entry<String, Integer>> topPlayers(String dungeonId, int limit) {
        RankEntry e = get(dungeonId);
        List<Map.Entry<String, Integer>> list = new ArrayList<>(e.playerClears.entrySet());
        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        if (list.size() > limit) list = list.subList(0, limit);
        return list;
    }

    public static String formatTime(long sec) {
        if (sec <= 0 || sec == Long.MAX_VALUE) return "—";
        if (sec < 60) return sec + "秒";
        long m = sec / 60;
        long s = sec % 60;
        return m + "分" + s + "秒";
    }
}