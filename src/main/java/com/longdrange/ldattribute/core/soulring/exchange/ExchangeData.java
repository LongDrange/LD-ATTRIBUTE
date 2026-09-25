package com.longdrange.ldattribute.core.soulring.exchange;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.data.PlayerModuleData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class ExchangeData {

    public static final String MODULE = "soulring";

    private final PlayerModuleData raw;

    public ExchangeData(LDAttribute plugin, UUID uuid) {
        this.raw = plugin.getModuleDataManager().get(uuid, MODULE);
    }

    private static String today() {
        return new SimpleDateFormat("yyyyMMdd").format(new Date());
    }

    public int getDailyCount(String exchangeId) {
        return raw.getInt("exchange." + exchangeId + ".daily." + today(), 0);
    }

    public int getTotalCount(String exchangeId) {
        return raw.getInt("exchange." + exchangeId + ".total", 0);
    }

    public void addCount(String exchangeId, int amount) {
        String dailyKey = "exchange." + exchangeId + ".daily." + today();
        String totalKey = "exchange." + exchangeId + ".total";
        raw.set(dailyKey, getDailyCount(exchangeId) + amount);
        raw.set(totalKey, getTotalCount(exchangeId) + amount);
    }

    public boolean canExchange(ExchangeConfig.Exchange ex) {
        if (ex.dailyLimit > 0 && getDailyCount(ex.id) >= ex.dailyLimit) return false;
        if (ex.totalLimit > 0 && getTotalCount(ex.id) >= ex.totalLimit) return false;
        return true;
    }

    public int remainTimes(ExchangeConfig.Exchange ex) {
        int daily = ex.dailyLimit > 0 ? Math.max(0, ex.dailyLimit - getDailyCount(ex.id)) : Integer.MAX_VALUE;
        int total = ex.totalLimit > 0 ? Math.max(0, ex.totalLimit - getTotalCount(ex.id)) : Integer.MAX_VALUE;
        int min = Math.min(daily, total);
        return min == Integer.MAX_VALUE ? -1 : min;
    }
}