package com.longdrange.ldattribute.core.guide.gui;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.guide.GuideConfig;
import com.longdrange.ldattribute.core.guide.GuideData;
import com.longdrange.ldattribute.core.util.PapiUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GuideGUI {

    public static final int BTN_PREV  = 45;
    public static final int BTN_INFO  = 49;
    public static final int BTN_NEXT  = 53;

    private static final short GLASS_LOCKED   = 7;   // 灰玻璃
    private static final short GLASS_UNLOCKED = 5;   // 绿玻璃

    private final LDAttribute plugin;
    public GuideGUI(LDAttribute plugin) { this.plugin = plugin; }

    public void open(Player player) { open(player, null, 0); }

    public void open(Player player, String groupId, int page) {
        GuideData data = plugin.getGuideManager().get(player);
        List<GuideConfig.MonsterDef> list = GuideConfig.byGroup(groupId);

        int perPage = 36;
        int totalPages = Math.max(1, (list.size() + perPage - 1) / perPage);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        String groupName = (groupId == null)
                ? "全部"
                : ChatColor.stripColor(GuideConfig.getGroup(groupId) == null
                        ? groupId
                        : GuideConfig.getGroup(groupId).name);

        String title = GuideConfig.getTitle()
                + ChatColor.DARK_GRAY + " " + groupName
                + ChatColor.DARK_GRAY + " (" + (page + 1) + "/" + totalPages + ")";
        title = PapiUtil.parse(player, title);

        GuideHolder holder = new GuideHolder(player.getUniqueId(), groupId, page);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        // 顶部 tab
        int i = 0;
        for (GuideConfig.GroupDef g : GuideConfig.allGroups()) {
            if (i >= 9) break;
            inv.setItem(i, tabIcon(g, groupId));
            i++;
        }

        // 怪物图标
        int start = page * perPage;
        for (int s = 0; s < perPage; s++) {
            int idx = start + s;
            if (idx >= list.size()) break;
            GuideConfig.MonsterDef def = list.get(idx);
            inv.setItem(9 + s, monsterIcon(def, data));
        }

        // 翻页按钮
        if (page > 0) {
            inv.setItem(BTN_PREV, icon(Material.ARROW,
                    ChatColor.GRAY + "\u2190 上一页 " + ChatColor.DARK_GRAY + "(" + page + "/" + totalPages + ")"));
        } else {
            inv.setItem(BTN_PREV, glass(GLASS_LOCKED, ChatColor.DARK_GRAY + "\u2190 已是首页"));
        }
        if (page < totalPages - 1) {
            inv.setItem(BTN_NEXT, icon(Material.ARROW,
                    ChatColor.GRAY + "下一页 \u2192 " + ChatColor.DARK_GRAY + "(" + (page + 2) + "/" + totalPages + ")"));
        } else {
            inv.setItem(BTN_NEXT, glass(GLASS_LOCKED, ChatColor.DARK_GRAY + "已是末页 \u2192"));
        }

        // 进度
        int total = GuideConfig.allMonsters().size();
        int unlocked = 0;
        for (GuideConfig.MonsterDef d : GuideConfig.allMonsters()) {
            if (data.isUnlocked(d.id)) unlocked++;
        }
        inv.setItem(BTN_INFO, icon(Material.BOOK,
                ChatColor.AQUA + "图鉴进度",
                ChatColor.GRAY + "总进度: " + ChatColor.WHITE + unlocked + "/" + total,
                ChatColor.GRAY + "当前页: " + ChatColor.WHITE + (page + 1) + "/" + totalPages));

        // ===== PAPI 变量解析 =====
        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack it = inv.getItem(slot);
            if (it != null) PapiUtil.parseItem(player, it);
        }
        // =======================
        player.openInventory(inv);
    }

    // ==================== 顶部 Tab ====================

    private ItemStack tabIcon(GuideConfig.GroupDef g, String current) {
        boolean active = (g.id.equals(current)) || (current == null && isFirstGroup(g.id));
        Material m = Material.getMaterial(g.icon);
        if (m == null) m = Material.CHEST;
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName((active ? ChatColor.GREEN + "\u25B6 " : ChatColor.GRAY + "  ") + g.name);
            List<String> lore = new ArrayList<>();
            if (active) lore.add(ChatColor.GRAY + "(当前选中)");
            else lore.add(ChatColor.GRAY + "点击切换分组");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private boolean isFirstGroup(String id) {
        for (GuideConfig.GroupDef g : GuideConfig.allGroups()) return g.id.equals(id);
        return false;
    }

    // ==================== 怪物图标（含进度条、概率、属性预览）====================

    private ItemStack monsterIcon(GuideConfig.MonsterDef def, GuideData data) {
        boolean unlocked = data.isUnlocked(def.id);
        int kills = data.getKills(def.id);
        int req = def.requiredKills;

        ItemStack it = new ItemStack(Material.STAINED_GLASS_PANE, 1,
                unlocked ? GLASS_UNLOCKED : GLASS_LOCKED);

        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;

        String prefix = unlocked
                ? ChatColor.GREEN + "\u2714 "
                : ChatColor.GRAY + "\u2718 ";
        meta.setDisplayName(prefix + def.name);

        List<String> lore = new ArrayList<>();

        // ==================== 状态行 ====================
        if (unlocked) {
            lore.add(ChatColor.GREEN + "状态: " + ChatColor.WHITE + "已解锁");
        } else {
            lore.add(ChatColor.GRAY + "状态: " + ChatColor.YELLOW + "未解锁");
        }
        lore.add("");

        // ==================== 击杀进度 ====================
        if (!unlocked) {
            // 进度条：10 格
            int filled = Math.min(10, (int) Math.round(kills * 10.0 / Math.max(1, req)));
            StringBuilder bar = new StringBuilder();
            for (int k = 0; k < 10; k++) {
                bar.append(k < filled ? ChatColor.GREEN + "\u2588" : ChatColor.DARK_GRAY + "\u2591");
            }
            double pct = kills * 100.0 / Math.max(1, req);
            lore.add(ChatColor.WHITE + "击杀进度: "
                    + ChatColor.YELLOW + kills + ChatColor.GRAY + "/" + ChatColor.YELLOW + req
                    + ChatColor.GRAY + " (" + String.format("%.1f%%", pct) + ")");
            lore.add(ChatColor.GRAY + "  " + bar.toString());
            lore.add("");
        }

        // ==================== 概率解锁 ====================
        if (def.unlockChance > 0) {
            String chanceColor = (def.unlockChance >= 0.1 ? ChatColor.GREEN
                    : def.unlockChance >= 0.02 ? ChatColor.YELLOW
                    : ChatColor.RED).toString();
            lore.add(ChatColor.WHITE + "概率解锁: " + chanceColor
                    + String.format("%.2f%%", def.unlockChance * 100)
                    + ChatColor.GRAY + "  (每次击杀)");
            if (def.unlockChance < 1.0) {
                long oneIn = Math.round(1.0 / Math.max(0.0001, def.unlockChance));
                lore.add(ChatColor.DARK_GRAY + "  约 " + oneIn + " 次击杀必中一次");
            }
            lore.add("");
        }

        // ==================== 描述 ====================
        if (!def.lore.isEmpty()) {
            lore.addAll(def.lore);
            lore.add("");
        }

        // ==================== 属性预览 ====================
        lore.add(ChatColor.WHITE + "属性加成:");
        if (def.attribute.isEmpty()) {
            lore.add(ChatColor.GRAY + "  （无）");
        } else {
            for (String a : def.attribute) {
                lore.add((unlocked ? ChatColor.GREEN : ChatColor.GRAY) + "  " + a);
            }
        }

        // ==================== 解锁奖励预览 ====================
        if (def.rewardPoints > 0 || def.rewardVault > 0
                || !def.rewardItems.isEmpty() || !def.rewardCommands.isEmpty()) {
            lore.add("");
            lore.add(ChatColor.WHITE + "解锁奖励:");
            if (def.rewardPoints > 0) lore.add(ChatColor.GOLD + "  点券: " + def.rewardPoints);
            if (def.rewardVault > 0) lore.add(ChatColor.GOLD + "  金币: " + (long) def.rewardVault);
            for (String s : def.rewardItems) lore.add(ChatColor.YELLOW + "  物品: " + s);
            for (String s : def.rewardCommands) lore.add(ChatColor.YELLOW + "  命令: " + s);
        }

        // ==================== 解锁方式 ====================
        if (!unlocked) {
            lore.add("");
            lore.add(ChatColor.YELLOW + "\u00A7l如何解锁:");
            lore.add(ChatColor.GRAY + "  \u00B7 击杀该怪物累计 " + req + " 次");
            if (def.unlockChance > 0)
                lore.add(ChatColor.GRAY + "  \u00B7 或每次击杀有 " + String.format("%.2f%%", def.unlockChance * 100) + " 概率");
            lore.add(ChatColor.GRAY + "  \u00B7 或用 " + ChatColor.WHITE + "圖鑑解鎖石(" + def.id + ")"
                    + ChatColor.GRAY + " 右键");
        }

        meta.setLore(lore);
        it.setItemMeta(meta);
        return it;
    }

    // ==================== 工具 ====================

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