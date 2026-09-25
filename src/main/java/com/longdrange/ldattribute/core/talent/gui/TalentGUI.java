package com.longdrange.ldattribute.core.talent.gui;


import com.longdrange.ldattribute.core.util.PapiUtil;import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.talent.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class TalentGUI {

    public static final int BTN_PREV  = 45;
    public static final int BTN_BACK  = 49;
    public static final int BTN_CLOSE = 50;
    public static final int BTN_NEXT  = 53;

    private final LDAttribute plugin;
    public TalentGUI(LDAttribute plugin) { this.plugin = plugin; }

    public void open(Player player) { open(player, TalentConfig.getDefaultPage()); }

    public void open(Player player, String pageId) {
        TalentConfig.PageDef page = TalentConfig.getPage(pageId);
        if (page == null) page = TalentConfig.getPage(TalentConfig.getDefaultPage());
        if (page == null) { player.sendMessage(ChatColor.RED + "没有配置天赋页"); return; }

        TalentData data = plugin.getTalentManager().get(player);
        int points = data.getPoints(page.id);

        String title = ChatColor.DARK_GRAY + "天赋 - " + ChatColor.stripColor(page.name)
                + ChatColor.DARK_GRAY + " | 点数: " + points;
        title = PapiUtil.parse(player, title);

        TalentHolder holder = new TalentHolder(player.getUniqueId(), page.id);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        for (TalentConfig.TalentDef def : page.talents.values()) {
            if (def.guiSlot < 0 || def.guiSlot >= 45) continue;
            inv.setItem(def.guiSlot, buildIcon(def, data, points));
        }

        // 底栏
        List<TalentConfig.PageDef> list = new ArrayList<>(TalentConfig.allPages());
        int idx = -1;
        for (int i = 0; i < list.size(); i++) if (list.get(i).id.equals(page.id)) { idx = i; break; }
        if (idx > 0) inv.setItem(BTN_PREV, icon(Material.ARROW, ChatColor.GRAY + "\u2190 上一页"));
        else inv.setItem(BTN_PREV, glass((short) 15, ChatColor.DARK_GRAY + "已是首页"));
        if (idx >= 0 && idx < list.size() - 1) inv.setItem(BTN_NEXT, icon(Material.ARROW, ChatColor.GRAY + "下一页 \u2192"));
        else inv.setItem(BTN_NEXT, glass((short) 15, ChatColor.DARK_GRAY + "已是末页"));

        inv.setItem(BTN_BACK,  glass((short) 4, ChatColor.YELLOW + "返回默认页"));
        inv.setItem(BTN_CLOSE, icon(Material.BARRIER, ChatColor.RED + "\u2715 关闭"));

        // ===== PAPI 变量解析 =====
        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack it = inv.getItem(slot);
            if (it != null) PapiUtil.parseItem(player, it);
        }
        // =======================
        player.openInventory(inv);
    }

    private ItemStack buildIcon(TalentConfig.TalentDef def, TalentData data, int points) {
        Material m = Material.getMaterial(def.icon);
        if (m == null) m = Material.PAPER;
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;

        int curLv = data.getLevel(def.id);
        boolean maxed = curLv >= def.maxLevel;
        boolean locked = data.hasAnyRequirementUnmet(def);
        boolean canAdd = !maxed && !locked && points >= def.pointsPerLevel;

        // 名字
        String prefix = maxed ? ChatColor.GREEN + "\u2714 " : (locked ? ChatColor.RED + "\u2718 " : ChatColor.YELLOW + "\u2726 ");
        meta.setDisplayName(prefix + def.name);

        // Lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "等级: " + ChatColor.WHITE + curLv + " / " + def.maxLevel);
        lore.add(ChatColor.GRAY + "消耗点数/级: " + ChatColor.YELLOW + def.pointsPerLevel);
        lore.add("");
        lore.add(ChatColor.WHITE + "每级加成:");
        for (String a : def.attributes) lore.add(ChatColor.GRAY + "  " + a);
        lore.add("");

        if (!def.requires.isEmpty()) {
            lore.add(ChatColor.WHITE + "前置天赋:");
            for (String r : def.requires) {
                TalentConfig.TalentDef rd = TalentConfig.get(r);
                String rn = rd == null ? r : ChatColor.stripColor(rd.name);
                int have = data.getLevel(r);
                lore.add((have > 0 ? ChatColor.GREEN + "  \u2714 " : ChatColor.RED + "  \u2718 ")
                        + ChatColor.GRAY + rn + (have > 0 ? "" : " (未解锁)"));
            }
            lore.add("");
        }

        // 当前加成
        if (curLv > 0) {
            lore.add(ChatColor.GREEN + "当前加成:");
            for (String a : def.attributes) {
                String plain = ChatColor.stripColor(a).trim();
                int idx = plain.indexOf(':');
                if (idx < 0) continue;
                String k = plain.substring(0, idx).trim();
                String v = plain.substring(idx + 1).trim();
                try {
                    double d = Double.parseDouble(v.replace("+", "").replace("%", "").trim());
                    lore.add(ChatColor.GRAY + "  " + k + ": " + ChatColor.GREEN + formatNum(d * curLv));
                } catch (Exception ignored) {}
            }
            lore.add("");
        }

        if (maxed) lore.add(ChatColor.GREEN + "\u2714 已满级");
        else if (locked) lore.add(ChatColor.RED + "\u2718 前置天赋未解锁");
        else if (!canAdd) lore.add(ChatColor.RED + "\u2718 点数不足（需 " + def.pointsPerLevel + "）");
        else {
            lore.add(ChatColor.GREEN + "\u2714 点击加点（-1 点）");
            lore.add(ChatColor.GREEN + "\u2714 Shift+点击连续加到满级");
        }

        meta.setLore(lore);
        it.setItemMeta(meta);
        return it;
    }

    private static String formatNum(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((long) v);
        return String.format("%.1f", v);
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
    private ItemStack glass(short data, String name) {
        ItemStack it = new ItemStack(Material.STAINED_GLASS_PANE, 1, data);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); it.setItemMeta(meta); }
        return it;
    }
}