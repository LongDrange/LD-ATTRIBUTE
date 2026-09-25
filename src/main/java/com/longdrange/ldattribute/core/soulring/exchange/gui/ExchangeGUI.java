package com.longdrange.ldattribute.core.soulring.exchange.gui;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.soulring.SoulRingData;
import com.longdrange.ldattribute.core.soulring.exchange.ExchangeConfig;
import com.longdrange.ldattribute.core.soulring.exchange.ExchangeData;
import com.longdrange.ldattribute.core.soulring.exchange.ExchangeManager;
import com.longdrange.ldattribute.core.soulring.exchange.ItemNames;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ExchangeGUI {

    public static final int BTN_BACK = 49;

    private final LDAttribute plugin;
    public ExchangeGUI(LDAttribute plugin) { this.plugin = plugin; }

    public void openDefault(Player player) {
        String firstPage = null;
        for (ExchangeConfig.Page p : ExchangeConfig.allPages()) { firstPage = p.id; break; }
        if (firstPage == null) { player.sendMessage(ChatColor.RED + "没有配置任何兑换页"); return; }
        open(player, firstPage);
    }

    public void open(Player player, String pageId) {
        ExchangeConfig.Page page = ExchangeConfig.getPage(pageId);
        if (page == null) { player.sendMessage(ChatColor.RED + "未知兑换页: " + pageId); return; }
        if (!page.permission.isEmpty() && !player.hasPermission(page.permission)) {
            player.sendMessage(ChatColor.RED + "你没有权限打开此兑换页");
            return;
        }

        ExchangeHolder holder = new ExchangeHolder(player.getUniqueId(), page.id);
        Inventory inv = Bukkit.createInventory(holder, 54, page.title);
        holder.setInventory(inv);

        render(holder, page);
        player.openInventory(inv);
    }

    public void refresh(Player player, ExchangeHolder holder) {
        if (player == null || holder == null) return;
        ExchangeConfig.Page page = ExchangeConfig.getPage(holder.getPageId());
        if (page == null) return;
        render(holder, page);
        try { player.updateInventory(); } catch (Throwable ignored) {}
    }

    private void render(ExchangeHolder holder, ExchangeConfig.Page page) {
        Inventory inv = holder.getInventory();
        if (inv == null) return;
        Player player = Bukkit.getPlayer(holder.getOwner());
        if (player == null) return;

        for (int i = 0; i < 54; i++) inv.setItem(i, null);

        SoulRingData ringData = plugin.getSoulRingManager().get(player);
        ExchangeData ed = new ExchangeData(plugin, player.getUniqueId());

        for (ExchangeConfig.Exchange ex : page.exchanges.values()) {
            if (ex.slot < 0 || ex.slot >= 45) continue;
            inv.setItem(ex.slot, buildIcon(ex, player, ringData, ed));
        }

        inv.setItem(BTN_BACK, glass((short) 4, ChatColor.YELLOW + "返回灵魂空间"));
    }

    private ItemStack buildIcon(ExchangeConfig.Exchange ex, Player player, SoulRingData ringData, ExchangeData ed) {
        Material m = Material.getMaterial(ex.icon);
        if (m == null) m = Material.PAPER;
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;

        meta.setDisplayName(ChatColor.YELLOW + "\u2726 " + ex.name);

        List<String> lore = new ArrayList<>();
        boolean canAll = true;

        lore.add(ChatColor.WHITE + "【消耗】");
        for (ExchangeConfig.ItemLine req : ex.input) {
            long have = getHave(player, ringData, req, ex);
            boolean ok = have >= req.amount;
            if (!ok) canAll = false;

            String name = displayNameOf(req);
            lore.add((ok ? ChatColor.GREEN + "  \u2714 " : ChatColor.RED + "  \u2718 ")
                    + ChatColor.GRAY + name + " x" + req.amount
                    + ChatColor.DARK_GRAY + " (" + have + ")");
        }

        lore.add("");
        lore.add(ChatColor.WHITE + "【获得】");
        for (ExchangeConfig.ItemLine out : ex.output) {
            lore.add(ChatColor.GRAY + "  " + displayNameOf(out) + " x" + out.amount);
        }

        if (ex.chance < 100.0) {
            lore.add(ChatColor.LIGHT_PURPLE + "  概率: " + String.format("%.2f%%", ex.chance));
        }

        if (!ex.displayLore.isEmpty()) {
            lore.add("");
            lore.addAll(ex.displayLore);
        }

        lore.add("");

        if (!ex.permission.isEmpty()) {
            boolean has = player.hasPermission(ex.permission);
            lore.add((has ? ChatColor.GREEN + "  \u2714 " : ChatColor.RED + "  \u2718 ")
                    + ChatColor.GRAY + "权限: " + ex.permission);
        }

        int remain = ed.remainTimes(ex);
        if (remain == 0) {
            lore.add(ChatColor.RED + "已兑换上限");
            canAll = false;
        } else if (remain > 0) {
            lore.add(ChatColor.YELLOW + "可兑换: " + ChatColor.WHITE + remain + " 次");
        }

        int maxByStock = ExchangeManager.calcMaxTimes(plugin, player, ex);
        lore.add(ChatColor.GRAY + "存量可兑换: " + ChatColor.WHITE + maxByStock + " 次");

        lore.add("");
        if (!canAll) lore.add(ChatColor.RED + "条件不满足");
        else {
            lore.add(ChatColor.GREEN + "左键 = 兑换 1 次");
            lore.add(ChatColor.GREEN + "Shift+左键 = 全部兑换");
        }

        meta.setLore(lore);
        it.setItemMeta(meta);
        return it;
    }

    /** ★ 显示名：@名字 > 中文名 > 材质名 */
    private String displayNameOf(ExchangeConfig.ItemLine line) {
        // 有 @名字 直接用
        if (line.nameHint != null && !line.nameHint.isEmpty()) return line.nameHint;
        // 材质物品 → 用中文名
        if (line.material != null) return ItemNames.getDisplayName(line.material, line.data);
        // VAULT/POINT/VALUE/CMD
        if (line.valueId != null && line.valueId.startsWith("CMD:")) return "执行命令";
        if (line.source == ExchangeConfig.SourceType.VAULT) return "金币";
        if (line.source == ExchangeConfig.SourceType.POINT) return "点券";
        if (line.valueId != null) return line.valueId;
        return line.prettyName();
    }

    private long getHave(Player player, SoulRingData ringData,
                         ExchangeConfig.ItemLine req, ExchangeConfig.Exchange ex) {
        switch (req.source) {
            case SOULRING: return ExchangeManager.countInSoulRing(ringData, req, ex);
            case INVENTORY: return ExchangeManager.countInInventory(player, req, ex);
            case VAULT: {
                try {
                    if (Bukkit.getPluginManager().getPlugin("Vault") == null) return 0;
                    org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
                            Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                    if (rsp == null) return 0;
                    return (long) rsp.getProvider().getBalance(player);
                } catch (Throwable t) { return 0; }
            }
            case POINT: {
                try { return com.longdrange.ldattribute.points.PointAPI.getPlayerPoints(player.getName()); }
                catch (Throwable t) { return 0; }
            }
            case VALUE: {
                try { return (long) LDAttribute.getInstance().getValueManager().get(player).get(req.valueId); }
                catch (Throwable t) { return 0; }
            }
        }
        return 0;
    }

    private ItemStack glass(short data, String name) {
        ItemStack it = new ItemStack(Material.STAINED_GLASS_PANE, 1, data);
        ItemMeta m = it.getItemMeta();
        if (m != null) { m.setDisplayName(name); it.setItemMeta(m); }
        return it;
    }
}