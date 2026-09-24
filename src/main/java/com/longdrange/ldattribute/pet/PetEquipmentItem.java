package com.longdrange.ldattribute.pet;

import com.longdrange.ldattribute.card.CardNBT;
import com.longdrange.ldattribute.item.ItemType;
import com.longdrange.ldattribute.item.ItemTypeNBT;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PetEquipmentItem {

    public static final String KEY_ID = "pet_equip_id";
    public static final String KEY_SLOT = "pet_equip_slot";

    public static ItemStack create(PetEquipmentConfig.Equip e, int amount) {
        if (e == null) return null;
        ItemStack item = new ItemStack(e.material, Math.max(1, amount));
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(ChatColor.translateAlternateColorCodes((char) 38, e.name));
        List<String> lore = new ArrayList<>();
        lore.add("§8§m--------");
        lore.add("§7类型: §e宠物装备");
        lore.add("§7槽位: §e" + slotDisplay(e.slot));
        if (e.description != null && !e.description.isEmpty()) {
            lore.add("§7" + e.description);
        }
        lore.add("");
        lore.add("§7属性:");
        for (String a : e.attributes) {
            lore.add("  " + ChatColor.translateAlternateColorCodes((char) 38, a));
        }
        lore.add("§8§m--------");
        lore.add("§7把装备放到宠物详情");
        m.setLore(lore);
        item.setItemMeta(m);
        item = CardNBT.setString(item, KEY_ID, e.id);
        item = CardNBT.setString(item, KEY_SLOT, e.slot);
        return ItemTypeNBT.setType(item, ItemType.PET_EQUIP);
    }

    public static String getEquipId(ItemStack item) {
        if (item == null) return null;
        if (!ItemTypeNBT.isPetEquip(item)) return null;
        String id = CardNBT.getString(item, KEY_ID, "");
        return id.isEmpty() ? null : id;
    }

    public static String getSlot(ItemStack item) {
        if (item == null) return null;
        return CardNBT.getString(item, KEY_SLOT, "");
    }

    private static String slotDisplay(String slot) {
        switch (slot) {
            case "WEAPON": return "武器";
            case "ARMOR": return "护甲";
            case "ACCESSORY": return "饰品";
            default: return slot;
        }
    }
}