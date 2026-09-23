package com.longdrange.ldattribute.combat;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.card.TempBuffManager;
import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StateManager {

    private static LDAttribute plugin;
    // 玩家当前激活的状态集合（用于检测状态切换）
    private static final Map<UUID, Set<String>> activeStates = new ConcurrentHashMap<>();

    public static void init(LDAttribute pl) { plugin = pl; activeStates.clear(); }

    public static void clear(UUID uuid) { activeStates.remove(uuid); }

    /**
     * 根据玩家当前状态返回要附加的属性 Lore
     */
    public static List<String> getActiveAttributes(Player player) {
        List<String> result = new ArrayList<>();
        Set<String> nowActive = new HashSet<>();

        for (StateConfig.State s : StateConfig.getAll()) {
            boolean active = checkCondition(player, s);
            if (!active) continue;

            nowActive.add(s.id);
            result.addAll(s.attributes);
        }

        // 检查新激活的（发消息）
        Set<String> before = activeStates.getOrDefault(player.getUniqueId(), Collections.emptySet());
        for (String id : nowActive) {
            if (!before.contains(id)) {
                StateConfig.State s = findState(id);
                if (s != null && s.message != null && !s.message.isEmpty()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', s.message));
                }
            }
        }
        activeStates.put(player.getUniqueId(), nowActive);
        return result;
    }

    private static StateConfig.State findState(String id) {
        for (StateConfig.State s : StateConfig.getAll()) if (s.id.equals(id)) return s;
        return null;
    }

    private static boolean checkCondition(Player p, StateConfig.State s) {
        try {
            switch (s.conditionType) {
                case "HP_BELOW": {
                    double percent = p.getHealth() / p.getMaxHealth() * 100;
                    return percent < s.conditionValue;
                }
                case "HP_ABOVE": {
                    double percent = p.getHealth() / p.getMaxHealth() * 100;
                    return percent > s.conditionValue;
                }
                case "KILL_STREAK": {
                    int streak = TempBuffManager.getKillStreak(p.getUniqueId());
                    return streak >= s.conditionValue;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }
}