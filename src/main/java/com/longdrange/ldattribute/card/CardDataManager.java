package com.longdrange.ldattribute.card;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class CardDataManager {

    private static final Map<String, CardData> cards = new LinkedHashMap<>();

    public static void load(LDAttribute plugin) {
        cards.clear();

        File file = com.longdrange.ldattribute.util.ConfigPaths.resolve(plugin, "item.yml");
        if (!file.exists()) {
            try { plugin.saveResource("item.yml", false); } catch (Exception ignored) {}
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String id : cfg.getKeys(false)) {
            ConfigurationSection sec = cfg.getConfigurationSection(id);
            if (sec == null) continue;

            String name = org.bukkit.ChatColor.translateAlternateColorCodes((char) 38, sec.getString("Name", id));
            int materialId = sec.getInt("ID", 339);
            String type = sec.getString("Type", "");
            List<String> lore = sec.getStringList("Lore");
            boolean enchant = sec.getBoolean("Enchant", false);
            boolean unbreakable = sec.getBoolean("Unbreakable", true);

            Material mat = Material.getMaterial(materialId);
            if (mat == null) mat = Material.PAPER;
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes((char)38, name));

            List<String> colored = new ArrayList<>();
            for (String line : lore) colored.add(ChatColor.translateAlternateColorCodes((char)38, line));
            meta.setLore(colored);

            if (enchant) meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.setUnbreakable(unbreakable);
            item.setItemMeta(meta);

            // 标记物品类型
            item = com.longdrange.ldattribute.item.ItemTypeNBT.setType(item, com.longdrange.ldattribute.item.ItemType.CARD);

            // 初始化等級 NBT + 動態 Lore
            if (CardLevelConfig.isUpgradable(id)) {
                item = CardLevel.recalc(item, id, 1, 0);
            }

            boolean bindOnPickup = sec.getBoolean("BindOnPickup", false);
            boolean tradeable = sec.getBoolean("Tradeable", true);
            if (!tradeable) bindOnPickup = true;   // 不可交易 = 强制绑定
            boolean allowUnbind = sec.getBoolean("AllowUnbind", false);
            int unbindCost = sec.getInt("UnbindCost", 0);
            String element = sec.getString("Element", "");
            cards.put(id, new CardData(id, item, type, bindOnPickup, tradeable, allowUnbind, unbindCost, element));
        }

        plugin.getLogger().info("已載入 " + cards.size() + " 種卡片");
    }

    public static CardData getCard(String id) { return cards.get(id); }

    public static CardData findCard(ItemStack item) {
        if (item == null) return null;
        for (CardData card : cards.values()) {
            if (card.matches(item)) return card;
        }
        return null;
    }

    public static Collection<CardData> getAllCards() { return cards.values(); }

    /** 只回傳真正的卡片（不含經驗石） */
    public static Collection<CardData> getAllCardOnly() {
        java.util.List<CardData> list = new java.util.ArrayList<>();
        for (CardData c : cards.values()) if (c.isCard()) list.add(c);
        return list;
    }
    public static Set<String> getAllIds() { return cards.keySet(); }
}
