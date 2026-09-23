package com.longdrange.ldattribute.card;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class CollectionChecker {

    private static final Map<UUID, Set<String>> lastCompleted = new HashMap<>();

    public static void check(Player player) {
        Set<String> owned = PlayerData.getOwnedCardIds(player.getUniqueId());
        Set<String> nowCompleted = new HashSet<>();
        Set<String> wasCompleted = lastCompleted.getOrDefault(player.getUniqueId(), new HashSet<>());

        for (CollectionConfig.Reward r : CollectionConfig.getAll()) {
            boolean all = true;
            for (String c : r.cards) {
                if (!owned.contains(c)) { all = false; break; }
            }
            boolean was = wasCompleted.contains(r.id);

            if (!all) {
                // 之前集齊、現在不齊 → 提示失效
                if (was) {
                    player.sendMessage("§8[§d圖鑑§8] §c套裝 §e" + r.id + " §c已失效 §8(§7請放回缺少的卡片§8)");
                }
                continue;
            }

            // 集齊
            nowCompleted.add(r.id);
            boolean justNow = !was;

            if (PlayerData.hasClaimed(player.getUniqueId(), r.id)) {
                if (justNow)
                    player.sendMessage("§8[§d圖鑑§8] §7套裝 §e" + r.id + " §7已集齊 §8(§c獎勵已領取過§8)");
                continue;
            }

            PlayerData.markClaimed(player.getUniqueId(), r.id);
            for (String cmd : r.commands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        cmd.replace("{player}", player.getName()));
            }
            player.sendMessage("§8[§d圖鑑§8] §a集齊套裝 §e" + r.id + " §a，獎勵已發放！");
        }

        lastCompleted.put(player.getUniqueId(), nowCompleted);
        PlayerData.savePlayer(player.getUniqueId());
    }

    public static void unload(UUID uuid) {
        lastCompleted.remove(uuid);
    }
}
