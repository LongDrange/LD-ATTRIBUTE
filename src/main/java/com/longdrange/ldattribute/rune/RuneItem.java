package com.longdrange.ldattribute.rune;

import com.longdrange.ldattribute.card.CardNBT;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class RuneItem {

    public static final String KEY = "ld_rune_id";

    public static ItemStack create(RuneConfig.Rune r, int amount) {
        if (r == null) return null;
        ItemStack item = new ItemStack(r.material, Math.max(1, amount));
        ItemMeta m = item.getItemMeta();
        m.setDisplayName("\u00a7d\u00a7l\u2726 " + r.name);
        List<String> lore = new ArrayList<>();
        lore.add("\u00a77\u985e\u578b: \u00a7f" + r.type);
        lore.add("");
        lore.add("\u00a77\u5c6c\u6027:");
        for (String a : r.attributes) lore.add("  " + ChatColor.translateAlternateColorCodes((char)38, a));
        lore.add("");
        lore.add("\u00a7e\u5c07\u6b64\u7b26\u6587\u947d\u5d4c\u5230\u5361\u7247\u5b54\u4f4d");
        m.setLore(lore);
        item.setItemMeta(m);
        return com.longdrange.ldattribute.item.ItemTypeNBT.setType(CardNBT.setString(item, KEY, r.id),
                com.longdrange.ldattribute.item.ItemType.RUNE);
    }

    public static String getRuneId(ItemStack item) {
        if (item == null) return null;
        String id = CardNBT.getString(item, KEY, "");
        return (id == null || id.isEmpty()) ? null : id;
    }

    public static boolean isRune(ItemStack item) {
        return getRuneId(item) != null;
    }
}