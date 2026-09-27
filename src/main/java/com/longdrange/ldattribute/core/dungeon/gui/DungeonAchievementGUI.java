package com.longdrange.ldattribute.core.dungeon.gui;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.dungeon.DungeonAchievementConfig;
import com.longdrange.ldattribute.core.dungeon.DungeonConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class DungeonAchievementGUI {

    public static final int PER_PAGE = 45;
    public static final int BTN_PREV = 45;
    public static final int BTN_INFO = 49;
    public static final int BTN_NEXT = 53;

    private final LDAttribute plugin;
    public DungeonAchievementGUI(LDAttribute plugin) { this.plugin = plugin; }

    public void open(Player p) { open(p, 0); }

    public void open(Player p, int page) {
        List<DungeonAchievementConfig.AchievementDef> list = new ArrayList<>(DungeonAchievementConfig.all());
        int total = list.size();
        int unlockedCnt = 0;
        for (DungeonAchievementConfig.AchievementDef a : list) {
            if (plugin.getDungeonAchievementManager().has(p.getUniqueId(), a.id)) unlockedCnt++;
        }

        int totalPages = Math.max(1, (total + PER_PAGE - 1) / PER_PAGE);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        String title = ChatColor.DARK_GRAY + "✦ 副本成就 " + ChatColor.GRAY
                + "(" + unlockedCnt + "/" + total + ")";

        Inventory inv = Bukkit.createInventory(null, 54, title);

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = start + i;
            if (idx >= list.size()) break;
            DungeonAchievementConfig.AchievementDef a = list.get(idx);
            inv.setItem(i, renderIcon(p, a));
        }

        if (page > 0) inv.setItem(BTN_PREV, icon(Material.ARROW, ChatColor.GRAY + "← 上一页"));
        else inv.setItem(BTN_PREV, glass((short) 15, ChatColor.DARK_GRAY + "已是首页"));

        if (page < totalPages - 1) inv.setItem(BTN_NEXT, icon(Material.ARROW, ChatColor.GRAY + "下一页 →"));
        else inv.setItem(BTN_NEXT, glass((short) 15, ChatColor.DARK_GRAY + "已是末页"));

        List<String> info = new ArrayList<>();
        info.add(ChatColor.YELLOW + "已解锁: " + ChatColor.WHITE + unlockedCnt + "/" + total);
        info.add(ChatColor.YELLOW + "页码: " + ChatColor.WHITE + (page + 1) + "/" + totalPages);
        inv.setItem(BTN_INFO, icon(Material.BOOK, ChatColor.AQUA + "成就进度", info));

        p.openInventory(inv);
    }

    private ItemStack renderIcon(Player p, DungeonAchievementConfig.AchievementDef a) {
        boolean unlocked = plugin.getDungeonAchievementManager().has(p.getUniqueId(), a.id);
        Material mat = Material.getMaterial(a.icon);
        if (mat == null) mat = Material.STONE;
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            String prefix = unlocked ? ChatColor.GREEN + "✔ " : ChatColor.GRAY + "✘ ";
            meta.setDisplayName(prefix + (unlocked ? a.name : ChatColor.DARK_GRAY + ChatColor.stripColor(a.name)));
            List<String> lore = new ArrayList<>();
            lore.add("");
            if (!a.description.isEmpty()) lore.add(ChatColor.GRAY + a.description);
            lore.add("");
            String typeStr = "";
            switch (a.type) {
                case FIRST_CLEAR: typeStr = "首次通关"; break;
                case FAST_CLEAR: typeStr = "限时通关（" + a.seconds + "秒）"; break;
                case NO_DEATH: typeStr = "无死亡通关"; break;
                case SOLO_CLEAR: typeStr = "单人通关"; break;
                case FULL_TEAM: typeStr = "满队通关"; break;
                case CLEAR_COUNT: typeStr = "累计通关 " + a.count + " 次"; break;
            }
            lore.add(ChatColor.YELLOW + "条件: " + ChatColor.WHITE + typeStr);
            if (!a.dungeonId.isEmpty()) lore.add(ChatColor.YELLOW + "副本: " + ChatColor.WHITE + a.dungeonId);
            lore.add("");
            if (unlocked) {
                lore.add(ChatColor.GREEN + "✔ 已解锁");
            } else {
                lore.add(ChatColor.GRAY + "✘ 未解锁");
            }
            lore.add("");
            lore.add(ChatColor.GOLD + "奖励:");
            if (a.rewardPoints > 0) lore.add(ChatColor.GRAY + "  · 点券: " + ChatColor.GOLD + a.rewardPoints);
            if (a.rewardVault > 0) lore.add(ChatColor.GRAY + "  · 金币: " + ChatColor.GOLD + (long) a.rewardVault);
            for (String s : a.rewardItems) lore.add(ChatColor.GRAY + "  · " + s);
            for (String s : a.rewardCards) lore.add(ChatColor.GRAY + "  · 卡片: " + s);

            meta.setLore(lore);
            if (unlocked) {
                try {
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                } catch (Throwable ignored) {}
            }
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack icon(Material mat, String name, String... lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            if (lore != null && lore.length > 0) m.setLore(Arrays.asList(lore));
            it.setItemMeta(m);
        }
        return it;
    }

    private ItemStack icon(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            m.setLore(lore);
            it.setItemMeta(m);
        }
        return it;
    }

    private ItemStack glass(short data, String name) {
        ItemStack it = new ItemStack(Material.STAINED_GLASS_PANE, 1, data);
        ItemMeta m = it.getItemMeta();
        if (m != null) { m.setDisplayName(name); it.setItemMeta(m); }
        return it;
    }
}