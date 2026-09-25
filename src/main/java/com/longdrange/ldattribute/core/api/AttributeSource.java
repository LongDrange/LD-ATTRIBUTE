package com.longdrange.ldattribute.core.api;

import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * 属性来源接口
 * 任何模块只要实现这个接口并注册，就会自动出现在 /ldc stats 和属性面板里。
 */
public interface AttributeSource {

    /** 大类名（用于日志/调试），如 "魂珠空间" / "卡片" */
    String getName();

    /** 返回该玩家此来源的全部条目（每一条有自己的 label 和属性数据） */
    List<Entry> getEntries(Player player);

    /** 是否启用，默认 true */
    default boolean isEnabled() { return true; }

    /** 单条来源 */
    final class Entry {
        public final String label;                 // 具体标签，如 "卡片 xxx (Lv.5)"
        public final LDAttributeData data;
        public Entry(String label, LDAttributeData data) {
            this.label = label;
            this.data = data;
        }
    }
}