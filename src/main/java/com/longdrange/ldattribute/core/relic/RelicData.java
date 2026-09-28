package com.longdrange.ldattribute.core.relic;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.data.PlayerModuleData;

import java.util.*;

/**
 * 玩家遗物数据
 * 每个槽位一个 RelicInstance
 *
 * 序列化格式：
 *   relicId|mainValue|level|locked|attr1:val1,attr2:val2,...
 */
public class RelicData {

    public static final String MODULE = "relic";

    /** 一条副属性 */
    public static class Sub {
        public String attr;
        public double value;
        public Sub(String attr, double value) { this.attr = attr; this.value = value; }
    }

    /** 遗物实例 */
    public static class RelicInstance {
        public String relicId;
        public double mainValue;
        public List<Sub> subs = new ArrayList<>();
        public int level = 0;
        public boolean locked = false;    // 锁定状态
        public RelicInstance(String relicId) { this.relicId = relicId; }
    }

    private final LDAttribute plugin;
    private final UUID uuid;
    private final PlayerModuleData raw;
    private final Map<String, RelicInstance> equipped = new LinkedHashMap<>();

    public RelicData(LDAttribute plugin, UUID uuid) {
        this.plugin = plugin;
        this.uuid = uuid;
        this.raw = plugin.getModuleDataManager().get(uuid, MODULE);
        loadFrom(raw);
    }

    private void loadFrom(PlayerModuleData data) {
        equipped.clear();
        for (String slotId : RelicConfig.getAllSlotIds()) {
            String key = "slot." + slotId;
            if (!data.has(key)) continue;
            try {
                String s = data.getString(key, "");
                if (s == null || s.isEmpty()) continue;
                // 兼容 4 段（旧）和 5 段（新，含 locked）格式
                String[] parts = s.split("\\|", 5);
                RelicInstance ins = new RelicInstance(parts[0]);
                if (parts.length > 1) ins.mainValue = Double.parseDouble(parts[1]);
                if (parts.length > 2) ins.level = Integer.parseInt(parts[2]);
                int subIdx = 3;
                if (parts.length >= 5) {
                    try { ins.locked = Boolean.parseBoolean(parts[3]); } catch (Throwable ignored) {}
                    subIdx = 4;
                }
                if (parts.length > subIdx && !parts[subIdx].isEmpty()) {
                    for (String sub : parts[subIdx].split(",")) {
                        String[] sp = sub.split(":");
                        if (sp.length == 2) {
                            ins.subs.add(new Sub(sp[0], Double.parseDouble(sp[1])));
                        }
                    }
                }
                equipped.put(slotId, ins);
            } catch (Throwable ignored) {}
        }
    }

    public void save() {
        for (String slotId : RelicConfig.getAllSlotIds()) {
            RelicInstance ins = equipped.get(slotId);
            if (ins == null) {
                raw.set("slot." + slotId, null);
                continue;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(ins.relicId).append("|")
              .append(ins.mainValue).append("|")
              .append(ins.level).append("|")
              .append(ins.locked).append("|");
            for (int i = 0; i < ins.subs.size(); i++) {
                if (i > 0) sb.append(",");
                Sub sub = ins.subs.get(i);
                sb.append(sub.attr).append(":").append(sub.value);
            }
            raw.set("slot." + slotId, sb.toString());
        }
    }

    public RelicInstance get(String slotId) { return equipped.get(slotId); }
    public Map<String, RelicInstance> getAll() { return equipped; }

    public void equip(String slotId, RelicInstance ins) {
        equipped.put(slotId, ins);
        save();
    }

    public RelicInstance unequip(String slotId) {
        RelicInstance old = equipped.remove(slotId);
        save();
        return old;
    }

    public UUID getUuid() { return uuid; }
}