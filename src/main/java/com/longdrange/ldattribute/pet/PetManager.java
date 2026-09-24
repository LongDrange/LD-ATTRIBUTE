package com.longdrange.ldattribute.pet;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.util.*;

public class PetManager {

    /** 找出战宠物 */
    public static PetInstance getActivePet(Player player) {
        int ap = PetData.getActivePage(player.getUniqueId());
        int as = PetData.getActiveSlot(player.getUniqueId());
        if (ap < 0 || as < 0) return null;
        return PetData.getPet(player.getUniqueId(), ap, as);
    }

    /** 取所有宠物属性（宠物背包里所有宠物都生效） */
    public static List<String> getAllPetsAttributes(Player player) {
        List<String> result = new ArrayList<>();
        try {
            List<PetInstance> all = PetData.getAllPets(player.getUniqueId());
            for (PetInstance pi : all) {
                if (pi == null) continue;
                PetConfig.Pet def = pi.getDef();
                if (def == null) continue;
                double growth = Math.pow(1 + PetConfig.getLevelGrowth(), pi.level - 1);
                for (String line : def.attributes) {
                    result.add(scaleAttr(line, growth));
                }
            }
        } catch (Throwable ignored) {}
        return result;
    }

    /** 取出战宠物属性 */
    public static List<String> getActiveAttributes(Player player) {
        PetInstance pi = getActivePet(player);
        if (pi == null) return new ArrayList<>();
        PetConfig.Pet def = pi.getDef();
        if (def == null) return new ArrayList<>();
        double growth = Math.pow(1 + PetConfig.getLevelGrowth(), pi.level - 1);
        List<String> result = new ArrayList<>();
        for (String line : def.attributes) result.add(scaleAttr(line, growth));
        return result;
    }

    private static String scaleAttr(String line, double mult) {
        try {
            int idx = line.lastIndexOf('+');
            if (idx < 0) return line;
            String prefix = line.substring(0, idx + 1);
            String numStr = line.substring(idx + 1).trim();
            double v = Double.parseDouble(numStr);
            double scaled = v * mult;
            String fmt = scaled == Math.floor(scaled)
                    ? String.valueOf((long) scaled)
                    : String.format("%.2f", scaled);
            return prefix + fmt;
        } catch (Exception e) {
            return line;
        }
    }

    /** 加经验 */
    public static int addExp(Player player, int page, int slot, int delta) {
        PetInstance pi = PetData.getPet(player.getUniqueId(), page, slot);
        if (pi == null) return 0;
        int ups = pi.addExp(delta);
        PetData.save(player.getUniqueId());
        if (ups > 0) {
            PetConfig.Pet def = pi.getDef();
            String name = def != null ? def.name : pi.petId;
            player.sendMessage("§a✦ 宠物 " + name + " 升级了 " + ups + " 次！当前 §eLv." + pi.level);
            try { player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f); } catch (Throwable ignored) {}
            try { com.longdrange.ldattribute.card.StatsDataRead.updatePlayer(player); } catch (Throwable ignored) {}
        }
        return ups;
    }

    /** 进化 */
    public static boolean evolve(Player player, int page, int slot) {
        PetInstance pi = PetData.getPet(player.getUniqueId(), page, slot);
        if (pi == null) return false;
        PetConfig.Pet def = pi.getDef();
        if (def == null || def.evolution == null) return false;
        if (pi.level < def.evolution.level) return false;
        for (String matSpec : def.evolution.materials) {
            if (!hasItem(player, matSpec)) {
                player.sendMessage("§c缺少材料: §e" + matSpec);
                return false;
            }
        }
        for (String matSpec : def.evolution.materials) removeItem(player, matSpec);
        PetConfig.Pet newDef = PetConfig.get(def.evolution.to);
        if (newDef == null) return false;
        PetInstance newPi = new PetInstance(newDef.id, Math.min(pi.level, newDef.maxLevel), 0);
        PetData.setPet(player.getUniqueId(), page, slot, newPi);
        player.sendMessage("§d✦ 宠物进化成功！ " + def.name + " → " + newDef.name);
        try { player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_LIGHTNING_THUNDER, 1f, 1.5f); } catch (Throwable ignored) {}
        try { com.longdrange.ldattribute.card.StatsDataRead.updatePlayer(player); } catch (Throwable ignored) {}
        return true;
    }

    /** 取出宠物（变回蛋，等级保留） */
    public static boolean extractPet(Player player, int page, int slot) {
        PetInstance pi = PetData.getPet(player.getUniqueId(), page, slot);
        if (pi == null) return false;
        ItemStack egg = createPetEgg(pi.petId, pi.level, pi.exp);
        if (egg == null) return false;
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), egg);
            player.sendMessage("§e背包已满，宠物蛋掉落在脚下！");
        } else {
            player.getInventory().addItem(egg);
        }
        PetData.setPet(player.getUniqueId(), page, slot, null);
        PetConfig.Pet def = pi.getDef();
        String name = def != null ? def.name : pi.petId;
        player.sendMessage("§a✦ 已取出宠物 " + name + " §7(Lv." + pi.level + ")");
        try { player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1f, 1f); } catch (Throwable ignored) {}
        return true;
    }

    // ========== 工具 ==========

    private static boolean hasItem(Player player, String spec) {
        String[] p = spec.split(":");
        Material mat = Material.getMaterial(p[0].toUpperCase());
        if (mat == null) return false;
        int need = p.length >= 2 ? Integer.parseInt(p[1]) : 1;
        int have = 0;
        for (ItemStack it : player.getInventory().getContents()) {
            if (it != null && it.getType() == mat) have += it.getAmount();
        }
        return have >= need;
    }

    private static void removeItem(Player player, String spec) {
        String[] p = spec.split(":");
        Material mat = Material.getMaterial(p[0].toUpperCase());
        if (mat == null) return;
        int need = p.length >= 2 ? Integer.parseInt(p[1]) : 1;
        int removed = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && removed < need; i++) {
            ItemStack it = contents[i];
            if (it == null || it.getType() != mat) continue;
            int take = Math.min(it.getAmount(), need - removed);
            it.setAmount(it.getAmount() - take);
            removed += take;
            if (it.getAmount() <= 0) player.getInventory().setItem(i, null);
        }
    }

    /** 生成宠物蛋 */
    public static ItemStack createPetEgg(String petId) {
        return createPetEgg(petId, 1, 0);
    }

    public static ItemStack createPetEgg(String petId, int level, int exp) {
        PetConfig.Pet def = PetConfig.get(petId);
        if (def == null) return null;
        ItemStack item = new ItemStack(Material.MONSTER_EGG);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d§l✦ " + def.name + " 宠物蛋");
        List<String> lore = new ArrayList<>();
        lore.add("§7宠物ID: §e" + petId);
        lore.add("§7稀有度: §e" + def.rarity);
        lore.add("§7类型: §e" + def.type);
        lore.add("§7等级: §e" + level);
        if (level > 1 || exp > 0) lore.add("§7经验: §b" + exp);
        lore.add("");
        lore.add("§e右键宠物背包空槽放入");
        meta.setLore(lore);
        item.setItemMeta(meta);
        item = com.longdrange.ldattribute.card.CardNBT.setString(item, "pet_egg", petId);
        item = com.longdrange.ldattribute.card.CardNBT.setInt(item, "pet_lv", level);
        item = com.longdrange.ldattribute.card.CardNBT.setInt(item, "pet_exp", exp);
        item = com.longdrange.ldattribute.item.ItemTypeNBT.setType(item,
                com.longdrange.ldattribute.item.ItemType.PET_EGG);
        return item;
    }

    public static String getPetEggId(ItemStack item) {
        if (item == null) return null;
        String s = com.longdrange.ldattribute.card.CardNBT.getString(item, "pet_egg", "");
        return (s == null || s.isEmpty()) ? null : s;
    }

    public static int getPetEggLevel(ItemStack item) {
        if (item == null) return 1;
        int v = com.longdrange.ldattribute.card.CardNBT.getInt(item, "pet_lv", 1);
        return v < 1 ? 1 : v;
    }

    public static int getPetEggExp(ItemStack item) {
        if (item == null) return 0;
        return com.longdrange.ldattribute.card.CardNBT.getInt(item, "pet_exp", 0);
    }
}