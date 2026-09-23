package com.longdrange.ldattribute.data.attribute;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 屬性資料容器
 * 每個實體、每個物品都會有一個 LDAttributeData
 *
 * 內部持有一個「屬性映射表」，可以：
 *   - 與其他容器合併（add）
 *   - 從字串載入（loadFromString）
 *   - 序列化為字串（saveToString）
 *   - 計算總值（calculationValue）
 */
public class LDAttributeData {

    /** 總值（所有屬性 getValue() 的總和） */
    private double value = 0.0;

    /** 是否有效（至少載入過一個屬性） */
    private boolean valid = false;

    /** 屬性映射表（優先級 → 屬性實例） */
    private Map<Integer, LDSubAttribute> attributeMap = LDAttributeManager.cloneAttributeMap();

    /**
     * 依名稱取得屬性
     */
    public LDSubAttribute getSubAttribute(String attributeName) {
        for (LDSubAttribute sub : this.attributeMap.values()) {
            if (sub.getName().equalsIgnoreCase(attributeName)) {
                return sub;
            }
        }
        return null;
    }

    /**
     * 標記為有效
     */
    public void valid() {
        this.valid = true;
    }

    public boolean isValid() {
        return valid;
    }

    /**
     * 合併另一個資料容器
     * 會把 other 的每個屬性數值加到自己的對應屬性上
     */
    public LDAttributeData add(LDAttributeData other) {
        if (other == null || !other.isValid()) {
            return this;
        }
        this.valid();
        Iterator<LDSubAttribute> it = this.attributeMap.values().iterator();
        Iterator<LDSubAttribute> addIt = other.attributeMap.values().iterator();
        while (it.hasNext() && addIt.hasNext()) {
            it.next().addAttribute(addIt.next().getAttributes());
        }
        return this;
    }

    /**
     * 修正所有屬性數值（負數歸零）
     */
    void correct() {
        attributeMap.values().forEach(LDSubAttribute::correct);
    }

    /**
     * 計算總值
     */
    public double calculationValue() {
        this.value = 0.0;
        attributeMap.values().forEach(a -> this.value += a.getValue());
        return this.value;
    }

    public double getValue() {
        return value;
    }

    /**
     * 從字串載入（格式：屬性1#值//屬性2#值）
     */
    public LDAttributeData loadFromString(String string) {
        return loadFromList(Arrays.asList(string.split("//")));
    }

    /**
     * 從字串列表載入
     */
    public LDAttributeData loadFromList(List<String> list) {
        list.forEach(str -> attributeMap.values().forEach(a -> a.loadFromString(str)));
        return this;
    }

    /**
     * 序列化為字串（格式：屬性1#值//屬性2#值）
     */
    public String saveToString() {
        List<String> list = saveToList();
        return IntStream.range(0, list.size())
                .mapToObj(i -> i == list.size() - 1 ? list.get(i) : list.get(i) + "//")
                .collect(Collectors.joining());
    }

    /**
     * 序列化為字串列表
     */
    public List<String> saveToList() {
        return attributeMap.values().stream()
                .map(LDSubAttribute::saveToString)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Map<Integer, LDSubAttribute> getAttributeMap() {
        return attributeMap;
    }
}