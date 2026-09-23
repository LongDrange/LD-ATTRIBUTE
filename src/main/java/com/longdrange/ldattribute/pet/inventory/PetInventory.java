package com.longdrange.ldattribute.pet.inventory;

import com.longdrange.ldattribute.card.StatsDataRead;
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

import java.util.*;

/**
 * 宠物背包界面（多页 + 权限槽位）
 * 布局：
 *   10~16 第一行（最多 7 格）
 *   19~25 第二行
 *   45 = 上一页  49 = 页码  53 = 下一页  48 = 关闭
 */
public class PetInventory {

    public static int SLOT_PREV = 45;
    public static int SLOT_PAGE = 49;
    public static int SLOT_NEXT = 53;
    public static int SLOT_CLOSE = 48;
    public static int SLOT_STATS = 50;   // 查看属性

    private static final int[] PET_SLOTS = {11, 12, 13, 14, 15, 16, 17};

    public static int getMaxSlots(Player player) {
        int max = PetConfig.getDefaultSlots();
        for (Map.Entry<String, Integer> e : PetConfig.getPermissionSlots().entrySet()) {
            if (player.hasPermission(e.getKey()) && e.getValue() > max) {
                max = e.getValue();
            }
        }
        return max;
    }

    public static void open(Player player) {
        open(player, PetData.getCurrentPage(player.getUniqueId()));
    }

    public static void open(Player player, int page) {
        int maxPages = PetConfig.getMaxPages();
        if (page < 0) page = 0;
        if (page >= maxPages) page = maxPages - 1;
        PetData.setCurrentPage(player.getUniqueId(), page);

        boolean unlocked = page < PetData.getUnlockedPages(player.getUniqueId());
        String title = "§8§l✦ 宠物背包 §7(" + (page + 1) + "/" + maxPages + ")";
        if (title.replaceAll("§.", "").length() > 32) title = "§8§l✦ 宠物背包";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        int maxSlots = getMaxSlots(player);

        if (unlocked) {
            Map<Integer, PetInstance> pets = PetData.getPage(player.getUniqueId(), page);
            for (int i = 0; i < maxSlots && i < PET_SLOTS.length; i++) {
                int guiSlot = PET_SLOTS[i];
                PetInstance pi = pets.get(i);
                if (pi == null) {
                    inv.setItem(guiSlot, emptySlot(i));
                } else {
                    inv.setItem(guiSlot, buildPetIcon(player, pi, page, i));
                }
            }
        } else {
            ItemStack locked = createLocked();
            for (int i = 0; i < maxSlots && i < PET_SLOTS.length; i++) {
                inv.setItem(PET_SLOTS[i], locked);
            }
        }

        // 底部装饰
        ItemStack sep = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta sm = sep.getItemMeta();
        sm.setDisplayName(" ");
        sep.setItemMeta(sm);
        for (int i = 45; i < 54; i++) {
            if (i == SLOT_PREV || i == SLOT_PAGE || i == SLOT_NEXT || i == SLOT_CLOSE || i == SLOT_STATS) continue;
            inv.setItem(i, sep);
        }

        // 翻页
        if (page > 0) inv.setItem(SLOT_PREV, nav("§e§l◀ 上一页", "§7前往第 " + page + " 页"));
        if (page < maxPages - 1) inv.setItem(SLOT_NEXT, nav("§e§l下一页 ▶", "§7前往第 " + (page + 2) + " 页"));

        // 页码
        ItemStack pageItem = new ItemStack(Material.PAPER);
        ItemMeta pm = pageItem.getItemMeta();
        pm.setDisplayName("§a§l第 " + (page + 1) + " / " + maxPages + " 页");
        List<String> plore = new ArrayList<>();
        plore.add("§7槽位: §e" + maxSlots);
        if (!unlocked) plore.add("§c此页未解锁");
        pm.setLore(plore);
        pageItem.setItemMeta(pm);
        inv.setItem(SLOT_PAGE, pageItem);

        // 属性入口
        ItemStack statsBtn = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta stm = statsBtn.getItemMeta();
        stm.setDisplayName("§b§l✦ 出战宠物属性");
        stm.setLore(Arrays.asList("§7查看出战宠物给主人的加成"));
        statsBtn.setItemMeta(stm);
        inv.setItem(SLOT_STATS, statsBtn);

        // 关闭
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        cm.setDisplayName("§c§l✘ 关闭");
        close.setItemMeta(cm);
        inv.setItem(SLOT_CLOSE, close);

        player.openInventory(inv);
    }

    /** 空槽 */
    private static ItemStack emptySlot(int index) {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§7空槽位 #" + (index + 1));
        meta.setLore(Arrays.asList("§7右键宠物蛋放入此处"));
        item.setItemMeta(meta);
        return item;
    }

    /** 宠物图标 */
    public static ItemStack buildPetIcon(Player player, PetInstance pi, int page, int slot) {
        PetConfig.Pet def = pi.getDef();
        if (def == null) return new ItemStack(Material.BARRIER);

        Material mat = materialForRarity(def.rarity);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        String rarityColor = rarityColor(def.rarity);
        meta.setDisplayName(rarityColor + "✦ " + def.name + " §7[Lv." + pi.level + "]");

        List<String> lore = new ArrayList<>();
        lore.add("§7稀有度: " + rarityColor + def.rarity);
        lore.add("§7类型: §e" + def.type);
        lore.add("");
        if (pi.isMaxLevel()) {
            lore.add("§7等级: §e" + pi.level + " §a(满级)");
        } else {
            lore.add("§7等级: §e" + pi.level + " §7/ §e" + def.maxLevel);
            lore.add("§7经验: §b" + pi.exp + " §7/ §b" + PetConfig.getRequiredExp(pi.petId, pi.level));
        }

        // 属性预览
        if (!def.attributes.isEmpty()) {
            lore.add("");
            lore.add("§7属性加成:");
            double growth = Math.pow(1 + PetConfig.getLevelGrowth(), pi.level - 1);
            for (String attr : def.attributes) {
                lore.add("  " + ChatColor.translateAlternateColorCodes('&',
                        scaleAttrLine(attr, growth)));
            }
        }

        // 是否出战
        int activePage = PetData.getActivePage(player.getUniqueId());
        int activeSlot = PetData.getActiveSlot(player.getUniqueId());
        lore.add("");
        if (activePage == page && activeSlot == slot) {
            lore.add("§a✦ 出战中");
            lore.add("§e左键 §7取消出战");
        } else {
            lore.add("§e左键 §7出战");
        }
        lore.add("§e右键 §7查看详情");

        // 进化提示
        if (def.evolution != null && pi.level >= def.evolution.level) {
            lore.add("§d✦ 可进化！");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /** 按倍率缩放属性行 */
    private static String scaleAttrLine(String line, double mult) {
        // 格式: "&c攻击力: +30"
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

    private static Material materialForRarity(String rarity) {
        switch (rarity) {
            case "MYTHIC": return Material.NETHER_STAR;
            case "LEGENDARY": return Material.DRAGON_EGG;
            case "EPIC": return Material.DIAMOND;
            case "RARE": return Material.EMERALD;
            default: return Material.SLIME_BALL;
        }
    }

    private static String rarityColor(String rarity) {
        switch (rarity) {
            case "MYTHIC": return "§4§l";
            case "LEGENDARY": return "§6§l";
            case "EPIC": return "§5§l";
            case "RARE": return "§b§l";
            default: return "§f";
        }
    }

    private static ItemStack nav(String name, String lore) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createLocked() {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c§l🔒 未解锁的页面");
        meta.setLore(Arrays.asList("§7请先解锁此页面"));
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isPetInventory(String title) {
        return title.startsWith("§8§l✦ 宠物背包");
    }

    /** 判断点的是不是宠物槽（返回宠物slot索引，-1 = 不是） */
    public static int getPetSlotIndex(int guiSlot) {
        for (int i = 0; i < PET_SLOTS.length; i++) {
            if (PET_SLOTS[i] == guiSlot) return i;
        }
        return -1;
    }
}