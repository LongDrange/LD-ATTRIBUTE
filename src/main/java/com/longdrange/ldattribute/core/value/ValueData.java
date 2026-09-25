package com.longdrange.ldattribute.core.value;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.data.PlayerModuleData;

import java.util.*;

public class ValueData {

    public static final String MODULE = "value";

    private final UUID uuid;
    private final PlayerModuleData raw;

    public ValueData(LDAttribute plugin, UUID uuid) {
        this.uuid = uuid;
        this.raw = plugin.getModuleDataManager().get(uuid, MODULE);
    }

    public double get(String id) {
        ValueConfig.ValueDef def = ValueConfig.get(id);
        double defVal = def == null ? 0 : def.defaultValue;
        return raw.getDouble("v." + id, defVal);
    }

    /** 设置值（自动 clamp 到 min/max） */
    public void set(String id, double value) {
        ValueConfig.ValueDef def = ValueConfig.get(id);
        if (def != null) {
            if (value > def.max) value = def.max;
            if (value < def.min) value = def.min;
        }
        raw.set("v." + id, value);
    }

    public void add(String id, double amount) { set(id, get(id) + amount); }
    public void take(String id, double amount) { set(id, get(id) - amount); }

    /** 取剩余可增加空间 */
    public double remaining(String id) {
        ValueConfig.ValueDef def = ValueConfig.get(id);
        if (def == null) return Double.MAX_VALUE;
        return Math.max(0, def.max - get(id));
    }

    /** 自动恢复到 regenMax */
    public boolean regen(String id) {
        ValueConfig.ValueDef def = ValueConfig.get(id);
        if (def == null || !def.hasRegen()) return false;
        double cur = get(id);
        if (cur >= def.regenMax) return false;
        double next = Math.min(def.regenMax, cur + def.regenAmount);
        if (next == cur) return false;
        set(id, next);
        return true;
    }

    public Map<String, Double> getAll() {
        Map<String, Double> out = new LinkedHashMap<>();
        for (ValueConfig.ValueDef def : ValueConfig.all()) out.put(def.id, get(def.id));
        return out;
    }

    public UUID getUuid() { return uuid; }
}