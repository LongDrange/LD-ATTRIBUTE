package com.longdrange.ldattribute.compat;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.card.CardData;
import com.longdrange.ldattribute.card.CardDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

/**
 * MythicMobs 4.4.0 兼容
 * 通过反射挂载，无硬依赖
 */
public class MMCompat {

    private static LDAttribute plugin;
    private static boolean hooked = false;

    // MM怪物ID → (卡片/物品ID → 概率)
    private static final Map<String, Map<String, Double>> drops = new HashMap<>();
    // MM怪物ID → (卡片/物品ID → 数量)
    private static final Map<String, Map<String, Integer>> amounts = new HashMap<>();
    // MM怪物ID -> (符文ID -> 概率)
    private static final Map<String, Map<String, Double>> runeDrops = new HashMap<>();

    public static void init(LDAttribute pl) {
        plugin = pl;
        loadConfig();
        registerListener();
    }

    public static void loadConfig() {
        drops.clear();
        amounts.clear();

        File f = com.longdrange.ldattribute.util.ConfigPaths.resolve(plugin, "mm.yml");
        if (!f.exists()) { try { plugin.saveResource("mm.yml", false); } catch (Exception ignored) {} }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

        ConfigurationSection sec = cfg.getConfigurationSection("DropToCards");
        if (sec == null) return;

        for (String mobId : sec.getKeys(false)) {
            ConfigurationSection ms = sec.getConfigurationSection(mobId);
            if (ms == null) continue;
            Map<String, Double> d = new HashMap<>();
            Map<String, Integer> a = new HashMap<>();
            for (String itemSpec : ms.getKeys(false)) {
                Object val = ms.get(itemSpec);
                // 格式 A: 卡片ID: 0.01  （概率）
                // 格式 B: "DIAMOND:2:0.5"（物品:数量:概率）
                if (val instanceof Number) {
                    d.put(itemSpec, ((Number) val).doubleValue());
                    a.put(itemSpec, 1);
                } else {
                    String s = String.valueOf(val);
                    String[] parts = s.split(":");
                    if (parts.length >= 3) {
                        try {
                            a.put(itemSpec, Integer.parseInt(parts[1]));
                            d.put(itemSpec, Double.parseDouble(parts[2]));
                        } catch (Exception ignored) {}
                    } else if (parts.length == 1) {
                        try {
                            d.put(itemSpec, Double.parseDouble(parts[0]));
                            a.put(itemSpec, 1);
                        } catch (Exception ignored) {}
                    }
                }
            }
            drops.put(mobId, d);
            amounts.put(mobId, a);
        }
                ConfigurationSection rd = cfg.getConfigurationSection("RuneDrops");
        if (rd != null) {
            for (String mobId : rd.getKeys(false)) {
                ConfigurationSection ms = rd.getConfigurationSection(mobId);
                if (ms == null) continue;
                Map<String, Double> map = new HashMap<>();
                for (String runeId : ms.getKeys(false)) {
                    map.put(runeId, ms.getDouble(runeId));
                }
                runeDrops.put(mobId, map);
            }
        }
plugin.getLogger().info("已載入 " + drops.size() + " 個 MM 掉落配置");
    }

    private static void registerListener() {
        try {
            Class<?> clazz = Class.forName("io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent");
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) clazz;

            Bukkit.getPluginManager().registerEvent(
                    eventClass,
                    new Listener() {},
                    EventPriority.MONITOR,
                    (listener, event) -> handleMMDeath(event),
                    plugin
            );
            hooked = true;
            plugin.getLogger().info("已掛載 MythicMobs 4.4.0 死亡事件");
        } catch (ClassNotFoundException e) {
            hooked = false;
            plugin.getLogger().info("未檢測到 MythicMobs，跳過 MM 兼容");
        } catch (Throwable t) {
            hooked = false;
            plugin.getLogger().warning("MM 掛載失敗: " + t.getMessage());
        }
    }

    private static void handleMMDeath(Event event) {
        try {
            // 获取 mobType 的 internalName
            Method getMobType = event.getClass().getMethod("getMobType");
            Object mobType = getMobType.invoke(event);
            Method getInternalName = mobType.getClass().getMethod("getInternalName");
            String mobId = (String) getInternalName.invoke(mobType);
            if (mobId == null) return;

            // 获取实体
            Method getEntity = event.getClass().getMethod("getEntity");
            LivingEntity entity = (LivingEntity) getEntity.invoke(event);
            if (entity == null) return;

            Map<String, Double> mobDrops = drops.get(mobId);
            Map<String, Double> mobRunes = runeDrops.get(mobId);
            boolean noCardDrops = (mobDrops == null || mobDrops.isEmpty());
            boolean noRuneDrops = (mobRunes == null || mobRunes.isEmpty());
            if (noCardDrops && noRuneDrops) return;

            Map<String, Integer> mobAmounts = amounts.getOrDefault(mobId, Collections.emptyMap());

            // 计算幸运循环次数（击杀者身上的幸运属性）
            org.bukkit.entity.Player killer = null;
            try { killer = entity.getKiller(); } catch (Throwable ignored) {}
            double luck = getLuck(killer);
            int times = computeDropTimes(luck);

            Location loc = entity.getLocation();

            for (int t = 0; t < times; t++) {
                // 掉符文
                if (mobRunes != null && !mobRunes.isEmpty()) {
                    for (Map.Entry<String, Double> re : mobRunes.entrySet()) {
                        if (Math.random() > re.getValue()) continue;
                        com.longdrange.ldattribute.rune.RuneConfig.Rune rn =
                                com.longdrange.ldattribute.rune.RuneConfig.getRune(re.getKey());
                        if (rn == null) continue;
                        org.bukkit.inventory.ItemStack runeItem =
                                com.longdrange.ldattribute.rune.RuneItem.create(rn, 1);
                        loc.getWorld().dropItemNaturally(loc, runeItem);
                    }
                }
                // 掉卡片/物品
                if (mobDrops != null && !mobDrops.isEmpty()) {
                    for (Map.Entry<String, Double> e : mobDrops.entrySet()) {
                        if (Math.random() > e.getValue()) continue;
                        String itemId = e.getKey();
                        int amount = mobAmounts.getOrDefault(itemId, 1);

                        CardData card = CardDataManager.getCard(itemId);
                        if (card != null) {
                            ItemStack item = card.getItem();
                            item.setAmount(amount);
                            loc.getWorld().dropItemNaturally(loc, item);
                            continue;
                        }
                        Material mat = Material.getMaterial(itemId.toUpperCase());
                        if (mat != null) {
                            loc.getWorld().dropItemNaturally(loc, new ItemStack(mat, amount));
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public static boolean isHooked() { return hooked; }

    /** 从玩家读幸运属性，默认 100 */
    private static double getLuck(org.bukkit.entity.Player p) {
        if (p == null) return 100.0;
        try {
            com.longdrange.ldattribute.data.attribute.LDAttributeData data =
                    com.longdrange.ldattribute.card.StatsDataRead.loadPlayerStats(p);
            for (com.longdrange.ldattribute.data.attribute.LDSubAttribute a : data.getAttributeMap().values()) {
                if (a.getName().equals("幸运")) return a.getValue();
            }
        } catch (Throwable ignored) {}
        return 100.0;
    }

    /** 幸运 -> 掉落执行次数 */
    private static int computeDropTimes(double luck) {
        if (luck < 100) luck = 100;
        int times = (int) Math.floor(luck / 100.0);
        double extra = (luck % 100.0) / 100.0;
        if (extra > 0 && Math.random() < extra) times++;
        return Math.max(1, times);
    }

    /** 判断是否是 MythicMobs 的怪（反射，无硬依赖） */
    public static boolean isMythicMob(org.bukkit.entity.Entity entity) {
        if (entity == null) return false;
        try {
            Class<?> mmClass = Class.forName("io.lumine.xikage.mythicmobs.MythicMobs");
            Object mm = mmClass.getMethod("inst").invoke(null);
            Object apiHelper = mm.getClass().getMethod("getAPIHelper").invoke(mm);
            Object activeMob = apiHelper.getClass()
                    .getMethod("getMythicMobInstance", org.bukkit.entity.Entity.class)
                    .invoke(apiHelper, entity);
            return activeMob != null;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
