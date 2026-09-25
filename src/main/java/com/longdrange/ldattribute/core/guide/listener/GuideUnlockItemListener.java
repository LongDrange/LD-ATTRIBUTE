package com.longdrange.ldattribute.core.guide.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.guide.GuideConfig;
import com.longdrange.ldattribute.core.guide.GuideData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 圖鑑解鎖石
 * Lore 里写：物品类型: 圖鑑解鎖石(怪物id或怪物名)
 * 右键使用 → 解锁对应图鉴 + 消耗 1 个物品
 */
public class GuideUnlockItemListener implements Listener {

    private static final String TYPE_KEY = "物品类型";
    private static final String STONE_KEY = "圖鑑解鎖石";

    private final LDAttribute plugin;
    public GuideUnlockItemListener(LDAttribute plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = e.getPlayer();
        ItemStack item = e.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        String target = parseTarget(item);
        if (target == null) return;

        e.setCancelled(true);

        GuideConfig.MonsterDef def = GuideConfig.findByNameOrId(target);
        if (def == null) {
            player.sendMessage(ChatColor.RED + "未知图鉴: " + target);
            return;
        }

        GuideData data = plugin.getGuideManager().get(player);
        if (data.isUnlocked(def.id)) {
            player.sendMessage(ChatColor.YELLOW + "你已经解锁了 " + def.name);
            return;
        }

        GuideKillListener.unlockGuide(player, def, data, "解锁石");
        // 消耗一个
        if (item.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(item.getAmount() - 1);
            player.getInventory().setItemInMainHand(item);
        }
    }

    /** 解析 Lore 中 "物品类型: 圖鑑解鎖石(xxx)"，返回 xxx，不是解锁石返回 null */
    private static String parseTarget(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            String plain = ChatColor.stripColor(line).trim();
            if (!plain.contains(TYPE_KEY) || !plain.contains(STONE_KEY)) continue;

            // 提取括号内容（兼容中文括号）
            int l = plain.indexOf('(');
            if (l < 0) l = plain.indexOf('（');
            int r = plain.indexOf(')');
            if (r < 0) r = plain.indexOf('）');
            if (l < 0 || r < 0 || r <= l) continue;

            String val = plain.substring(l + 1, r).trim();
            if (!val.isEmpty()) return val;
        }
        return null;
    }
}