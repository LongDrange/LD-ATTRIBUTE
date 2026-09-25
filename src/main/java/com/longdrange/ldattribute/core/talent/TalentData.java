package com.longdrange.ldattribute.core.talent;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.data.PlayerModuleData;

import java.util.*;

public class TalentData {

    public static final String MODULE = "talent";

    private final LDAttribute plugin;
    private final UUID uuid;
    private final PlayerModuleData raw;

    public TalentData(LDAttribute plugin, UUID uuid) {
        this.plugin = plugin;
        this.uuid = uuid;
        this.raw = plugin.getModuleDataManager().get(uuid, MODULE);
    }

    // ==================== 点数 ====================

    public int getPoints(String pageId) {
        return raw.getInt("points." + pageId, 0);
    }
    public void setPoints(String pageId, int amount) {
        raw.set("points." + pageId, Math.max(0, amount));
    }
    public void addPoints(String pageId, int amount) {
        setPoints(pageId, getPoints(pageId) + amount);
    }
    public void takePoints(String pageId, int amount) {
        setPoints(pageId, Math.max(0, getPoints(pageId) - amount));
    }

    // ==================== 天赋等级 ====================

    public int getLevel(String talentId) {
        return raw.getInt("level." + talentId, 0);
    }
    public void setLevel(String talentId, int level) {
        if (level <= 0) raw.set("level." + talentId, null);
        else raw.set("level." + talentId, level);
    }

    public boolean hasAnyRequirementUnmet(TalentConfig.TalentDef def) {
        for (String req : def.requires) {
            if (getLevel(req) <= 0) return true;
        }
        return false;
    }

    public void save() { /* raw 自动持久化 */ }
    public UUID getUuid() { return uuid; }
}