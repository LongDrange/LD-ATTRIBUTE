package com.longdrange.ldattribute.item;

import com.longdrange.ldattribute.card.CardNBT;
import org.bukkit.inventory.ItemStack;

public class ItemTypeNBT {

    private static final String KEY = "ld_item_type";

    public static ItemStack setType(ItemStack item, ItemType type) {
        if (item == null || type == null) return item;
        return CardNBT.setString(item, KEY, type.name());
    }

    /** 读类型：先读 NBT 标记，没有则用其他 NBT 推断（兼容旧物品） */
    public static ItemType getType(ItemStack item) {
        if (item == null) return null;
        String s = CardNBT.getString(item, KEY, "");
        if (!s.isEmpty()) {
            ItemType t = ItemType.parse(s);
            if (t != null) return t;
        }
        // ===== 兼容旧物品：用其他 NBT 推断 =====
        try {
            if (!CardNBT.getString(item, "pet_egg", "").isEmpty()) return ItemType.PET_EGG;
            if (!CardNBT.getString(item, "spell", "").isEmpty()) return ItemType.SPELL_BOOK;
            if (!CardNBT.getString(item, "ld_rune_id", "").isEmpty()) return ItemType.RUNE;
            if (!CardNBT.getString(item, "pet_equip_id", "").isEmpty()) return ItemType.PET_EQUIP;
            if (!CardNBT.getString(item, "exp_stone_id", "").isEmpty()) return ItemType.EXP_STONE;
            // 卡片（有 card_id NBT）
            if (!CardNBT.getString(item, "card_id", "").isEmpty()) return ItemType.CARD;
            // 兜底：CardDataManager 检查
            if (com.longdrange.ldattribute.card.CardDataManager.findCard(item) != null) return ItemType.CARD;
        } catch (Throwable ignored) {}
        return null;
    }

    public static boolean isType(ItemStack item, ItemType type) {
        if (type == null) return false;
        return getType(item) == type;
    }

    public static boolean isCard(ItemStack item) { return isType(item, ItemType.CARD); }
    public static boolean isRune(ItemStack item) { return isType(item, ItemType.RUNE); }
    public static boolean isPetEgg(ItemStack item) { return isType(item, ItemType.PET_EGG); }
    public static boolean isPetEquip(ItemStack item) { return isType(item, ItemType.PET_EQUIP); }
    public static boolean isExpStone(ItemStack item) { return isType(item, ItemType.EXP_STONE); }
    public static boolean isSpellBook(ItemStack item) { return isType(item, ItemType.SPELL_BOOK); }

    public static String checkType(ItemStack item, ItemType expect) {
        if (item == null) return "§c请放入物品";
        ItemType actual = getType(item);
        if (actual == null) return "§c此物品无效（无类型）";
        if (actual != expect) return "§c此槽只接受 §e" + expect.getDisplayName() + "§c，你放的是 §e" + actual.getDisplayName();
        return null;
    }
}