package com.longdrange.ldattribute.core.source;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.api.AttributeSource;
import com.longdrange.ldattribute.core.relic.RelicConfig;
import com.longdrange.ldattribute.core.relic.RelicData;
import com.longdrange.ldattribute.core.relic.RelicManager;
import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 遗物属性来源（含套装效果）
 */
public class RelicSource implements AttributeSource {

    private static final Set<String> warned = ConcurrentHashMap.newKeySet();

    @Override public String getName() { return "遗物"; }

    @Override
    public List<Entry> getEntries(Player player) {
        List<Entry> out = new ArrayList<>();
        try {
            LDAttribute plugin = LDAttribute.getInstance();
            if (plugin == null || plugin.getCoreManager() == null) return out;
            RelicManager mgr = plugin.getCoreManager().getRelicManager();
            if (mgr == null) return out;
            RelicData data = mgr.get(player);
            if (data == null) return out;

            // ① 每件遗物的独立属性
            for (Map.Entry<String, RelicData.RelicInstance> e : data.getAll().entrySet()) {
                String slotId = e.getKey();
                RelicData.RelicInstance ins = e.getValue();
                RelicConfig.RelicDef def = RelicConfig.getRelic(ins.relicId);
                if (def == null) continue;
                RelicConfig.SlotDef slot = RelicConfig.getSlot(slotId);

                Map<String, Double> merged = new LinkedHashMap<>();
                if (slot != null && slot.mainAttr != null && !slot.mainAttr.isEmpty()) {
                    merged.merge(slot.mainAttr, ins.mainValue, Double::sum);
                }
                for (RelicData.Sub s : ins.subs) {
                    if (s.attr == null || s.attr.isEmpty()) continue;
                    merged.merge(s.attr, s.value, Double::sum);
                }
                if (merged.isEmpty()) continue;

                LDAttributeData d = buildData(plugin, merged);
                if (d == null) continue;
                out.add(new Entry("遗物 " + def.id + " (" + slotId + ")", d));
            }

            // ② 套装效果
            Map<String, Integer> setCount = countSets(data);
            for (Map.Entry<String, Integer> se : setCount.entrySet()) {
                String setId = se.getKey();
                int count = se.getValue();
                RelicConfig.SetDef set = RelicConfig.getSet(setId);
                if (set == null) continue;

                // 收集所有生效的 bonus（2件、4件、6件...）
                Map<String, Double> merged = new LinkedHashMap<>();
                List<Integer> activated = new ArrayList<>();
                for (Map.Entry<Integer, List<String>> be : set.bonuses.entrySet()) {
                    int need = be.getKey();
                    if (count < need) continue;
                    activated.add(need);
                    for (String line : be.getValue()) {
                        parseBonusLine(line, merged);
                    }
                }
                if (merged.isEmpty()) continue;

                LDAttributeData d = buildData(plugin, merged);
                if (d == null) continue;
                String label = "套装 " + setId + " (" + count + "件 - " + activated + "件效果)";
                out.add(new Entry(label, d));
            }
        } catch (Throwable t) {
            LDAttribute.getInstance().getLogger().warning("[Relic] getEntries 失败: " + t);
        }
        return out;
    }

    /** 统计各套装件数 */
    public static Map<String, Integer> countSets(RelicData data) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (RelicData.RelicInstance ins : data.getAll().values()) {
            RelicConfig.RelicDef def = RelicConfig.getRelic(ins.relicId);
            if (def == null || def.set == null || def.set.isEmpty()) continue;
            out.merge(def.set, 1, Integer::sum);
        }
        return out;
    }

    /** 解析套装 bonus 行："攻击力: +100" 或 "攻击力: 100" */
    private static void parseBonusLine(String line, Map<String, Double> out) {
        if (line == null) return;
        try {
            int idx = line.indexOf(':');
            if (idx < 0) idx = line.indexOf('：');
            if (idx < 0) return;
            String attr = line.substring(0, idx).trim();
            String valStr = line.substring(idx + 1).replace("+", "").trim();
            double v = Double.parseDouble(valStr);
            if (!attr.isEmpty() && v != 0) out.merge(attr, v, Double::sum);
        } catch (Throwable ignored) {}
    }

    /** 从 Map<attrName, value> 构建 LDAttributeData */
    private static LDAttributeData buildData(LDAttribute plugin, Map<String, Double> merged) {
        LDAttributeData d = new LDAttributeData();
        boolean valid = false;
        for (Map.Entry<String, Double> me : merged.entrySet()) {
            LDSubAttribute target = findAttribute(d, me.getKey());
            if (target != null) {
                target.setAttributes(me.getValue());
                valid = true;
            } else {
                if (warned.add(me.getKey())) {
                    plugin.getLogger().warning("[Relic] 未知属性名: " + me.getKey()
                            + " （请检查 配置/遗物 的属性名是否与已注册属性一致）");
                }
            }
        }
        if (!valid) return null;
        d.valid();
        return d;
    }

    /** 精确查找属性：精确名 → 简繁归一化 → 语言别名 */
    private static LDSubAttribute findAttribute(LDAttributeData d, String key) {
        if (key == null) return null;
        for (LDSubAttribute a : d.getAttributeMap().values()) {
            if (a.getName().equalsIgnoreCase(key)) return a;
        }
        String simp = com.longdrange.ldattribute.util.ChineseConverter.toSimplified(key);
        for (LDSubAttribute a : d.getAttributeMap().values()) {
            if (com.longdrange.ldattribute.util.ChineseConverter.toSimplified(a.getName()).equalsIgnoreCase(simp)) return a;
        }
        for (LDSubAttribute a : d.getAttributeMap().values()) {
            try {
                List<String> aliases = com.longdrange.ldattribute.util.LanguageManager.getAliases(a.getName());
                for (String al : aliases) {
                    if (al.equalsIgnoreCase(key)) return a;
                    if (com.longdrange.ldattribute.util.ChineseConverter.toSimplified(al).equalsIgnoreCase(simp)) return a;
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }
}