package com.longdrange.ldattribute.card;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

/**
 * 协同系统：卡片共鸣 + 卡片羁绊
 */
public class SynergyData {

    public static class Synergy {
        public final String id;
        public final String type;
        public final String name;
        public final List<String> cards;
        public final int required;
        public final List<String> attributes;
        public final String message;
        public final List<String> rewardCommands;
        public final String rewardMessage;

        public Synergy(String id, String type, String name, List<String> cards, int required,
                       List<String> attributes, String message,
                       List<String> rewardCommands, String rewardMessage) {
            this.id = id; this.type = type; this.name = name;
            this.cards = cards; this.required = required;
            this.attributes = attributes; this.message = message;
            this.rewardCommands = rewardCommands; this.rewardMessage = rewardMessage;
        }
    }

    // 玩家上次激活状态：UUID → (synergyId → true/false)
    private static final Map<UUID, Map<String, Boolean>> lastActive = new HashMap<>();
    private static final Map<UUID, Map<String, Boolean>> lastClaimed = new HashMap<>();

    private static final Map<String, Synergy> resonance = new LinkedHashMap<>();
    private static final Map<String, Synergy> bonds = new LinkedHashMap<>();

    public static void load(LDAttribute plugin) {
        resonance.clear();
        bonds.clear();
        resonance.putAll(loadFile(plugin, "resonance.yml", "Resonances", "RESONANCE"));
        bonds.putAll(loadFile(plugin, "bond.yml", "Bonds", "BOND"));
        plugin.getLogger().info("已載入 " + resonance.size() + " 個共鳴, " + bonds.size() + " 個羈絆");
    }

    private static Map<String, Synergy> loadFile(LDAttribute plugin, String fileName, String rootKey, String type) {
        Map<String, Synergy> map = new LinkedHashMap<>();
        File f = com.longdrange.ldattribute.util.ConfigPaths.resolve(plugin, fileName);
        if (!f.exists()) {
            try { plugin.saveResource(fileName, false); } catch (Exception ignored) {}
        }
        if (!f.exists()) return map;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection sec = cfg.getConfigurationSection(rootKey);
        if (sec == null) return map;

        for (String key : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) continue;
            List<String> cards = s.getStringList("Cards");
            if (cards == null) cards = new ArrayList<>();
            int required = s.getInt("Required", cards.size());
            List<String> attrs = s.getStringList("Attributes");
            if (attrs == null) attrs = new ArrayList<>();
            String msg = org.bukkit.ChatColor.translateAlternateColorCodes((char)38, s.getString("Message", ""));
            String name = org.bukkit.ChatColor.translateAlternateColorCodes((char) 38, s.getString("Name", key));

            List<String> rewardCmds = new ArrayList<>();
            String rewardMsg = "";
            ConfigurationSection ro = s.getConfigurationSection("RewardOnce");
            if (ro != null) {
                List<String> tmp = ro.getStringList("Commands");
                if (tmp != null) rewardCmds = tmp;
                rewardMsg = org.bukkit.ChatColor.translateAlternateColorCodes((char)38, ro.getString("Message", ""));
            }
            map.put(key, new Synergy(key, type, name, cards, required, attrs, msg, rewardCmds, rewardMsg));
        }
        return map;
    }

    public static void apply(Player player, List<ItemStack> cards, LDAttributeData statsData) {
        applyGroup(player, resonance, cards, statsData);
        applyGroup(player, bonds, cards, statsData);
    }

    private static void applyGroup(Player player, Map<String, Synergy> map, List<ItemStack> cards, LDAttributeData statsData) {
        for (Synergy s : map.values()) {
            int matched = 0;
            for (String c : s.cards) if (hasCard(cards, c)) matched++;
            // 记录状态
            Map<String, Boolean> activeMap = lastActive.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
            Map<String, Boolean> claimedMap = lastClaimed.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
            boolean wasActive = activeMap.getOrDefault(s.id, false);

            if (matched < s.required) {
                // 未集齐
                if (wasActive) {
                    // 之前集齐现在没了 → 提示失效
                    String typeName = s.type.equals("RESONANCE") ? "共鳴" : "羈絆";
                    player.sendMessage("§8[§d" + typeName + "§8] §c" + s.name + " §c已失效 §8(§7請放回缺少的卡片§8)");
                }
                activeMap.put(s.id, false);
                continue;
            }

            // === 集齐 ===
            activeMap.put(s.id, true);

            // 套用属性
            if (!s.attributes.isEmpty()) {
                LDAttributeData d = LDAttribute.getInstance().getApi().getLoreData(player, null, s.attributes);
                statsData.add(d);
            }

            String typeName2 = s.type.equals("RESONANCE") ? "共鳴" : "羈絆";
            String claimKey = "synergy_" + s.id;

            // 首次激活
            if (!wasActive) {
                player.sendMessage("§8[§d" + typeName2 + "§8] §a✦ 已激活 §e" + s.name + " §7(" + matched + "/" + s.required + ")");
            }

            // 奖励判定
            if (!s.rewardCommands.isEmpty()) {
                if (PlayerData.hasClaimed(player.getUniqueId(), claimKey)) {
                    // 已领取过 → 首次激活这个阶段给一次提示
                    if (!wasActive && !claimedMap.getOrDefault(s.id, false)) {
                        player.sendMessage("§8[§d" + typeName2 + "§8] §7" + s.name + " §7的獎勵 §c已領取過 §8(§7不會重複發放§8)");
                        claimedMap.put(s.id, true);
                    }
                } else {
                    PlayerData.markClaimed(player.getUniqueId(), claimKey);
                    claimedMap.put(s.id, true);
                    for (String cmd : s.rewardCommands) {
                        try {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                    cmd.replace("{player}", player.getName()));
                        } catch (Throwable ignored) {}
                    }
                    if (s.rewardMessage != null && !s.rewardMessage.isEmpty()) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', s.rewardMessage));
                    }
                }
            }
        }
    }

    private static boolean hasCard(List<ItemStack> cards, String cardId) {
        CardData target = CardDataManager.getCard(cardId);
        if (target == null) return false;
        for (ItemStack card : cards) if (target.matches(card)) return true;
        return false;
    }

    public static int getMatched(List<ItemStack> cards, Synergy s) {
        int m = 0;
        for (String c : s.cards) if (hasCard(cards, c)) m++;
        return m;
    }

    public static Collection<Synergy> getAllResonances() { return resonance.values(); }
    public static Collection<Synergy> getAllBonds() { return bonds.values(); }

    public static void unload(UUID uuid) {
        lastActive.remove(uuid);
        lastClaimed.remove(uuid);
    }
}