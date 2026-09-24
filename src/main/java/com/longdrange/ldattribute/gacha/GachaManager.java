package com.longdrange.ldattribute.gacha;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.card.CardData;
import com.longdrange.ldattribute.card.CardDataManager;
import com.longdrange.ldattribute.points.PointAPI;
import com.longdrange.ldattribute.rune.RuneConfig;
import com.longdrange.ldattribute.rune.RuneItem;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class GachaManager {

    public static class Result {
        public boolean success;
        public String message;
        public ItemStack item;
        public String displayName;
    }

    public static Result draw(Player player, GachaConfig.Gacha g) {
        Result r = new Result();
        if (g == null) { r.message = "§c卡池不存在"; return r; }

        // 检查点券
        int points = PointAPI.getPlayerPoints(player.getName());
        if (g.costPoints > 0 && points < g.costPoints) {
            r.message = "§c点券不足！需要 §6" + g.costPoints + " §c你有 §6" + points;
            return r;
        }
        // 检查物品
        for (String spec : g.costItems) {
            if (!hasItem(player, spec)) {
                r.message = "§c缺少物品: §e" + spec;
                return r;
            }
        }

        // 保底判定
        int pity = GachaData.getPityCount(player.getUniqueId(), g.id);
        boolean forceGuarantee = false;
        GachaConfig.PoolEntry picked = null;

        if (g.pityEnabled && pity + 1 >= g.pityCount) {
            // 触发保底
            picked = findPoolEntry(g, g.pityGuarantee);
            forceGuarantee = (picked != null);
        }
        if (picked == null) {
            picked = roll(g);
        }
        if (picked == null) {
            r.message = "§c卡池为空";
            return r;
        }

        // 扣费
        if (g.costPoints > 0) PointAPI.takePlayerPoints(player.getName(), g.costPoints);
        for (String spec : g.costItems) takeItem(player, spec);

        // 生成物品
        ItemStack item = buildItem(picked);
        if (item == null) {
            r.message = "§c奖励生成失败: " + picked.raw;
            return r;
        }
        // 给予
        HashMap<Integer, ItemStack> left = player.getInventory().addItem(item);
        for (ItemStack drop : left.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        // 保底更新
        if (forceGuarantee) {
            GachaData.resetPity(player.getUniqueId(), g.id);
            player.sendMessage("§6✦ §l保底触发！§r §7(第 " + g.pityCount + " 次)");
        } else {
            GachaData.incrementPity(player.getUniqueId(), g.id);
        }

        r.success = true;
        r.item = item;
        r.displayName = item.getItemMeta() != null ? item.getItemMeta().getDisplayName() : item.getType().name();
        r.message = "§a✦ 抽到 §e" + r.displayName;
        try { player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f); } catch (Throwable ignored) {}
        return r;
    }

    private static GachaConfig.PoolEntry roll(GachaConfig.Gacha g) {
        if (g.totalWeight <= 0) return null;
        int roll = new Random().nextInt(g.totalWeight);
        int cur = 0;
        for (GachaConfig.PoolEntry pe : g.pool) {
            cur += pe.weight;
            if (roll < cur) return pe;
        }
        return g.pool.isEmpty() ? null : g.pool.get(0);
    }

    private static GachaConfig.PoolEntry findPoolEntry(GachaConfig.Gacha g, String raw) {
        if (raw == null || raw.isEmpty()) return null;
        // 匹配 "卡片ID" 或 "rune:符文ID"
        for (GachaConfig.PoolEntry pe : g.pool) {
            if (pe.kind.equals("rune")) {
                if (raw.equalsIgnoreCase("rune:" + pe.id) || raw.equalsIgnoreCase(pe.id)) return pe;
            } else {
                if (raw.equalsIgnoreCase(pe.id)) return pe;
            }
        }
        // 不在池中 → 创建一个临时 entry
        String[] parts = raw.split(":");
        GachaConfig.PoolEntry pe = new GachaConfig.PoolEntry();
        pe.raw = raw;
        if (parts[0].equalsIgnoreCase("rune") && parts.length >= 2) {
            pe.kind = "rune";
            pe.id = parts[1];
        } else {
            pe.kind = "card";
            pe.id = parts[0];
        }
        pe.weight = 1;
        return pe;
    }

    private static ItemStack buildItem(GachaConfig.PoolEntry pe) {
        if ("rune".equals(pe.kind)) {
            RuneConfig.Rune r = RuneConfig.getRune(pe.id);
            if (r == null) return null;
            return RuneItem.create(r, 1);
        } else {
            CardData cd = CardDataManager.getCard(pe.id);
            if (cd == null) return null;
            return cd.getItem().clone();
        }
    }

    private static boolean hasItem(Player p, String spec) {
        String[] parts = spec.split(":");
        Material mat = Material.getMaterial(parts[0].toUpperCase());
        if (mat == null) return false;
        int need = parts.length >= 2 ? Integer.parseInt(parts[1]) : 1;
        int have = 0;
        for (ItemStack it : p.getInventory().getContents()) {
            if (it != null && it.getType() == mat) have += it.getAmount();
        }
        return have >= need;
    }

    private static void takeItem(Player p, String spec) {
        String[] parts = spec.split(":");
        Material mat = Material.getMaterial(parts[0].toUpperCase());
        if (mat == null) return;
        int need = parts.length >= 2 ? Integer.parseInt(parts[1]) : 1;
        int removed = 0;
        for (int i = 0; i < p.getInventory().getSize() && removed < need; i++) {
            ItemStack it = p.getInventory().getItem(i);
            if (it == null || it.getType() != mat) continue;
            int take = Math.min(it.getAmount(), need - removed);
            it.setAmount(it.getAmount() - take);
            removed += take;
            if (it.getAmount() <= 0) p.getInventory().setItem(i, null);
        }
    }
}