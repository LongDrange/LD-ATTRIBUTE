package com.longdrange.ldattribute.core.ring;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class RingSlotConfig {

    public static class SlotUnlock {
        public final int id;
        public final String permission;
        public final int costPoints;
        public final double costVault;
        public final List<String> items;
        public SlotUnlock(int id, String permission, int costPoints, double costVault, List<String> items) {
            this.id = id; this.permission = permission;
            this.costPoints = costPoints; this.costVault = costVault;
            this.items = items;
        }
    }

    private static int defaultUnlocked = 6;
    private static final Map<Integer, SlotUnlock> slots = new HashMap<>();

    public static void load(LDAttribute plugin) {
        slots.clear();
        File f = new File(plugin.getDataFolder(), "ring-slots.yml");
        if (!f.exists()) { try { plugin.saveResource("ring-slots.yml", false); } catch (Throwable ignored) {} }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

        ConfigurationSection st = cfg.getConfigurationSection("Settings");
        if (st != null) defaultUnlocked = Math.max(0, st.getInt("DefaultUnlocked", 6));

        ConfigurationSection sec = cfg.getConfigurationSection("Slots");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                int id;
                try { id = Integer.parseInt(key); } catch (Exception e) { continue; }
                ConfigurationSection s = sec.getConfigurationSection(key);
                if (s == null) continue;
                String perm = s.getString("Permission", "");
                int cp = s.getInt("CostPoints", 0);
                double cv = s.getDouble("CostVault", 0);
                List<String> items = s.getStringList("Items");
                if (items == null) items = new ArrayList<>();
                slots.put(id, new SlotUnlock(id, perm, cp, cv, items));
            }
        }
        plugin.getLogger().info("[Ring] 已加载 " + slots.size() + " 个槽位解锁配置");
    }

    public static int getDefaultUnlocked() { return defaultUnlocked; }
    public static SlotUnlock get(int slotId) { return slots.get(slotId); }
    public static boolean hasConfig(int slotId) { return slots.containsKey(slotId); }
}