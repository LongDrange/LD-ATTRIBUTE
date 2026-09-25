package com.longdrange.ldattribute.core.ring.gui;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.ring.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RingUpgradeGUI {

    public static final int BTN_CONFIRM = 11;
    public static final int BTN_INFO    = 13;
    public static final int BTN_CANCEL  = 15;
    public static final String TITLE = ChatColor.DARK_GRAY + "魂珠升级";

    public static class UpgradeHolder implements InventoryHolder {
        public final UUID owner;
        public final int slotId;
        public final int backPage;
        private Inventory inv;
        public UpgradeHolder(UUID owner, int slotId, int backPage) {
            this.owner = owner; this.slotId = slotId; this.backPage = backPage;
        }
        @Override public Inventory getInventory() { return inv; }
        public void setInventory(Inventory inv) { this.inv = inv; }
    }

    private final LDAttribute plugin;
    public RingUpgradeGUI(LDAttribute plugin) { this.plugin = plugin; }

    public void open(Player player, int slotId, int backPage) {
        UpgradeHolder holder = new UpgradeHolder(player.getUniqueId(), slotId, backPage);
        Inventory inv = Bukkit.createInventory(holder, 27, TITLE);
        holder.setInventory(inv);

        RingData data = plugin.getRingManager().get(player);
        RingData.Slot slot = data.getSlot(slotId);
        if (slot == null) { player.sendMessage(ChatColor.RED + "该槽位无魂珠"); return; }

        int curLv = slot.level;
        int maxLv = RingUpgradeConfig.getMaxLevel(slot.ringType);
        double bonus = RingUpgradeConfig.getBonusPerLevel(slot.ringType);
        int targetLv = curLv + 1;
        RingUpgradeConfig.Cost cost = RingUpgradeConfig.getCost(slot.ringType, targetLv);

        // ===== 升级消耗 =====
        // 剩余 = 上限 - (上限×30% + 上限×10%×BonusPerLevel)
        // 消耗 = 上限 - 剩余
        double remainDouble = slot.maxStack - (slot.maxStack * 0.30 + slot.maxStack * 0.10 * bonus);
        int remainAfter = (int) Math.floor(remainDouble);
        if (remainAfter < 0) remainAfter = 0;
        int consumeAmount = slot.maxStack - remainAfter;
        if (consumeAmount < 0) consumeAmount = 0;
        if (consumeAmount > slot.count) consumeAmount = slot.count;
        boolean full = slot.count >= slot.maxStack;

        // ============ 确认按钮 ============
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add(ChatColor.GRAY + "类型: " + slot.ringType);
        confirmLore.add(ChatColor.YELLOW + "等级: Lv." + curLv + " \u2192 Lv." + targetLv);
        confirmLore.add("");
        if (!full) {
            confirmLore.add(ChatColor.RED + "\u2718 魂珠未堆满（" + slot.count + "/" + slot.maxStack + "）");
            confirmLore.add(ChatColor.GRAY + "  需堆满才能升级");
        }
        confirmLore.add(ChatColor.WHITE + "升级需求:");
        confirmLore.add(ChatColor.RED + "  消耗魂珠: " + consumeAmount + " 个"
                + ChatColor.GRAY + "  (升级后剩余 " + remainAfter + " 个)");
        if (cost == null) {
            confirmLore.add(ChatColor.RED + "  已达最大等级或没有成本配置");
        } else {
            confirmLore.addAll(RingCost.describe("", cost.points, cost.vault, cost.items));
        }
        inv.setItem(BTN_CONFIRM, icon(Material.STAINED_GLASS_PANE,
                ChatColor.GREEN + "\u2714 确认升级", confirmLore, (short) 5));

        // ============ 信息按钮 ============
        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.GRAY + "类型: " + slot.ringType);
        infoLore.add(ChatColor.YELLOW + "当前等级: " + ChatColor.WHITE + "Lv." + curLv + " / " + maxLv);
        infoLore.add(ChatColor.YELLOW + "每级加成: " + ChatColor.WHITE + String.format("%.1f%%", bonus * 100));
        infoLore.add(ChatColor.YELLOW + "当前倍数: " + ChatColor.WHITE + "x" + String.format("%.2f", 1.0 + (curLv - 1) * bonus));
        if (targetLv <= maxLv) {
            infoLore.add(ChatColor.YELLOW + "升级后倍数: " + ChatColor.WHITE + "x" + String.format("%.2f", 1.0 + (targetLv - 1) * bonus));
        }
        infoLore.add("");
        infoLore.add(ChatColor.YELLOW + "当前数量: " + ChatColor.WHITE + slot.count + "/" + slot.maxStack
                + (full ? ChatColor.GREEN + " \u2714 已满" : ChatColor.RED + " \u2718 未满"));
        infoLore.add("");
        infoLore.add(ChatColor.WHITE + "升级消耗公式:");
        infoLore.add(ChatColor.GRAY + "  剩余 = 上限 - (上限×30% + 上限×10%×加成)");
        infoLore.add(ChatColor.GRAY + "  消耗 = 上限 - 剩余 = " + consumeAmount);
        infoLore.add(ChatColor.GRAY + "  \u2192 升级后剩余 " + remainAfter + " 个");

        // 属性预览
        Map<String, String> base = RingAttributeReader.parseItemLore(slot.template);
        if (!base.isEmpty()) {
            infoLore.add("");
            infoLore.add(ChatColor.WHITE + "属性预览（升级前 \u2192 升级后）:");
            for (Map.Entry<String, String> be : base.entrySet()) {
                try {
                    double v = Double.parseDouble(be.getValue().replace("+", "").replace("%", "").trim());
                    double cur = v * slot.count * (1.0 + (curLv - 1) * bonus);
                    double next;
                    if (targetLv <= maxLv) {
                        // 升级后：数量 = remainAfter, 等级 = targetLv
                        next = v * remainAfter * (1.0 + (targetLv - 1) * bonus);
                    } else {
                        next = cur;
                    }
                    infoLore.add(ChatColor.GRAY + "  " + be.getKey() + ": "
                            + ChatColor.GREEN + RingGUI.formatNum(cur)
                            + ChatColor.GRAY + " \u2192 " + ChatColor.AQUA + RingGUI.formatNum(next));
                } catch (Exception ignored) {}
            }
        }
        inv.setItem(BTN_INFO, icon(Material.PAPER, ChatColor.AQUA + "升级信息", infoLore, (short) 0));

        inv.setItem(BTN_CANCEL, icon(Material.STAINED_GLASS_PANE,
                ChatColor.RED + "\u2718 返回",
                Collections.singletonList(ChatColor.GRAY + "返回魂珠空间"), (short) 14));

        player.openInventory(inv);
    }

    private ItemStack icon(Material mat, String name, List<String> lore, short data) {
        ItemStack it = new ItemStack(mat, 1, data);
        ItemMeta m = it.getItemMeta();
        if (m != null) { m.setDisplayName(name); if (lore != null) m.setLore(lore); it.setItemMeta(m); }
        return it;
    }
}