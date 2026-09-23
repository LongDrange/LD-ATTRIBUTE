package com.longdrange.ldattribute.data.attribute;

import java.util.TreeMap;

/**
 * 屬性優先級映射表
 * key   = 優先級（越小越先執行）
 * value = 屬性實例
 *
 * 使用 TreeMap 自動按 key 排序
 */
public class LDAttributeMap extends TreeMap<Integer, LDSubAttribute> {

    @Override
    public LDSubAttribute put(Integer key, LDSubAttribute value) {
        return super.put(key, value);
    }
}