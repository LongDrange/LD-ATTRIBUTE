package com.longdrange.ldattribute.core.soulring;

import com.longdrange.ldattribute.core.soulring.SoulRingConfig.CategoryDef;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class SoulRingFilter {

    /** 判断物品是否属于该分类 */
    public static boolean matches(ItemStack stack, CategoryDef def) {
        if (stack == null) return false;
        if (def == null) return true;
        if (!def.hasRules()) return true;   // 没有规则 = 全部匹配

        // 单独规则
        boolean blockMatched = !def.isBlock || safeIsBlock(stack);
        boolean edibleMatched = !def.isEdible || safeIsEdible(stack);
        boolean materialMatched = def.materialContains.isEmpty() || matchMaterial(stack, def.materialContains);
        boolean loreMatched = def.loreContains.isEmpty() || matchLore(stack, def.loreContains);
        boolean nameMatched = def.nameContains.isEmpty() || matchName(stack, def.nameContains);

        if (def.matchAll) {
            return blockMatched && edibleMatched && materialMatched && loreMatched && nameMatched;
        } else {
            // 任一满足（但 isBlock / isEdible 是硬性条件，要一起算）
            // 逻辑：isBlock/isEdible 若有配置，必须满足；material/lore/name 任一满足即可
            boolean hardOk = blockMatched && edibleMatched;
            if (!hardOk) return false;

            boolean anySoft = false;
            boolean hasSoft = false;
            if (!def.materialContains.isEmpty()) { hasSoft = true; if (materialMatched) anySoft = true; }
            if (!def.loreContains.isEmpty()) { hasSoft = true; if (loreMatched) anySoft = true; }
            if (!def.nameContains.isEmpty()) { hasSoft = true; if (nameMatched) anySoft = true; }

            if (!hasSoft) return true;   // 只有 isBlock/isEdible 规则，且已经满足
            return anySoft;
        }
    }

    private static boolean matchMaterial(ItemStack stack, List<String> keys) {
        String mat = stack.getType().name().toUpperCase();
        for (String k : keys) {
            if (k != null && !k.isEmpty() && mat.contains(k.toUpperCase())) return true;
        }
        return false;
    }

    private static boolean matchLore(ItemStack stack, List<String> keys) {
        if (!stack.hasItemMeta()) return false;
        ItemMeta meta = stack.getItemMeta();
        if (!meta.hasLore()) return false;
        for (String line : meta.getLore()) {
            String plain = ChatColor.stripColor(line);
            for (String k : keys) {
                if (k != null && !k.isEmpty() && plain.contains(k)) return true;
            }
        }
        return false;
    }

    private static boolean matchName(ItemStack stack, List<String> keys) {
        if (!stack.hasItemMeta()) return false;
        ItemMeta meta = stack.getItemMeta();
        if (!meta.hasDisplayName()) return false;
        String dn = ChatColor.stripColor(meta.getDisplayName());
        for (String k : keys) {
            if (k != null && !k.isEmpty() && dn.contains(k)) return true;
        }
        return false;
    }

    private static boolean safeIsBlock(ItemStack stack) {
        try { return stack.getType().isBlock(); } catch (Throwable t) { return false; }
    }
    private static boolean safeIsEdible(ItemStack stack) {
        try { return stack.getType().isEdible(); } catch (Throwable t) { return false; }
    }
}