package com.longdrange.ldattribute.data.eventdata;

import org.bukkit.entity.LivingEntity;

/**
 * 事件資料抽象基類
 * 所有屬性事件（傷害、更新…）都繼承這個類
 */
public abstract class LDEventData {

    private final LivingEntity entity;

    public LDEventData(LivingEntity entity) {
        this.entity = entity;
    }

    /**
     * 取得事件關聯的實體
     * 對傷害事件來說，這個是「攻擊者」
     */
    public LivingEntity getEntity() {
        return entity;
    }
}