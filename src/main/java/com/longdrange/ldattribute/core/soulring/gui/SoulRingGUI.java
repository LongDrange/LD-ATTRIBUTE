package com.longdrange.ldattribute.core.soulring.gui;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.soulring.SoulRingConfig;
import com.longdrange.ldattribute.core.soulring.SoulRingData;
import com.longdrange.ldattribute.core.soulring.SoulRingFilter;
import com.longdrange.ldattribute.core.soulring.gui.SoulRingHolder.SortMode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SoulRingGUI {

    public static final int BTN_PREV = 45;
    public static final int BTN_CAT  = 47;
    public static final int BTN_EXCHANGE = 46;
    public static final int BTN_INFO = 49;
    public static final int BTN_SORT = 51;
    public static final int BTN_NEXT = 53;
    public static final int BTN_TRASH = 48;

    private final LDAttribute plugin;
    public SoulRingGUI(LDAttribute plugin) { this.plugin = plugin; }

    public void open(Player player) { open(player, 0, null); }

    public void open(Player player, int page, String filter) {
        String title = SoulRingConfig.getTitle();
        SoulRingData data = plugin.getSoulRingManager().get(player);

        SoulRingHolder holder = new SoulRingHolder(player.getUniqueId(), page, filter);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        render(holder, data);
        player.openInventory(inv);
    }

    public void refresh(Player player, SoulRingHolder holder) {
        if (player == null || holder == null) return;
        Inventory inv = holder.getInventory();
        if (inv == null) return;
        SoulRingData data = plugin.getSoulRingManager().get(player);
        render(holder, data);
        try { player.updateInventory(); } catch (Throwable ignored) {}
    }

    // ==================== 渲染 ====================

    private void render(SoulRingHolder holder, SoulRingData data) {
        Inventory inv = holder.getInventory();
        if (inv == null) return;

        SoulRingConfig.CategoryDef cat = SoulRingConfig.getCategory(holder.getCategoryIndex());

        // 1. 过滤
        List<SoulRingData.Entry> all = new ArrayList<>();
        for (SoulRingData.Entry e : data.getEntries()) {
            if (e == null || e.template == null) continue;
            if (!SoulRingFilter.matches(e.template, cat)) continue;
            if (!matchesSearch(e.template, holder.getFilter())) continue;
            all.add(e);
        }

        // 2. 排序
        sort(all, holder.getSortMode());

        // 3. 分页
        int perPage = SoulRingConfig.getSlotsPerPage();
        int totalPages = Math.max(1, (all.size() + perPage - 1) / perPage);
        int page = holder.getPage();
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;
        holder.setPage(page);

        // 4. 清空
        for (int i = 0; i < 45; i++) inv.setItem(i, null);

        // 5. 物品
        List<SoulRingData.Entry> pageList = new ArrayList<>();
        int start = page * perPage;
        for (int i = 0; i < perPage; i++) {
            int idx = start + i;
            if (idx >= all.size()) break;
            SoulRingData.Entry e = all.get(idx);
            inv.setItem(i, renderEntry(e));
            pageList.add(e);
        }
        holder.setPageEntries(pageList);

        // 6. 按钮
        if (page > 0) inv.setItem(BTN_PREV, icon(Material.ARROW, ChatColor.GRAY + "\u2190 上一页"));
        else inv.setItem(BTN_PREV, glass((short) 15, ChatColor.DARK_GRAY + "已是首页"));

        if (page < totalPages - 1) inv.setItem(BTN_NEXT, icon(Material.ARROW, ChatColor.GRAY + "下一页 \u2192"));
        else inv.setItem(BTN_NEXT, glass((short) 15, ChatColor.DARK_GRAY + "已是末页"));

        // 分类按钮（图标 = 分类 icon；名字显示当前分类）
        Material catIcon = Material.getMaterial(cat.icon);
        if (catIcon == null) catIcon = Material.CHEST;
        List<String> catLore = new ArrayList<>();
        catLore.add(ChatColor.YELLOW + "当前分类: " + cat.name);
        catLore.add("");
        StringBuilder sb = new StringBuilder();
        List<SoulRingConfig.CategoryDef> cats = SoulRingConfig.getCategories();
        for (int i = 0; i < cats.size(); i++) {
            SoulRingConfig.CategoryDef c = cats.get(i);
            if (i == holder.getCategoryIndex()) sb.append(ChatColor.GREEN).append("[").append(c.name).append("] ");
            else sb.append(ChatColor.DARK_GRAY).append(c.name).append(" ");
        }
        catLore.add(sb.toString().trim());
        catLore.add("");
        catLore.add(ChatColor.GRAY + "点击切换到下一个分类");
        inv.setItem(BTN_CAT, iconLore(catIcon, ChatColor.AQUA + "分类: " + cat.name, catLore));

        // 垃圾桶按钮
        inv.setItem(BTN_TRASH, icon(Material.LAVA_BUCKET,
                ChatColor.RED + "灵魂垃圾桶 ",
                ChatColor.GRAY + "点击打开垃圾桶界面",
                ChatColor.GRAY + "批量删除不要的物品"));

        // 兑换按钮
        inv.setItem(BTN_EXCHANGE, icon(Material.EMERALD,
                ChatColor.AQUA + "灵魂兑换 ",
                ChatColor.GRAY + "点击打开兑换界面"));

        // 排序按钮
        List<String> sortLore = new ArrayList<>();
        sortLore.add(ChatColor.YELLOW + "当前排序: " + ChatColor.WHITE + holder.getSortMode().label);
        sortLore.add("");
        StringBuilder sb2 = new StringBuilder();
        for (SortMode s : SortMode.values()) {
            if (s == holder.getSortMode()) sb2.append(ChatColor.GREEN).append(s.label).append(" ");
            else sb2.append(ChatColor.DARK_GRAY).append(s.label).append(" ");
        }
        sortLore.add(sb2.toString().trim());
        sortLore.add("");
        sortLore.add(ChatColor.GRAY + "点击切换到下一个排序");
        inv.setItem(BTN_SORT, iconLore(Material.PAPER, ChatColor.AQUA + "排序切换", sortLore));

        // 信息
        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.AQUA + "第 " + (page + 1) + " / " + totalPages + " 页");
        infoLore.add(ChatColor.GRAY + "总计 " + all.size() + " 种");
        infoLore.add(ChatColor.GRAY + "分类: " + cat.name);
        infoLore.add(ChatColor.GRAY + "排序: " + holder.getSortMode().label);
        infoLore.add("");
        infoLore.add(ChatColor.YELLOW + "【存入】点击背包物品:");
        infoLore.add(ChatColor.GRAY + "  左键=1  右键=16");
        infoLore.add(ChatColor.GRAY + "  Shift+左键=1组");
        infoLore.add(ChatColor.GRAY + "  Shift+右键=全部同类");
        infoLore.add(ChatColor.YELLOW + "【取出】点击 GUI 物品:");
        infoLore.add(ChatColor.GRAY + "  左键=1  右键=16");
        infoLore.add(ChatColor.GRAY + "  Shift+左键=1组");
        infoLore.add(ChatColor.GRAY + "  Shift+右键=全部");
        inv.setItem(BTN_INFO, iconLore(Material.BOOK, ChatColor.AQUA + "灵魂空间信息", infoLore));
    }

    private boolean matchesSearch(ItemStack stack, String filter) {
        if (filter == null || filter.isEmpty()) return true;
        String f = filter.toLowerCase();
        String mat = stack.getType().name().toLowerCase();
        if (mat.contains(f)) return true;
        if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
            String dn = ChatColor.stripColor(stack.getItemMeta().getDisplayName()).toLowerCase();
            if (dn.contains(f)) return true;
        }
        return false;
    }

    private void sort(List<SoulRingData.Entry> list, SortMode mode) {
        switch (mode) {
            case NAME_ASC:
                list.sort(Comparator.comparing(e -> nameOf(e.template), String.CASE_INSENSITIVE_ORDER));
                break;
            case NAME_DESC:
                list.sort((a, b) -> nameOf(b.template).compareToIgnoreCase(nameOf(a.template)));
                break;
            case COUNT_ASC:
                list.sort(Comparator.comparingLong(e -> e.count));
                break;
            case COUNT_DESC:
                list.sort((a, b) -> Long.compare(b.count, a.count));
                break;
            case TYPE:
                list.sort(Comparator.comparing(e -> e.template.getType().name()));
                break;
            case DEFAULT:
            default:
                break;
        }
    }

    private static String nameOf(ItemStack it) {
        if (it == null) return "";
        if (it.hasItemMeta() && it.getItemMeta().hasDisplayName()) {
            return ChatColor.stripColor(it.getItemMeta().getDisplayName());
        }
        return it.getType().name();
    }

    private ItemStack renderEntry(SoulRingData.Entry e) {
        ItemStack it = e.template.clone();
        it.setAmount((int) Math.min(e.count, 64));
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.YELLOW + "数量: " + ChatColor.WHITE + e.count);
            lore.add(ChatColor.GRAY + "左键=1 / 右键=16 / Shift+左键=1组 / Shift+右键=全部");
            meta.setLore(lore);
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
    private ItemStack iconLore(Material mat, String name, List<String> lore) {
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
