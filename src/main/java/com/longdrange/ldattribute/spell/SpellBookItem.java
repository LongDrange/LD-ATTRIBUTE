package com.longdrange.ldattribute.spell;

import com.longdrange.ldattribute.card.CardNBT;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SpellBookItem {

    public static ItemStack create(String spellId, int level) {
        SpellConfig.Spell sp = SpellConfig.get(spellId);
        if (sp == null) return null;
        if (level < 1) level = 1;
        if (level > sp.maxLevel) level = sp.maxLevel;

        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        String lvStr = level > 1 ? " §e[Lv." + level + "]" : "";
        meta.setDisplayName("§d§l✦ " + sp.name + lvStr);
        List<String> lore = new ArrayList<>();
        lore.add("§7法術ID: §e" + sp.id);
        lore.add("§7等級: §e" + level + " §7/ §e" + sp.maxLevel);
        lore.add("§7消耗法力: §b" + sp.mana);
        lore.add("§7冷卻: §b" + sp.cooldown + " §7秒");
        lore.add("§7類型: §e" + sp.target);
        if (level > 1) {
            double mult = Math.pow(sp.levelMultiplier, level - 1);
            lore.add("§7傷害係數: §a×" + String.format("%.2f", mult));
        }
        lore.add("");
        lore.add("§e右鍵 §7釋放");
        meta.setLore(lore);
        item.setItemMeta(meta);

        item = CardNBT.setSpell(item, spellId);
        item = CardNBT.setSpellLevel(item, level);
        return item;
    }

    public static ItemStack create(String spellId) {
        return create(spellId, 1);
    }
}