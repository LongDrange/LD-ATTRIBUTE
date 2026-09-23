package com.longdrange.ldattribute.data.attribute;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 屬性管理器
 *
 * 負責：
 *   - 維護全局屬性表（所有已註冊屬性）
 *   - 從物品 / Lore 讀取屬性
 *   - 計算實體所有屬性
 *   - 投射物資料暫存
 */
public class LDAttributeManager {

    /** 全局屬性表（優先級 → 屬性實例） */
    private static final LDAttributeMap ATTRIBUTE_MAP = new LDAttributeMap();

    private final LDAttribute plugin;

    /** 投射物資料暫存（弓箭、雪球等） */
    private final Map<UUID, LDAttributeData> projectileDataMap = new HashMap<>();

    public LDAttributeManager(LDAttribute plugin) {
        this.plugin = plugin;
    }

    // ==================== 全局屬性表 ====================

    /**
     * 取得全局屬性表（靜態，供 LDSubAttribute.register() 使用）
     */
    public static LDAttributeMap getAttributeMap() {
        return ATTRIBUTE_MAP;
    }

    /**
     * 複製一份全局屬性表
     * 用於建立新的 LDAttributeData
     */
    public static Map<Integer, LDSubAttribute> cloneAttributeMap() {
        Map<Integer, LDSubAttribute> map = new TreeMap<>();
        for (Map.Entry<Integer, LDSubAttribute> entry : ATTRIBUTE_MAP.entrySet()) {
            LDSubAttribute copy = entry.getValue().newAttribute();
            if (copy != null) {
                map.put(entry.getKey(), copy);
            }
        }
        return map;
    }

    // ==================== 實體資料 ====================

    /**
     * 計算實體所有屬性（裝備 + 手持 + API 附加 + 自身）
     */
    public LDAttributeData getEntityData(LivingEntity entity, LDAttributeData... extras) {
        LDAttributeData data = new LDAttributeData();
        for (LDAttributeData extra : extras) {
            data.add(extra);
        }
        return data;
    }

    /**
     * 從物品讀取屬性
     */
    public LDAttributeData getItemData(LivingEntity entity, LDAttributeType type, ItemStack... items) {
        LDAttributeData data = new LDAttributeData();
        for (ItemStack item : items) {
            if (item == null) continue;
            if (!item.hasItemMeta()) continue;
            if (!item.getItemMeta().hasLore()) continue;
            List<String> lore = item.getItemMeta().getLore();
            data.add(getListData(entity, type, lore));
        }
        return data;
    }

    /**
     * 從 Lore 讀取屬性
     */
    public LDAttributeData getListData(LivingEntity entity, LDAttributeType type, List<String> lore) {
        LDAttributeData data = new LDAttributeData();
        for (String line : lore) {
            for (LDSubAttribute attr : data.getAttributeMap().values()) {
                if (type != null && !attr.containsType(type)) continue;
                if (attr.loadAttribute(line)) {
                    data.valid();
                }
            }
        }
        return data;
    }

    // ==================== 更新 ====================

    /**
     * 更新實體所有屬性
     * 目前先留空，後續擴充 UPDATE 類型屬性
     */
    public void updateStats(LivingEntity entity) {
        // 之後會在這裡觸發 UPDATE 類型屬性的 eventMethod
    }

    public void updateEquipmentData(LivingEntity entity) {
        updateStats(entity);
    }

    public void updateHandData(LivingEntity entity) {
        updateStats(entity);
    }

    public void updateSlotData(Player player) {
        updateStats(player);
    }

    // ==================== 投射物 ====================

    public void setProjectileData(UUID uuid, LDAttributeData data) {
        projectileDataMap.put(uuid, data);
    }

    public LDAttributeData getProjectileData(UUID uuid) {
        return projectileDataMap.get(uuid);
    }

    // ==================== 儲存 ====================

    /**
     * 儲存所有玩家資料（後續擴充）
     */
    public void saveAll() {
        // 之後會在這裡儲存玩家屬性資料到檔案
    }
}