package com.longdrange.ldattribute.card.inventory;

import com.longdrange.ldattribute.card.CardData;
import com.longdrange.ldattribute.card.CardDataManager;
import com.longdrange.ldattribute.card.PlayerData;
import com.longdrange.ldattribute.card.SynergyData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SynergyDetailInventory {
    public static final String TITLE_PREFIX = "§8[§6共鸣详情§8] ";
    public static final int SLOT_PREV = 45;
    public static final int SLOT_BACK = 49;
    public static final int SLOT_NEXT = 53;

    private static final Map<UUID, Integer> lastPage = new HashMap<>();
    private static final Map<UUID, String> lastSynId = new HashMap<>();

    public static void open(Player player, String synId) { open(player, synId, 0); }

    public static void openPrev(Player player) {
        UUID u = player.getUniqueId();
        String s = lastSynId.get(u);
        if (s == null) { player.closeInventory(); return; }
        open(player, s, lastPage.getOrDefault(u, 0) - 1);
    }
    public static void openNext(Player player) {
        UUID u = player.getUniqueId();
        String s = lastSynId.get(u);
        if (s == null) { player.closeInventory(); return; }
        open(player, s, lastPage.getOrDefault(u, 0) + 1);
    }

    public static void open(Player player, String synId, int page) {
        SynergyData.Synergy s = null;
        boolean isBond = false;
        for (SynergyData.Synergy x : SynergyData.getAllResonances()) {
            if (x.id.equalsIgnoreCase(synId)) { s = x; break; }
        }
        if (s == null) {
            for (SynergyData.Synergy x : SynergyData.getAllBonds()) {
                if (x.id.equalsIgnoreCase(synId)) { s = x; isBond = true; break; }
            }
        }
        if (s == null) { player.sendMessage("§c未找到: " + synId); return; }

        UUID uuid = player.getUniqueId();
        lastSynId.put(uuid, s.id);

        List<ItemStack> cards = PlayerData.getCards(player);
        int matched = SynergyData.getMatched(cards, s);
        boolean active = matched >= s.required;
        boolean claimed = PlayerData.hasClaimed(uuid, "synergy_" + s.id);

        int cardCount = s.cards == null ? 0 : s.cards.size();
        int attrCount = s.attributes == null ? 0 : s.attributes.size();
        int totalPages = Math.max(1, Math.max((cardCount + 8) / 9, (attrCount + 8) / 9));
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;
        lastPage.put(uuid, page);

        String title = TITLE_PREFIX + s.name + " §7[" + (page + 1) + "/" + totalPages + "]";
        if (title.replaceAll("§.", "").length() > 32)
            title = TITLE_PREFIX + s.id + " §7[" + (page + 1) + "/" + totalPages + "]";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // 第一排：标题书
        ItemStack titleBook = new ItemStack(Material.BOOK);
        ItemMeta tm = titleBook.getItemMeta();
        if (tm != null) {
            tm.setDisplayName("§b【理想龙猫】§c伺服器");
            List<String> tl = new ArrayList<>();
            tl.add("§7名称: §f" + s.name);
            tl.add("§7类型: §f" + (isBond ? "羁绊" : "共鸣"));
            tl.add("§7进度: §e" + matched + "§7/§e" + s.required);
            tl.add("§7状态: " + (active ? "§a✦ 已激活" : "§c未激活"));
            tl.add("§7奖励: " + (claimed ? "§8已领取" : (active ? "§a可领取" : "§7未达成")));
            tm.setLore(tl);
            titleBook.setItemMeta(tm);
        }
        for (int i = 0; i < 9; i++) inv.setItem(i, titleBook.clone());

        // 第二排：卡片 + 第三排：状态玻璃
        for (int i = 0; i < 9; i++) {
            int idx = page * 9 + i;
            if (idx >= cardCount) break;
            String cid = s.cards.get(idx);
            boolean has = false;
            CardData target = CardDataManager.getCard(cid);
            if (target != null) {
                for (ItemStack it : cards) {
                    if (target.matches(it)) { has = true; break; }
                }
            }
            ItemStack icon;
            if (target != null) {
                icon = target.getItem();
                ItemMeta im = icon.getItemMeta();
                if (im != null) {
                    List<String> lore = im.getLore() == null ? new ArrayList<>() : new ArrayList<>(im.getLore());
                    lore.add("");
                    lore.add(has ? "§a✓ 已放入" : "§c✗ 尚未放入");
                    im.setLore(lore);
                    icon.setItemMeta(im);
                }
            } else {
                icon = new ItemStack(Material.PAPER);
                ItemMeta im = icon.getItemMeta();
                if (im != null) {
                    im.setDisplayName("§7" + cid);
                    im.setLore(Arrays.asList("§c未知卡片"));
                    icon.setItemMeta(im);
                }
            }
            inv.setItem(9 + i, icon);

            ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) (has ? 5 : 14));
            ItemMeta gm = glass.getItemMeta();
            if (gm != null) {
                gm.setDisplayName(has ? "§a✓ 已放入" : "§c✗ 尚未放入");
                gm.setLore(Arrays.asList("§7卡片: §f" + cid));
                glass.setItemMeta(gm);
            }
            inv.setItem(18 + i, glass);
        }

        // 第四排：属性加成（每个属性一本书）
        if (s.attributes != null) {
            for (int i = 0; i < 9; i++) {
                int idx = page * 9 + i;
                if (idx >= attrCount) break;
                String attr = s.attributes.get(idx);
                ItemStack book = new ItemStack(Material.BOOK);
                ItemMeta bm = book.getItemMeta();
                if (bm != null) {
                    bm.setDisplayName(ChatColor.translateAlternateColorCodes((char) 38, attr));
                    bm.setLore(Arrays.asList("§7属性加成"));
                    book.setItemMeta(bm);
                }
                inv.setItem(27 + i, book);
            }
        }

        // 第五排：汇总书，lore 里放所有属性
        ItemStack summaryBook = new ItemStack(Material.BOOK);
        ItemMeta sm = summaryBook.getItemMeta();
        if (sm != null) {
            sm.setDisplayName("§b【理想龙猫】§c伺服器");
            List<String> sl = new ArrayList<>();
            sl.add("§7共鸣: §f" + s.name);
            sl.add("§7类型: §f" + (isBond ? "羁绊" : "共鸣"));
            sl.add("§7进度: §e" + matched + "§7/§e" + s.required);
            sl.add("");
            sl.add("§e✦ 完整属性加成:");
            if (s.attributes != null && !s.attributes.isEmpty()) {
                for (String attr : s.attributes) {
                    sl.add("  " + ChatColor.translateAlternateColorCodes((char) 38, attr));
                }
            } else {
                sl.add("  §7(无属性加成)");
            }
            sl.add("");
            sl.add("§7状态: " + (active ? "§a✦ 已激活" : "§c未激活"));
            sl.add("§7奖励: " + (claimed ? "§8已领取" : (active ? "§a可领取" : "§7未达成")));
            sm.setLore(sl);
            summaryBook.setItemMeta(sm);
        }
        for (int i = 0; i < 9; i++) inv.setItem(36 + i, summaryBook.clone());

        // 底排：翻页 + 返回
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

        player.openInventory(inv);
    }

    public static boolean isDetail(String title) { return title.startsWith(TITLE_PREFIX); }
}