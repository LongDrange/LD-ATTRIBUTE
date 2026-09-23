package com.longdrange.ldattribute.card;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * 页面配置（支持整页解锁 + 逐格解锁）
 */
public class PageConfig {

    public static class SlotTier {
        public final int startSlot;
        public final int count;
        public final int costPoints;
        public final double costVault;
        public final String permission;
        public final List<String> items;
        public final List<String> commands;
        public SlotTier(int startSlot, int count, int costPoints, double costVault,
                        String permission, List<String> items, List<String> commands) {
            this.startSlot = startSlot; this.count = count;
            this.costPoints = costPoints; this.costVault = costVault;
            this.permission = permission; this.items = items; this.commands = commands;
        }
    }

    public static class Page {
        public final String id;
        public final int slots;
        public final int costPoints;
        public final double costVault;
        public final String permission;
        public final List<String> items;
        public final List<String> commands;
        public final int defaultSlots;
        public final List<SlotTier> tiers;

        public Page(String id, int slots, int costPoints, double costVault,
                    String permission, List<String> items, List<String> commands,
                    int defaultSlots, List<SlotTier> tiers) {
            this.id = id; this.slots = slots;
            this.costPoints = costPoints; this.costVault = costVault;
            this.permission = permission; this.items = items; this.commands = commands;
            this.defaultSlots = defaultSlots; this.tiers = tiers;
        }

        public boolean isDefaultUnlocked() {
            return costPoints <= 0 && costVault <= 0
                    && (permission == null || permission.isEmpty())
                    && items.isEmpty();
        }

        /** 根据已解锁格数取得下一档 */
        public SlotTier getNextTier(int currentUnlocked) {
            for (SlotTier t : tiers) {
                if (currentUnlocked < t.startSlot + t.count) return t;
            }
            return null;
        }
    }

    private static final List<Page> pages = new ArrayList<>();

    public static void load(LDAttribute plugin) {
        pages.clear();
        File file = new File(plugin.getDataFolder(), "page.yml");
        if (!file.exists()) {
            try { plugin.saveResource("page.yml", false); } catch (Exception ignored) {}
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection pagesSec = cfg.getConfigurationSection("Pages");
        if (pagesSec == null) {
            plugin.getLogger().warning("page.yml 沒有 Pages 區塊");
            return;
        }
        List<String> keys = new ArrayList<>(pagesSec.getKeys(false));
        Collections.sort(keys);

        for (String key : keys) {
            ConfigurationSection sec = pagesSec.getConfigurationSection(key);
            if (sec == null) continue;
            int slots = sec.getInt("Slots", 36);
            if (slots < 9) slots = 9;
            if (slots > 45) slots = 45;
            if (slots % 9 != 0) slots = (slots / 9) * 9;

            // 整页解锁
            ConfigurationSection unlock = sec.getConfigurationSection("Unlock");
            int points = 0; double vault = 0; String perm = "";
            List<String> items = new ArrayList<>(); List<String> commands = new ArrayList<>();
            if (unlock != null) {
                points = unlock.getInt("Points", 0);
                vault = unlock.getDouble("Vault", 0);
                perm = unlock.getString("Permission", "");
                List<String> tmp = unlock.getStringList("Items");
                if (tmp != null) items = tmp;
                List<String> tmp2 = unlock.getStringList("Commands");
                if (tmp2 != null) commands = tmp2;
            }

            // 逐格解锁
            int defaultSlots = slots;
            List<SlotTier> tiers = new ArrayList<>();
            ConfigurationSection slotUnlock = sec.getConfigurationSection("SlotUnlock");
            if (slotUnlock != null) {
                defaultSlots = slotUnlock.getInt("DefaultSlots", slots);
                if (defaultSlots < 0) defaultSlots = 0;
                if (defaultSlots > slots) defaultSlots = slots;

                List<Map<?, ?>> tierList = slotUnlock.getMapList("Tiers");
                int cum = defaultSlots;
                for (Map<?, ?> m : tierList) {
                    int count = toInt(m.get("Slots"), 1);
                    int tp = toInt(m.get("Points"), 0);
                    double tv = toDouble(m.get("Vault"), 0);
                    String tperm = m.get("Permission") == null ? "" : m.get("Permission").toString();
                    List<String> titems = toStringList(m.get("Items"));
                    List<String> tcmds = toStringList(m.get("Commands"));
                    tiers.add(new SlotTier(cum, count, tp, tv, tperm, titems, tcmds));
                    cum += count;
                }
            }

            pages.add(new Page(key, slots, points, vault, perm, items, commands, defaultSlots, tiers));
        }
        plugin.getLogger().info("已載入 " + pages.size() + " 個卡片頁面");
    }

    private static int toInt(Object o, int def) {
        if (o == null) return def;
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return def; }
    }
    private static double toDouble(Object o, double def) {
        if (o == null) return def;
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return def; }
    }
    private static List<String> toStringList(Object o) {
        List<String> list = new ArrayList<>();
        if (o instanceof List) for (Object x : (List<?>) o) if (x != null) list.add(x.toString());
        return list;
    }

    public static int getPageCount() { return pages.size(); }
    public static Page getPage(int index) {
        if (index < 0 || index >= pages.size()) return null;
        return pages.get(index);
    }
    public static List<Page> getAllPages() { return pages; }
}