package com.longdrange.ldattribute.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import com.longdrange.ldattribute.data.attribute.LDAttributeType;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import com.longdrange.ldattribute.data.eventdata.LDUpdateEventData;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 屬性更新監聽器
 * 負責在適當時機觸發 UPDATE 類型屬性（生命上限、移動速度…）
 */
public class OnUpdateStatsListener implements Listener {

    private final LDAttribute plugin;

    public OnUpdateStatsListener(LDAttribute plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> updatePlayer(player), 5L);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> updatePlayer(player), 1L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> updatePlayer(player), 1L);
    }

    private void updatePlayer(Player player) {
        // 1. 重置為基礎值
        player.setMaxHealth(20.0);
        player.setWalkSpeed(0.2f);

        // 2. 讀取裝備屬性
        LDAttributeData data = loadEntityData(player);

        // 3. 觸發所有 UPDATE 類型屬性
        LDUpdateEventData updateData = new LDUpdateEventData(player);
        for (LDSubAttribute attr : data.getAttributeMap().values()) {
            if (attr.containsType(LDAttributeType.UPDATE)) {
                attr.eventMethod(updateData);
            }
        }

        // 4. 若當前血量 > 最大血量，修正
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private LDAttributeData loadEntityData(LivingEntity entity) {
        LDAttributeData data = new LDAttributeData();
        if (entity instanceof Player) {
            Player player = (Player) entity;
            for (ItemStack item : player.getInventory().getArmorContents()) {
                data.add(plugin.getManager().getItemData(player, null, item));
            }
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            data.add(plugin.getManager().getItemData(player, null, mainHand));
            ItemStack offHand = player.getInventory().getItemInOffHand();
            data.add(plugin.getManager().getItemData(player, null, offHand));
        }

        // 加上 API 附加資料（TcCardStats 等插件存入的卡片屬性）
        LDAttributeData apiData = plugin.getApi().getAPIStats(entity.getUniqueId());
        if (apiData != null) {
            data.add(apiData);
        }

        return data;
    }
}