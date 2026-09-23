package com.longdrange.ldattribute.card.inventory;

import com.longdrange.ldattribute.card.PlayerData;
import com.longdrange.ldattribute.card.SuitData;
import com.longdrange.ldattribute.card.SynergyData;
import com.longdrange.ldattribute.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 套装 / 共鸣 / 羁绊 列表界面
 */
public class SuitInventory {

    public static int SLOT_BACK = 49;

    public static void open(Player player) {
        String title = Message.get("Inventory.Suit.Name");
        Inventory inv = Bukkit.createInventory(null, 54, title);

        List<ItemStack> cards = PlayerData.getCards(player);

        // ============ 第 1 行：套装 ============
        Map<String, Integer> progress = SuitData.getProgress(player.getName());
        int slot = 9;
        for (String suitName : SuitData.getAllSuitNames()) {
            if (slot > 17) break;
            int matched = progress.getOrDefault(suitName, 0);
            int required = SuitData.getRequiredCount(suitName);
            boolean complete = matched >= required;

            ItemStack item = new ItemStack(complete ? Material.DIAMOND : Material.COAL);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§b[套裝] §f" + suitName);
            List<String> lore = new ArrayList<>();
            lore.add("§7進度: §e" + matched + "§7/§e" + required);
            lore.add("§7狀態: " + (complete ? "§a✔ 已激活" : "§c✘ 未激活"));
            List<String> effects = SuitData.getEffect(suitName);
            if (effects != null && !effects.isEmpty()) {
                lore.add("");
                lore.add("§7效果:");
                for (String e : effects) lore.add("  " + ChatColor.translateAlternateColorCodes('&', e));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot, item);
            slot++;
        }

        // ============ 第 2 行：共鸣 ============
        slot = 18;
        for (SynergyData.Synergy s : SynergyData.getAllResonances()) {
            if (slot > 26) break;
            int matched = SynergyData.getMatched(cards, s);
            boolean active = matched >= s.required;

            ItemStack item = new ItemStack(active ? Material.NETHER_STAR : Material.SULPHUR);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§d[共鳴] §f" + s.name);
            List<String> lore = new ArrayList<>();
            lore.add("§7進度: §e" + matched + "§7/§e" + s.required);
            lore.add("§7狀態: " + (active ? "§a✔ 已激活" : "§c✘ 未激活"));
            lore.add("§7需要卡片:");
            for (String cid : s.cards) {
                boolean has = hasCard(cards, cid);
                lore.add("  " + (has ? "§e✔ " + cid : "§c✘ " + cid));
            }
            if (!s.attributes.isEmpty()) {
                lore.add("");
                lore.add("§7效果:");
                for (String a : s.attributes) {
                    lore.add("  " + ChatColor.translateAlternateColorCodes('&', a));
                }
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot, item);
            slot++;
        }

        // ============ 第 3 行：羁绊 ============
        slot = 27;
        for (SynergyData.Synergy s : SynergyData.getAllBonds()) {
            if (slot > 35) break;
            int matched = SynergyData.getMatched(cards, s);
            boolean active = matched >= s.required;

            ItemStack item = new ItemStack(active ? Material.EMERALD : Material.IRON_INGOT);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§a[羈絆] §f" + s.name);
            List<String> lore = new ArrayList<>();
            lore.add("§7進度: §e" + matched + "§7/§e" + s.required);
            lore.add("§7狀態: " + (active ? "§a✔ 已激活" : "§c✘ 未激活"));
            lore.add("§7需要卡片:");
            for (String cid : s.cards) {
                boolean has = hasCard(cards, cid);
                lore.add("  " + (has ? "§e✔ " + cid : "§c✘ " + cid));
            }
            if (!s.attributes.isEmpty()) {
                lore.add("");
                lore.add("§7效果:");
                for (String a : s.attributes) {
                    lore.add("  " + ChatColor.translateAlternateColorCodes('&', a));
                }
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot, item);
            slot++;
        }

        // ============ 底部分隔 + 返回 ============
        ItemStack sep = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta sepMeta = sep.getItemMeta();
        sepMeta.setDisplayName(" ");
        sep.setItemMeta(sepMeta);
        for (int i = 45; i < 54; i++) {
            if (i == SLOT_BACK) continue;
            inv.setItem(i, sep);
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(Message.get("Inventory.Suit.Back.Name"));
        backMeta.setLore(Message.getList("Inventory.Suit.Back.Lore"));
        back.setItemMeta(backMeta);
        inv.setItem(SLOT_BACK, back);

        player.openInventory(inv);
    }

    public static boolean isSuitInventory(String title) {
        return title.equals(Message.get("Inventory.Suit.Name"));
    }

    private static boolean hasCard(List<ItemStack> cards, String cardId) {
        try {
            com.longdrange.ldattribute.card.CardData target =
                    com.longdrange.ldattribute.card.CardDataManager.getCard(cardId);
            if (target == null) return false;
            for (ItemStack card : cards) {
                if (target.matches(card)) return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }
}