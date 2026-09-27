package com.longdrange.ldattribute.core.dungeon;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.card.CardData;
import com.longdrange.ldattribute.card.CardDataManager;
import com.longdrange.ldattribute.core.data.PlayerModuleData;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DungeonManager {

    public static final String MODULE = "dungeon";
    private static final java.util.concurrent.atomic.AtomicInteger WORLD_COUNTER = new java.util.concurrent.atomic.AtomicInteger(0);

    private final LDAttribute plugin;
    private final Map<UUID, DungeonInstance> playerInstances = new ConcurrentHashMap<>();
    private final Map<String, DungeonInstance> instances = new ConcurrentHashMap<>();

    public DungeonManager(LDAttribute plugin) {
        this.plugin = plugin;
        registerDeathListener();
    }

    public void reload() {
        DungeonConfig.load(plugin);
    }


    private static boolean tryCreateJunction(File link, File target) {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) return false;
        try {
            Process p = Runtime.getRuntime().exec(new String[]{
                "cmd", "/c", "mklink", "/J",
                link.getAbsolutePath(),
                target.getAbsolutePath()
            });
            int code = p.waitFor();
            return code == 0 && link.exists();
        } catch (Throwable t) {
            return false;
        }
    }

    /** 从模板复制一份独立世界，返回加载好的 World（失败 null） */
    private World createInstanceWorld(DungeonConfig.DungeonDef def, String worldName) {
        File template = def.worldFolder;
        File target = new File(Bukkit.getWorldContainer(), worldName);

        // 复制模板
        if (template.exists() && template.isDirectory()) {
            try {
                copyFolder(template.toPath(), target.toPath());
                plugin.getLogger().info("[Dungeon] 已复制世界模板到 " + worldName);
            } catch (Throwable t) {
                plugin.getLogger().warning("[Dungeon] 复制模板失败: " + t.getMessage());
                return null;
            }
        } else {
            plugin.getLogger().warning("[Dungeon] 模板不存在: " + template.getAbsolutePath());
            return null;
        }

        // 加载世界
        try {
            WorldCreator wc = new WorldCreator(worldName);
            wc.generateStructures(false);
            World w = wc.createWorld();
            if (w != null) {
                w.setGameRuleValue("doMobSpawning", "false");
                w.setGameRuleValue("doDaylightCycle", "false");
                w.setGameRuleValue("doWeatherCycle", "false");
                w.setGameRuleValue("keepInventory", "true");
                w.setGameRuleValue("mobGriefing", "false");
                plugin.getLogger().info("[Dungeon] 世界已加载: " + worldName);
            }
            return w;
        } catch (Throwable t) {
            plugin.getLogger().warning("[Dungeon] 加载世界失败: " + t.getMessage());
            return null;
        }
    }

    /** 删除副本世界文件夹（异步安全，重试 5 次） */
    private void deleteInstanceWorld(String worldName, int retry) {
        File target = new File(Bukkit.getWorldContainer(), worldName);
        if (!target.exists()) return;

        if (retry > 5) {
            plugin.getLogger().warning("[Dungeon] 删除世界失败（已重试 5 次）: " + worldName);
            return;
        }

        try {
            deleteRecursive(target);
            if (!target.exists()) {
                plugin.getLogger().info("[Dungeon] 已删除世界: " + worldName);
                return;
            }
        } catch (Throwable ignored) {}

        // 重试
        final int nextRetry = retry + 1;
        Bukkit.getScheduler().runTaskLater(plugin, () -> deleteInstanceWorld(worldName, nextRetry), 40L);
    }

    private static void deleteRecursive(File f) throws IOException {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursive(c);
            }
        }
        // 先删 session.lock（可能被锁）
        if (f.getName().equals("session.lock")) {
            try { f.setWritable(true); } catch (Throwable ignored) {}
        }
        if (!f.delete()) {
            // 尝试放宽权限再删
            try { f.setWritable(true); } catch (Throwable ignored) {}
            if (!f.delete()) throw new IOException("无法删除: " + f.getAbsolutePath());
        }
    }

    private static void copyFolder(Path src, Path dst) throws IOException {
        Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path tgt = dst.resolve(src.relativize(dir));
                Files.createDirectories(tgt);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, dst.resolve(src.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    // ==================== CD 管理 ====================
    public long getCooldownEnd(UUID uuid, String dungeonId) {
        try {
            PlayerModuleData raw = plugin.getModuleDataManager().get(uuid, MODULE);
            return raw.getLong("cd." + dungeonId, 0);
        } catch (Throwable t) { return 0; }
    }

    public void setCooldown(UUID uuid, String dungeonId, int cooldownSec) {
        try {
            PlayerModuleData raw = plugin.getModuleDataManager().get(uuid, MODULE);
            raw.set("cd." + dungeonId, System.currentTimeMillis() + cooldownSec * 1000L);
        } catch (Throwable ignored) {}
    }

    public long getRemainingCooldown(UUID uuid, String dungeonId) {
        long end = getCooldownEnd(uuid, dungeonId);
        long now = System.currentTimeMillis();
        return Math.max(0, (end - now) / 1000L);
    }

    // ==================== 工具：发消息 / 执行命令 ====================
    private void sendToInstance(DungeonInstance inst, String msg) {
        for (UUID u : inst.players) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) p.sendMessage(msg);
        }
    }

    private void sendListToInstance(DungeonInstance inst, List<String> msgs) {
        if (msgs == null || msgs.isEmpty()) return;
        for (UUID u : inst.players) {
            Player p = Bukkit.getPlayer(u);
            if (p == null) continue;
            for (String m : msgs) p.sendMessage(m);
        }
    }

    private void runCommandsForInstance(DungeonInstance inst, List<String> cmds) {
        if (cmds == null || cmds.isEmpty()) return;
        for (UUID u : inst.players) {
            Player p = Bukkit.getPlayer(u);
            if (p == null) continue;
            for (String cmd : cmds) {
                String run = cmd.replace("%player%", p.getName());
                try {
                    Bukkit.dispatchCommand(p, run.startsWith("/") ? run.substring(1) : run);
                } catch (Throwable ignored) {}
            }
        }
    }

    // ==================== 开始副本 ====================
    public boolean startDungeon(Player player, DungeonConfig.DungeonDef def) {
        if (playerInstances.containsKey(player.getUniqueId())) {
            player.sendMessage(DungeonConfig.msg("AlreadyInDungeon", "\u00a7c你已在副本中"));
            return false;
        }

        long cdRemaining = getRemainingCooldown(player.getUniqueId(), def.id);
        if (cdRemaining > 0) {
            player.sendMessage(DungeonConfig.msg("Cooldown", "\u00a7c冷却中，还需 \u00a7e{0}", formatTime(cdRemaining)));
            return false;
        }

        if (def.entry.costPoints > 0) {
            try {
                int cur = com.longdrange.ldattribute.points.PointAPI.getPlayerPoints(player.getName());
                if (cur < def.entry.costPoints) {
                    player.sendMessage(DungeonConfig.msg("NotEnoughPoints", "\u00a7c点券不足（需要 \u00a7e{0}\u00a7c）", def.entry.costPoints));
                    return false;
                }
            } catch (Throwable ignored) {}
        }

        String uniqueWorldName = def.worldName + "_" + WORLD_COUNTER.incrementAndGet();
        World w = createInstanceWorld(def, uniqueWorldName);
        if (w == null) {
            player.sendMessage(DungeonConfig.msg("WorldNotReady", "\u00a7c副本世界未就绪，请联系管理员"));
            return false;
        }

        if (def.entry.costPoints > 0) {
            try {
                com.longdrange.ldattribute.points.PointAPI.takePlayerPoints(player.getName(), def.entry.costPoints);
            } catch (Throwable ignored) {}
        }

        DungeonInstance inst = new DungeonInstance(def.id);
        inst.worldName = uniqueWorldName;
        Location prev = player.getLocation().clone();
        inst.addPlayer(player, prev);
        instances.put(def.id, inst);
        playerInstances.put(player.getUniqueId(), inst);

        // ===== 组队拉人 =====
        com.longdrange.ldattribute.core.dungeon.DungeonTeam team = plugin.getDungeonTeamManager().getTeam(player.getUniqueId());
        if (team != null && team.isLeader(player.getUniqueId())) {
            for (UUID u : new ArrayList<>(team.members)) {
                if (u.equals(player.getUniqueId())) continue;
                Player m = Bukkit.getPlayer(u);
                if (m == null) continue;
                if (playerInstances.containsKey(u)) {
                    m.sendMessage("\u00a7c\u4f60\u5df2\u5728\u5176\u4ed6\u526f\u672c\u4e2d\uff0c\u8df3\u8fc7");
                    continue;
                }
                Location mPrev = m.getLocation().clone();
                inst.addPlayer(m, mPrev);
                playerInstances.put(u, inst);
            }
        }

        Location spawnLoc = new Location(w, def.spawn.x, def.spawn.y, def.spawn.z,
                def.spawn.yaw, def.spawn.pitch);
        for (UUID u : inst.players) { Player p = Bukkit.getPlayer(u); if (p != null) p.teleport(spawnLoc); }
        DungeonScoreboard.start(plugin, inst, def);

        // 开场剧情
        for (String line : def.intro) {
            player.sendMessage(line);
        }

        // 开始消息 + 命令
        sendListToInstance(inst, def.startMessages);
        runCommandsForInstance(inst, def.startCommands);

        inst.state = DungeonInstance.State.RUNNING;
        inst.currentWave = 0;
        final int introDelay = def.introDelay;
        new BukkitRunnable() {
            @Override public void run() {
                if (inst.state != DungeonInstance.State.RUNNING) return;
                spawnWave(inst, def, 0);
            }
        }.runTaskLater(plugin, introDelay * 20L);

        inst.timeoutTask = new BukkitRunnable() {
            @Override public void run() {
                if (inst.state == DungeonInstance.State.RUNNING) {
                    for (UUID u : new ArrayList<>(inst.players)) {
                        Player p = Bukkit.getPlayer(u);
                        if (p != null) p.sendMessage("\u00a7c\u00a7l副本失败：时间到了！");
                    }
                    failDungeon(inst, def);
                }
            }
        }.runTaskLater(plugin, def.timeLimit * 20L);

        player.sendMessage(DungeonConfig.msg("EnterDungeon", "\u00a7a已进入副本 \u00a7e{0}\u00a7a，限时 \u00a7e{1} \u00a7a秒", def.name, def.timeLimit));
        return true;
    }

    // ==================== 刷一波怪（支持同波多怪） ====================
    private void spawnWave(DungeonInstance inst, DungeonConfig.DungeonDef def, int waveIdx) {
        if (waveIdx >= def.waves.size()) {
            finishDungeon(inst, def);
            return;
        }

        DungeonConfig.WaveDef wave = def.waves.get(waveIdx);
        inst.currentWave = waveIdx;

        // 波次提示
        sendToInstance(inst, "");
        sendToInstance(inst, DungeonConfig.msg("WaveStart", "\u00a76\u00a7l\u2694 {0}", wave.name));
        if (wave.subtitle != null && !wave.subtitle.isEmpty()) {
            sendToInstance(inst, wave.subtitle);
        }
        sendToInstance(inst, DungeonConfig.msg("WaveIncoming", "\u00a7e怪物将在 \u00a7c{0} \u00a7e秒后出现！", wave.delay));

        // 波次开始前 消息 + 命令
        sendListToInstance(inst, wave.beforeMessages);
        runCommandsForInstance(inst, wave.beforeCommands);

        // 延迟 delay 秒后刷怪
        new BukkitRunnable() {
            @Override public void run() {
                if (inst.state != DungeonInstance.State.RUNNING) return;
                World w = Bukkit.getWorld(inst.worldName);
                if (w == null) return;

                // 遍历同波所有怪（支持多种）
                for (DungeonConfig.MobSpawn ms : wave.mobs) {
                    Location loc = new Location(w, ms.x, ms.y, ms.z);
                    for (int i = 0; i < ms.count; i++) {
                        org.bukkit.entity.Entity ent = spawnMythicMob(ms.mobId, loc);
                        if (ent == null) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                    "mm mobs spawn " + ms.mobId + " 1 " + inst.worldName
                                            + "," + ms.x + "," + ms.y + "," + ms.z);
                        } else {
                            inst.spawnedMobs.add(ent.getUniqueId());
                        }
                    }
                }

                sendToInstance(inst, DungeonConfig.msg("WaveBegin", "\u00a7c\u00a7l\u2757 {0} 已开始！", wave.name));
            }
        }.runTaskLater(plugin, wave.delay * 20L);
    }

    /** 反射刷 MM 怪，返回 entity（失败 null） */
    private org.bukkit.entity.Entity spawnMythicMob(String mobId, Location loc) {
        try {
            Class<?> mmClass = Class.forName("io.lumine.xikage.mythicmobs.MythicMobs");
            Object mm = mmClass.getMethod("inst").invoke(null);
            Object apiHelper = mm.getClass().getMethod("getAPIHelper").invoke(mm);
            // spawnMythicMob(String, Location) 返回 Entity
            Object result = apiHelper.getClass()
                    .getMethod("spawnMythicMob", String.class, Location.class)
                    .invoke(apiHelper, mobId, loc);
            if (result instanceof org.bukkit.entity.Entity) {
                return (org.bukkit.entity.Entity) result;
            }
            return null;
        } catch (Throwable t) {
            plugin.getLogger().warning("[Dungeon] 刷怪失败 " + mobId + ": " + t.getMessage());
            return null;
        }
    }

    // ==================== 怪物死亡监听 ====================
    private void registerDeathListener() {
        try {
            Class<?> clazz = Class.forName("io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent");
            @SuppressWarnings("unchecked")
            Class<? extends Event> evClass = (Class<? extends Event>) clazz;
            Bukkit.getPluginManager().registerEvent(
                    evClass,
                    new Listener() {},
                    EventPriority.MONITOR,
                    (listener, event) -> handleMMDeath(event),
                    plugin
            );
            plugin.getLogger().info("[Dungeon] 已挂钩 MythicMobDeathEvent");
        } catch (Throwable t) {
            plugin.getLogger().info("[Dungeon] 未检测到 MM，跳过监听");
        }
    }

    private void handleMMDeath(Event event) {
        try {
            java.lang.reflect.Method getEntity = event.getClass().getMethod("getEntity");
            LivingEntity entity = (LivingEntity) getEntity.invoke(event);
            if (entity == null) return;

            World w = entity.getWorld();
            if (w == null) return;

            DungeonInstance inst = null;
            DungeonConfig.DungeonDef def = null;
            for (DungeonInstance i : instances.values()) {
                DungeonConfig.DungeonDef d = DungeonConfig.get(i.dungeonId);
                if (d != null && i.worldName != null && i.worldName.equals(w.getName())) {
                    inst = i; def = d; break;
                }
            }
            if (inst == null || def == null) return;

            // 直接触发（不再判断 spawnedMobs）
            if (inst.spawnedMobs.remove(entity.getUniqueId())) {
            checkWaveCleared(inst, def);
            }
        } catch (Throwable ignored) {}
    }

    private void checkWaveCleared(DungeonInstance inst, DungeonConfig.DungeonDef def) {
        // 用 spawnedMobs 里记录的 UUID 判断是否还有活怪
        if (!inst.spawnedMobs.isEmpty()) return;

        // 延迟 2 秒确认（防止某些情况下 UUID 已被移除但实体还活着）
        new BukkitRunnable() {
            @Override public void run() {
                if (inst.state != DungeonInstance.State.RUNNING) return;
                if (!inst.spawnedMobs.isEmpty()) return;

                // 当前波清空 → 播报"波次结束"消息/命令
                int cur = inst.currentWave;
                if (cur >= 0 && cur < def.waves.size()) {
                    DungeonConfig.WaveDef wave = def.waves.get(cur);
                    sendListToInstance(inst, wave.afterMessages);

                    // ===== 发放波次奖励 =====
                    if (wave.reward != null && hasReward(wave.reward)) {
                        for (UUID u : new ArrayList<>(inst.players)) {
                            Player p = Bukkit.getPlayer(u);
                            if (p == null) continue;
                            p.sendMessage("");
                            p.sendMessage(DungeonConfig.msg("WaveReward", "\u00a7a\u00a7l\u2714 {0} 奖励！", org.bukkit.ChatColor.stripColor(wave.name)));
                            giveReward(p, wave.reward);
                            if (wave.reward.commands != null) {
                                for (String cmd : wave.reward.commands) {
                                    try {
                                        String run = cmd.replace("%player%", p.getName());
                                        Bukkit.dispatchCommand(p, run.startsWith("/") ? run.substring(1) : run);
                                    } catch (Throwable ignored) {}
                                }
                            }
                            if (wave.reward.messages != null) {
                                for (String m : wave.reward.messages) p.sendMessage(m);
                            }
                        }
                    }
                    runCommandsForInstance(inst, wave.afterCommands);
                }

                int next = inst.currentWave + 1;
                if (next >= def.waves.size()) {
                    finishDungeon(inst, def);
                } else {
                    spawnWave(inst, def, next);
                }
            }
        }.runTaskLater(plugin, 40L);
    }

    // ==================== 通关 ====================
    private void finishDungeon(DungeonInstance inst, DungeonConfig.DungeonDef def) {
        if (inst.state != DungeonInstance.State.RUNNING) return;
        inst.state = DungeonInstance.State.FINISHED;
        inst.endTime = System.currentTimeMillis();
        long clearSec = (inst.endTime - inst.startTime) / 1000L;

        DungeonScoreboard.stop(inst);
        for (UUID u : new ArrayList<>(inst.players)) {
            Player p = Bukkit.getPlayer(u);
            if (p == null) continue;

            p.sendMessage("");
            p.sendMessage(DungeonConfig.msg("DungeonCleared", "\u00a7a\u00a7l\u2714 副本通关！\u00a7e {0}", def.name));

            // 通关消息 + 命令（来自 Reward 段）
            if (def.reward.messages != null) {
                for (String m : def.reward.messages) p.sendMessage(m);
            }
            if (def.reward.commands != null) {
                for (String cmd : def.reward.commands) {
                    try {
                        String run = cmd.replace("%player%", p.getName());
                        Bukkit.dispatchCommand(p, run.startsWith("/") ? run.substring(1) : run);
                    } catch (Throwable ignored) {}
                }
            }

            giveReward(p, def.reward);

            // 记录排行
            if (plugin.getDungeonRankManager() != null) {
                plugin.getDungeonRankManager().recordClear(def.id, p.getName(), clearSec, def.reward.points);
                plugin.getDungeonRankManager().save();
            }

            // 触发成就检查
            if (plugin.getDungeonAchievementManager() != null) {
                plugin.getDungeonAchievementManager().checkOnClear(inst, def, clearSec);
            }
            playerInstances.remove(u);  // 先移除，避免 teleport 触发失败
            setCooldown(u, def.id, def.cooldown);

            Location prev = inst.previousLocations.get(u);
            if (prev != null && prev.getWorld() != null) {
                p.teleport(prev);
            }
        }

        // 通关全局消息 + 命令（来自副本顶部 WinMessages/WinCommands）
        // 注意：玩家已传走，这里用离线广播代替（可以留空）
        // 如果需要，可以延迟到玩家回主城后广播

        cancelTasks(inst);
        instances.remove(def.id);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            unloadWorldAndRemove(inst, def);
        }, 60L);
    }

    // ==================== 失败 ====================
    private void failDungeon(DungeonInstance inst, DungeonConfig.DungeonDef def) {
        if (inst.state != DungeonInstance.State.RUNNING) return;
        inst.state = DungeonInstance.State.FAILED;

        DungeonScoreboard.stop(inst);
        for (UUID u : new ArrayList<>(inst.players)) {
            Player p = Bukkit.getPlayer(u);
            if (p == null) continue;
            p.sendMessage(DungeonConfig.msg("DungeonFailed", "\u00a7c\u00a7l\u2718 副本失败"));

            // 失败消息 + 命令
            if (def.failMessages != null) {
                for (String m : def.failMessages) p.sendMessage(m);
            }
            if (def.failCommands != null) {
                for (String cmd : def.failCommands) {
                    try {
                        String run = cmd.replace("%player%", p.getName());
                        Bukkit.dispatchCommand(p, run.startsWith("/") ? run.substring(1) : run);
                    } catch (Throwable ignored) {}
                }
            }

            playerInstances.remove(u);  // 先移除
            Location prev = inst.previousLocations.get(u);
            if (prev != null && prev.getWorld() != null) p.teleport(prev);
        }

        cancelTasks(inst);
        instances.remove(def.id);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            unloadWorldAndRemove(inst, def);
        }, 60L);
    }

    /** 卸载世界并从根目录删掉 Junction */
    /** 卸载动态世界 + 删除文件夹 */
    public void unloadWorldAndRemove(DungeonInstance inst, DungeonConfig.DungeonDef def) {
        if (inst == null || inst.worldName == null) return;
        String worldName = inst.worldName;
        World w = Bukkit.getWorld(worldName);
        if (w != null) {
            // 把还在里面的玩家传走
            for (Player p : new ArrayList<>(w.getPlayers())) {
                Location prev = inst.previousLocations.get(p.getUniqueId());
                final Location target = (prev != null && prev.getWorld() != null)
                        ? prev : Bukkit.getWorlds().get(0).getSpawnLocation();
                try { p.teleport(target); } catch (Throwable ignored) {}
            }

            if (!w.getPlayers().isEmpty()) {
                // 还有玩家 → 1 秒后重试
                Bukkit.getScheduler().runTaskLater(plugin, () -> unloadWorldAndRemove(inst, def), 20L);
                return;
            }

            // 清怪
            for (Entity e : w.getEntities()) {
                if (e instanceof Player) continue;
                if (e instanceof LivingEntity) e.remove();
            }

            // 卸载
            try {
                Bukkit.unloadWorld(w, true);
                plugin.getLogger().info("[Dungeon] 世界已卸载: " + worldName);
            } catch (Throwable t) {
                plugin.getLogger().warning("[Dungeon] 卸载世界失败: " + t.getMessage());
            }
        }

        // 异步删除文件夹
        Bukkit.getScheduler().runTaskLater(plugin, () -> deleteInstanceWorld(worldName, 0), 20L);
    }

    // ==================== 玩家离开 ====================
    /** 玩家单方面失败（死亡/传送/离开世界）*/
    public void failPlayerDungeon(Player player, String reason) {
        DungeonInstance inst = playerInstances.get(player.getUniqueId());
        if (inst == null) return;
        inst.playerDied = true;  // 标记有人死亡
        DungeonConfig.DungeonDef def = DungeonConfig.get(inst.dungeonId);
        if (def == null) return;

        plugin.getLogger().info("[Dungeon] " + player.getName() + " 副本失败: " + reason);

        player.sendMessage("\u00a7c\u00a7l\u2718 \u526f\u672c\u5931\u6557\uff1a " + reason);

        if (def.failMessages != null) {
            for (String m : def.failMessages) player.sendMessage(m);
        }
        if (def.failCommands != null) {
            for (String cmd : def.failCommands) {
                try {
                    String run = cmd.replace("%player%", player.getName());
                    Bukkit.dispatchCommand(player, run.startsWith("/") ? run.substring(1) : run);
                } catch (Throwable ignored) {}
            }
        }

        inst.removePlayer(player.getUniqueId());
        playerInstances.remove(player.getUniqueId());

        // 不再 teleport（respawn 事件已处理）

        if (inst.isEmpty()) {
            failDungeon(inst, def);
        }
    }

    public void leaveDungeon(Player player) {
        DungeonInstance inst = playerInstances.get(player.getUniqueId());
        if (inst == null) {
            player.sendMessage("\u00a7c你不在副本中");
            return;
        }
        DungeonConfig.DungeonDef def = DungeonConfig.get(inst.dungeonId);
        if (def == null) return;

        inst.removePlayer(player.getUniqueId());
        playerInstances.remove(player.getUniqueId());

        Location prev = inst.previousLocations.get(player.getUniqueId());
        if (prev != null && prev.getWorld() != null) {
            player.teleport(prev);
        }
        player.sendMessage(DungeonConfig.msg("LeftDungeon", "\u00a7e已离开副本"));

        if (inst.isEmpty()) {
            failDungeon(inst, def);
        }
    }

    private void cancelTasks(DungeonInstance inst) {
        try { if (inst.timeoutTask != null) inst.timeoutTask.cancel(); } catch (Throwable ignored) {}
        try { if (inst.waveTask != null) inst.waveTask.cancel(); } catch (Throwable ignored) {}
        try { if (inst.introTask != null) inst.introTask.cancel(); } catch (Throwable ignored) {}
    }

    // ==================== 奖励 ====================
    private void giveReward(Player player, DungeonConfig.RewardDef r) {
        double rate = 1.0;
        double luck = 1.0;
        if (r.applyRate) {
            try { rate = com.longdrange.ldattribute.core.soulring.rate.RateManager.getRate(player); } catch (Throwable ignored) {}
        }
        if (r.applyLuck) {
            try {
                double lk = com.longdrange.ldattribute.core.soulring.rate.RateManager.getLuck(player);
                luck = lk / 100.0;
            } catch (Throwable ignored) {}
        }

        if (r.points > 0) {
            int amt = (int) (r.points * rate * luck);
            try { com.longdrange.ldattribute.points.PointAPI.addPlayerPoints(player.getName(), amt); } catch (Throwable ignored) {}
            player.sendMessage("\u00a77- 点券: \u00a76" + amt);
        }
        if (r.vault > 0) {
            double amt = r.vault * rate * luck;
            try {
                org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
                        Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                if (rsp != null) rsp.getProvider().depositPlayer(player, amt);
            } catch (Throwable ignored) {}
            player.sendMessage("\u00a77- 金币: \u00a76" + (long) amt);
        }

        for (String s : r.items) {
            String[] parts = s.split(":");
            if (parts.length < 2) continue;
            Material mat = Material.getMaterial(parts[0].toUpperCase());
            if (mat == null) continue;
            int baseAmt = 1;
            try { baseAmt = Integer.parseInt(parts[1]); } catch (Throwable ignored) {}
            int amt = (int) (baseAmt * rate * luck);
            ItemStack item = new ItemStack(mat, Math.min(64, amt));
            giveToPlayer(player, item, r.toSoulRing);
        }

        for (String s : r.cards) {
            String[] parts = s.split(":");
            if (parts.length < 1) continue;
            String cardId = parts[0];
            int baseAmt = 1;
            if (parts.length >= 2) { try { baseAmt = Integer.parseInt(parts[1]); } catch (Throwable ignored) {} }
            int amt = (int) (baseAmt * rate * luck);
            CardData card = CardDataManager.getCard(cardId);
            if (card == null) continue;
            for (int i = 0; i < amt; i++) {
                ItemStack item = card.getItem();
                giveToPlayer(player, item, r.toSoulRing);
            }
        }
    }

    private static boolean hasReward(DungeonConfig.RewardDef r) {
        if (r == null) return false;
        return r.points > 0 || r.vault > 0
                || (r.items != null && !r.items.isEmpty())
                || (r.cards != null && !r.cards.isEmpty())
                || (r.messages != null && !r.messages.isEmpty())
                || (r.commands != null && !r.commands.isEmpty());
    }

    private void giveToPlayer(Player p, ItemStack item, boolean toSoulRing) {
        String name = displayName(item);
        if (toSoulRing) {
            try {
                com.longdrange.ldattribute.core.soulring.SoulRingData sr = plugin.getSoulRingManager().get(p);
                long given = sr.deposit(item, item.getAmount());
                if (given < item.getAmount()) {
                    ItemStack left = item.clone();
                    left.setAmount((int) (item.getAmount() - given));
                    p.getInventory().addItem(left);
                }
                p.sendMessage("\u00a77- \u7269\u54c1: \u00a7f" + name + " x" + item.getAmount() + " \u00a77(\u8fdb\u7075\u9b42\u7a7a\u95f4)");
                return;
            } catch (Throwable ignored) {}
        }
        p.getInventory().addItem(item);
        p.sendMessage("\u00a77- \u7269\u54c1: \u00a7f" + name + " x" + item.getAmount());
    }

    /** 获取物品显示名（剥色，无名字返回英文材质名美化） */
    private static String displayName(ItemStack item) {
        if (item == null) return "\u672a\u77e5\u7269\u54c1";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return org.bukkit.ChatColor.stripColor(item.getItemMeta().getDisplayName());
        }
        String mat = item.getType().name();
        String[] parts = mat.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (int pi = 0; pi < parts.length; pi++) {
            String pt = parts[pi];
            if (pt.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(pt.charAt(0)));
            if (pt.length() > 1) sb.append(pt.substring(1));
        }
        return sb.toString().trim();
    }

    public DungeonInstance getInstance(Player p) {
        return playerInstances.get(p.getUniqueId());
    }

    public Collection<DungeonInstance> allInstances() {
        return instances.values();
    }

    public static String formatTime(long sec) {
        if (sec < 60) return sec + "秒";
        long m = sec / 60;
        long s = sec % 60;
        if (m < 60) return m + "分" + s + "秒";
        long h = m / 60;
        m = m % 60;
        return h + "小时" + m + "分" + s + "秒";
    }
}
