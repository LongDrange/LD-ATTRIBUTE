package com.longdrange.ldattribute.core.relic.gui;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.relic.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 遗物 GUI - 动态槽位（数量/位置全可配置）
 */
public class RelicGUI {

    public static final String TITLE = ChatColor.DARK_GRAY + "✦ " + ChatColor.GOLD + "遗物系统";
    public static final int SIZE = 54;

    private final LDAttribute plugin;
    public RelicGUI(LDAttribute plugin) { this.plugin = plugin; }

    /** 动态计算每个槽位的格子位置 */
    public static Map<String, Integer> computeLayout() {
        List<RelicConfig.SlotDef> slots = RelicConfig.getOrderedSlots();
        Map<String, Integer> map = new LinkedHashMap<>();
        int auto = 0;
        int[] autoSlots = autoGridPositions(slots.size());
        for (RelicConfig.SlotDef s : slots) {
            int pos = s.guiSlot;
            if (pos < 0 || pos >= SIZE) {
                pos = (auto < autoSlots.length) ? autoSlots[auto] : (auto + 9);
                auto++;
            }
            map.put(s.id, pos);
        }
        return map;
    }

    private static int[] autoGridPositions(int n) {
        if (n <= 0) return new int[0];
        int cols = Math.min(7, n);
        int rows = (int) Math.ceil(n * 1.0 / cols);
        int startRow = (4 - rows) / 2;
        if (startRow < 1) startRow = 1;
        int startCol = (9 - cols) / 2;
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            int r = startRow + (i / cols);
            int c = startCol + (i % cols);
            out[i] = r * 9 + c;
        }
        return out;
    }

    public void open(Player player) {
        RelicData data = plugin.getCoreManager().getRelicManager().get(player);
        if (data == null) return;

        Inventory inv = Bukkit.createInventory(new RelicHolder(player.getUniqueId()), SIZE, TITLE);

        ItemStack border = glass((short) 7, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);


        // ===== 套装进度（底部一排）=====
        Map<String, Integer> setCount = countSets(data);
        int setSlot = 46;
        for (Map.Entry<String, Integer> se : setCount.entrySet()) {
            if (setSlot > 52) break;
            RelicConfig.SetDef set = RelicConfig.getSet(se.getKey());
            if (set == null) continue;
            inv.setItem(setSlot++, buildSetIcon(set, se.getValue()));
        }
        Map<String, Integer> layout = computeLayout();
        for (RelicConfig.SlotDef slot : RelicConfig.getOrderedSlots()) {
            Integer pos = layout.get(slot.id);
            if (pos == null || pos < 0 || pos >= SIZE) continue;
            RelicData.RelicInstance ins = data.get(slot.id);
            inv.setItem(pos, buildSlotIcon(slot, ins));
        }

        player.openInventory(inv);
    }

    /** 统计各套装件数 */
    private Map<String, Integer> countSets(RelicData data) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (RelicData.RelicInstance ins : data.getAll().values()) {
            RelicConfig.RelicDef def = RelicConfig.getRelic(ins.relicId);
            if (def == null || def.set == null || def.set.isEmpty()) continue;
            out.merge(def.set, 1, Integer::sum);
        }
        return out;
    }

    /** 套装图标 */
    private ItemStack buildSetIcon(RelicConfig.SetDef set, int count) {
        ItemStack it = new ItemStack(Material.BOOK);
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;
        meta.setDisplayName(set.name);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "已装备: " + ChatColor.WHITE + count + " 件");
        lore.add("");
        for (Map.Entry<Integer, List<String>> e : set.bonuses.entrySet()) {
            int need = e.getKey();
            boolean active = count >= need;
            lore.add((active ? ChatColor.GREEN + "✔ " : ChatColor.DARK_GRAY + "✘ ")
                    + ChatColor.GRAY + need + "件效果:");
            for (String line : e.getValue()) {
                lore.add((active ? ChatColor.GREEN : ChatColor.DARK_GRAY) + "  " + line);
            }
        }
        meta.setLore(lore);
        it.setItemMeta(meta);
        return it;
    }

    private ItemStack buildSlotIcon(RelicConfig.SlotDef slot, RelicData.RelicInstance ins) {
        Material mat;
        if (ins == null) {
            mat = Material.getMaterial(slot.icon.toUpperCase());
            if (mat == null) mat = Material.STAINED_GLASS_PANE;
        } else {
            RelicConfig.RelicDef def = RelicConfig.getRelic(ins.relicId);
            mat = Material.getMaterial(def != null ? def.icon.toUpperCase() : slot.icon.toUpperCase());
            if (mat == null) mat = Material.NETHER_STAR;
        }
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;

        if (ins == null) {
            meta.setDisplayName(ChatColor.GRAY + slot.name + ChatColor.DARK_GRAY + "（空）");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + "主属性: " + ChatColor.stripColor(slot.mainAttr));
            lore.add("");
            lore.add(ChatColor.YELLOW + "点击: 装备手上的遗物");
            meta.setLore(lore);
        } else {
            RelicConfig.RelicDef def = RelicConfig.getRelic(ins.relicId);
            String dn = (def != null ? def.name : ins.relicId); if (ins.locked) dn = "§c🔒 " + dn; meta.setDisplayName(dn);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + "槽位: " + slot.name);
            lore.add("");
            lore.add(ChatColor.YELLOW + "【主属性】");
            lore.add(ChatColor.GRAY + "  " + slot.mainAttr + ": " + ChatColor.WHITE + "+" + fmt(ins.mainValue));
            lore.add("");
            lore.add(ChatColor.AQUA + "【副属性】");
            for (RelicData.Sub s : ins.subs) {
                lore.add(ChatColor.GRAY + "  " + s.attr + ": " + ChatColor.WHITE + "+" + fmt(s.value));
            }
            if (ins.level > 0) {
                lore.add("");
                lore.add(ChatColor.GOLD + "强化等级: +" + ins.level);
            }
            lore.add("");
            // 锁定状态显示
            if (ins.locked) {
                lore.add("");
                lore.add(ChatColor.RED + "🔒 已锁定");
                lore.add(ChatColor.GRAY + "  右键: 解锁");
            } else {
                lore.add(ChatColor.RED + "Shift+左键: 卸下");
                lore.add(ChatColor.GRAY + "  右键: 锁定");
            }
            meta.setLore(lore);
        }
        it.setItemMeta(meta);
        return it;
    }

    private static String fmt(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }

    private ItemStack glass(short data, String name) {
        ItemStack it = new ItemStack(Material.STAINED_GLASS_PANE, 1, data);
        ItemMeta m = it.getItemMeta();
        if (m != null) { m.setDisplayName(name); it.setItemMeta(m); }
        return it;
    }
}
