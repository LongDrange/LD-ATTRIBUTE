package com.longdrange.ldattribute.core.value;

import com.longdrange.ldattribute.LDAttribute;
import me.clip.placeholderapi.external.EZPlaceholderHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * PAPI 变量：
 *   %ldvalue_info_<值Id>%     玩家某值当前值
 *   %ldvalue_name_<值Id>%     值显示名（带色）
 *   %ldvalue_max_<值Id>%      最大值
 *   %ldvalue_min_<值Id>%      最小值
 *   %ldvalue_remain_<值Id>%   距离最大值还有多少
 *   %ldvalue_has_<值Id>%      是否有此值（1/0）
 */
public class ValuePAPI {

    public static void register(LDAttribute plugin) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            plugin.getLogger().info("[Value] 未检测到 PlaceholderAPI，跳过注册");
            return;
        }
        try {
            new Hook(plugin).hook();
            plugin.getLogger().info("[Value] PAPI 变量已注册: ldvalue");
        } catch (Throwable t) {
            plugin.getLogger().warning("[Value] PAPI 注册失败: " + t.getMessage());
        }
    }

    public static class Hook extends EZPlaceholderHook {
        private final LDAttribute plugin;
        public Hook(LDAttribute plugin) { super(plugin, "ldvalue"); this.plugin = plugin; }

        @Override
        public String onPlaceholderRequest(Player player, String identifier) {
            if (player == null || identifier == null) return "";

            if (identifier.startsWith("info_")) {
                String id = identifier.substring("info_".length());
                ValueConfig.ValueDef def = ValueConfig.get(id);
                if (def == null) return "0";
                ValueData d = plugin.getValueManager().get(player);
                return format(d.get(id));
            }
            if (identifier.startsWith("name_")) {
                ValueConfig.ValueDef def = ValueConfig.get(identifier.substring("name_".length()));
                return def == null ? "" : def.name;
            }
            if (identifier.startsWith("max_")) {
                ValueConfig.ValueDef def = ValueConfig.get(identifier.substring("max_".length()));
                return def == null ? "0" : format(def.max);
            }
            if (identifier.startsWith("min_")) {
                ValueConfig.ValueDef def = ValueConfig.get(identifier.substring("min_".length()));
                return def == null ? "0" : format(def.min);
            }
            if (identifier.startsWith("remain_")) {
                String id = identifier.substring("remain_".length());
                ValueConfig.ValueDef def = ValueConfig.get(id);
                if (def == null) return "0";
                ValueData d = plugin.getValueManager().get(player);
                return format(d.remaining(id));
            }
            if (identifier.startsWith("has_")) {
                return ValueConfig.exists(identifier.substring("has_".length())) ? "1" : "0";
            }
            return null;
        }
    }

    public static String format(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }
}