package com.longdrange.ldattribute.pet.inventory;

import com.longdrange.ldattribute.pet.PetConfig;
import com.longdrange.ldattribute.pet.PetData;
import com.longdrange.ldattribute.pet.PetInstance;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 宠物详情界面
 */
public class PetDetailInventory {

    public static int SLOT_PET = 13;
    public static int SLOT_BACK = 49;
    public static int SLOT_EVOLVE = 29;
    public static int SLOT_EXTRACT = 33;   // 取出宠物
    public static int SLOT_FEED = 31;      // 喂食
    public static int SLOT_WEAPON = 21;    // 装备槽：武器
    public static int SLOT_ARMOR = 23;     // 装备槽：护甲
    public static int SLOT_ACCESSORY = 25; // 装备槽：饰品

    public static void open(Player player, int page, int slot) {
        PetInstance pi = PetData.getPet(player.getUniqueId(), page, slot);
        if (pi == null) return;
        PetConfig.Pet def = pi.getDef();
        if (def == null) return;

        String title = "§8§l✦ 宠物详情";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // 宠物本体图标
        inv.setItem(SLOT_PET, PetInventory.buildPetIcon(player, pi, page, slot));

        // 进化按钮
        if (def.evolution != null) {
            boolean canEvolve = pi.level >= def.evolution.level;
            ItemStack evo = new ItemStack(canEvolve ? Material.DIAMOND : Material.COAL);
            ItemMeta em = evo.getItemMeta();
            em.setDisplayName((canEvolve ? "§d§l✦ 可进化" : "§7§l进化"));
            List<String> lore = new ArrayList<>();
            lore.add("§7进化至: §e" + (PetConfig.get(def.evolution.to) != null
                    ? PetConfig.get(def.evolution.to).name : def.evolution.to));
            lore.add("§7需要等级: §e" + def.evolution.level + " §7(当前 §e" + pi.level + "§7)");
            lore.add("§7材料:");
            for (String m : def.evolution.materials) lore.add("  §7- §e" + m);
            lore.add("");
            if (canEvolve) lore.add("§e点击进化");
            else lore.add("§c等级不足");
            em.setLore(lore);
            evo.setItemMeta(em);
            inv.setItem(SLOT_EVOLVE, evo);
        }

        // 喂食按钮
        ItemStack feed = new ItemStack(Material.WHEAT);
        ItemMeta fm = feed.getItemMeta();
        fm.setDisplayName("§a§l✦ 喂食");
        List<String> fl = new ArrayList<>();
        fl.add("§7把食物拿在主手");
        fl.add("§7可喂:");
        fl.add("  §7- §e钻石 §7(+50 exp)");
        fl.add("  §7- §e绿宝石 §7(+30 exp)");
        fl.add("  §7- §e金锭 §7(+20 exp)");
        fl.add("  §7- §e铁锭 §7(+15 exp)");
        fl.add("  §7- §e经验瓶 §7(+100 exp)");
        fl.add("  §7- §e下界之星 §7(+500 exp)");
        fl.add("");
        fl.add("§e点击喂食");
        fm.setLore(fl);
        feed.setItemMeta(fm);
        inv.setItem(SLOT_FEED, feed);

        // 装备槽
        inv.setItem(SLOT_WEAPON, buildEquipmentIcon("WEAPON", pi.equipment.get("WEAPON"), "§c§l✦ 武器槽"));
        inv.setItem(SLOT_ARMOR, buildEquipmentIcon("ARMOR", pi.equipment.get("ARMOR"), "§b§l✦ 护甲槽"));
        inv.setItem(SLOT_ACCESSORY, buildEquipmentIcon("ACCESSORY", pi.equipment.get("ACCESSORY"), "§e§l✦ 饰品槽"));
        // 取出按钮
        int activePage = PetData.getActivePage(player.getUniqueId());
        int activeSlot = PetData.getActiveSlot(player.getUniqueId());
        boolean isActive = (activePage == page && activeSlot == slot);

        ItemStack extract = new ItemStack(isActive ? Material.BARRIER : Material.HOPPER);
        ItemMeta xm = extract.getItemMeta();
        xm.setDisplayName(isActive ? "§c§l✘ 出战中无法取出" : "§e§l✦ 取出宠物");
        List<String> xlore = new ArrayList<>();
        if (isActive) {
            xlore.add("§7请先取消出战");
        } else {
            xlore.add("§7将宠物变回宠物蛋放入背包");
            xlore.add("§7宠物等级与经验会保留");
            xlore.add("");
            xlore.add("§e点击取出");
        }
        xm.setLore(xlore);
        extract.setItemMeta(xm);
        inv.setItem(SLOT_EXTRACT, extract);

        // 返回
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName("§c§l← 返回");
        back.setItemMeta(bm);
        inv.setItem(SLOT_BACK, back);

        player.openInventory(inv);
    }

    private static ItemStack buildEquipmentIcon(String key, ItemStack equipped, String title) {
        if (equipped != null && equipped.getType() != Material.AIR) {
            ItemStack copy = equipped.clone();
            ItemMeta m = copy.getItemMeta();
            List<String> lore = m.hasLore() ? new ArrayList<>(m.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add("§e点击取出装备");
            m.setLore(lore);
            copy.setItemMeta(m);
            return copy;
        }
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName(title);
        m.setLore(Arrays.asList(
                "§7把装备拿在主手",
                "§7点击此处装备",
                "",
                "§8槽位: " + key
        ));
        item.setItemMeta(m);
        return item;
    }
    public static boolean isDetail(String title) {
        return title.equals("§8§l✦ 宠物详情");
    }
}