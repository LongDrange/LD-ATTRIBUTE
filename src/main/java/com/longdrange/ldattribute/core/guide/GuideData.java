package com.longdrange.ldattribute.core.guide;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.data.PlayerModuleData;

import java.util.UUID;

public class GuideData {

    public static final String MODULE = "guide";

    private final UUID uuid;
    private final PlayerModuleData raw;

    public GuideData(LDAttribute plugin, UUID uuid) {
        this.uuid = uuid;
        this.raw = plugin.getModuleDataManager().get(uuid, MODULE);
    }

    public int getKills(String mobId) { return raw.getInt("kills." + mobId, 0); }
    public void setKills(String mobId, int n) { raw.set("kills." + mobId, Math.max(0, n)); }

    public int addKills(String mobId, int n) {
        int cur = getKills(mobId) + n;
        setKills(mobId, cur);
        return cur;
    }

    public boolean isUnlocked(String mobId) { return raw.getBoolean("unlocked." + mobId, false); }
    public void setUnlocked(String mobId, boolean unlocked) { raw.set("unlocked." + mobId, unlocked); }

    public boolean isClaimed(String mobId) { return raw.getBoolean("claimed." + mobId, false); }
    public void setClaimed(String mobId, boolean claimed) { raw.set("claimed." + mobId, claimed); }

    public UUID getUuid() { return uuid; }
}