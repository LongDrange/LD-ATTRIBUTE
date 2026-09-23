package com.longdrange.ldattribute.combat;

import com.longdrange.ldattribute.card.CardData;
import com.longdrange.ldattribute.card.CardDataManager;
import com.longdrange.ldattribute.card.PlayerData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ElementHelper {

    /**
     * 从玩家卡片背包计算主元素（出现次数最多的）
     */
    public static String getPlayerElement(Player player) {
        try {
            List<ItemStack> cards = PlayerData.getCards(player);
            Map<String, Integer> counts = new HashMap<>();
            for (ItemStack card : cards) {
                CardData cd = CardDataManager.findCard(card);
                if (cd == null) continue;
                String e = cd.getElement();
                if (e == null || e.isEmpty()) continue;
                counts.merge(e.toUpperCase(), 1, Integer::sum);
            }
            return counts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("");
        } catch (Throwable ignored) {}
        return "";
    }

    /**
     * 实体元素（仅玩家有效，怪物返回空）
     */
    public static String getEntityElement(LivingEntity entity) {
        if (entity instanceof Player) return getPlayerElement((Player) entity);
        return "";
    }
}