package com.longdrange.ldattribute.core.jewelry.gui;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.jewelry.JewelryConfig;
import com.longdrange.ldattribute.core.jewelry.JewelryData;
import com.longdrange.ldattribute.core.jewelry.JewelryStatsProvider;
import com.longdrange.ldattribute.core.util.PapiUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class JewelryGUI {

    public static final int BTN_PREV  = 45;
    public static final int BTN_STATS = 48;
    public static final int BTN_BACK  = 49;
    public static final int BTN_CLOSE = 50;
    public static final int BTN_NEXT  = 53;
    public static final int BTN_HELP  = 52;

    private final LDAttribute plugin;
    public JewelryGUI(LDAttribute plugin) { this.plugin = plugin; }

    public void open(Player player) { open(player, JewelryConfig.getDefaultPage()); }

    public void open(Player player, String pageId) {
        JewelryConfig.PageDef page = JewelryConfig.getPage(pageId);
        if (page == null) page = JewelryConfig.getPage(JewelryConfig.getDefaultPage());
        if (page == null) { player.sendMessage(ChatColor.RED + "没有配置任何饰品页"); return; }

        JewelryHolder holder = new JewelryHolder(player.getUniqueId(), page.id);
        Inventory inv = Bukkit.createInventory(holder, 54, PapiUtil.parse(player, page.title));
        holder.setInventory(inv);

        JewelryData data = plugin.getJewelryManager().get(player);

        // 渲染该页所有槽位
        for (JewelryConfig.SlotDef def : page.slots.values()) {
            if (def.guiSlot < 0 || def.guiSlot >= 45) continue;
            ItemStack item = data.get(def.id);
            if (item != null) inv.setItem(def.guiSlot, renderJewelry(item, def));
            else inv.setItem(def.guiSlot, emptyIcon(def));
        }

        // 底栏按钮
        List<String> pageIds = new ArrayList<>();
        for (JewelryConfig.PageDef p : JewelryConfig.allPages()) pageIds.add(p.id);
        int idx = pageIds.indexOf(page.id);
        if (idx > 0) inv.setItem(BTN_PREV, icon(Material.ARROW, ChatColor.GRAY + "\u2190 上一页"));
        else inv.setItem(BTN_PREV, glass((short) 15, ChatColor.DARK_GRAY + "已是首页"));
        if (idx >= 0 && idx < pageIds.size() - 1) inv.setItem(BTN_NEXT, icon(Material.ARROW, ChatColor.GRAY + "下一页 \u2192"));
        else inv.setItem(BTN_NEXT, glass((short) 15, ChatColor.DARK_GRAY + "已是末页"));

        inv.setItem(BTN_STATS, statsIcon(player));
        inv.setItem(BTN_BACK,  icon(Material.STAINED_GLASS_PANE, ChatColor.YELLOW + "返回主页",
                ChatColor.GRAY + "点击打开 " + JewelryConfig.getDefaultPage(),
                (short) 4));
        inv.setItem(BTN_CLOSE, icon(Material.BARRIER, ChatColor.RED + "\u2715 关闭"));
        inv.setItem(BTN_HELP,  icon(Material.BOOK, ChatColor.GRAY + "? 帮助",
                ChatColor.GRAY + "点击背包里符合条件的饰品放入",
                ChatColor.GRAY + "饰品放入后无法取出"));

        // ===== PAPI 变量解析 =====
        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack it = inv.getItem(slot);
            if (it != null) PapiUtil.parseItem(player, it);
        }
        // =======================
        player.openInventory(inv);
    }

    private ItemStack renderJewelry(ItemStack item, JewelryConfig.SlotDef def) {
        ItemStack copy = item.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.RED + "已绑定，无法取出");
            meta.setLore(lore);
            copy.setItemMeta(meta);
        }
        return copy;
    }

    private ItemStack emptyIcon(JewelryConfig.SlotDef def) {
        Material m = Material.getMaterial(def.icon);
        if (m == null) m = Material.STAINED_GLASS_PANE;
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GRAY + "\u2718 空槽位: " + def.name);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "槽位ID: " + ChatColor.YELLOW + def.id);
            lore.add(ChatColor.GRAY + "Lore需写: " + ChatColor.YELLOW + "饰品槽位: " + def.loreKey);
            if (!def.permission.isEmpty()) lore.add(ChatColor.YELLOW + "权限: " + def.permission);
            lore.add("");
            lore.add(ChatColor.GRAY + "把符合条件的饰品放到背包，");
            lore.add(ChatColor.GRAY + "然后点击它即可放入。");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack statsIcon(Player player) {
        ItemStack it = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "饰品属性总览");
            List<String> lore = new ArrayList<>();
            Map<String, Double> total = JewelryStatsProvider.getTotal(player);
            if (total.isEmpty()) lore.add(ChatColor.GRAY + "  暂无饰品属性");
            else for (Map.Entry<String, Double> e : total.entrySet())
                lore.add(ChatColor.GREEN + e.getKey() + ": " + ChatColor.WHITE + JewelryStatsProvider.format(e.getValue()));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack icon(Material mat, String name, String... lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && lore.length > 0) meta.setLore(Arrays.asList(lore));
            it.setItemMeta(meta);
        }
        return it;
    }
    private ItemStack icon(Material mat, String name, String lore, short data) {
        ItemStack it = new ItemStack(mat, 1, data);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) meta.setLore(Arrays.asList(lore));
            it.setItemMeta(meta);
        }
        return it;
    }
    private ItemStack glass(short data, String name) {
        ItemStack it = new ItemStack(Material.STAINED_GLASS_PANE, 1, data);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); it.setItemMeta(meta); }
        return it;
    }
}