package com.longdrange.ldattribute.core.dungeon;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DungeonTeamManager {

    private final Map<UUID, DungeonTeam> teamsByPlayer = new ConcurrentHashMap<>();

    public DungeonTeam getTeam(UUID uuid) {
        DungeonTeam t = teamsByPlayer.get(uuid);
        if (t != null) t.cleanExpiredInvites();
        return t;
    }

    public DungeonTeam createTeam(Player leader) {
        DungeonTeam t = new DungeonTeam(leader.getUniqueId());
        teamsByPlayer.put(leader.getUniqueId(), t);
        return t;
    }

    public void disbandTeam(DungeonTeam t) {
        if (t == null) return;
        for (UUID u : new ArrayList<>(t.members)) {
            teamsByPlayer.remove(u);
        }
        for (UUID u : new ArrayList<>(t.invites.keySet())) {
            teamsByPlayer.remove(u);
        }
    }

    public void leaveTeam(Player p) {
        DungeonTeam t = getTeam(p.getUniqueId());
        if (t == null) return;

        if (t.isLeader(p.getUniqueId())) {
            // 队长解散队伍
            broadcast(t, DungeonConfig.msg("TeamDisbanded", "&e[队伍] 队长解散了队伍"));
            disbandTeam(t);
        } else {
            t.members.remove(p.getUniqueId());
            teamsByPlayer.remove(p.getUniqueId());
            broadcast(t, DungeonConfig.msg("TeamLeft", "&e[队伍] {0} 离开了队伍", p.getName()));
        }
    }

    public boolean invite(Player leader, Player target, int timeoutSec, int maxSize) {
        DungeonTeam t = getTeam(leader.getUniqueId());
        if (t == null) {
            leader.sendMessage(DungeonConfig.msg("TeamNotInTeam", "&c你不在队伍中（先用 /dg team create）"));
            return false;
        }
        if (!t.isLeader(leader.getUniqueId())) {
            leader.sendMessage(DungeonConfig.msg("TeamOnlyLeader", "&c只有队长能邀请"));
            return false;
        }
        if (t.size() >= maxSize) {
            leader.sendMessage(DungeonConfig.msg("TeamFull", "&c队伍已满（最多 {0} 人）", maxSize));
            return false;
        }
        if (t.has(target.getUniqueId())) {
            leader.sendMessage(DungeonConfig.msg("TeamAlreadyIn", "&c{0} 已在队伍中", target.getName()));
            return false;
        }
        DungeonTeam otherTeam = getTeam(target.getUniqueId());
        if (otherTeam != null) {
            leader.sendMessage(DungeonConfig.msg("TeamInOther", "&c{0} 已在其他队伍中", target.getName()));
            return false;
        }

        t.invite(target.getUniqueId(), timeoutSec);
        teamsByPlayer.putIfAbsent(target.getUniqueId(), t);

        target.sendMessage(ChatColor.YELLOW + "[队伍] " + leader.getName() + " 邀请你加入队伍，"
                + timeoutSec + " 秒内输入 " + ChatColor.WHITE + "/dg team accept" + ChatColor.YELLOW + " 接受");
        leader.sendMessage(ChatColor.GREEN + "已邀请 " + target.getName());
        return true;
    }

    public boolean accept(Player p, int maxSize) {
        DungeonTeam t = getTeam(p.getUniqueId());
        if (t == null) {
            p.sendMessage(DungeonConfig.msg("TeamNotInvited", "&c你没有被邀请"));
            return false;
        }
        if (!t.consumeInvite(p.getUniqueId())) {
            p.sendMessage(DungeonConfig.msg("TeamInviteExpired", "&c邀请已过期"));
            teamsByPlayer.remove(p.getUniqueId());
            return false;
        }
        if (t.size() >= maxSize) {
            p.sendMessage(ChatColor.RED + "队伍已满");
            teamsByPlayer.remove(p.getUniqueId());
            return false;
        }
        t.members.add(p.getUniqueId());
        teamsByPlayer.put(p.getUniqueId(), t);
        broadcast(t, DungeonConfig.msg("TeamJoined", "&a[队伍] {0} 加入了队伍", p.getName()));
        return true;
    }

    public void kick(Player leader, Player target) {
        DungeonTeam t = getTeam(leader.getUniqueId());
        if (t == null) return;
        if (!t.isLeader(leader.getUniqueId())) {
            leader.sendMessage(DungeonConfig.msg("TeamNotInTeamKick", "&c只有队长能踢人"));
            return;
        }
        if (!t.has(target.getUniqueId())) {
            leader.sendMessage(DungeonConfig.msg("TeamTargetNotIn", "&c{0} 不在队伍中", target.getName()));
            return;
        }
        if (t.isLeader(target.getUniqueId())) return;

        t.members.remove(target.getUniqueId());
        teamsByPlayer.remove(target.getUniqueId());
        broadcast(t, DungeonConfig.msg("TeamKicked", "&e[队伍] {0} 被踢出队伍", target.getName()));
        target.sendMessage(DungeonConfig.msg("TeamYouKicked", "&c[队伍] 你被踢出了队伍"));
    }

    public void broadcast(DungeonTeam t, String msg) {
        if (t == null) return;
        for (UUID u : t.members) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) p.sendMessage(msg);
        }
    }
}
