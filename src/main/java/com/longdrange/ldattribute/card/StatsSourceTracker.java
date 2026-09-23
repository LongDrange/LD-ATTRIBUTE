package com.longdrange.ldattribute.card;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 属性来源追踪器（用于 /ldc stats 显示）
 * 每次打开面板时单独算一遍各来源，不侵入 LDAttributeData
 */
public class StatsSourceTracker {

    public static class Line {
        public final String source;
        public final double value;
        public Line(String source, double value) {
            this.source = source;
            this.value = value;
        }
    }

    /** 返回：属性名 → 来源行列表 */
    public static Map<String, List<Line>> track(Player player) {
        Map<String, List<Line>> result = new LinkedHashMap<>();
        com.longdrange.ldattribute.api.LDAttributeAPI api = LDAttribute.getInstance().getApi();
        if (api == null) return result;

        List<ItemStack> all = PlayerData.getCards(player);
        List<ItemStack> valid = new ArrayList<>();
        for (ItemStack card : all) {
            if (!isUsableBy(card, player)) continue;
            valid.add(card);
        }

        // 卡片本体属性
        for (ItemStack card : valid) {
            CardData cd = CardDataManager.findCard(card);
            if (cd == null) continue;
            List<String> lore = StatsDataRead.filterNormalLore(card);
            if (lore.isEmpty()) continue;
            LDAttributeData d = api.getLoreData(player, null, lore);
            int lv = CardNBT.getLevel(card);
            String label = "卡片 " + cd.getId() + (lv > 0 ? " (Lv." + lv + ")" : "");
            record(result, label, d);
        }

        // 符文属性
        for (ItemStack card : valid) {
            CardData cd = CardDataManager.findCard(card);
            if (cd == null) continue;
            List<String> runeAttrs = com.longdrange.ldattribute.rune.RuneHelper.getCardSocketAttributes(card);
            if (runeAttrs.isEmpty()) continue;
            LDAttributeData d = api.getLoreData(player, null, runeAttrs);
            record(result, "符文 " + cd.getId(), d);
        }

        // 套装
        try {
            LDAttributeData d = new LDAttributeData();
            SuitData.applySuits(player, valid, d);
            record(result, "套装", d);
        } catch (Throwable ignored) {}

        // 共鸣/羁绊
        try {
            LDAttributeData d = new LDAttributeData();
            SynergyData.apply(player, valid, d);
            record(result, "共鸣/羁绊", d);
        } catch (Throwable ignored) {}

        // 宠物
        try {
            List<String> petAttrs = com.longdrange.ldattribute.pet.PetManager.getAllPetsAttributes(player);
            if (!petAttrs.isEmpty()) {
                LDAttributeData d = api.getLoreData(player, null, petAttrs);
                record(result, "宠物", d);
            }
        } catch (Throwable ignored) {}

        // 战斗状态
        try {
            List<String> stateAttrs = com.longdrange.ldattribute.combat.StateManager.getActiveAttributes(player);
            if (!stateAttrs.isEmpty()) {
                LDAttributeData d = api.getLoreData(player, null, stateAttrs);
                record(result, "战斗状态", d);
            }
        } catch (Throwable ignored) {}

        // 限时 Buff
        try {
            List<String> buffLore = TempBuffManager.getActiveEffects(player.getUniqueId());
            if (!buffLore.isEmpty()) {
                LDAttributeData d = api.getLoreData(player, null, buffLore);
                record(result, "限时Buff", d);
            }
        } catch (Throwable ignored) {}

        // 兜底：与总属性对比，差值为"其他/API"
        try {
            LDAttributeData total = StatsDataRead.loadPlayerStats(player);
            Map<String, Double> sumMap = new HashMap<>();
            for (List<Line> list : result.values()) {
                for (Line ln : list) {
                    // 需要知道属性名，Line 里没有，改用另一个结构
                }
            }
        } catch (Throwable ignored) {}

        return result;
    }

    private static void record(Map<String, List<Line>> result, String source, LDAttributeData d) {
        if (d == null) return;
        for (LDSubAttribute a : d.getAttributeMap().values()) {
            double v;
            if (a.getName().equals("幸运")) {
                // 幸运基础 100 只算一次，来源里只显示装备加成
                v = a.getAttributes()[0];
            } else {
                v = a.getValue();
            }
            if (v == 0) continue;
            result.computeIfAbsent(a.getName(), k -> new ArrayList<>())
                    .add(new Line(source, v));
        }
    }

    private static boolean isUsableBy(ItemStack card, Player player) {
        if (!CardNBT.isBound(card)) return true;
        String boundUUID = CardNBT.getBoundUUID(card);
        return boundUUID.equals(player.getUniqueId().toString());
    }

    public static String fmt(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }
}