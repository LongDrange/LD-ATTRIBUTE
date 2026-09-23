package com.longdrange.ldattribute.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import com.longdrange.ldattribute.data.attribute.sub.other.ExpAdditionAttribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 經驗監聽器
 * 玩家獲得經驗時，套用經驗加成屬性
 */
public class OnExpListener implements Listener {

    private final LDAttribute plugin;

    public OnExpListener(LDAttribute plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        LDAttributeData data = loadEntityData(player);

        for (LDSubAttribute attr : data.getAttributeMap().values()) {
            if (attr instanceof ExpAdditionAttribute) {
                double multiplier = ((ExpAdditionAttribute) attr).getMultiplier();
                int newAmount = (int) (event.getAmount() * multiplier);
                event.setAmount(newAmount);
                return;
            }
        }
    }

    private LDAttributeData loadEntityData(Player player) {
        LDAttributeData data = new LDAttributeData();
        for (ItemStack item : player.getInventory().getArmorContents()) {
            data.add(plugin.getManager().getItemData(player, null, item));
        }
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        data.add(plugin.getManager().getItemData(player, null, mainHand));
        ItemStack offHand = player.getInventory().getItemInOffHand();
        data.add(plugin.getManager().getItemData(player, null, offHand));
        return data;
    }
}