package com.longdrange.ldattribute.core.soulring.rate;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.data.PlayerModuleData;

import java.util.*;

/**
 * 玩家限时倍率数据
 * key: rate.<id>.value   → 倍率值
 *      rate.<id>.expire  → 过期时间戳（毫秒）
 */
public class RateData {

    public static final String MODULE = "soulring";

    public static class TempRate {
        public final String id;
        public final double value;
        public final long expire;

        public TempRate(String id, double value, long expire) {
            this.id = id; this.value = value; this.expire = expire;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= expire;
        }

        public long remainSeconds() {
            long r = (expire - System.currentTimeMillis()) / 1000;
            return r < 0 ? 0 : r;
        }
    }

    private final LDAttribute plugin;
    private final UUID uuid;
    private final PlayerModuleData raw;
    private final Map<String, TempRate> tempRates = new LinkedHashMap<>();

    public RateData(LDAttribute plugin, UUID uuid) {
        this.plugin = plugin;
        this.uuid = uuid;
        this.raw = plugin.getModuleDataManager().get(uuid, MODULE);
        loadFrom();
    }

    private void loadFrom() {
        tempRates.clear();
        for (Map.Entry<String, Object> e : raw.getRaw().entrySet()) {
            String key = e.getKey();
            if (!key.startsWith("rate.")) continue;
            try {
                String rest = key.substring("rate.".length());
                int idx = rest.lastIndexOf('.');
                if (idx < 0) continue;
                String id = rest.substring(0, idx);
                if (!rest.endsWith(".value")) continue;
                double val = raw.getDouble("rate." + id + ".value", 1.0);
                long exp = raw.getLong("rate." + id + ".expire", 0L);
                tempRates.put(id, new TempRate(id, val, exp));
            } catch (Exception ignored) {}
        }
    }

    /** 加/更新限时倍率 */
    public void setTempRate(String id, double value, long seconds) {
        long expire = System.currentTimeMillis() + seconds * 1000L;
        TempRate r = new TempRate(id, value, expire);
        tempRates.put(id, r);
        raw.set("rate." + id + ".value", value);
        raw.set("rate." + id + ".expire", expire);
    }

    /** 增加时长（已有则累加时间，否则新建） */
    public void addTempRateTime(String id, double value, long seconds) {
        TempRate old = tempRates.get(id);
        if (old != null && !old.isExpired()) {
            // 已有则累加，取较高倍率
            long newExpire = old.expire + seconds * 1000L;
            double newVal = Math.max(old.value, value);
            TempRate r = new TempRate(id, newVal, newExpire);
            tempRates.put(id, r);
            raw.set("rate." + id + ".value", newVal);
            raw.set("rate." + id + ".expire", newExpire);
        } else {
            setTempRate(id, value, seconds);
        }
    }

    /** 移除某限时倍率 */
    public void removeTempRate(String id) {
        tempRates.remove(id);
        raw.set("rate." + id + ".value", null);
        raw.set("rate." + id + ".expire", null);
    }

    /** 清理过期 */
    public void cleanup() {
        Iterator<Map.Entry<String, TempRate>> it = tempRates.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, TempRate> e = it.next();
            if (e.getValue().isExpired()) {
                it.remove();
                raw.set("rate." + e.getKey() + ".value", null);
                raw.set("rate." + e.getKey() + ".expire", null);
            }
        }
    }

    /** 所有有效限时倍率 */
    public List<TempRate> getActiveTempRates() {
        cleanup();
        return new ArrayList<>(tempRates.values());
    }

    /** 限时倍率中的最高值 */
    public double getBestTempRate() {
        double best = 1.0;
        for (TempRate r : getActiveTempRates()) {
            if (r.value > best) best = r.value;
        }
        return best;
    }

    public void save() {}
    public UUID getUuid() { return uuid; }
}