package com.longdrange.ldattribute.item;

/**
 * 物品类型系统
 * 每种物品有唯一类型，GUI 中不能混用
 */
public enum ItemType {
    CARD("卡牌"),
    RUNE("符文"),
    PET_EGG("宠物蛋"),
    PET_EQUIP("宠物装备"),
    EXP_STONE("经验石"),
    SPELL_BOOK("法术书"),
    MATERIAL("材料");

    private final String displayName;

    ItemType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }

    public static ItemType parse(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return ItemType.valueOf(s.toUpperCase()); }
        catch (Exception e) { return null; }
    }
}