package com.longdrange.ldattribute.card.inventory;

import com.longdrange.ldattribute.card.CardData;
import com.longdrange.ldattribute.card.CardDataManager;
import com.longdrange.ldattribute.card.PlayerData;
import com.longdrange.ldattribute.card.RecipeConfig;
import com.longdrange.ldattribute.points.PointAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MergeInventory {

    public static final String TITLE_PREFIX = "§8[§b合成§8] §7";
    public static int SLOT_BACK = 45;
    public static int SLOT_INFO = 49;

    public static void open(Player player) {
        List<RecipeConfig.Recipe> all = new ArrayList<>(RecipeConfig.getAll());
        int total = all.size();
        String title = TITLE_PREFIX + "配方 (" + total + ")";
        if (title.replaceAll("§.", "").length() > 32) title = TITLE_PREFIX + "配方";

        Inventory inv = Bukkit.createInventory(null, 54, title);
        for (int i = 0; i < Math.min(all.size(), 45); i++) {
            inv.setItem(i, buildIcon(player, all.get(i)));
        }

        ItemStack sep = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta sm = sep.getItemMeta(); sm.setDisplayName(" "); sep.setItemMeta(sm);
        for (int i = 45; i < 54; i++) {
            if (i == SLOT_BACK || i == SLOT_INFO) continue;
            inv.setItem(i, sep);
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName("§e§l◀ 返回卡片背包");
        back.setItemMeta(bm);
        inv.setItem(SLOT_BACK, back);

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§a§l合成配方");
        List<String> il = new ArrayList<>();
        il.add("§7共 §e" + total + " §7個配方");
        il.add("§7點擊圖標嘗試合成");
        il.add("§7累計合成: §e" + PlayerData.getMergeCount(player.getUniqueId()) + " §7次");
        il.add("§7累計合成: §e" + PlayerData.getMergeCount(player.getUniqueId()) + " §7次");
        il.add("§a綠字 §7= 材料齊全");
        il.add("§c紅字 §7= 缺少材料");
        im.setLore(il);
        info.setItemMeta(im);
        inv.setItem(SLOT_INFO, info);

        player.openInventory(inv);
    }

    public static boolean isMerge(String title) {
        return title.startsWith(TITLE_PREFIX);
    }

    public static RecipeConfig.Recipe getRecipeAt(int slot) {
        List<RecipeConfig.Recipe> all = new ArrayList<>(RecipeConfig.getAll());
        if (slot < 0 || slot >= all.size() || slot >= 45) return null;
        return all.get(slot);
    }

    private static ItemStack buildIcon(Player player, RecipeConfig.Recipe r) {
        ItemStack icon = null;
        // 优先：输出卡第一张做图标
        if (!r.outputCards.isEmpty()) {
            String cardId = r.outputCards.keySet().iterator().next();
            CardData cd = CardDataManager.getCard(cardId);
            if (cd != null) icon = cd.getItem().clone();
        }
        // 其次：Icon 配置
        if (icon == null && r.icon != null && !r.icon.isEmpty()) {
            String[] parts = r.icon.split(":");
            Material m = Material.getMaterial(parts[0].toUpperCase());
            short data = 0;
            if (parts.length >= 2) { try { data = Short.parseShort(parts[1]); } catch (Exception ignored) {} }
            if (m == null) m = Material.PAPER;
            icon = new ItemStack(m, 1, data);
        }
        // 兜底：纸
        if (icon == null) icon = new ItemStack(Material.PAPER);
        if (icon.getAmount() > 1) icon.setAmount(1);

        boolean can = canCraft(player, r);
        ItemMeta meta = icon.getItemMeta();
        if (meta == null) return icon;

        String displayName = ChatColor.translateAlternateColorCodes((char)38, r.display);
        String cleanName = ChatColor.stripColor(displayName);
        meta.setDisplayName((can ? "§a§l" : "§c§l") + cleanName);

        List<String> lore = new ArrayList<>();
        lore.add("§7配方ID: §f" + r.id);
        lore.add("");
        if (!r.inputCards.isEmpty()) {
            lore.add("§7需要卡片:");
            for (Map.Entry<String, Integer> e : r.inputCards.entrySet()) {
                int have = PlayerData.countCard(player.getUniqueId(), e.getKey());
                boolean ok = have >= e.getValue();
                lore.add("§8- " + (ok ? "§a" : "§c") + e.getKey() + " §7" + have + "/" + e.getValue());
            }
        }
        if (r.costPoints > 0 || r.costVault > 0 || !r.costItems.isEmpty()) {
            lore.add("§7消耗:");
            if (r.costPoints > 0) {
                int have = PointAPI.getPlayerPoints(player.getName());
                boolean ok = have >= r.costPoints;
                lore.add("§8- " + (ok ? "§a" : "§c") + "點券 §7" + have + "/" + r.costPoints);
            }
            if (r.costVault > 0) lore.add("§8- §6金幣 §7" + (int) r.costVault);
            for (String s : r.costItems) lore.add("§8- §e" + s);
        }
        lore.add("§7產出:");
        for (Map.Entry<String, Integer> e : r.outputCards.entrySet())
            lore.add("§8- §e" + e.getKey() + " §7x" + e.getValue());
        if (!r.randomPool.isEmpty()) {
            lore.add("§7隨機產出:");
            for (String k : r.randomPool.keySet()) lore.add("§8- §e" + k);
        }
        if (r.permission != null && !r.permission.isEmpty() && !player.hasPermission(r.permission)) {
            lore.add("");
            lore.add("§c缺少權限: §e" + r.permission);
        }
        lore.add("");
        lore.add(can ? "§a✔ 點擊合成" : "§c✘ 材料不足");
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private static boolean canCraft(Player player, RecipeConfig.Recipe r) {
        if (r.permission != null && !r.permission.isEmpty() && !player.hasPermission(r.permission)) return false;
        for (Map.Entry<String, Integer> e : r.inputCards.entrySet())
            if (PlayerData.countCard(player.getUniqueId(), e.getKey()) < e.getValue()) return false;
        if (r.costPoints > 0 && PointAPI.getPlayerPoints(player.getName()) < r.costPoints) return false;
        // Vault / 物品 交給 RecipeEngine 執行時再精確檢查
        return true;
    }
}
