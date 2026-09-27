package com.longdrange.ldattribute.core.dungeon.gui;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.dungeon.DungeonConfig;
import com.longdrange.ldattribute.core.dungeon.DungeonManager;
import com.longdrange.ldattribute.core.dungeon.DungeonInstance;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class DungeonGUI {

    public static final int PER_PAGE = 45;
    public static final int BTN_PREV = 45;
    public static final int BTN_INFO = 49;
    public static final int BTN_NEXT = 53;

    private static final String TITLE_ICON = "\u00a7b\u3010\u7406\u60f3\u9f99\u732b\u3011\u00a7c\u4f3a\u670d\u5668\u00a72\u526f\u672c";

    private final LDAttribute plugin;
    public DungeonGUI(LDAttribute plugin) { this.plugin = plugin; }

    public void open(Player player) { open(player, 0); }

    public void open(Player player, int page) {
        List<DungeonConfig.DungeonDef> list = DungeonConfig.visibleFor(player);

        int totalPages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        String title = ChatColor.DARK_GRAY + "\u2726 " + ChatColor.AQUA + "副本列表 "
                + ChatColor.DARK_GRAY + "(" + (page + 1) + "/" + totalPages + ")";

        DungeonHolder holder = new DungeonHolder(player.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        List<String> pageIds = new ArrayList<>();
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = start + i;
            if (idx >= list.size()) break;
            DungeonConfig.DungeonDef def = list.get(idx);
            inv.setItem(i, renderIcon(player, def));
            pageIds.add(def.id);
        }
        holder.setPageIds(pageIds);

        if (page > 0) inv.setItem(BTN_PREV, icon(Material.ARROW, ChatColor.GRAY + "\u2190 \u4e0a\u4e00\u9875"));
        else inv.setItem(BTN_PREV, glass((short) 15, ChatColor.DARK_GRAY + "\u5df2\u662f\u9996\u9875"));

        if (page < totalPages - 1) inv.setItem(BTN_NEXT, icon(Material.ARROW, ChatColor.GRAY + "\u4e0b\u4e00\u9875 \u2192"));
        else inv.setItem(BTN_NEXT, glass((short) 15, ChatColor.DARK_GRAY + "\u5df2\u662f\u672b\u9875"));

        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.YELLOW + "副本数: " + ChatColor.WHITE + list.size());
        infoLore.add(ChatColor.YELLOW + "页码: " + ChatColor.WHITE + (page + 1) + "/" + totalPages);
        inv.setItem(BTN_INFO, iconLore(Material.BOOK, TITLE_ICON, infoLore));

        player.openInventory(inv);
    }

    private ItemStack renderIcon(Player p, DungeonConfig.DungeonDef def) {
        Material mat = Material.getMaterial(def.icon);
        if (mat == null) mat = Material.STONE;
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;

        meta.setDisplayName(def.name);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "\u00a7m----------------");

        // 状态
        DungeonManager mgr = plugin.getDungeonManager();
        DungeonInstance in = mgr.getInstance(p);
        boolean inThis = in != null && def.id.equals(in.dungeonId);
        long cd = mgr.getRemainingCooldown(p.getUniqueId(), def.id);

        if (inThis) {
            lore.add(ChatColor.GREEN + "\u25b6 \u4f60\u6b63\u5728\u6b64\u526f\u672c\u4e2d");
        } else if (in != null) {
            lore.add(ChatColor.YELLOW + "\u26a0 \u4f60\u5df2\u5728\u5176\u4ed6\u526f\u672c\u4e2d");
        } else if (cd > 0) {
            lore.add(ChatColor.RED + "\u51b7\u5374\u4e2d: " + ChatColor.WHITE + DungeonManager.formatTime(cd));
        } else {
            lore.add(ChatColor.GREEN + "\u2714 \u53ef\u8fdb\u5165");
        }
        lore.add("");

        // 进入条件
        lore.add(ChatColor.YELLOW + "\u8fdb\u5165\u6761\u4ef6:");
        if (def.entry.costPoints > 0) lore.add(ChatColor.GRAY + "  \u00b7 \u70b9\u5238: " + ChatColor.GOLD + def.entry.costPoints);
        if (def.entry.costVault > 0) lore.add(ChatColor.GRAY + "  \u00b7 \u91d1\u5e01: " + ChatColor.GOLD + (long) def.entry.costVault);
        if (def.entry.costPoints == 0 && def.entry.costVault == 0) lore.add(ChatColor.GRAY + "  \u00b7 \u514d\u8d39");
        lore.add("");

        // 时间与 CD
        lore.add(ChatColor.YELLOW + "\u65f6\u95f4\u9650\u5236: " + ChatColor.WHITE + DungeonManager.formatTime(def.timeLimit));
        lore.add(ChatColor.YELLOW + "\u51b7\u5374: " + ChatColor.WHITE + DungeonManager.formatTime(def.cooldown));
        lore.add("");

        // 波次
        lore.add(ChatColor.YELLOW + "\u6ce2\u6b21: " + ChatColor.WHITE + def.waves.size());
        for (int i = 0; i < def.waves.size(); i++) {
            DungeonConfig.WaveDef w = def.waves.get(i);
            lore.add(ChatColor.GRAY + "  " + (i + 1) + ". " + w.name + ChatColor.DARK_GRAY + " (" + w.mobs.size() + "\u79cd\u602a)");
        }
        lore.add("");

        // 奖励
        lore.add(ChatColor.YELLOW + "\u901a\u5173\u5956\u52b1:");
        if (def.reward.points > 0) lore.add(ChatColor.GRAY + "  \u00b7 \u70b9\u5238: " + ChatColor.GOLD + def.reward.points);
        if (def.reward.vault > 0) lore.add(ChatColor.GRAY + "  \u00b7 \u91d1\u5e01: " + ChatColor.GOLD + (long) def.reward.vault);
        for (String s : def.reward.items) lore.add(ChatColor.GRAY + "  \u00b7 " + s);
        for (String s : def.reward.cards) lore.add(ChatColor.GRAY + "  \u00b7 \u5361\u7247: " + s);
        if (def.reward.points == 0 && def.reward.vault == 0 && def.reward.items.isEmpty() && def.reward.cards.isEmpty()) {
            lore.add(ChatColor.GRAY + "  \uff08\u65e0\uff09");
        }
        lore.add("");

        lore.add(ChatColor.YELLOW + "\u70b9\u51fb\u8fdb\u5165\u526f\u672c");

        meta.setLore(lore);
        it.setItemMeta(meta);
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
