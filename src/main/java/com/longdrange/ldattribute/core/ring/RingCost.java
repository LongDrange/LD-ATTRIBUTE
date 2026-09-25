package com.longdrange.ldattribute.core.ring;

import com.longdrange.ldattribute.points.PointAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用成本支付
 * 物品格式: MATERIAL:COUNT 或 MATERIAL:COUNT:DATA
 * 例: DIAMOND:64 / WOOL:10:14 / PAPER
 */
public class RingCost {

    public static class ItemReq {
        public final Material material;
        public final int count;
        public final short data;
        public final String raw;
        public ItemReq(Material material, int count, short data, String raw) {
            this.material = material; this.count = count; this.data = data; this.raw = raw;
        }
    }

    public static ItemReq parseItem(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String s = raw.trim();
        String[] parts = s.split(":");
        if (parts.length == 0) return null;
        Material m = Material.getMaterial(parts[0].toUpperCase());
        if (m == null) return null;
        int count = 1;
        if (parts.length >= 2) {
            try { count = Math.max(1, Integer.parseInt(parts[1].trim())); } catch (Exception ignored) {}
        }
        short data = 0;
        if (parts.length >= 3) {
            try { data = (short) Integer.parseInt(parts[2].trim()); } catch (Exception ignored) {}
        }
        return new ItemReq(m, count, data, raw);
    }

    public static List<ItemReq> parseItems(List<String> list) {
        List<ItemReq> out = new ArrayList<>();
        if (list == null) return out;
        for (String s : list) {
            ItemReq r = parseItem(s);
            if (r != null) out.add(r);
        }
        return out;
    }

    // ==================== 查询 ====================

    public static int getPoints(Player p) {
        try { return PointAPI.getPlayerPoints(p.getName()); }
        catch (Throwable t) { return -1; }
    }

    public static Economy getEconomy() {
        try {
            if (Bukkit.getPluginManager().getPlugin("Vault") == null) return null;
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            return rsp == null ? null : rsp.getProvider();
        } catch (Throwable t) { return null; }
    }

    public static int countItem(Player p, ItemReq req) {
        int total = 0;
        for (ItemStack it : p.getInventory().getContents()) {
            if (it == null) continue;
            if (it.getType() != req.material) continue;
            if (req.data > 0 && it.getDurability() != req.data) continue;
            total += it.getAmount();
        }
        return total;
    }

    /**
     * 检查是否满足所有条件
     * @return null = 满足；否则返回错误提示
     */
    public static String check(Player p, String permission, int points, double vault, List<ItemReq> items) {
        if (permission != null && !permission.isEmpty() && !p.hasPermission(permission)) {
            return "缺少权限: " + permission;
        }
        if (points > 0) {
            int have = getPoints(p);
            if (have < 0) return "无法读取点券（服务器未装点券系统？）";
            if (have < points) return "点券不足（需 " + points + "，有 " + have + "）";
        }
        if (vault > 0) {
            Economy eco = getEconomy();
            if (eco == null) return "服务器未启用 Vault 经济";
            if (eco.getBalance(p) < vault)
                return "金币不足（需 " + (long) vault + "，有 " + (long) eco.getBalance(p) + "）";
        }
        if (items != null) {
            for (ItemReq r : items) {
                int have = countItem(p, r);
                if (have < r.count)
                    return "缺少 " + nameOf(r) + "（需 " + r.count + "，有 " + have + "）";
            }
        }
        return null;
    }

    /** 执行扣款（必须先 check 通过） */
    public static void pay(Player p, String permission, int points, double vault, List<ItemReq> items) {
        if (points > 0) {
            try { PointAPI.takePlayerPoints(p.getName(), points); } catch (Throwable ignored) {}
        }
        if (vault > 0) {
            Economy eco = getEconomy();
            if (eco != null) eco.withdrawPlayer(p, vault);
        }
        if (items != null) {
            for (ItemReq r : items) removeItem(p, r);
        }
    }

    private static void removeItem(Player p, ItemReq req) {
        int left = req.count;
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length && left > 0; i++) {
            ItemStack it = contents[i];
            if (it == null) continue;
            if (it.getType() != req.material) continue;
            if (req.data > 0 && it.getDurability() != req.data) continue;
            int take = Math.min(left, it.getAmount());
            left -= take;
            if (take >= it.getAmount()) {
                p.getInventory().setItem(i, null);
            } else {
                it.setAmount(it.getAmount() - take);
                p.getInventory().setItem(i, it);
            }
        }
    }

    public static String nameOf(ItemReq r) {
        return r.material.name() + (r.data > 0 ? ":" + r.data : "");
    }

    /** 生成 lore 描述 */
    public static List<String> describe(String permission, int points, double vault, List<String> rawItems) {
        List<String> out = new ArrayList<>();
        if (permission != null && !permission.isEmpty())
            out.add(ChatColor.YELLOW + "  权限: " + permission);
        if (points > 0)
            out.add(ChatColor.GOLD + "  点券: " + points);
        if (vault > 0)
            out.add(ChatColor.GOLD + "  金币: " + (long) vault);
        if (rawItems != null && !rawItems.isEmpty()) {
            out.add(ChatColor.YELLOW + "  物品:");
            for (String s : rawItems) {
                ItemReq r = parseItem(s);
                if (r != null) out.add(ChatColor.GRAY + "    " + nameOf(r) + " x" + r.count);
                else out.add(ChatColor.RED + "    " + s + " (格式错误)");
            }
        }
        if (out.isEmpty()) out.add(ChatColor.GRAY + "  免费解锁");
        return out;
    }
}