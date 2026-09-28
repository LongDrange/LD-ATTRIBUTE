package com.longdrange.ldattribute.core.relic;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * 遗物物品生成
 */
public class RelicItem {

    /** 生成一个随机遗物（主属性随机 + 4 条副属性随机） */
    public static RelicData.RelicInstance roll(String relicId) {
        RelicConfig.RelicDef def = RelicConfig.getRelic(relicId);
        if (def == null) return null;
        RelicData.RelicInstance ins = new RelicData.RelicInstance(relicId);
        // 主属性
        ins.mainValue = rand(def.mainMin, def.mainMax);

        // ① 固定副属性（优先级最高）
        if (def.subFixed != null && !def.subFixed.isEmpty()) {
            for (String line : def.subFixed) {
                try {
                    String[] p = line.split(":");
                    if (p.length < 2) continue;
                    String attr = p[0].trim();
                    String valStr = p[1].replace("+", "").trim();
                    double v;
                    if (valStr.contains("~")) {
                        String[] rng = valStr.split("~");
                        v = rand((int) Double.parseDouble(rng[0].trim()), (int) Double.parseDouble(rng[1].trim()));
                    } else {
                        v = Double.parseDouble(valStr);
                    }
                    ins.subs.add(new RelicData.Sub(attr, v));
                } catch (Throwable ignored) {}
            }
            return ins;
        }

        // ② 随机副属性
        List<RelicConfig.SubEntry> pool;
        if (def.subPool != null && !def.subPool.isEmpty()) {
            pool = new ArrayList<>(def.subPool);
        } else {
            pool = new ArrayList<>(RelicConfig.getSubPool());
        }
        Collections.shuffle(pool);
        int n = Math.max(0, Math.min(def.subCount, pool.size()));
        Set<String> used = new HashSet<>();
        for (int i = 0; i < pool.size() && ins.subs.size() < n; i++) {
            RelicConfig.SubEntry se = pool.get(i);
            if (used.contains(se.attr)) continue;
            used.add(se.attr);
            double v = rand(se.min, se.max);
            ins.subs.add(new RelicData.Sub(se.attr, v));
        }
        return ins;
    }

    private static int rand(int min, int max) {
        if (max <= min) return min;
        return min + new Random().nextInt(max - min + 1);
    }

    /** 把实例转成可展示的物品（用于 GUI 或 /ldrelic give） */
    public static ItemStack toItem(RelicData.RelicInstance ins) {
        if (ins == null) return null;
        RelicConfig.RelicDef def = RelicConfig.getRelic(ins.relicId);
        if (def == null) return null;
        Material mat = Material.getMaterial(def.icon.toUpperCase());
        if (mat == null) mat = Material.NETHER_STAR;
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;

        meta.setDisplayName(def.name);

        RelicConfig.SlotDef slot = RelicConfig.getSlot(def.slot);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "遗物 · " + (slot != null ? ChatColor.stripColor(slot.name) : def.slot));
        if (def.set != null && !def.set.isEmpty()) {
            RelicConfig.SetDef set = RelicConfig.getSet(def.set);
            if (set != null) lore.add(ChatColor.DARK_GRAY + "套装: " + set.name);
        }
        lore.add("");
        // 主属性
        String mainAttr = (slot != null ? slot.mainAttr : "?");
        lore.add(ChatColor.DARK_GRAY + "§m----------------");
        lore.add(ChatColor.YELLOW + "【主属性】");
        lore.add(ChatColor.GRAY + "  " + mainAttr + ": " + ChatColor.WHITE + "+" + fmt(ins.mainValue));
        lore.add("");
        // 副属性
        lore.add(ChatColor.DARK_GRAY + "§m----------------");
        lore.add(ChatColor.AQUA + "【副属性】");
        for (RelicData.Sub s : ins.subs) {
            lore.add(ChatColor.GRAY + "  " + s.attr + ": " + ChatColor.WHITE + "+" + fmt(s.value));
        }
        lore.add("");
        if (ins.level > 0) lore.add(ChatColor.GOLD + "强化等级: +" + ins.level);
        lore.add(ChatColor.DARK_GRAY + "id: " + ins.relicId);

        meta.setLore(lore);
        it.setItemMeta(meta);
        return it;
    }

    private static String fmt(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }
}
