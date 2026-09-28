package com.longdrange.ldattribute.core.relic.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.relic.RelicData;
import com.longdrange.ldattribute.core.relic.RelicItem;
import com.longdrange.ldattribute.core.relic.RelicConfig;
import com.longdrange.ldattribute.core.relic.gui.RelicGUI;
import com.longdrange.ldattribute.core.relic.gui.RelicHolder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RelicGUIListener implements Listener {

    private final LDAttribute plugin;
    public RelicGUIListener(LDAttribute plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!(e.getInventory().getHolder() instanceof RelicHolder)) return;
        Player player = (Player) e.getWhoClicked();
        e.setCancelled(true);

        int raw = e.getRawSlot();

        java.util.Map<String, Integer> layout = RelicGUI.computeLayout();
        String slotId = null;
        for (java.util.Map.Entry<String, Integer> en : layout.entrySet()) {
            if (en.getValue() == raw) { slotId = en.getKey(); break; }
        }
        if (slotId == null) return;

        RelicData data = plugin.getCoreManager().getRelicManager().get(player);
        if (data == null) return;
        RelicData.RelicInstance existing = data.get(slotId);

        // ===== 右键：切换锁定（只对已装备的） =====
        if (e.isRightClick() && existing != null) {
            existing.locked = !existing.locked;
            data.save();
            player.sendMessage(existing.locked
                    ? ChatColor.RED + "🔒 遗物已锁定（无法卸下/覆盖）"
                    : ChatColor.GREEN + "🔓 遗物已解锁");
            new RelicGUI(plugin).open(player);
            return;
        }

        // ===== Shift+左键：卸下（锁定时禁止） =====
        if (e.isShiftClick() && e.isLeftClick() && existing != null) {
            if (existing.locked) {
                player.sendMessage(ChatColor.RED + "🔒 遗物已锁定，请先右键解锁");
                return;
            }
            data.unequip(slotId);
            ItemStack back = RelicItem.toItem(existing);
            if (back != null) {
                java.util.HashMap<Integer, ItemStack> left = player.getInventory().addItem(back);
                for (ItemStack drop : left.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
            player.sendMessage(ChatColor.GREEN + "已卸下 " + slotId + " 槽位的遗物（已返还）");
            try { com.longdrange.ldattribute.card.StatsDataRead.updatePlayer(player); } catch (Throwable ignored) {}
            new RelicGUI(plugin).open(player);
            return;
        }

        // ===== 左键：装备 =====
        if (!e.isLeftClick()) return;
        if (existing != null && existing.locked) {
            player.sendMessage(ChatColor.RED + "🔒 此槽位已锁定，请先右键解锁再更换");
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "请把遗物拿在主手");
            return;
        }
        RelicData.RelicInstance parsed = parseFromItem(hand);
        if (parsed == null) {
            player.sendMessage(ChatColor.RED + "手上的物品不是遗物");
            return;
        }
        RelicConfig.RelicDef def = RelicConfig.getRelic(parsed.relicId);
        if (def == null || !def.slot.equals(slotId)) {
            player.sendMessage(ChatColor.RED + "此遗物不能装备到 " + slotId + " 槽位");
            return;
        }

        // 返还旧遗物
        if (existing != null) {
            ItemStack oldBack = RelicItem.toItem(existing);
            if (oldBack != null) {
                java.util.HashMap<Integer, ItemStack> left = player.getInventory().addItem(oldBack);
                for (ItemStack drop : left.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }
        data.equip(slotId, parsed);
        player.getInventory().setItemInMainHand(null);
        player.sendMessage(ChatColor.GREEN + "已装备遗物到 " + slotId);
        try { com.longdrange.ldattribute.card.StatsDataRead.updatePlayer(player); } catch (Throwable ignored) {}
        new RelicGUI(plugin).open(player);
    }

    public static RelicData.RelicInstance parseFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        String relicId = null;
        double mainValue = 0;
        List<RelicData.Sub> subs = new ArrayList<>();
        List<String> lore = item.getItemMeta().getLore();
        int section = 0;
        for (String line : lore) {
            String plain = ChatColor.stripColor(line).trim();
            if (plain.startsWith("id: ")) {
                relicId = plain.substring(4).trim();
                continue;
            }
            if (plain.contains("【主属性】")) { section = 1; continue; }
            if (plain.contains("【副属性】")) { section = 2; continue; }
            if (plain.contains(":") && !plain.contains("【")) {
                int idx = plain.indexOf(':');
                String k = plain.substring(0, idx).trim();
                String v = plain.substring(idx + 1).replace("+", "").trim();
                try {
                    double d = Double.parseDouble(v);
                    if (section == 1) mainValue = d;
                    else if (section == 2) subs.add(new RelicData.Sub(k, d));
                } catch (Throwable ignored) {}
            }
        }
        if (relicId == null) return null;
        RelicData.RelicInstance ins = new RelicData.RelicInstance(relicId);
        ins.mainValue = mainValue;
        ins.subs = subs;
        return ins;
    }
}