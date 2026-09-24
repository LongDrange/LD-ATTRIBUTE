package com.longdrange.ldattribute.card;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

/**
 * 套裝系統
 * 從 suit.yml 載入套裝定義，判定玩家是否集齊
 */
public class SuitData {

    /** 套裝名稱 -> 卡片 ID 清單 */
    private static final Map<String, List<String>> suitItems = new LinkedHashMap<>();

    /** 套裝名稱 -> 套裝屬性 Lore */
    private static final Map<String, List<String>> suitEffects = new LinkedHashMap<>();

    /** 套裝顯示名稱 -> 玩家當前集齊進度 */
    private static final Map<String, Map<String, Integer>> progressCache = new HashMap<>();

    public static void load(LDAttribute plugin) {
        suitItems.clear();
        suitEffects.clear();

        File file = com.longdrange.ldattribute.util.ConfigPaths.resolve(plugin, "suit.yml");
        if (!file.exists()) {
            try { plugin.saveResource("suit.yml", false); } catch (Exception ignored) {}
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String key : cfg.getKeys(false)) {
            ConfigurationSection sec = cfg.getConfigurationSection(key);
            if (sec == null) continue;

            String name = org.bukkit.ChatColor.translateAlternateColorCodes((char) 38, sec.getString("Name", key));
            List<String> list = sec.getStringList("List");
            List<String> effect = sec.getStringList("Effect");

            suitItems.put(name, list);
            suitEffects.put(name, effect);
        }

        plugin.getLogger().info("已載入 " + suitItems.size() + " 個套裝");
    }

    /**
     * 判定玩家是否集齊套裝，並套用套裝屬性
     */
    public static void applySuits(Player player, List<ItemStack> cards, LDAttributeData statsData) {
        for (String suitName : suitItems.keySet()) {
            List<String> required = suitItems.get(suitName);
            int matched = 0;

            for (String cardId : required) {
                if (hasCard(cards, cardId)) {
                    matched++;
                }
            }

            // 記錄進度
            progressCache.computeIfAbsent(player.getName(), k -> new HashMap<>())
                    .put(suitName, matched);

            // 集齊 → 套用套裝屬性
            if (matched == required.size()) {
                List<String> effect = suitEffects.get(suitName);
                if (effect != null && !effect.isEmpty()) {
                    LDAttributeData suitData = LDAttribute.getInstance().getApi()
                            .getLoreData(player, null, effect);
                    statsData.add(suitData);
                }
            }
        }
    }

    /**
     * 判斷卡片清單中是否有指定 ID 的卡片（用顯示名稱比對）
     */
    private static boolean hasCard(List<ItemStack> cards, String cardId) {
        CardData target = CardDataManager.getCard(cardId);
        if (target == null) return false;
        for (ItemStack card : cards) {
            if (target.matches(card)) return true;
        }
        return false;
    }

    /**
     * 取得玩家套裝進度（用於顯示在 GUI 上）
     */
    public static Map<String, Integer> getProgress(String playerName) {
        return progressCache.getOrDefault(playerName, new HashMap<>());
    }

    public static List<String> getEffect(String suitName) {
        return suitEffects.getOrDefault(suitName, new java.util.ArrayList<>());
    }

    public static Set<String> getAllSuitNames() {
        return suitItems.keySet();
    }

    public static int getRequiredCount(String suitName) {
        List<String> list = suitItems.get(suitName);
        return list == null ? 0 : list.size();
    }
}
