package com.longdrange.ldattribute.core.source;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.card.*;
import com.longdrange.ldattribute.core.api.AttributeSource;
import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CardSource implements AttributeSource {

    @Override public String getName() { return "卡片"; }

    @Override
    public List<Entry> getEntries(Player player) {
        List<Entry> out = new ArrayList<>();
        LDAttribute plugin = LDAttribute.getInstance();
        if (plugin == null || plugin.getApi() == null) return out;
        try {
            for (ItemStack card : PlayerData.getCards(player)) {
                if (!isUsableBy(card, player)) continue;
                CardData cd = CardDataManager.findCard(card);
                if (cd == null) continue;
                List<String> lore = StatsDataRead.filterNormalLore(card);
                if (lore.isEmpty()) continue;
                LDAttributeData d = plugin.getApi().getLoreData(player, null, lore);
                int lv = CardNBT.getLevel(card);
                String label = "卡片 " + cd.getId() + (lv > 0 ? " (Lv." + lv + ")" : "");
                out.add(new Entry(label, d));
            }
        } catch (Throwable ignored) {}
        return out;
    }

    private static boolean isUsableBy(ItemStack card, Player player) {
        if (!CardNBT.isBound(card)) return true;
        return CardNBT.getBoundUUID(card).equals(player.getUniqueId().toString());
    }
}