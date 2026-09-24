package com.longdrange.ldattribute.card.inventory;

import com.longdrange.ldattribute.card.CardData;
import com.longdrange.ldattribute.card.CardDataManager;
import com.longdrange.ldattribute.card.CardNBT;
import com.longdrange.ldattribute.card.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class PlayerInspectInventory {
    public static final String TITLE_PREFIX = "§8[§6玩家详情§8] ";
    public static final int SLOT_PREV = 45;
    public static final int SLOT_BACK = 49;
    public static final int SLOT_NEXT = 53;

    private static final Map<UUID, Integer> lastPage = new HashMap<>();
    private static final Map<UUID, String> lastTarget = new HashMap<>();

    public static void open(Player viewer, String targetName) { open(viewer, targetName, 0); }

    public static void openPrev(Player viewer) {
        UUID u = viewer.getUniqueId();
        String t = lastTarget.get(u);
        if (t == null) { viewer.closeInventory(); return; }
        open(viewer, t, lastPage.getOrDefault(u, 0) - 1);
    }
    public static void openNext(Player viewer) {
        UUID u = viewer.getUniqueId();
        String t = lastTarget.get(u);
        if (t == null) { viewer.closeInventory(); return; }
        open(viewer, t, lastPage.getOrDefault(u, 0) + 1);
    }

    public static void open(Player viewer, String targetName, int page) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || target.getUniqueId() == null) {
            viewer.sendMessage("§c找不到玩家: " + targetName);
            return;
        }
        UUID uuid = target.getUniqueId();
        String name = target.getName() == null ? targetName : target.getName();
        Player online = Bukkit.getPlayer(uuid);
        lastTarget.put(viewer.getUniqueId(), name);

        // 卡片列表
        List<ItemStack> cards = new ArrayList<>();
        if (online != null) {
            try {
                List<ItemStack> raw = PlayerData.getCards(online);
                if (raw != null) cards.addAll(raw);
            } catch (Throwable ignored) {}
        }
        int cardCount = cards.size();
        int levelSum = 0;
        for (ItemStack it : cards) {
            try { levelSum += CardNBT.getLevel(it); } catch (Throwable ignored) {}
        }

        int petCount = 0;
        try {
            List<com.longdrange.ldattribute.pet.PetInstance> ps =
                    com.longdrange.ldattribute.pet.PetData.getAllPets(uuid);
            if (ps != null) petCount = ps.size();
        } catch (Throwable ignored) {}
        int runeCount = 0;
        try { runeCount = com.longdrange.ldattribute.rune.RuneData.count(uuid); } catch (Throwable ignored) {}
        int points = 0;
        try { points = com.longdrange.ldattribute.points.PointAPI.getPlayerPoints(name); } catch (Throwable ignored) {}
        int achDone = 0, achTotal = 0;
        try {
            for (com.longdrange.ldattribute.achievement.AchievementConfig.Achievement a :
                    com.longdrange.ldattribute.achievement.AchievementConfig.getAll()) {
                achTotal++;
                if (com.longdrange.ldattribute.achievement.AchievementData.isClaimed(uuid, a.id)) achDone++;
            }
        } catch (Throwable ignored) {}

        int perPage = 9;
        int totalPages = Math.max(1, (cardCount + perPage - 1) / perPage);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;
        lastPage.put(viewer.getUniqueId(), page);

        String title = TITLE_PREFIX + name + " §7[" + (page + 1) + "/" + totalPages + "]";
        if (title.replaceAll("§.", "").length() > 32) title = TITLE_PREFIX + name;
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // 第1排：标题书
        ItemStack titleBook = new ItemStack(Material.BOOK);
        ItemMeta tm = titleBook.getItemMeta();
        if (tm != null) {
            tm.setDisplayName("§b【理想龙猫】§c伺服器");
            List<String> tl = new ArrayList<>();
            tl.add("§7玩家: §f" + name);
            tl.add("§7UUID: §f" + uuid);
            tl.add("§7状态: " + (online != null ? "§a● 在线" : "§7○ 离线"));
            tl.add("§7点数: §e" + points);
            tl.add("§7卡片数: §e" + cardCount);
            tl.add("§7卡片等级总和: §e" + levelSum);
            tm.setLore(tl);
            titleBook.setItemMeta(tm);
        }
        for (int i = 0; i < 9; i++) inv.setItem(i, titleBook.clone());

        // 第2排：卡片
        for (int i = 0; i < 9; i++) {
            int idx = page * perPage + i;
            if (idx >= cardCount) break;
            inv.setItem(9 + i, cards.get(idx));
        }

        // 第3排：玻璃（等级/星级）
        for (int i = 0; i < 9; i++) {
            int idx = page * perPage + i;
            if (idx >= cardCount) break;
            ItemStack it = cards.get(idx);
            CardData cd = CardDataManager.findCard(it);
            ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 5);
            ItemMeta gm = glass.getItemMeta();
            if (gm != null) {
                gm.setDisplayName("§e" + (cd != null ? cd.getId() : "?"));
                List<String> gl = new ArrayList<>();
                try { gl.add("§7等级: §e" + CardNBT.getLevel(it)); } catch (Throwable ignored) {}
                try { gl.add("§7星级: §e" + CardNBT.getStar(it)); } catch (Throwable ignored) {}
                gm.setLore(gl);
                glass.setItemMeta(gm);
            }
            inv.setItem(18 + i, glass);
        }

        // 第4排：统计
        ItemStack stat = new ItemStack(Material.PAPER);
        ItemMeta sm = stat.getItemMeta();
        if (sm != null) {
            sm.setDisplayName("§e统计详情");
            sm.setLore(Arrays.asList(
                "§7在线: " + (online != null ? "§a是" : "§c否"),
                "§7卡片数: §e" + cardCount,
                "§7等级总和: §e" + levelSum,
                "§7宠物数: §e" + petCount,
                "§7符文图鉴: §e" + runeCount,
                "§7点数: §e" + points,
                "§7成就已领: §e" + achDone + "§7/§e" + achTotal
            ));
            stat.setItemMeta(sm);
        }
        for (int i = 0; i < 9; i++) inv.setItem(27 + i, stat.clone());

        // 第5排：汇总书（lore 列出所有卡片）
        ItemStack summary = new ItemStack(Material.BOOK);
        ItemMeta sum = summary.getItemMeta();
        if (sum != null) {
            sum.setDisplayName("§b【理想龙猫】§c伺服器");
            List<String> sl = new ArrayList<>();
            sl.add("§7玩家: §f" + name);
            sl.add("§7卡片总数: §e" + cardCount);
            sl.add("");
            sl.add("§e✦ 卡片清单:");
            if (cardCount == 0) {
                sl.add("  §7(离线或无数据)");
            } else {
                int shown = 0;
                for (ItemStack it : cards) {
                    if (shown >= 20) { sl.add("  §7... 还有 " + (cardCount - 20) + " 张"); break; }
                    CardData cd = CardDataManager.findCard(it);
                    String cid = cd != null ? cd.getId() : "?";
                    int lv = 1, star = 0;
                    try { lv = CardNBT.getLevel(it); } catch (Throwable ignored) {}
                    try { star = CardNBT.getStar(it); } catch (Throwable ignored) {}
                    sl.add("  §7- §f" + cid + " §7Lv." + lv + (star > 0 ? " §6★" + star : ""));
                    shown++;
                }
            }
            sum.setLore(sl);
            summary.setItemMeta(sum);
        }
        for (int i = 0; i < 9; i++) inv.setItem(36 + i, summary.clone());

        // 第6排：翻页 + 返回
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pm = prev.getItemMeta();
            if (pm != null) pm.setDisplayName("§e§l← 上一页");
            prev.setItemMeta(pm);
            inv.setItem(SLOT_PREV, prev);
        }
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta bkm = back.getItemMeta();
        if (bkm != null) bkm.setDisplayName("§c§l← 返回");
        back.setItemMeta(bkm);
        inv.setItem(SLOT_BACK, back);
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nm = next.getItemMeta();
            if (nm != null) nm.setDisplayName("§e§l下一页 →");
            next.setItemMeta(nm);
            inv.setItem(SLOT_NEXT, next);
        }

        viewer.openInventory(inv);
    }

    public static boolean isInspect(String title) { return title.startsWith(TITLE_PREFIX); }
}