package com.longdrange.ldattribute.core.dungeon;

import java.util.*;

public class DungeonTeam {
    public final UUID leader;
    public final Set<UUID> members = new LinkedHashSet<>();
    public final Map<UUID, Long> invites = new HashMap<>();  // 被邀请人 -> 过期时间
    public long createdAt;

    public DungeonTeam(UUID leader) {
        this.leader = leader;
        this.members.add(leader);
        this.createdAt = System.currentTimeMillis();
    }

    public boolean isLeader(UUID uuid) { return leader.equals(uuid); }
    public boolean has(UUID uuid) { return members.contains(uuid); }
    public int size() { return members.size(); }

    public void invite(UUID uuid, int seconds) {
        invites.put(uuid, System.currentTimeMillis() + seconds * 1000L);
    }

    public boolean consumeInvite(UUID uuid) {
        Long exp = invites.get(uuid);
        if (exp == null) return false;
        invites.remove(uuid);
        return exp >= System.currentTimeMillis();
    }

    public void cleanExpiredInvites() {
        long now = System.currentTimeMillis();
        invites.entrySet().removeIf(e -> e.getValue() < now);
    }
}