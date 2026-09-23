package com.longdrange.ldattribute.card.inventory;

import com.longdrange.ldattribute.card.CardData;
import com.longdrange.ldattribute.card.CardDataManager;
import com.longdrange.ldattribute.card.PlayerData;
import com.longdrange.ldattribute.util.Config;
import com.longdrange.ldattribute.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * 販賣卡片界面
 */
public class SellInventory {

    public static int SLOT_CANCEL = 49;

    public static void open(Player player) {
        String title = Message.get("Inventory.Sell.Name");
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // 顯示玩家卡片背包中的所有卡片（遍歷所有已解鎖頁）
        List<ItemStack> cards = PlayerData.getCards(player);
        double rate = Config.getDouble("SellCard.Rate", 1.0);

        int slot = 0;
        for (ItemStack card : cards) {
            if (slot >= 45) break;
            if (card == null || card.getType() == Material.AIR) continue;

            CardData data = CardDataManager.findCard(card);
            if (data == null) continue;

            // 複製並修改 Lore 顯示價值
            ItemStack display = card.clone();
            ItemMeta meta = display.getItemMeta();
            int value = (int) (data.getValue() * rate);
            meta.setLore(Arrays.asList(
                    "&7價值: &6" + value + " &7點券",
                    "",
                    "&e左鍵 &7販賣 1 張",
                    "&e右鍵 &7販賣全部"
            ));
            display.setItemMeta(meta);
            inv.setItem(slot, display);
            slot++;
        }

        // 底部分隔線
        ItemStack separator = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta sepMeta = separator.getItemMeta();
        sepMeta.setDisplayName(" ");
        separator.setItemMeta(sepMeta);
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, separator);
        }

        // 取消按鈕
        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(Message.get("Inventory.Sell.Cancel.Name"));
        cancelMeta.setLore(Message.getList("Inventory.Sell.Cancel.Lore"));
        cancel.setItemMeta(cancelMeta);
        inv.setItem(SLOT_CANCEL, cancel);

        player.openInventory(inv);
    }

    public static boolean isSellInventory(String title) {
        return title.equals(Message.get("Inventory.Sell.Name"));
    }
}
