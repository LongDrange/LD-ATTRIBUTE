package com.longdrange.ldattribute.data.eventdata;

import org.bukkit.entity.LivingEntity;

/**
 * 屬性更新事件資料
 * UPDATE 類型屬性會收到此物件
 */
public class LDUpdateEventData extends LDEventData {

    public LDUpdateEventData(LivingEntity entity) {
        super(entity);
    }
}