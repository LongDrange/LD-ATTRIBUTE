package com.longdrange.ldattribute.rune;

import com.longdrange.ldattribute.card.CardData;
import com.longdrange.ldattribute.card.CardDataManager;
import com.longdrange.ldattribute.card.CardNBT;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RuneHelper {

    /** 返回卡片上所有已镶嵌符文的属性 Lore 行（已转色码） */
    public static List<String> getCardSocketAttributes(ItemStack card) {
        List<String> result = new ArrayList<>();
        if (card == null) return result;
        CardData cd = CardDataManager.findCard(card);
        if (cd == null) return result;
        List<String> socketIds = RuneConfig.getCardSockets(cd.getId());
        if (socketIds.isEmpty()) return result;
        for (int i = 0; i < socketIds.size(); i++) {
            if (!CardNBT.isSocketUnlocked(card, i)) continue;
            String runeId = CardNBT.getSocketRune(card, i);
            if (runeId == null || runeId.isEmpty()) continue;
            RuneConfig.Rune r = RuneConfig.getRune(runeId);
            if (r == null) continue;
            for (String a : r.attributes) {
                result.add(ChatColor.translateAlternateColorCodes((char) 38, a));
            }
        }
        return result;
    }

    public static boolean hasAnyRune(ItemStack card) {
        return !getCardSocketAttributes(card).isEmpty();
    }
}