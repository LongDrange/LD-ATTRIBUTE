package com.longdrange.ldattribute.core.drop;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.soulring.rate.RateManager;
import com.longdrange.ldattribute.util.ItemResolver;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 掉落管理器：按掉落表 + 爆率属性计算最终几率并发放
 */
public class DropTableManager {

    private final LDAttribute plugin;
    private final Map<String, Map<UUID, Integer>> dailyCounts = new ConcurrentHashMap<>();
    private static final SimpleDateFormat DAY_FMT = new SimpleDateFormat("yyyy-MM-dd");
    private String today = "";

    public DropTableManager(LDAttribute plugin) {
        this.plugin = plugin;
        today = DAY_FMT.format(new Date());
    }

    public void reload() {
        DropTableConfig.load(plugin);
        MMDropsCache.load(plugin);
        dailyCounts.clear();
        today = DAY_FMT.format(new Date());
    }

    /**
     * 处理一个 MM 怪物的掉落
     * @return 实际掉落的物品列表
     */
    public List<ItemStack> processDrops(Player killer, String mobId) {
        return processDrops(killer, mobId, null);
    }

    /** 完整版：支持多重掉落 */
    public List<ItemStack> processDrops(Player killer, String mobId, org.bukkit.entity.LivingEntity killedMob) {
        List<ItemStack> out = new ArrayList<>();
        if (killer == null || mobId == null) return out;

        String nowDay = DAY_FMT.format(new Date());
        if (!nowDay.equals(today)) { today = nowDay; dailyCounts.clear(); }

        if (!DropTableConfig.hasTable(mobId)) {
            return processMMDropsMulti(killer, mobId, killedMob);
        }
        List<DropTableConfig.Entry> entries = DropTableConfig.getDrops(mobId);
        if (entries.isEmpty()) return out;

        double dropRateAttr = RateManager.getDropRate(killer);

        for (DropTableConfig.Entry e : entries) {
            if (e.permission != null && !e.permission.isEmpty() && !killer.hasPermission(e.permission)) continue;
            if (e.dailyLimit > 0) {
                int used = getDailyCount(killer.getUniqueId(), mobId + ":" + e.id);
                if (used >= e.dailyLimit) continue;
            }

            double finalChance = e.chance;
            if (e.useDropRate && dropRateAttr > 0) finalChance = e.chance * (1.0 + dropRateAttr / 100.0);
            if (finalChance > 1.0) finalChance = 1.0;
            if (Math.random() >= finalChance) continue;

            int amount = e.amountMin;
            if (e.amountMax > e.amountMin) amount = e.amountMin + new Random().nextInt(e.amountMax - e.amountMin + 1);
            if (amount <= 0) continue;

            ItemResolver.Result r = ItemResolver.resolveEx(e.id, amount);
            if (r.item == null) {
                plugin.getLogger().warning("[Drop] 生成失败: " + e.id + " -> " + r.error);
                continue;
            }
            r.item.setAmount(amount);

            giveToTargets(e.target, killer, killedMob, r.item);
            out.add(r.item);

            if (e.dailyLimit > 0) incDailyCount(killer.getUniqueId(), mobId + ":" + e.id);
            if (e.broadcast) broadcast(killer, r.displayName != null ? r.displayName : e.id, amount);
        }
        return out;
    }

    /** 按 target 发放 */
    private void giveToTargets(String target, Player killer, org.bukkit.entity.LivingEntity killedMob, ItemStack item) {
        if (target == null || target.isEmpty()) target = "killer";
        List<Player> receivers = new ArrayList<>();
        org.bukkit.Location loc = (killedMob != null) ? killedMob.getLocation() : killer.getLocation();

        if (target.startsWith("nearby:")) {
            double radius = 8;
            try { radius = Double.parseDouble(target.substring(7).trim()); } catch (Throwable ignored) {}
            if (loc != null && loc.getWorld() != null) {
                for (org.bukkit.entity.Entity ent : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                    if (ent instanceof Player) receivers.add((Player) ent);
                }
            }
        } else if (target.equals("world")) {
            if (loc != null && loc.getWorld() != null) receivers.addAll(loc.getWorld().getPlayers());
        } else if (target.equals("server")) {
            receivers.addAll(Bukkit.getOnlinePlayers());
        } else {
            receivers.add(killer);
        }
        if (receivers.isEmpty()) receivers.add(killer);

        for (Player p : receivers) {
            if (p == null || !p.isOnline()) continue;
            HashMap<Integer, ItemStack> left = p.getInventory().addItem(item.clone());
            for (ItemStack drop : left.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), drop);
            }
            if (!p.equals(killer)) {
                p.sendMessage(ChatColor.GRAY + "[" + ChatColor.LIGHT_PURPLE + "掉落" + ChatColor.GRAY + "] " +
                        ChatColor.WHITE + killer.getName() + ChatColor.GRAY + " 击杀掉落分享" +
                        ChatColor.GREEN + " x" + item.getAmount());
            }
        }
    }

    /** MM 掉落 + 多重目标 */
    private List<ItemStack> processMMDropsMulti(Player killer, String mobId, org.bukkit.entity.LivingEntity killedMob) {
        List<ItemStack> out = new ArrayList<>();
        List<MMDropsCache.Entry> entries = MMDropsCache.get(mobId);
        if (entries.isEmpty()) return out;

        double dropRateAttr = RateManager.getDropRate(killer);

        for (MMDropsCache.Entry e : entries) {
            if (e == null) continue;
            if (e.isExp) { try { killer.giveExp(e.amountMin); } catch (Throwable ignored) {} continue; }
            if (e.isSpecial) continue;

            double finalChance = e.chance;
            if (dropRateAttr > 0) finalChance = e.chance * (1.0 + dropRateAttr / 100.0);
            if (finalChance > 1.0) finalChance = 1.0;
            if (Math.random() >= finalChance) continue;

            int amount = e.amountMin;
            if (e.amountMax > e.amountMin) amount = e.amountMin + new Random().nextInt(e.amountMax - e.amountMin + 1);
            if (amount <= 0) continue;

            ItemStack it = parseMMItem(e.item, amount);
            if (it != null) {
                giveToTargets("killer", killer, killedMob, it);
                out.add(it);
            }
        }
        return out;
    }

    private void broadcast(Player killer, String itemName, int amount) {
        String msg = ChatColor.GOLD + "✦ " + ChatColor.YELLOW + killer.getName()
                + ChatColor.GRAY + " 擊殺了 " + ChatColor.RED + "怪物" + ChatColor.GRAY + " 獲得了 "
                + ChatColor.AQUA + itemName + ChatColor.GRAY + " x" + amount;
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(msg);
        Bukkit.broadcastMessage("");
    }

    private int getDailyCount(UUID uuid, String key) {
        return dailyCounts.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                .getOrDefault(uuid, 0);
    }

    private void incDailyCount(UUID uuid, String key) {
        dailyCounts.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                .merge(uuid, 1, Integer::sum);
    }

    /** 用 MM 掉落表生成（爆率加成）*/
    private List<ItemStack> processMMDrops(Player killer, String mobId) {
        List<ItemStack> out = new ArrayList<>();
        List<MMDropsCache.Entry> entries = MMDropsCache.get(mobId);
        if (entries.isEmpty()) return out;

        double dropRateAttr = RateManager.getDropRate(killer);

        for (MMDropsCache.Entry e : entries) {
            if (e == null) continue;
            // 经验：直接给
            if (e.isExp) {
                try { killer.giveExp(e.amountMin); } catch (Throwable ignored) {}
                continue;
            }
            // 未展开的引用：跳过
            if (e.isSpecial) continue;

            // 最终几率
            double finalChance = e.chance;
            if (dropRateAttr > 0) {
                finalChance = e.chance * (1.0 + dropRateAttr / 100.0);
            }
            if (finalChance > 1.0) finalChance = 1.0;

            // 掷骰
            if (Math.random() >= finalChance) continue;

            // 数量
            int amount = e.amountMin;
            if (e.amountMax > e.amountMin) {
                amount = e.amountMin + new Random().nextInt(e.amountMax - e.amountMin + 1);
            }
            if (amount <= 0) continue;

            ItemStack it = parseMMItem(e.item, amount);
            if (it != null) out.add(it);
        }
        return out;
    }

    /** 解析 MM 物品字符串：数字ID / 名:数据 / 材质名 / RING: 等 */
    private ItemStack parseMMItem(String item, int amount) {
        // 1.12 材质兼容：新版名 -> 旧版名
        if (item != null) {
            String compat = item.toUpperCase();
            if (compat.equals("EXPERIENCE_BOTTLE") || compat.equals("EXPERIENCE_BOTTLE:1")) {
                item = item.replace("EXPERIENCE_BOTTLE", "EXP_BOTTLE");
            }
        }
        if (item == null || item.isEmpty()) return null;
        // 特殊前缀（RING/CARD/RUNE）
        if (ItemResolver.hasSpecialPrefix(item)) {
            ItemResolver.Result r = ItemResolver.resolveEx(item + ":" + amount, amount);
            if (r.item != null) r.item.setAmount(amount);
            return r.item;
        }
        // 数字ID:数据（如 371:0）
        if (item.contains(":")) {
            try {
                String[] p = item.split(":");
                int id = Integer.parseInt(p[0].trim());
                short data = p.length >= 2 ? Short.parseShort(p[1].trim()) : 0;
                org.bukkit.Material mat = org.bukkit.Material.getMaterial(id);
                if (mat != null) return new ItemStack(mat, amount, data);
            } catch (Throwable ignored) {}
        }
        // 纯数字ID
        try {
            int id = Integer.parseInt(item);
            org.bukkit.Material mat = org.bukkit.Material.getMaterial(id);
            if (mat != null) return new ItemStack(mat, amount);
        } catch (Throwable ignored) {}
        // 材质名
        org.bukkit.Material mat = org.bukkit.Material.getMaterial(item.toUpperCase());
        if (mat != null) return new ItemStack(mat, amount);
        return null;
    }
}
