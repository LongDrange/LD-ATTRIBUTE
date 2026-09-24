package com.longdrange.ldattribute.achievement;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class AchievementInventory {
    public static final String TITLE_PREFIX = "\u00a78[\u00a76\u6210\u5c31\u00a78] ";
    public static final int PER_PAGE = 45;
    public static int SLOT_BACK = 49;
    public static int SLOT_PREV = 45;
    public static int SLOT_INFO = 48;
    public static int SLOT_NEXT = 53;

    private static final Map<UUID, Integer> lastPage = new HashMap<>();

    public static void open(Player player) { open(player, 0); }

    public static void open(Player player, int page) {
        List<AchievementConfig.Achievement> all = new ArrayList<>(AchievementConfig.getAll());
        int total = all.size();
        if (total == 0) { player.sendMessage("\u00a7c\u5c1a\u672a\u8f09\u5165\u6210\u5c31"); return; }
        int maxPage = (total + PER_PAGE - 1) / PER_PAGE - 1;
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;

        int completedCount = 0;
        for (AchievementConfig.Achievement a : all)
            if (AchievementData.isCompleted(player.getUniqueId(), a.id)) completedCount++;

        String title = TITLE_PREFIX + "\u5df2\u5b8c\u6210 " + completedCount + "/" + total;
        if (title.replaceAll("\u00a7.", "").length() > 32) title = TITLE_PREFIX + completedCount + "/" + total;

        Inventory inv = Bukkit.createInventory(null, 54, title);
        lastPage.put(player.getUniqueId(), page);
        int start = page * PER_PAGE;

        for (int i = 0; i < PER_PAGE; i++) {
            int idx = start + i;
            if (idx >= total) break;
            inv.setItem(i, buildIcon(player, all.get(idx)));
        }

        ItemStack sep = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta sm = sep.getItemMeta(); sm.setDisplayName(" "); sep.setItemMeta(sm);
        for (int i = 45; i < 54; i++) {
            if (i == SLOT_BACK || i == SLOT_PREV || i == SLOT_INFO || i == SLOT_NEXT) continue;
            inv.setItem(i, sep);
        }

        if (page > 0) {
            ItemStack p = new ItemStack(Material.ARROW);
            ItemMeta pm = p.getItemMeta(); pm.setDisplayName("\u00a7e\u00a7l\u2190 \u4e0a\u4e00\u9801"); p.setItemMeta(pm);
            inv.setItem(SLOT_PREV, p);
        }
        if (page < maxPage) {
            ItemStack p = new ItemStack(Material.ARROW);
            ItemMeta pm = p.getItemMeta(); pm.setDisplayName("\u00a7e\u00a7l\u4e0b\u4e00\u9801 \u2192"); p.setItemMeta(pm);
            inv.setItem(SLOT_NEXT, p);
        }

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("\u00a7a\u6210\u5c31\u7d71\u8a08");
        im.setLore(Arrays.asList(
                "\u00a77\u5df2\u5b8c\u6210: \u00a7e" + completedCount + "\u00a77/\u00a7e" + total,
                "\u00a77\u9801\u6578: \u00a7e" + (page + 1) + "\u00a77/\u00a7e" + (maxPage + 1)
        ));
        info.setItemMeta(im);
        inv.setItem(SLOT_INFO, info);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta(); bm.setDisplayName("\u00a7c\u00a7l\u2190 \u8fd4\u56de"); back.setItemMeta(bm);
        inv.setItem(SLOT_BACK, back);

        player.openInventory(inv);
    }

    private static ItemStack buildIcon(Player player, AchievementConfig.Achievement a) {
        UUID uuid = player.getUniqueId();
        boolean completed = AchievementData.isCompleted(uuid, a.id);
        boolean claimed = AchievementData.isClaimed(uuid, a.id);
        int progress = AchievementData.getProgress(uuid, a.id);

        ItemStack item;
        if (claimed) item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 5);
        else if (completed) item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 4);
        else item = new ItemStack(a.icon);
        ItemMeta m = item.getItemMeta();

        String status;
        if (claimed) status = "\u00a78[\u5df2\u767c\u653e]";
        else if (completed) status = "\u00a7a[\u53ef\u9818\u53d6]";
        else status = "\u00a77[" + progress + "/" + a.target + "]";

        m.setDisplayName((claimed ? "\u00a78" : completed ? "\u00a7a" : "\u00a7f") + a.name + " " + status);

        List<String> lore = new ArrayList<>();
        lore.add("\u00a77" + a.description);
        lore.add("");
        lore.add("\u00a77\u985e\u578b: \u00a7f" + a.type);
        lore.add("\u00a77\u9032\u5ea6: \u00a7e" + progress + "\u00a77/\u00a7e" + a.target);
        int barLen = 20;
        int filled = a.target <= 0 ? 0 : Math.min(barLen, progress * barLen / a.target);
        StringBuilder bar = new StringBuilder("\u00a7a");
        for (int i = 0; i < filled; i++) bar.append("\u2588");
        bar.append("\u00a77");
        for (int i = filled; i < barLen; i++) bar.append("\u2588");
        lore.add(bar.toString());
        lore.add("");
        lore.add("\u00a77\u734e\u52b5:");
        if (a.rewardPoints > 0) lore.add("  \u00a77- \u9ede\u5238: \u00a76" + a.rewardPoints);
        for (String c : a.rewardCards) lore.add("  \u00a77- \u5361\u7247: \u00a7e" + c);
        for (String r : a.rewardRunes) lore.add("  \u00a77- \u7b26\u6587: \u00a7e" + r);
        lore.add("");
        if (claimed) lore.add("\u00a78\u2714 \u5df2\u81ea\u52d5\u767c\u653e");
        else if (completed) lore.add("\u00a7e\u9ede\u64ca\u9818\u53d6\u734e\u52b5");
        else lore.add("\u00a77\u5c1a\u672a\u5b8c\u6210");
        m.setLore(lore);
        item.setItemMeta(m);
        return item;
    }

    public static int getLastPage(UUID uuid) { return lastPage.getOrDefault(uuid, 0); }
    public static boolean isAchievement(String title) { return title.startsWith(TITLE_PREFIX); }
}