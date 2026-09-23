package com.longdrange.ldattribute.api;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import com.longdrange.ldattribute.data.attribute.LDAttributeType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LD-Attribute 對外 API
 * 結構參考 SX-Attribute 的 SXAttributeAPI，方便 TCardStats 等插件遷移
 *
 * 取得方式：
 *   LDAttributeAPI api = Bukkit.getServicesManager().load(LDAttributeAPI.class);
 */
public class LDAttributeAPI {

    /** 每個玩家、每個插件儲存的屬性資料 */
    private final Map<UUID, Map<Class<?>, LDAttributeData>> map = new ConcurrentHashMap<>();
    private final LDAttribute plugin;

    public LDAttributeAPI(LDAttribute plugin) {
        this.plugin = plugin;
    }

    // ==================== 查詢 ====================

    /**
     * 取得某玩家所有插件附加的屬性總和
     */
    public LDAttributeData getAPIStats(UUID uuid) {
        LDAttributeData data = new LDAttributeData();
        if (map.containsKey(uuid)) {
            for (Class<?> c : map.get(uuid).keySet()) {
                data.add(map.get(uuid).get(c));
            }
        }
        return data;
    }

    /**
     * 取得實體全部屬性（裝備 + 手持 + 附加）
     */
    public LDAttributeData getEntityAllData(LivingEntity entity, LDAttributeData... extras) {
        return plugin.getManager().getEntityData(entity, extras);
    }

    /**
     * 取得某插件儲存的屬性
     */
    public LDAttributeData getEntityAPIData(Class<?> c, UUID uuid) {
        return map.containsKey(uuid) ? map.get(uuid).get(c) : null;
    }

    public boolean isEntityAPIData(Class<?> c, UUID uuid) {
        return map.containsKey(uuid) && map.get(uuid).containsKey(c);
    }

    /**
     * 從物品讀取屬性
     */
    public LDAttributeData getItemData(LivingEntity entity, LDAttributeType type, ItemStack... items) {
        return plugin.getManager().getItemData(entity, type, items);
    }

    /**
     * 從 Lore 讀取屬性
     */
    public LDAttributeData getLoreData(LivingEntity entity, LDAttributeType type, java.util.List<String> lore) {
        return plugin.getManager().getListData(entity, type, lore);
    }

    // ==================== 寫入 ====================

    /**
     * 儲存插件附加的屬性
     */
    public void setEntityAPIData(Class<?> c, UUID uuid, LDAttributeData data) {
        map.computeIfAbsent(uuid, k -> new HashMap<>()).put(c, data);
    }
    public LDAttributeData removeEntityAPIData(Class<?> c, UUID uuid) {
        if (map.containsKey(uuid) && map.get(uuid).containsKey(c)) {
            return map.get(uuid).remove(c);
        }
        return null;
    }

    public void removePluginAllEntityData(Class<?> c) {
        map.values().forEach(m -> m.remove(c));
    }

    public void removeEntityAllPluginData(UUID uuid) {
        map.remove(uuid);
    }

    // ==================== 更新觸發 ====================

    public void updateEquipmentData(LivingEntity entity) {
        plugin.getManager().updateEquipmentData(entity);
    }

    public void updateHandData(LivingEntity entity) {
        plugin.getManager().updateHandData(entity);
    }

    public void updateSlotData(Player player) {
        plugin.getManager().updateSlotData(player);
    }

    public void updateStats(LivingEntity entity) {
        plugin.getManager().updateStats(entity);
    }

    // ==================== 插件資訊 ====================

    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    public LDAttribute getPlugin() {
        return plugin;
    }
}