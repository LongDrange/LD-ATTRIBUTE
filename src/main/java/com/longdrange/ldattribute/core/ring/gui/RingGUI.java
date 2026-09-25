package com.longdrange.ldattribute.core.ring.gui;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.ring.RingAttributeReader;
import com.longdrange.ldattribute.core.ring.RingConfig;
import com.longdrange.ldattribute.core.ring.RingData;
import com.longdrange.ldattribute.core.util.PapiUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RingGUI {

    public static final int SLOT_COUNT_PER_PAGE = 36;
    public static final int BTN_PREV   = 36;
    public static final int BTN_PAGE   = 40;
    public static final int BTN_NEXT   = 44;
    public static final int BTN_STATS  = 45;
    public static final int BTN_CLOSE  = 49;
    public static final int BTN_HELP   = 53;
    public static final int BTN_SETS   = 48;

    private static final Map<UUID, Set<Integer>> flashSlots = new ConcurrentHashMap<>();
    public static void flash(UUID uuid, int gid) {
        flashSlots.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(gid);
    }
    public static void clearFlash(UUID uuid) { flashSlots.remove(uuid); }
    public static boolean isFlashing(UUID uuid, int gid) {
        Set<Integer> s = flashSlots.get(uuid);
        return s != null && s.contains(gid);
    }

    private final LDAttribute plugin;
    public RingGUI(LDAttribute plugin) { this.plugin = plugin; }

    public void open(Player player, int page) {
        if (page < 0) page = 0;
        RingData data = plugin.getRingManager().get(player);
        int maxPage = data.getUnlockedPages() - 1;
        if (page > maxPage) page = maxPage;

        RingHolder holder = new RingHolder(player.getUniqueId(), page);
        String title = RingConfig.getTitle()
                .replace("%page%", String.valueOf(page + 1))
                .replace("%max_page%", String.valueOf(data.getUnlockedPages()));
        title = PapiUtil.parse(player, title);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        UUID uuid = player.getUniqueId();
        int base = page * SLOT_COUNT_PER_PAGE;
        for (int i = 0; i < SLOT_COUNT_PER_PAGE; i++) {
            int gid = base + i;
            RingData.Slot slot = data.getSlot(gid);
            if (slot != null && slot.count > 0) inv.setItem(i, renderSlot(slot, uuid, gid));
            else if (!data.isUnlocked(gid)) inv.setItem(i, lockedIcon(gid));
        }

        // 上一页
        inv.setItem(BTN_PREV, page > 0
                ? icon(Material.ARROW, ChatColor.GRAY + "\u2190 上一页 " + tag(page), ChatColor.GREEN + "点击打开上一页")
                : icon(Material.STAINED_GLASS_PANE, ChatColor.DARK_GRAY + "\u2190 已是首页", ChatColor.DARK_GRAY + "没有上一页了", (short) 15));

        // 下一页
        int nextPage = page + 1;
        if (page < maxPage) {
            inv.setItem(BTN_NEXT, icon(Material.ARROW,
                    ChatColor.GRAY + "下一页 " + tag(nextPage + 1),
                    ChatColor.GREEN + "点击打开下一页"));
        } else {
            RingConfig.PageUnlock pu = RingConfig.getPageUnlock(nextPage);
            if (pu != null) {
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GREEN + "点击解锁第 " + (nextPage + 1) + " 页");
                lore.add(ChatColor.GRAY + "解锁条件：");
                lore.addAll(com.longdrange.ldattribute.core.ring.RingCost.describe(
                        pu.permission, pu.costPoints, pu.costVault, pu.items));
                inv.setItem(BTN_NEXT, icon(Material.STAINED_GLASS_PANE,
                        ChatColor.YELLOW + "解锁第 " + (nextPage + 1) + " 页", lore, (short) 4));
            } else {
                inv.setItem(BTN_NEXT, icon(Material.STAINED_GLASS_PANE,
                        ChatColor.DARK_GRAY + "已是末页 \u2192", ChatColor.DARK_GRAY + "没有下一页了", (short) 15));
            }
        }

        inv.setItem(BTN_PAGE, icon(Material.PAPER,
                ChatColor.AQUA + "第 " + (page + 1) + " / " + data.getUnlockedPages() + " 页",
                ChatColor.GRAY + "总解锁页数: " + data.getUnlockedPages()));

        inv.setItem(BTN_STATS, statsIcon(player, data));
        inv.setItem(BTN_SETS, icon(Material.BOOK_AND_QUILL,
                ChatColor.AQUA + "套装图鉴 ",
                ChatColor.GRAY + "查看所有魂珠套装"));
        inv.setItem(BTN_CLOSE, icon(Material.BARRIER, ChatColor.RED + "\u2715 关闭", ChatColor.GRAY + "关闭魂珠空间"));
        inv.setItem(BTN_HELP, icon(Material.BOOK, ChatColor.GRAY + "? 帮助",
                ChatColor.GRAY + "点击背包里的魂珠即可放入",
                ChatColor.GRAY + "魂珠放入后无法取出",
                ChatColor.GRAY + "点击未解锁格子可以解锁"));

        // ===== PAPI 变量解析（标题已处理，这里处理物品）=====
        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack it = inv.getItem(slot);
            if (it != null) {
                PapiUtil.parseItem(player, it);
            }
        }
        // ==============================================
        player.openInventory(inv);
    }

    private ItemStack renderSlot(RingData.Slot slot, UUID uuid, int gid) {
        ItemStack it = slot.template.clone();
        it.setAmount(Math.min(slot.count, 64));
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            if (isFlashing(uuid, gid)) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            int maxLv = com.longdrange.ldattribute.core.ring.RingUpgradeConfig.getMaxLevel(slot.ringType);
            double bonus = com.longdrange.ldattribute.core.ring.RingUpgradeConfig.getBonusPerLevel(slot.ringType);
            double mult = 1.0 + (slot.level - 1) * bonus;
            lore.add(ChatColor.YELLOW + "等级: " + ChatColor.WHITE + "Lv." + slot.level + "/" + maxLv
                    + ChatColor.GRAY + " (" + String.format("x%.2f", mult) + ")");
            lore.add(ChatColor.YELLOW + "数量: " + ChatColor.WHITE + slot.count + "/" + slot.maxStack);
            lore.add(ChatColor.RED + "已绑定，无法取出");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack lockedIcon(int gid) {
        ItemStack it = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(ChatColor.RED + "未解锁 " + tag(gid));
            m.setLore(Arrays.asList(ChatColor.GREEN + "点击解锁", ChatColor.GRAY + "解锁后可放入魂珠"));
            it.setItemMeta(m);
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
    private ItemStack icon(Material mat, String name, String lore, short data) {
        ItemStack it = new ItemStack(mat, 1, data);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) m.setLore(Arrays.asList(lore));
            it.setItemMeta(m);
        }
        return it;
    }
    private ItemStack icon(Material mat, String name, String l1, String l2, short data) {
        ItemStack it = new ItemStack(mat, 1, data);
        ItemMeta m = it.getItemMeta();
        if (m != null) { m.setDisplayName(name); m.setLore(Arrays.asList(l1, l2)); it.setItemMeta(m); }
        return it;
    }
    private ItemStack icon(Material mat, String name, List<String> lore, short data) {
        ItemStack it = new ItemStack(mat, 1, data);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) m.setLore(lore);
            it.setItemMeta(m);
        }
        return it;
    }

    private ItemStack statsIcon(Player player, RingData data) {
        ItemStack it = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            int code = Math.abs(player.getUniqueId().hashCode()) % 10000;
            m.setDisplayName(ChatColor.GOLD + "您的属性 " + tag(code));
            List<String> lore = new ArrayList<>();
            Map<String, Double> total = RingAttributeReader.sumAll(data);
            if (total.isEmpty()) lore.add(ChatColor.GRAY + "  暂无魂珠属性");
            else for (Map.Entry<String, Double> e : total.entrySet())
                lore.add(ChatColor.GREEN + e.getKey() + ": " + ChatColor.WHITE + formatNum(e.getValue()));
            m.setLore(lore);
            it.setItemMeta(m);
        }
        return it;
    }

    public static String tag(int id) { return ChatColor.DARK_GRAY + "(#" + String.format("%04d", id) + ")"; }

    public static String formatNum(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((long) v);
        return String.format("%.1f", v);
    }
}