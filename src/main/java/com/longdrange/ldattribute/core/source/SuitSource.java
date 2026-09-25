package com.longdrange.ldattribute.core.source;

import com.longdrange.ldattribute.card.*;
import com.longdrange.ldattribute.core.api.AttributeSource;
import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SuitSource implements AttributeSource {

    @Override public String getName() { return "套装"; }

    @Override
    public List<Entry> getEntries(Player player) {
        List<Entry> out = new ArrayList<>();
        try {
            List<ItemStack> valid = getValidCards(player);
            LDAttributeData d = new LDAttributeData();
            SuitData.applySuits(player, valid, d);
            out.add(new Entry("套装", d));
        } catch (Throwable ignored) {}
        return out;
    }

    private static List<ItemStack> getValidCards(Player player) {
        List<ItemStack> valid = new ArrayList<>();
        for (ItemStack card : PlayerData.getCards(player)) {
            if (!CardNBT.isBound(card)) { valid.add(card); continue; }
            if (CardNBT.getBoundUUID(card).equals(player.getUniqueId().toString())) valid.add(card);
        }
        return valid;
    }
}