package com.longdrange.ldattribute.core.ring;

import com.longdrange.ldattribute.LDAttribute;
import me.clip.placeholderapi.external.EZPlaceholderHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * 魂珠 PAPI 变量
 *   %ldring_total_<属性>%     所有魂珠累计该属性值（会自动应用映射表）
 *   %ldring_count_<类型>%     玩家拥有的该类型魂珠数量
 *   %ldring_max_<类型>%       该类型上限（-1 = 没放入，无参考）
 *   %ldring_limit_<类型>%     该类型剩余可放数量
 *   %ldring_has_<类型>%       是否拥有（1/0）
 *   %ldring_slots%            已解锁槽位数
 *   %ldring_pages%            已解锁页数
 *   %ldring_types%            已占用的类型数
 */
public class RingPAPI {

    public static void register(LDAttribute plugin) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            plugin.getLogger().info("[Ring] 未检测到 PlaceholderAPI，跳过注册");
            return;
        }
        try {
            new Hook(plugin).hook();
            plugin.getLogger().info("[Ring] PAPI 变量已注册: ldring");
        } catch (Throwable t) {
            plugin.getLogger().warning("[Ring] PAPI 注册失败: " + t.getMessage());
        }
    }

    public static class Hook extends EZPlaceholderHook {
        private final LDAttribute plugin;
        public Hook(LDAttribute plugin) { super(plugin, "ldring"); this.plugin = plugin; }

        @Override
        public String onPlaceholderRequest(Player player, String identifier) {
            if (player == null || identifier == null) return "";

            if (identifier.startsWith("total_")) {
                String attr = identifier.substring("total_".length());
                Map<String, Double> total = RingStatsProvider.getTotal(player);
                Double v = total.get(attr);
                if (v == null) v = total.get(RingStatsProvider.normalize(attr));
                return v == null ? "0" : RingStatsProvider.format(v);
            }
            if (identifier.startsWith("count_"))
                return String.valueOf(RingStatsProvider.count(player, identifier.substring("count_".length())));
            if (identifier.startsWith("max_"))
                return String.valueOf(RingStatsProvider.getMax(player, identifier.substring("max_".length())));
            if (identifier.startsWith("limit_"))
                return String.valueOf(RingStatsProvider.getRemaining(player, identifier.substring("limit_".length())));
            if (identifier.startsWith("has_"))
                return RingStatsProvider.count(player, identifier.substring("has_".length())) > 0 ? "1" : "0";

            if (identifier.equals("slots")) return String.valueOf(RingStatsProvider.getUnlockedSlotCount(player));
            if (identifier.equals("pages")) return String.valueOf(RingStatsProvider.getUnlockedPages(player));
            if (identifier.equals("types")) return String.valueOf(RingStatsProvider.getUsedTypes(player));
            return null;
        }
    }
}