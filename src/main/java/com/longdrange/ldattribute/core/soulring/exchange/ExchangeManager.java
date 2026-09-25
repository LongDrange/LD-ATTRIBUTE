package com.longdrange.ldattribute.core.soulring.exchange;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.soulring.SoulRingData;
import com.longdrange.ldattribute.points.PointAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;

public class ExchangeManager {

    public static class Result {
        public final boolean success;
        public final int times;
        public final List<ItemStack> given;
        public final String message;
        public Result(boolean success, int times, List<ItemStack> given, String message) {
            this.success = success; this.times = times; this.given = given; this.message = message;
        }
    }

    public static Result exchange(LDAttribute plugin, Player player,
                                  ExchangeConfig.Exchange ex, int times) {
        if (player == null || ex == null) return fail("参数错误");
        if (!ex.permission.isEmpty() && !player.hasPermission(ex.permission)) return fail("你没有权限兑换");

        ExchangeData ed = new ExchangeData(plugin, player.getUniqueId());
        if (!ed.canExchange(ex)) return fail("已达兑换上限");
        int remainLimit = ed.remainTimes(ex);

        if (countEmptySlots(player) < 2) return fail("背包至少需要 2 格空位");

        int maxByStock = calcMaxTimes(plugin, player, ex);
        if (maxByStock <= 0) return fail("材料不足");

        int finalTimes = times <= 0 ? maxByStock : Math.min(times, maxByStock);
        if (remainLimit > 0) finalTimes = Math.min(finalTimes, remainLimit);
        if (finalTimes <= 0) return fail("无法兑换");

        // 扣 Input (AND)
        for (ExchangeConfig.ItemLine req : ex.input) {
            long need = (long) req.amount * finalTimes;
            if (!consume(plugin, player, req, need, ex)) return fail("扣除失败（内部错误）");
        }
        // 扣 AnyOf (OR)
        if (!ex.anyOf.isEmpty()) {
            int left = finalTimes;
            for (ExchangeConfig.ItemLine req : ex.anyOf) {
                if (left <= 0) break;
                int canByThis = calcOne(plugin, player, req, ex);
                if (canByThis <= 0) continue;
                int use = Math.min(left, canByThis);
                long need = (long) req.amount * use;
                if (!consume(plugin, player, req, need, ex)) continue;
                left -= use;
            }
            if (left > 0) return fail("扣除失败（AnyOf 材料不足）");
        }

        // 产出
        List<ItemStack> given = new ArrayList<>();
        Random rnd = new Random();
        int actualTimes = 0;
        for (int i = 0; i < finalTimes; i++) {
            if (ex.chance < 100.0 && rnd.nextDouble() * 100.0 > ex.chance) continue;
            actualTimes++;
            for (ExchangeConfig.ItemLine out : ex.output) {
                giveOutput(plugin, player, out, given);
            }
        }

        ed.addCount(ex.id, finalTimes);
        String msg = "兑换 " + finalTimes + " 次";
        if (ex.chance < 100.0) msg += "（成功 " + actualTimes + " 次）";
        return new Result(true, finalTimes, given, msg);
    }

    private static Result fail(String msg) {
        return new Result(false, 0, Collections.emptyList(), msg);
    }

    public static int calcMaxTimes(LDAttribute plugin, Player player, ExchangeConfig.Exchange ex) {
        int max = Integer.MAX_VALUE;
        for (ExchangeConfig.ItemLine req : ex.input) {
            int can = calcOne(plugin, player, req, ex);
            if (can < max) max = can;
            if (max <= 0) return 0;
        }
        if (!ex.anyOf.isEmpty()) {
            int totalUnits = 0;
            for (ExchangeConfig.ItemLine req : ex.anyOf) {
                totalUnits += Math.max(0, calcOne(plugin, player, req, ex));
            }
            if (totalUnits < max) max = totalUnits;
            if (max <= 0) return 0;
        }
        return max == Integer.MAX_VALUE ? 0 : max;
    }

    private static int calcOne(LDAttribute plugin, Player player,
                               ExchangeConfig.ItemLine req, ExchangeConfig.Exchange ex) {
        switch (req.source) {
            case SOULRING: return (int) (countInSoulRing(plugin, player, req, ex) / Math.max(1, req.amount));
            case INVENTORY: return (int) (countInInventory(player, req, ex) / Math.max(1, req.amount));
            case BOTH: {
                long have = countInSoulRing(plugin, player, req, ex) + countInInventory(player, req, ex);
                return (int) (have / Math.max(1, req.amount));
            }
            case VAULT: {
                Economy eco = getEconomy();
                if (eco == null) return 0;
                return (int) (eco.getBalance(player) / Math.max(1, req.amount));
            }
            case POINT: return queryPoints(player) / Math.max(1, req.amount);
            case VALUE: return (int) (queryValue(player, req.valueId) / Math.max(1, req.amount));
        }
        return 0;
    }

    private static boolean consume(LDAttribute plugin, Player player,
                                   ExchangeConfig.ItemLine req, long need, ExchangeConfig.Exchange ex) {
        switch (req.source) {
            case SOULRING: {
                SoulRingData data = plugin.getSoulRingManager().get(player);
                return takeFromSoulRing(data, req, need, ex) >= need;
            }
            case INVENTORY: return takeFromInventory(player, req, need, ex) >= need;
            case BOTH: {
                // ★ 先扣灵魂空间，不够再扣背包
                SoulRingData data = plugin.getSoulRingManager().get(player);
                long fromSoul = takeFromSoulRing(data, req, need, ex);
                long left = need - fromSoul;
                if (left <= 0) return true;
                long fromInv = takeFromInventory(player, req, left, ex);
                return (fromSoul + fromInv) >= need;
            }
            case VAULT: {
                Economy eco = getEconomy();
                if (eco == null || eco.getBalance(player) < need) return false;
                eco.withdrawPlayer(player, need);
                return true;
            }
            case POINT: {
                if (queryPoints(player) < need) return false;
                return takePoints(player, (int) need);
            }
            case VALUE: {
                if (queryValue(player, req.valueId) < need) return false;
                takeValue(player, req.valueId, need);
                return true;
            }
        }
        return false;
    }

    private static void giveOutput(LDAttribute plugin, Player player,
                                   ExchangeConfig.ItemLine out, List<ItemStack> given) {
        switch (out.source) {
            case VAULT: {
                Economy eco = getEconomy();
                if (eco != null) eco.depositPlayer(player, out.amount);
                return;
            }
            case POINT: { addPoints(player, out.amount); return; }
            case VALUE: {
                if (out.valueId != null && out.valueId.startsWith("CMD:")) {
                    String cmd = out.valueId.substring(4).replace("%player%", player.getName());
                    final String finalCmd = cmd;
                    Bukkit.getScheduler().runTask(plugin, () ->
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd));
                } else {
                    addValue(player, out.valueId, out.amount);
                }
                return;
            }
            default: {
                if (out.material != null) {
                    ItemStack item = new ItemStack(out.material, out.amount, out.data);
                    ItemMeta meta = item.getItemMeta();
                    boolean changed = false;
                    if (meta != null) {
                        if (out.nameHint != null && !out.nameHint.isEmpty()) {
                            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', out.nameHint));
                            changed = true;
                        }
                        if (!out.lore.isEmpty()) {
                            meta.setLore(new ArrayList<>(out.lore));
                            changed = true;
                        }
                        if (changed) item.setItemMeta(meta);
                    }
                    player.getInventory().addItem(item);
                    given.add(item.clone());
                }
            }
        }
    }

    public static long countInSoulRing(LDAttribute plugin, Player player,
                                       ExchangeConfig.ItemLine req, ExchangeConfig.Exchange ex) {
        return countInSoulRing(plugin.getSoulRingManager().get(player), req, ex);
    }

    public static long countInSoulRing(SoulRingData data, ExchangeConfig.ItemLine req, ExchangeConfig.Exchange ex) {
        long count = 0;
        for (SoulRingData.Entry e : data.getEntries()) {
            if (e == null || e.template == null) continue;
            if (!matches(e.template, req, ex)) continue;
            count += e.count;
        }
        return count;
    }

    public static long takeFromSoulRing(SoulRingData data, ExchangeConfig.ItemLine req,
                                        long amount, ExchangeConfig.Exchange ex) {
        long left = amount;
        List<SoulRingData.Entry> copy = new ArrayList<>(data.getEntries());
        for (SoulRingData.Entry e : copy) {
            if (left <= 0) break;
            if (e == null || e.template == null) continue;
            if (!matches(e.template, req, ex)) continue;
            long take = Math.min(left, e.count);
            data.takeByEntry(e, take);
            left -= take;
        }
        return amount - left;
    }

    public static long countInInventory(Player player, ExchangeConfig.ItemLine req, ExchangeConfig.Exchange ex) {
        long count = 0;
        for (ItemStack s : player.getInventory().getStorageContents()) {
            if (s == null || s.getType() == Material.AIR) continue;
            if (!matches(s, req, ex)) continue;
            count += s.getAmount();
        }
        return count;
    }

    public static long takeFromInventory(Player player, ExchangeConfig.ItemLine req,
                                          long amount, ExchangeConfig.Exchange ex) {
        long left = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            if (left <= 0) break;
            ItemStack s = contents[i];
            if (s == null || s.getType() == Material.AIR) continue;
            if (!matches(s, req, ex)) continue;
            int take = (int) Math.min(left, s.getAmount());
            left -= take;
            if (take >= s.getAmount()) player.getInventory().setItem(i, null);
            else {
                ItemStack c = s.clone();
                c.setAmount(s.getAmount() - take);
                player.getInventory().setItem(i, c);
            }
        }
        return amount - left;
    }

    private static boolean matches(ItemStack item, ExchangeConfig.ItemLine req, ExchangeConfig.Exchange ex) {
        if (item == null || req.material == null) return false;
        if (item.getType() != req.material) return false;
        if (req.data > 0 && item.getDurability() != req.data) return false;

        if (req.nameHint != null && !req.nameHint.isEmpty()) {
            if (!item.hasItemMeta()) return false;
            if (item.getItemMeta().hasDisplayName()) {
                String dn = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                if (dn.contains(req.nameHint)) return true;
            }
            if (item.getItemMeta().hasLore()) {
                for (String line : item.getItemMeta().getLore()) {
                    if (ChatColor.stripColor(line).contains(req.nameHint)) return true;
                }
            }
            return false;
        }

        switch (ex.matchMode) {
            case LOOSE: return true;
            case STRICT: {
                if (!item.hasItemMeta()) return true;
                ItemMeta m = item.getItemMeta();
                return !m.hasDisplayName() && !m.hasLore();
            }
            case LORE: {
                if (ex.matchLore.isEmpty()) return true;
                if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
                for (String line : item.getItemMeta().getLore()) {
                    String plain = ChatColor.stripColor(line);
                    for (String key : ex.matchLore) if (plain.contains(key)) return true;
                }
                return false;
            }
            case NAME: {
                if (ex.matchName.isEmpty()) return true;
                if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
                String dn = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                return dn.equals(ex.matchName) || dn.contains(ex.matchName);
            }
        }
        return true;
    }

    private static Economy getEconomy() {
        try {
            if (Bukkit.getPluginManager().getPlugin("Vault") == null) return null;
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            return rsp == null ? null : rsp.getProvider();
        } catch (Throwable t) { return null; }
    }
    private static int queryPoints(Player p) {
        try { return PointAPI.getPlayerPoints(p.getName()); } catch (Throwable t) { return 0; }
    }
    private static boolean takePoints(Player p, int amt) {
        try { PointAPI.takePlayerPoints(p.getName(), amt); return true; } catch (Throwable t) { return false; }
    }
    private static void addPoints(Player p, int amt) {
        try { PointAPI.addPlayerPoints(p.getName(), amt); } catch (Throwable ignored) {}
    }
    private static double queryValue(Player p, String id) {
        try { return LDAttribute.getInstance().getValueManager().get(p).get(id); } catch (Throwable t) { return 0; }
    }
    private static void takeValue(Player p, String id, double amt) {
        try { LDAttribute.getInstance().getValueManager().get(p).take(id, amt); } catch (Throwable ignored) {}
    }
    private static void addValue(Player p, String id, double amt) {
        try { LDAttribute.getInstance().getValueManager().get(p).add(id, amt); } catch (Throwable ignored) {}
    }
    private static int countEmptySlots(Player p) {
        int n = 0;
        for (ItemStack s : p.getInventory().getStorageContents()) {
            if (s == null || s.getType() == Material.AIR) n++;
        }
        return n;
    }
}