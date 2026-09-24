package com.longdrange.ldattribute.util;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.stream.Collectors;

/**
 * PlaceholderAPI 佔位符擴展
 *
 * 使用方式：
 *   %ldattr_攻擊力%              → 玩家的攻擊力值
 *   %ldattr_暴擊%                → 暴擊（預設格式：機率/倍率）
 *   %ldattr_暴擊_chance%         → 暴擊機率
 *   %ldattr_暴擊_multiplier%     → 暴擊倍率
 *   %ldattr_list%                → 所有屬性名稱（逗號分隔）
 */
public class Placeholders extends PlaceholderExpansion {

    private final LDAttribute plugin;

    public Placeholders(LDAttribute plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "ldattr";
    }

    @Override
    public String getAuthor() {
        return "LongDrange";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) return "";

        String p2 = params.toLowerCase();

        // ===== 排行榜占位符 =====
        if (p2.startsWith("top_cards_") || p2.startsWith("top_points_") ||
            p2.startsWith("top_pets_") || p2.startsWith("top_runes_")) {
            String[] parts = p2.split("_");
            if (parts.length < 3) return "-";
            String kind = parts[1];
            int rank;
            try { rank = Integer.parseInt(parts[2]); } catch (Exception e) { return "-"; }
            boolean wantValue = parts.length >= 4 && parts[3].equals("value");
            java.util.List<java.util.Map.Entry<String, Integer>> sorted = new java.util.ArrayList<>();
            if (kind.equals("cards") || kind.equals("points")) {
                for (org.bukkit.OfflinePlayer op : org.bukkit.Bukkit.getOfflinePlayers()) {
                    if (op == null || op.getUniqueId() == null) continue;
                    int v = 0;
                    try {
                        if (kind.equals("cards")) {
                            v = com.longdrange.ldattribute.card.PlayerData.getUnlockedPagesFromFile(op.getUniqueId().toString());
                        } else {
                            String nm = op.getName();
                            if (nm == null) continue;
                            v = com.longdrange.ldattribute.points.PointAPI.getPlayerPoints(nm);
                        }
                    } catch (Throwable ignored) {}
                    if (v > 0) sorted.add(new java.util.AbstractMap.SimpleEntry<>(op.getUniqueId().toString(), v));
                }
            } else if (kind.equals("pets")) {
                for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    int v = 0;
                    try {
                        java.util.List<com.longdrange.ldattribute.pet.PetInstance> ps =
                                com.longdrange.ldattribute.pet.PetData.getAllPets(p.getUniqueId());
                        if (ps != null) v = ps.size();
                    } catch (Throwable ignored) {}
                    if (v > 0) sorted.add(new java.util.AbstractMap.SimpleEntry<>(p.getUniqueId().toString(), v));
                }
            } else {
                for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    int v = 0;
                    try { v = com.longdrange.ldattribute.rune.RuneData.count(p.getUniqueId()); }
                    catch (Throwable ignored) {}
                    if (v > 0) sorted.add(new java.util.AbstractMap.SimpleEntry<>(p.getUniqueId().toString(), v));
                }
            }
            sorted.sort((a, b) -> b.getValue() - a.getValue());
            if (rank < 1 || rank > sorted.size()) return wantValue ? "0" : "-";
            java.util.Map.Entry<String, Integer> entry = sorted.get(rank - 1);
            if (wantValue) return String.valueOf(entry.getValue());
            try {
                org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(entry.getKey()));
                return op != null && op.getName() != null ? op.getName() : entry.getKey().substring(0, 8);
            } catch (Throwable t) { return entry.getKey().substring(0, 8); }
        }

        if (p2.equals("rank_points") || p2.equals("点数排名")) {
            try {
                String myName = player.getName();
                int my = com.longdrange.ldattribute.points.PointAPI.getPlayerPoints(myName);
                int rank = 1;
                for (org.bukkit.OfflinePlayer op : org.bukkit.Bukkit.getOfflinePlayers()) {
                    if (op == null || op.getName() == null || op.getName().equals(myName)) continue;
                    if (com.longdrange.ldattribute.points.PointAPI.getPlayerPoints(op.getName()) > my) rank++;
                }
                return String.valueOf(rank);
            } catch (Throwable t) { return "-"; }
        }

        if (p2.equals("synergy_active") || p2.equals("已激活共鸣")) {
            try {
                java.util.List<org.bukkit.inventory.ItemStack> cards =
                        com.longdrange.ldattribute.card.PlayerData.getCards(player);
                int count = 0;
                for (com.longdrange.ldattribute.card.SynergyData.Synergy s :
                        com.longdrange.ldattribute.card.SynergyData.getAllResonances()) {
                    if (com.longdrange.ldattribute.card.SynergyData.getMatched(cards, s) >= s.required) count++;
                }
                for (com.longdrange.ldattribute.card.SynergyData.Synergy s :
                        com.longdrange.ldattribute.card.SynergyData.getAllBonds()) {
                    if (com.longdrange.ldattribute.card.SynergyData.getMatched(cards, s) >= s.required) count++;
                }
                return String.valueOf(count);
            } catch (Throwable t) { return "0"; }
        }
        // ===== 自定义占位符（数据统计 / 卡片等级 / 共鸣状态）=====
        if (p2.equals("cards") || p2.equals("卡片数")) {
            try { return String.valueOf(com.longdrange.ldattribute.card.PlayerData.getCards(player).size()); }
            catch (Throwable t) { return "0"; }
        }
        if (p2.equals("points") || p2.equals("点数")) {
            try { return String.valueOf(com.longdrange.ldattribute.points.PointAPI.getPlayerPoints(player.getName())); }
            catch (Throwable t) { return "0"; }
        }
        if (p2.equals("pets") || p2.equals("宠物数")) {
            try {
                java.util.List<com.longdrange.ldattribute.pet.PetInstance> ps =
                        com.longdrange.ldattribute.pet.PetData.getAllPets(player.getUniqueId());
                return String.valueOf(ps == null ? 0 : ps.size());
            } catch (Throwable t) { return "0"; }
        }
        if (p2.equals("runes") || p2.equals("符文数")) {
            try { return String.valueOf(com.longdrange.ldattribute.rune.RuneData.count(player.getUniqueId())); }
            catch (Throwable t) { return "0"; }
        }
        if (p2.startsWith("level_") || p2.startsWith("等级_")) {
            String cardId = params.substring(params.indexOf('_') + 1);
            try {
                for (org.bukkit.inventory.ItemStack it : com.longdrange.ldattribute.card.PlayerData.getCards(player)) {
                    com.longdrange.ldattribute.card.CardData cd = com.longdrange.ldattribute.card.CardDataManager.findCard(it);
                    if (cd != null && cd.getId().equalsIgnoreCase(cardId)) {
                        return String.valueOf(com.longdrange.ldattribute.card.CardNBT.getLevel(it));
                    }
                }
            } catch (Throwable ignored) {}
            return "0";
        }
        if (p2.startsWith("synergy_") || p2.startsWith("共鸣_")) {
            String synId = params.substring(params.indexOf('_') + 1);
            try {
                java.util.List<org.bukkit.inventory.ItemStack> cards =
                        com.longdrange.ldattribute.card.PlayerData.getCards(player);
                for (com.longdrange.ldattribute.card.SynergyData.Synergy s :
                        com.longdrange.ldattribute.card.SynergyData.getAllResonances()) {
                    if (s.id.equalsIgnoreCase(synId)) {
                        int m = com.longdrange.ldattribute.card.SynergyData.getMatched(cards, s);
                        return (m >= s.required) ? "true" : "false";
                    }
                }
                for (com.longdrange.ldattribute.card.SynergyData.Synergy s :
                        com.longdrange.ldattribute.card.SynergyData.getAllBonds()) {
                    if (s.id.equalsIgnoreCase(synId)) {
                        int m = com.longdrange.ldattribute.card.SynergyData.getMatched(cards, s);
                        return (m >= s.required) ? "true" : "false";
                    }
                }
            } catch (Throwable ignored) {}
            return "false";
        }

        // ===== 排行榜 =====
        // %ldattr_top_攻击力_1%         → 第 1 名玩家名
        // %ldattr_top_攻击力_1_value%   → 第 1 名属性值
        if (params.toLowerCase().startsWith("top_")) {
            String[] parts = params.split("_");
            if (parts.length < 3) return "-";
            String attrName = parts[1];
            int rank;
            try { rank = Integer.parseInt(parts[2]); } catch (Exception e) { return "-"; }
            boolean wantValue = parts.length >= 4 && parts[3].equalsIgnoreCase("value");

            java.util.List<org.bukkit.entity.Player> sorted =
                    new java.util.ArrayList<>(org.bukkit.Bukkit.getOnlinePlayers());
            sorted.sort((a, b) -> Double.compare(getAttrValue(b, attrName), getAttrValue(a, attrName)));

            if (rank < 1 || rank > sorted.size()) return wantValue ? "0" : "-";
            org.bukkit.entity.Player target = sorted.get(rank - 1);
            double v = getAttrValue(target, attrName);
            if (wantValue) {
                if (v == Math.floor(v)) return String.valueOf((long) v);
                return String.format("%.2f", v);
            }
            return target.getName();
        }

        // ===== 法力系統 =====
        String pl = params.toLowerCase();
        if (pl.equals("mana") || pl.equals("法力")) {
            return String.valueOf(com.longdrange.ldattribute.card.ManaManager.getCurrent(player));
        }
        if (pl.equals("mana_max") || pl.equals("max_mana") || pl.equals("法力上限")) {
            return String.valueOf(com.longdrange.ldattribute.card.ManaManager.getMax(player));
        }
        if (pl.equals("mana_percent") || pl.equals("法力百分比")) {
            int max = com.longdrange.ldattribute.card.ManaManager.getMax(player);
            if (max <= 0) return "0";
            int cur = com.longdrange.ldattribute.card.ManaManager.getCurrent(player);
            return String.valueOf((int) (cur * 100.0 / max));
        }
        if (pl.equals("mana_bar") || pl.equals("法力条")) {
            int max = com.longdrange.ldattribute.card.ManaManager.getMax(player);
            if (max <= 0) return "";
            int cur = com.longdrange.ldattribute.card.ManaManager.getCurrent(player);
            int total = 10;
            int filled = (int) Math.round(cur * 1.0 / max * total);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < total; i++) sb.append(i < filled ? "■" : "□");
            return sb.toString();
        }

        // %ldattr_list%
        if (params.equalsIgnoreCase("list")) {
            return com.longdrange.ldattribute.data.attribute.LDAttributeManager
                    .getAttributeMap().values().stream()
                    .map(LDSubAttribute::getName)
                    .collect(Collectors.joining(","));
        }

        // 解析 params：可能是 "攻擊力" 或 "暴擊_chance"
        String attrName = params;
        String subParam = null;

        // 找出最長的匹配屬性名稱（支援多語言別名）
        String bestMatch = null;
        int longest = -1;
        for (LDSubAttribute attr : com.longdrange.ldattribute.data.attribute.LDAttributeManager
                .getAttributeMap().values()) {
            String name = attr.getName();
            for (String alias : LanguageManager.getAliases(name)) {
                if (params.equalsIgnoreCase(alias)) {
                    bestMatch = name;
                    subParam = null;
                    break;
                }
                if (params.toLowerCase().startsWith(alias.toLowerCase() + "_")) {
                    if (alias.length() > longest) {
                        longest = alias.length();
                        bestMatch = name;
                        subParam = params.substring(alias.length() + 1);
                    }
                }
            }
            if (subParam == null && bestMatch != null) break;
        }

        if (bestMatch == null) {
            return "0";
        }

        // 讀取玩家的屬性資料
        LDAttributeData data = loadEntityData(player);

        // 找出對應的屬性實例
        LDSubAttribute targetAttr = null;
        for (LDSubAttribute attr : data.getAttributeMap().values()) {
            if (attr.getName().equalsIgnoreCase(bestMatch)) {
                targetAttr = attr;
                break;
            }
        }

        if (targetAttr == null) {
            return "0";
        }


        return targetAttr.getPlaceholder(player, subParam);
    }

    /** 读玩家完整属性（含卡片/符文/宠物/共鸣），用于排行榜 */
    private double getAttrValue(org.bukkit.entity.Player p, String attrName) {
        try {
            com.longdrange.ldattribute.data.attribute.LDAttributeData data =
                    com.longdrange.ldattribute.card.StatsDataRead.loadPlayerStats(p);
            for (LDSubAttribute a : data.getAttributeMap().values()) {
                if (a.getName().equalsIgnoreCase(attrName)) return a.getValue();
            }
        } catch (Throwable ignored) {}
        return 0;
    }
    /**
     * 從玩家裝備讀取屬性
     */
    private LDAttributeData loadEntityData(Player player) {
        LDAttributeData data = new LDAttributeData();
        for (ItemStack item : player.getInventory().getArmorContents()) {
            data.add(plugin.getManager().getItemData(player, null, item));
        }
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        data.add(plugin.getManager().getItemData(player, null, mainHand));
        ItemStack offHand = player.getInventory().getItemInOffHand();
        data.add(plugin.getManager().getItemData(player, null, offHand));

        // 加上 API 附加資料（TcCardStats 等插件存入的卡片屬性）
        LDAttributeData apiData = plugin.getApi().getAPIStats(player.getUniqueId());
        if (apiData != null) {
            data.add(apiData);
        }

        return data;
    }
}
