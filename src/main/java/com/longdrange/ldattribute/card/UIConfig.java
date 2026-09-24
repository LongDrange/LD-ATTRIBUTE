package com.longdrange.ldattribute.card;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.card.inventory.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class UIConfig {

    public static class Button {
        public int slot;
        public Material material;
        public String name;
        public List<String> lore;
    }

    private static final Map<String, Map<String, Button>> screens = new HashMap<>();
    private static YamlConfiguration lastCfg;

    public static void load(LDAttribute plugin) {
        screens.clear();
        File f = com.longdrange.ldattribute.util.ConfigPaths.resolve(plugin, "ui.yml");
        if (!f.exists()) { try { plugin.saveResource("ui.yml", false); } catch (Exception ignored) {} }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

        for (String screen : cfg.getKeys(false)) {
            ConfigurationSection sec = cfg.getConfigurationSection(screen);
            if (sec == null) continue;
            Map<String, Button> buttons = new LinkedHashMap<>();
            Map<Integer, String> slotsUsed = new HashMap<>();

            for (String key : sec.getKeys(false)) {
                ConfigurationSection bs = sec.getConfigurationSection(key);
                if (bs == null) continue;
                Button b = new Button();
                b.slot = bs.getInt("Slot", -1);
                if (b.slot < 0) continue;
                String matName = bs.getString("Material", "STONE");
                Material m = Material.getMaterial(matName.toUpperCase());
                b.material = m != null ? m : Material.STONE;
                b.name = colorize(bs.getString("Name", ""));
                b.lore = new ArrayList<>();
                for (String s : bs.getStringList("Lore")) b.lore.add(colorize(s));

                if (slotsUsed.containsKey(b.slot)) {
                    plugin.getLogger().warning("[UI] " + screen + " 槽位 " + b.slot
                            + " 被 " + slotsUsed.get(b.slot) + " 和 " + key + " 同時佔用，後者覆蓋");
                }
                slotsUsed.put(b.slot, key);
                buttons.put(key, b);
            }
            screens.put(screen, buttons);
        }

        lastCfg = cfg;

        applyToFields();
        plugin.getLogger().info("已載入 UI 配置: " + screens.size() + " 個界面");
    }

    private static void applyToFields() {
        // CardInfoInventory
        try {
            CardInfoInventory.SLOT_CARD = getSlot("CardInfo", "Card", CardInfoInventory.SLOT_CARD);
            CardInfoInventory.SLOT_CLOSE = getSlot("CardInfo", "Close", CardInfoInventory.SLOT_CLOSE);
            CardInfoInventory.SLOT_USE_EXPSTONE = getSlot("CardInfo", "UseExpStone", CardInfoInventory.SLOT_USE_EXPSTONE);
            CardInfoInventory.SLOT_DECOMPOSE = getSlot("CardInfo", "Decompose", CardInfoInventory.SLOT_DECOMPOSE);
            CardInfoInventory.SLOT_LOCK = getSlot("CardInfo", "Lock", CardInfoInventory.SLOT_LOCK);
            CardInfoInventory.SLOT_BIND = getSlot("CardInfo", "Bind", CardInfoInventory.SLOT_BIND);
            CardInfoInventory.SLOT_STAR = getSlot("CardInfo", "Star", CardInfoInventory.SLOT_STAR);
            CardInfoInventory.SLOT_RUNE = getSlot("CardInfo", "Rune", CardInfoInventory.SLOT_RUNE);
        } catch (Throwable ignored) {}

        try {
            CardInventory.SLOT_PREV = getSlot("CardBag", "Prev", CardInventory.SLOT_PREV);
            CardInventory.SLOT_COLLECTION = getSlot("CardBag", "Collection", CardInventory.SLOT_COLLECTION);
            CardInventory.SLOT_SELL = getSlot("CardBag", "Sell", CardInventory.SLOT_SELL);
            CardInventory.SLOT_INFO = getSlot("CardBag", "Info", CardInventory.SLOT_INFO);
            CardInventory.SLOT_SUIT = getSlot("CardBag", "Suit", CardInventory.SLOT_SUIT);
            CardInventory.SLOT_NEXT = getSlot("CardBag", "Next", CardInventory.SLOT_NEXT);
            CardInventory.SLOT_STATS = getSlot("CardBag", "Stats", CardInventory.SLOT_STATS);
            CardInventory.SLOT_RANK = getSlot("CardBag", "Rank", CardInventory.SLOT_RANK);
        } catch (Throwable ignored) {}

        try {
            CollectionInventory.SLOT_BACK = getSlot("Collection", "Back", CollectionInventory.SLOT_BACK);
            CollectionInventory.SLOT_PREV = getSlot("Collection", "Prev", CollectionInventory.SLOT_PREV);
            CollectionInventory.SLOT_INFO = getSlot("Collection", "Info", CollectionInventory.SLOT_INFO);
            CollectionInventory.SLOT_NEXT = getSlot("Collection", "Next", CollectionInventory.SLOT_NEXT);
        } catch (Throwable ignored) {}

        try {
            DecomposeConfirmInventory.SLOT_CARD = getSlot("DecomposeConfirm", "Card", DecomposeConfirmInventory.SLOT_CARD);
            DecomposeConfirmInventory.SLOT_CONFIRM = getSlot("DecomposeConfirm", "Confirm", DecomposeConfirmInventory.SLOT_CONFIRM);
            DecomposeConfirmInventory.SLOT_CANCEL = getSlot("DecomposeConfirm", "Cancel", DecomposeConfirmInventory.SLOT_CANCEL);
        } catch (Throwable ignored) {}

        try {
            ExpStoneSelectInventory.SLOT_BACK = getSlot("ExpStoneSelect", "Back", ExpStoneSelectInventory.SLOT_BACK);
        } catch (Throwable ignored) {}

        try {
            MergeInventory.SLOT_BACK = getSlot("Merge", "Back", MergeInventory.SLOT_BACK);
            MergeInventory.SLOT_INFO = getSlot("Merge", "Info", MergeInventory.SLOT_INFO);
        } catch (Throwable ignored) {}

        try {
            SellInventory.SLOT_CANCEL = getSlot("Sell", "Cancel", SellInventory.SLOT_CANCEL);
        } catch (Throwable ignored) {}

        try {
            SuitInventory.SLOT_BACK = getSlot("Suit", "Back", SuitInventory.SLOT_BACK);
        } catch (Throwable ignored) {}
    }

    private static String colorize(String s) {
        return ChatColor.translateAlternateColorCodes((char) 38, s);
    }

    public static Button getButton(String screen, String key) {
        Map<String, Button> m = screens.get(screen);
        return m != null ? m.get(key) : null;
    }

    /** 读原始数值配置（不受 Button 解析影响） */
    public static int getInt(String screen, String key, int def) {
        if (lastCfg == null) return def;
        return lastCfg.getInt(screen + "." + key, def);
    }

    public static String getString(String screen, String key, String def) {
        if (lastCfg == null) return def;
        return lastCfg.getString(screen + "." + key, def);
    }

    public static List<Integer> getIntList(String screen, String key, List<Integer> def) {
        if (lastCfg == null) return def;
        List<Integer> r = lastCfg.getIntegerList(screen + "." + key);
        return (r == null || r.isEmpty()) ? def : r;
    }
    public static int getSlot(String screen, String key, int def) {
        Button b = getButton(screen, key);
        return b != null ? b.slot : def;
    }

    public static ItemStack build(String screen, String key, Object... loreArgs) {
        Button b = getButton(screen, key);
        if (b == null) return null;
        ItemStack item = new ItemStack(b.material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(b.name);
            if (!b.lore.isEmpty()) {
                List<String> l = new ArrayList<>();
                for (String line : b.lore) {
                    String s = line;
                    for (int i = 0; i < loreArgs.length; i++) {
                        s = s.replace("{" + i + "}", String.valueOf(loreArgs[i]));
                    }
                    l.add(s);
                }
                meta.setLore(l);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}