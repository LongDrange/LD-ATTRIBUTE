package com.longdrange.ldattribute.pet;

/**
 * 宠物实例（玩家拥有的某只宠物）
 */
public class PetInstance {

    public String petId;
    public int level;
    public int exp;

    public PetInstance(String petId, int level, int exp) {
        this.petId = petId;
        this.level = level;
        this.exp = exp;
    }

    public PetConfig.Pet getDef() {
        return PetConfig.get(petId);
    }

    /** 加经验，返回升级次数 */
    public int addExp(int delta) {
        PetConfig.Pet def = getDef();
        if (def == null) return 0;
        exp += delta;
        int ups = 0;
        while (level < def.maxLevel) {
            int req = PetConfig.getRequiredExp(petId, level);
            if (exp < req) break;
            exp -= req;
            level++;
            ups++;
        }
        if (level >= def.maxLevel) exp = 0;
        return ups;
    }

    public boolean isMaxLevel() {
        PetConfig.Pet def = getDef();
        return def != null && level >= def.maxLevel;
    }
}