package com.longdrange.ldattribute.core.dungeon.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.dungeon.DungeonConfig;
import com.longdrange.ldattribute.core.dungeon.DungeonInstance;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class DungeonListener implements Listener {

    private final LDAttribute plugin;
    public DungeonListener(LDAttribute plugin) { this.plugin = plugin; }

    private boolean inDungeonWorld(World w) {
        if (w == null) return false;
        String name = w.getName();
        // 动态世界：前缀匹配
        for (DungeonConfig.DungeonDef d : DungeonConfig.all()) {
            if (name.equals(d.worldName) || name.startsWith(d.worldName + "_")) return true;
        }
        return false;
    }

    private boolean isInDungeon(Player p) {
        return plugin.getDungeonManager().getInstance(p) != null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent e) {
        if (!DungeonConfig.protectBreak) return;
        if (inDungeonWorld(e.getBlock().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent e) {
        if (!DungeonConfig.protectPlace) return;
        if (inDungeonWorld(e.getBlock().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent e) {
        if (!DungeonConfig.protectDrop) return;
        if (inDungeonWorld(e.getPlayer().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (!DungeonConfig.protectInteract) return;
        if (inDungeonWorld(e.getPlayer().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (!DungeonConfig.commandBlockEnabled) return;
        Player p = e.getPlayer();
        if (p.isOp() && p.hasPermission("ldattribute.dungeon.admin")) return;

        World w = p.getWorld();
        if (!inDungeonWorld(w)) return;

        String msg = e.getMessage();
        if (msg == null || msg.isEmpty()) return;
        String cmd = msg.substring(1);
        int sp = cmd.indexOf(' ');
        if (sp >= 0) cmd = cmd.substring(0, sp);
        cmd = cmd.toLowerCase();

        boolean listed = false;
        for (String s : DungeonConfig.commandBlockList) {
            if (cmd.equals(s) || cmd.startsWith(s + ":")) {
                listed = true;
                break;
            }
        }

        boolean allow;
        if ("BLACKLIST".equals(DungeonConfig.commandBlockMode)) {
            allow = !listed;
        } else {
            allow = listed;
        }

        if (!allow) {
            e.setCancelled(true);
            p.sendMessage(DungeonConfig.commandBlockMsg);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTeleport(PlayerTeleportEvent e) {
        Player p = e.getPlayer();
        if (p.isDead()) return;

        DungeonInstance inst = plugin.getDungeonManager().getInstance(p);
        if (inst == null) return;
        DungeonConfig.DungeonDef def = DungeonConfig.get(inst.dungeonId);
        if (def == null) return;

        World to = e.getTo() == null ? null : e.getTo().getWorld();
        if (to == null) return;

        if (inst.worldName != null && (to.getName().equals(inst.worldName)
                || to.getName().startsWith(def.worldName + "_"))) return;

        plugin.getDungeonManager().failPlayerDungeon(p, "离开了副本世界");
    }

    /** 死亡 → 下一 tick 强制 respawn → respawn 后处理失败 */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent e) {
        final Player p = e.getEntity();
        if (!isInDungeon(p)) return;

        p.sendMessage(com.longdrange.ldattribute.core.dungeon.DungeonConfig.msg("DeathInDungeon", "\u00a7c\u00a7l\u2718 你在副本中死亡了，即将传送回原位"));

        // 下一 tick 强制 respawn（玩家根本看不到复活界面）
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            try {
                p.spigot().respawn();
                plugin.getLogger().info("[Dungeon] " + p.getName() + " 强制 respawn");
            } catch (Throwable t) {
                plugin.getLogger().warning("[Dungeon] respawn 失败: " + t.getMessage());
            }
        });

        // 3 tick 后触发副本失败（此时 respawn 已完成）
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            if (plugin.getDungeonManager().getInstance(p) != null) {
                plugin.getDungeonManager().failPlayerDungeon(p, "你已死亡");
            }
        }, 3L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (isInDungeon(p)) {
            plugin.getDungeonManager().leaveDungeon(p);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        DungeonInstance inst = plugin.getDungeonManager().getInstance(p);
        if (inst == null) return;

        // 死亡 respawn：回原位/主城
        Location prev = inst.previousLocations.get(p.getUniqueId());
        if (prev != null && prev.getWorld() != null) {
            e.setRespawnLocation(prev);
            plugin.getLogger().info("[Dungeon] " + p.getName() + " respawn 到原位");
        } else {
            e.setRespawnLocation(Bukkit.getWorlds().get(0).getSpawnLocation());
            plugin.getLogger().info("[Dungeon] " + p.getName() + " respawn 到主城");
        }
    }
}
