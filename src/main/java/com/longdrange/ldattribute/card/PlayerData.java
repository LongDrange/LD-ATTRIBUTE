package com.longdrange.ldattribute.card;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

/**
 * 玩家卡片背包資料（多頁）
 *
 * 記憶體結構：
 *   Map<UUID, PlayerCardData>
 *     - unlockedPages：已解鎖頁數
 *     - currentPage：當前頁（臨時，不存檔）
 *     - pages：頁碼 → 卡片陣列
 */
public class PlayerData {

    /** 每張卡的冷卻/每日計數 */
    public static class CardCooldown {
        public long lastUpgrade = 0;
        public String todayDate = "";
        public int todayCount = 0;
    }

    /** 單一玩家的卡片資料 */
    public static class PlayerCardData {
        public int unlockedPages = 1;
        public int currentPage = 0;
        public final Map<Integer, ItemStack[]> pages = new HashMap<>();
        public final Map<Integer, Integer> unlockedSlots = new HashMap<>();
        public final Set<String> claimedSets = new HashSet<>();
        public final Map<String, CardCooldown> cardCooldowns = new HashMap<>();
        public long lastSeen = 0;
        public int mergeCount = 0;
    }

    /** 相容舊 API：最大格數上限 */
    public static final int MAX_SLOTS = 45;

    private static final Map<UUID, PlayerCardData> cache = new HashMap<>();
    private static File file;
    private static YamlConfiguration data;

    public static void init(LDAttribute plugin) {
        File dir = plugin.getDataFolder();
        if (!dir.exists()) dir.mkdirs();
        file = new File(dir, "data" + File.separator + "players.yml");
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        if (!file.exists()) {
            try { file.createNewFile(); } catch (Exception ignored) {}
        }
        data = YamlConfiguration.loadConfiguration(file);
    }

    // ==================== 內部：取得或載入 ====================

    private static PlayerCardData getOrLoad(UUID uuid) {
        if (cache.containsKey(uuid)) return cache.get(uuid);
        PlayerCardData pcd = new PlayerCardData();
        String path = uuid.toString();

        // 讀取已解鎖頁數
        pcd.unlockedPages = data.getInt(path + ".unlockedPages", 1);
        if (pcd.unlockedPages < 1) pcd.unlockedPages = 1;

        // 新格式：pages.<n>
        if (data.contains(path + ".pages")) {
            org.bukkit.configuration.ConfigurationSection sec =
                    data.getConfigurationSection(path + ".pages");
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    try {
                        int page = Integer.parseInt(key);
                        ItemStack[] items = loadItems(sec.getList(key));
                        pcd.pages.put(page, items);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        // 舊格式：<uuid>.items → 轉到 page 0
        if (data.contains(path + ".items")) {
            ItemStack[] items = loadItems(data.getList(path + ".items"));
            pcd.pages.put(0, items);
        }

        // 保證 page 0 存在
        if (!pcd.pages.containsKey(0)) {
            pcd.pages.put(0, new ItemStack[MAX_SLOTS]);
        }

        pcd.mergeCount = data.getInt(path + ".mergeCount", 0);
        pcd.lastSeen = data.getLong(path + ".lastSeen", 0);

        // 讀取已領取的集齊獎勵
        List<String> claimed = data.getStringList(path + ".claimedSets");
        if (claimed != null) pcd.claimedSets.addAll(claimed);

        // 讀取卡片冷卻資料
        org.bukkit.configuration.ConfigurationSection cdSec = data.getConfigurationSection(path + ".cardCooldowns");
        if (cdSec != null) {
            for (String cardId : cdSec.getKeys(false)) {
                org.bukkit.configuration.ConfigurationSection s = cdSec.getConfigurationSection(cardId);
                if (s == null) continue;
                CardCooldown cd = new CardCooldown();
                cd.lastUpgrade = s.getLong("lastUpgrade", 0);
                cd.todayDate = s.getString("todayDate", "");
                cd.todayCount = s.getInt("todayCount", 0);
                pcd.cardCooldowns.put(cardId, cd);
            }
        }

        cache.put(uuid, pcd);
        return pcd;
    }

    @SuppressWarnings("unchecked")
    private static ItemStack[] loadItems(List<?> list) {
        ItemStack[] items = new ItemStack[MAX_SLOTS];
        if (list == null) return items;
        for (int i = 0; i < Math.min(list.size(), MAX_SLOTS); i++) {
            Object obj = list.get(i);
            if (obj instanceof ItemStack) {
                items[i] = (ItemStack) obj;
            }
        }
        return items;
    }

    // ==================== 頁面存取 ====================

    /**
     * 取得指定頁的卡片陣列
     */
    public static ItemStack[] getPageInventory(UUID uuid, int page) {
        PlayerCardData pcd = getOrLoad(uuid);
        ItemStack[] items = pcd.pages.get(page);
        if (items == null) {
            items = new ItemStack[MAX_SLOTS];
            pcd.pages.put(page, items);
        }
        return items;
    }

    /**
     * 設定指定頁的卡片陣列
     */
    public static void setPageInventory(UUID uuid, int page, ItemStack[] items) {
        PlayerCardData pcd = getOrLoad(uuid);
        pcd.pages.put(page, items);
    }

    /**
     * 取得玩家已解鎖的頁數
     */
    public static int getUnlockedPages(UUID uuid) {
        return getOrLoad(uuid).unlockedPages;
    }

    /**
     * 設定已解鎖頁數
     */
    public static void setUnlockedPages(UUID uuid, int n) {
        PlayerCardData pcd = getOrLoad(uuid);
        pcd.unlockedPages = Math.max(1, n);
    }

    /**
     * 解鎖下一頁（unlockedPages + 1）
     */
    public static int unlockNextPage(UUID uuid) {
        PlayerCardData pcd = getOrLoad(uuid);
        pcd.unlockedPages++;
        return pcd.unlockedPages;
    }

    /**
     * 當前頁（用於 UI）
     */
    public static int getCurrentPage(UUID uuid) {
        return getOrLoad(uuid).currentPage;
    }

    public static void setCurrentPage(UUID uuid, int page) {
        getOrLoad(uuid).currentPage = Math.max(0, page);
    }

    // ==================== 相容舊 API ====================

    /** 相容舊 API：等於 getPageInventory(uuid, 0) */
    public static ItemStack[] getInventory(UUID uuid) {
        return getPageInventory(uuid, 0);
    }

    /** 相容舊 API：等於 setPageInventory(uuid, 0, items) */
    public static void setInventory(UUID uuid, ItemStack[] items) {
        setPageInventory(uuid, 0, items);
    }

    /**
     * 相容舊 API：取得「目前頁」的最大格數（從 PageConfig 讀）
     */
    public static int getUnlockedSlots(UUID uuid) {
        PlayerCardData pcd = getOrLoad(uuid);
        PageConfig.Page page = PageConfig.getPage(pcd.currentPage);
        if (page != null) return Math.min(page.slots, MAX_SLOTS);
        return MAX_SLOTS;
    }

    // ==================== 全域卡片查詢 ====================

    /**
     * 取得玩家所有已解鎖頁的卡片
     */
    public static List<ItemStack> getCards(Player player) {
        PlayerCardData pcd = getOrLoad(player.getUniqueId());
        List<ItemStack> list = new ArrayList<>();
        for (int page = 0; page < pcd.unlockedPages; page++) {
            int unlockedSlots = pcd.unlockedSlots.getOrDefault(page, -1);
            if (unlockedSlots < 0) {
                PageConfig.Page cfg = PageConfig.getPage(page);
                unlockedSlots = cfg != null ? cfg.defaultSlots : 45;
            }
            ItemStack[] items = getPageInventory(player.getUniqueId(), page);
            for (int i = 0; i < unlockedSlots && i < items.length; i++) {
                ItemStack item = items[i];
                if (item == null) continue;
                if (item.getType().toString().contains("AIR")) continue;
                if (item.getType() == org.bukkit.Material.STAINED_GLASS_PANE) continue;
                list.add(item);
            }
        }
        return list;
    }

    /**
     * 從所有已解鎖頁移除指定卡片
     */
    public static void removeCard(UUID uuid, String cardId, int amount) {
        PlayerCardData pcd = getOrLoad(uuid);
        int removed = 0;
        for (int page = 0; page < pcd.unlockedPages; page++) {
            ItemStack[] items = getPageInventory(uuid, page);
            for (int i = 0; i < items.length; i++) {
                ItemStack item = items[i];
                if (item == null || item.getType().toString().contains("AIR")) continue;
                CardData card = CardDataManager.findCard(item);
                if (card == null || !card.getId().equals(cardId)) continue;
                if (amount == -1 || removed < amount) {
                    items[i] = null;
                    removed++;
                    if (amount != -1 && removed >= amount) break;
                }
            }
            if (amount != -1 && removed >= amount) break;
        }
        savePlayer(uuid);
    }

    /**
     * 計算所有已解鎖頁中指定卡片的數量
     */
    public static int countCard(UUID uuid, String cardId) {
        PlayerCardData pcd = getOrLoad(uuid);
        int count = 0;
        for (int page = 0; page < pcd.unlockedPages; page++) {
            ItemStack[] items = getPageInventory(uuid, page);
            for (ItemStack item : items) {
                if (item == null || item.getType().toString().contains("AIR")) continue;
                CardData card = CardDataManager.findCard(item);
                if (card != null && card.getId().equals(cardId)) {
                    count++;
                }
            }
        }
        return count;
    }

    // ==================== 儲存 ====================

    public static void savePlayer(UUID uuid) {
        PlayerCardData pcd = cache.get(uuid);
        if (pcd == null) return;

        String path = uuid.toString();

        // 清除舊格式
        data.set(path + ".items", null);

        // 寫入新格式
                data.set(path + ".unlockedPages", pcd.unlockedPages);
        // 寫入逐格解鎖
        data.set(path + ".unlockedSlots", null);
        for (Map.Entry<Integer, Integer> e : pcd.unlockedSlots.entrySet()) {
            data.set(path + ".unlockedSlots." + e.getKey(), e.getValue());
        }
        data.set(path + ".mergeCount", pcd.mergeCount);
        data.set(path + ".lastSeen", pcd.lastSeen);
        // 寫入卡片冷卻
        data.set(path + ".cardCooldowns", null);
        for (Map.Entry<String, CardCooldown> e : pcd.cardCooldowns.entrySet()) {
            String p = path + ".cardCooldowns." + e.getKey();
            data.set(p + ".lastUpgrade", e.getValue().lastUpgrade);
            data.set(p + ".todayDate", e.getValue().todayDate);
            data.set(p + ".todayCount", e.getValue().todayCount);
        }
        data.set(path + ".claimedSets", new ArrayList<>(pcd.claimedSets));
        for (Map.Entry<Integer, ItemStack[]> entry : pcd.pages.entrySet()) {
            data.set(path + ".pages." + entry.getKey(), Arrays.asList(entry.getValue()));
        }

        save();
    }

    public static void save() {
        try {
            data.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== 圖鑑 API ====================

    public static Set<String> getOwnedCardIds(UUID uuid) {
        Set<String> ids = new HashSet<>();
        PlayerCardData pcd = getOrLoad(uuid);
        for (int page = 0; page < pcd.unlockedPages; page++) {
            ItemStack[] items = getPageInventory(uuid, page);
            for (ItemStack item : items) {
                if (item == null || item.getType().toString().contains("AIR")) continue;
                CardData card = CardDataManager.findCard(item);
                if (card != null) ids.add(card.getId());
            }
        }
        return ids;
    }

    public static boolean hasClaimed(UUID uuid, String setId) {
        return getOrLoad(uuid).claimedSets.contains(setId);
    }

    public static void markClaimed(UUID uuid, String setId) {
        getOrLoad(uuid).claimedSets.add(setId);
    }

    public static boolean addCardToBag(UUID uuid, ItemStack card) {
        if (card == null) return false;
        PlayerCardData pcd = getOrLoad(uuid);
        for (int page = 0; page < pcd.unlockedPages; page++) {
            ItemStack[] items = getPageInventory(uuid, page);
            for (int i = 0; i < items.length; i++) {
                if (items[i] == null || items[i].getType().toString().contains("AIR")) {
                    items[i] = card.clone();
                    items[i].setAmount(1);
                    savePlayer(uuid);
                    return true;
                }
            }
        }
        return false;
    }

    public static int getMergeCount(UUID uuid) {
        return getOrLoad(uuid).mergeCount;
    }

    public static void incrementMergeCount(UUID uuid) {
        getOrLoad(uuid).mergeCount++;
    }

    /** 取得卡片冷卻物件（不存在會新建） */
    public static CardCooldown getCooldown(UUID uuid, String cardId) {
        PlayerCardData pcd = getOrLoad(uuid);
        CardCooldown cd = pcd.cardCooldowns.get(cardId);
        if (cd == null) {
            cd = new CardCooldown();
            pcd.cardCooldowns.put(cardId, cd);
        }
        return cd;
    }
    public static long getLastSeen(UUID uuid) { return getOrLoad(uuid).lastSeen; }
    public static void setLastSeen(UUID uuid, long time) { getOrLoad(uuid).lastSeen = time; }
    /** 取得玩家檔案中所有 UUID（從 data 讀，不依賴 cache） */
    public static java.util.List<String> getAllPlayerUUIDs() {
        java.util.List<String> list = new java.util.ArrayList<>();
        if (data == null) return list;
        for (String key : data.getKeys(false)) {
            list.add(key);
        }
        return list;
    }

    /** 從檔案讀指定玩家的已解鎖頁數（不經過 cache） */
    public static int getUnlockedPagesFromFile(String uuid) {
        return data.getInt(uuid + ".unlockedPages", 1);
    }

    /** 從檔案讀指定頁的卡片（不經過 cache） */
    @SuppressWarnings("unchecked")
    public static ItemStack[] getPageInventoryFromFile(String uuid, int page) {
        ItemStack[] arr = new ItemStack[MAX_SLOTS];
        java.util.List<?> list = data.getList(uuid + ".pages." + page);
        if (list == null) return arr;
        for (int i = 0; i < Math.min(list.size(), MAX_SLOTS); i++) {
            Object o = list.get(i);
            if (o instanceof ItemStack) arr[i] = (ItemStack) o;
        }
        return arr;
    }
    public static void unload(UUID uuid) {
        savePlayer(uuid);
        cache.remove(uuid);
    }

    // ==================== 逐格解锁 ====================

    public static int getPageUnlockedSlots(UUID uuid, int page) {
        PlayerCardData pcd = getOrLoad(uuid);
        return pcd.unlockedSlots.getOrDefault(page, -1);
    }

    public static void setPageUnlockedSlots(UUID uuid, int page, int count) {
        PlayerCardData pcd = getOrLoad(uuid);
        pcd.unlockedSlots.put(page, count);
    }

    public static int getOrInitPageUnlockedSlots(UUID uuid, int page, int defaultVal) {
        PlayerCardData pcd = getOrLoad(uuid);
        if (!pcd.unlockedSlots.containsKey(page)) {
            pcd.unlockedSlots.put(page, defaultVal);
            return defaultVal;
        }
        return pcd.unlockedSlots.get(page);
    }
}
