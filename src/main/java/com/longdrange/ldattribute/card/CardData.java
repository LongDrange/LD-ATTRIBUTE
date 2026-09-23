package com.longdrange.ldattribute.card;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

public class CardData {

    private final String id;
    private final ItemStack item;
    private final String type;
    private int value;

    // ===== 綁定配置 =====
    public final boolean bindOnPickup;
    public final boolean tradeable;
    public final boolean allowUnbind;
    public final int unbindCost;
    public final String element;

    public CardData(String id, ItemStack item, String type,
                    boolean bindOnPickup, boolean tradeable,
                    boolean allowUnbind, int unbindCost, String element) {
        this.id = id;
        this.item = item;
        this.type = type == null ? "" : type;
        this.value = 0;
        this.bindOnPickup = bindOnPickup;
        this.tradeable = tradeable;
        this.allowUnbind = allowUnbind;
        this.unbindCost = unbindCost;
        this.element = element == null ? "" : element;
    }

    public String getId() { return id; }
    public ItemStack getItem() { return item.clone(); }
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
    public String getType() { return type; }
    public String getElement() { return element; }

    /** 是否為真正的卡片（Lore 裡寫「物品类型: 卡牌」） */
    public boolean isCard() {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
        for (String line : item.getItemMeta().getLore()) {
            String plain = ChatColor.stripColor(line);
            if (plain.contains("物品类型") && plain.contains("卡牌")) return true;
        }
        return false;
    }

    public boolean isExpStone() {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
        for (String line : item.getItemMeta().getLore()) {
            String plain = ChatColor.stripColor(line);
            if (plain.contains("物品类型") && plain.contains("經驗石")) return true;
        }
        return false;
    }

    public boolean matches(ItemStack other) {
        if (other == null) return false;
        if (!other.hasItemMeta() || !other.getItemMeta().hasDisplayName()) return false;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        return item.getItemMeta().getDisplayName().equals(other.getItemMeta().getDisplayName());
    }
}
