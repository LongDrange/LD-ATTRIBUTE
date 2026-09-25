package com.longdrange.ldattribute.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import com.longdrange.ldattribute.data.attribute.LDAttributeType;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 属性更新监听器（一次性算总量版）
 *
 * 旧逻辑：
 *   setMaxHealth(20) → 遍历 eventMethod 累加 → 中间态 MaxHealth=20 导致血量被 clamp
 * 新逻辑：
 *   遍历所有 UPDATE 属性 → 在内存累加 → 一次性 setMaxHealth(total) → 恢复血量
 *   → 中间态不暴露，玩家看不到血条抖动
 */
public class OnUpdateStatsListener implements Listener {

    private final LDAttribute plugin;
    private final ConcurrentHashMap<UUID, Long> lastUpdate = new ConcurrentHashMap<>();

    /** 基础值（Minecraft 默认） */
    private static final double BASE_MAX_HEALTH = 20.0;
    private static final double BASE_WALK_SPEED = 0.2;

    public OnUpdateStatsListener(LDAttribute plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> updatePlayer(player), 5L);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> updatePlayer(player), 1L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        // 灵魂空间是纯存储空间，不挂属性 → 关闭时不刷新
        try {
            if (event.getInventory().getHolder()
                    instanceof com.longdrange.ldattribute.core.soulring.gui.SoulRingHolder) {
                return;
            }
        } catch (Throwable ignored) {}

        Player player = (Player) event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> updatePlayer(player), 1L);
    }

    private void updatePlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        if (player.isDead()) return;

        // 防抖：100ms 内只更新一次
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastUpdate.get(uuid);
        if (last != null && (now - last) < 100) return;
        lastUpdate.put(uuid, now);

        // 1. 备份当前血量
        double savedHealth = player.getHealth();

        // 2. 读取全部属性
        LDAttributeData data = loadEntityData(player);

        // 3. 一次性计算总量（不调 eventMethod，避免中间态）
        double maxHealth = BASE_MAX_HEALTH;
        double walkSpeed = BASE_WALK_SPEED;

        for (LDSubAttribute attr : data.getAttributeMap().values()) {
            if (!attr.containsType(LDAttributeType.UPDATE)) continue;
            String name = attr.getName();
            double val = attr.getValue();
            if (val == 0) continue;

            if (name.equals("生命上限")) {
                // 固定值：+N 血
                maxHealth += val;
            } else if (name.equals("生命加成")) {
                // 百分比：+N% 血（如果你是想固定值加，把这里改成 maxHealth += val）
                maxHealth *= (1.0 + val / 100.0);
            } else if (name.equals("移动速度")) {
                // 固定值：+0.02 之类
                walkSpeed += val;
            } else if (name.equals("速度百分比")) {
                // 百分比：+N%
                walkSpeed *= (1.0 + val / 100.0);
            }
            // 其它 UPDATE 属性暂不处理（如未来新增可在此扩展）
        }

        // 4. 边界保护
        if (maxHealth < 1.0) maxHealth = 1.0;
        if (walkSpeed < 0.0) walkSpeed = 0.0;
        if (walkSpeed > 1.0) walkSpeed = 1.0;   // MC 上限

        // 5. 一次性设置（关键：中间态不暴露）
        try { player.setMaxHealth(maxHealth); } catch (Throwable ignored) {}
        try { player.setWalkSpeed((float) walkSpeed); } catch (Throwable ignored) {}

        // 6. 恢复血量（按你要求的行为）
        //    100/100 放入 +20 → 120/100（不补满）
        //    120/120 拔掉 -40 → 80/80（clamp）
        //    80/80 放入 +25 → 105/80（不补满）
        try {
            double target = Math.min(savedHealth, maxHealth);
            if (target < 1.0) target = 1.0;
            if (target > maxHealth) target = maxHealth;
            if (Math.abs(player.getHealth() - target) > 0.01) {
                player.setHealth(target);
            }
        } catch (Throwable ignored) {}
    }

    private LDAttributeData loadEntityData(LivingEntity entity) {
        LDAttributeData data = new LDAttributeData();
        if (entity instanceof Player) {
            Player player = (Player) entity;
            for (ItemStack item : player.getInventory().getArmorContents()) {
                data.add(plugin.getManager().getItemData(player, null, item));
            }
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            data.add(plugin.getManager().getItemData(player, null, mainHand));
            ItemStack offHand = player.getInventory().getItemInOffHand();
            data.add(plugin.getManager().getItemData(player, null, offHand));
        }
        LDAttributeData apiData = plugin.getApi().getAPIStats(entity.getUniqueId());
        if (apiData != null) {
            data.add(apiData);
        }
        return data;
    }
}