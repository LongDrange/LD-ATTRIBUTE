package com.longdrange.ldattribute.core.soulring.rate;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.soulring.SoulRingConfig;
import com.longdrange.ldattribute.core.soulring.SoulRingData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 挂钩 MythicMobs 死亡事件，让 MM 配置的掉落也走倍率 + 自动拾取
 *
 * 通过反射注册，兼容 MM 4.x / 5.x：
 *   - 4.x: io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent
 *   - 5.x: io.lumine.mythic.bukkit.events.MythicMobDeathEvent
 */
public class MythicMobsRateListener implements Listener {

    private final LDAttribute plugin;

    public MythicMobsRateListener(LDAttribute plugin) { this.plugin = plugin; }

    @SuppressWarnings("unchecked")
    public void register() {
        String[] classNames = {
            "io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent",  // MM 4.x
            "io.lumine.mythic.bukkit.events.MythicMobDeathEvent"                    // MM 5.x
        };
        for (String cn : classNames) {
            try {
                Class<?> cls = Class.forName(cn);
                if (!Event.class.isAssignableFrom(cls)) continue;

                Bukkit.getPluginManager().registerEvent(
                        (Class<? extends Event>) cls,
                        this,
                        EventPriority.HIGHEST,
                        (listener, event) -> handleMmDeath(event),
                        plugin,
                        true
                );
                plugin.getLogger().info("[Rate] 已挂钩 MythicMobs 掉落倍率: " + cn);
                return;
            } catch (Throwable ignored) {}
        }
        plugin.getLogger().info("[Rate] 未找到 MythicMobs 死亡事件类，跳过 MM 掉落倍率");
    }

    @SuppressWarnings("unchecked")
    private void handleMmDeath(Event event) {
        try {
            // 1. 拿 killer
            Method getKiller = event.getClass().getMethod("getKiller");
            Object killerObj = getKiller.invoke(event);
            if (!(killerObj instanceof Player)) return;
            Player killer = (Player) killerObj;

            // 2. 拿 drops
            Method getDrops = event.getClass().getMethod("getDrops");
            Object dropsObj = getDrops.invoke(event);
            if (!(dropsObj instanceof List)) return;
            List<ItemStack> drops = (List<ItemStack>) dropsObj;
            if (drops.isEmpty()) return;

            // 3. 应用倍率
            double rate = RateManager.getRate(killer);
            List<ItemStack> boosted = new ArrayList<>();
            for (ItemStack drop : drops) {
                if (drop == null) continue;
                int newAmount = (int) Math.max(1, Math.round(drop.getAmount() * rate));
                ItemStack copy = drop.clone();
                copy.setAmount(newAmount);
                boosted.add(copy);
            }

            // 4. 自动拾取
            SoulRingData data = plugin.getSoulRingManager().get(killer);
            boolean doPickup = SoulRingConfig.isAutoPickupEnabled()
                    && SoulRingConfig.isAutoPickupMobDrops()
                    && data.isAutoPickupOn();

            long totalPicked = 0;
            List<ItemStack> leftover = new ArrayList<>();

            if (doPickup) {
                for (ItemStack item : boosted) {
                    if (SoulRingConfig.isFiltered(item)) {
                        leftover.add(item);
                        continue;
                    }
                    long added = data.deposit(item, item.getAmount());
                    if (added >= item.getAmount()) {
                        totalPicked += added;
                    } else if (added > 0) {
                        totalPicked += added;
                        ItemStack c = item.clone();
                        c.setAmount((int) (item.getAmount() - added));
                        leftover.add(c);
                    } else {
                        leftover.add(item);
                    }
                }
            } else {
                leftover.addAll(boosted);
            }

            // 5. 写回 drops
            try {
                Method setDrops = event.getClass().getMethod("setDrops", List.class);
                setDrops.invoke(event, leftover);
            } catch (Throwable ignored) {
                // 如果 MM 没 setDrops，就清空原 drops 并自然生成
                drops.clear();
                drops.addAll(leftover);
            }

            // 6. 提示
            if (totalPicked > 0 && SoulRingConfig.isAutoPickupMessage()) {
                String rateText = rate > 1 ? ChatColor.GOLD + " x" + String.format("%.2f", rate) : "";
                killer.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.LIGHT_PURPLE + "灵魂空间"
                        + ChatColor.DARK_GRAY + "] " + ChatColor.GREEN + "自动拾取 " + ChatColor.WHITE
                        + totalPicked + ChatColor.GREEN + " 个物品" + rateText);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[Rate] MM 掉落处理失败: " + t.getClass().getSimpleName()
                    + " - " + t.getMessage());
        }
    }
}