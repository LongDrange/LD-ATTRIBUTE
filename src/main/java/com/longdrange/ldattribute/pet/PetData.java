package com.longdrange.ldattribute.pet;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * 玩家宠物数据（多页 + 多宠物实例）
 */
public class PetData {

    public static class PlayerPets {
        public int unlockedPages = 1;
        public int currentPage = 0;
        /** page → slot → PetInstance */
        public final Map<Integer, Map<Integer, PetInstance>> pages = new HashMap<>();
        /** 已出战宠物所在页 */
        public int activePage = -1;
        /** 已出战宠物所在槽 */
        public int activeSlot = -1;
    }

    private static final Map<UUID, PlayerPets> cache = new HashMap<>();
    private static File file;
    private static YamlConfiguration data;

    public static void init(LDAttribute plugin) {
        File dir = plugin.getDataFolder();
        File dataDir = new File(dir, "data");
        if (!dataDir.exists()) dataDir.mkdirs();
        file = new File(dataDir, "pets.yml");
        if (!file.exists()) { try { file.createNewFile(); } catch (Exception ignored) {} }
        data = YamlConfiguration.loadConfiguration(file);
    }

    // ==================== 加载 ====================

    public static PlayerPets get(UUID uuid) {
        if (cache.containsKey(uuid)) return cache.get(uuid);
        PlayerPets pp = new PlayerPets();
        String path = uuid.toString();

        pp.unlockedPages = data.getInt(path + ".unlockedPages", 1);
        if (pp.unlockedPages < 1) pp.unlockedPages = 1;
        pp.currentPage = data.getInt(path + ".currentPage", 0);

        ConfigurationSection pages = data.getConfigurationSection(path + ".pages");
        if (pages != null) {
            for (String pKey : pages.getKeys(false)) {
                try {
                    int page = Integer.parseInt(pKey);
                    ConfigurationSection sec = pages.getConfigurationSection(pKey);
                    if (sec == null) continue;
                    Map<Integer, PetInstance> slotMap = new HashMap<>();
                    for (String sKey : sec.getKeys(false)) {
                        try {
                            int slot = Integer.parseInt(sKey);
                            String petId = sec.getString(sKey + ".petId", "");
                            int lv = sec.getInt(sKey + ".level", 1);
                            int exp = sec.getInt(sKey + ".exp", 0);
                            if (!petId.isEmpty()) {
                                slotMap.put(slot, new PetInstance(petId, lv, exp));
                            }
                        } catch (Exception ignored) {}
                    }
                    pp.pages.put(page, slotMap);
                } catch (Exception ignored) {}
            }
        }

        // 兼容旧格式：owned 列表 → 放到 page 0 的槽位
        List<String> owned = data.getStringList(path + ".owned");
        if (owned != null && !owned.isEmpty()) {
            Map<Integer, PetInstance> page0 = pp.pages.computeIfAbsent(0, k -> new HashMap<>());
            int slot = 0;
            for (String petId : owned) {
                while (page0.containsKey(slot)) slot++;
                page0.put(slot, new PetInstance(petId, 1, 0));
            }
            // 清掉旧格式
            data.set(path + ".owned", null);
        }

        // 兼容旧的 active
        String oldActive = data.getString(path + ".active", "");
        if (!oldActive.isEmpty() && pp.activePage < 0) {
            Map<Integer, PetInstance> page0 = pp.pages.get(0);
            if (page0 != null) {
                for (Map.Entry<Integer, PetInstance> e : page0.entrySet()) {
                    if (e.getValue().petId.equals(oldActive)) {
                        pp.activePage = 0;
                        pp.activeSlot = e.getKey();
                        break;
                    }
                }
            }
            data.set(path + ".active", null);
        }

        cache.put(uuid, pp);
        return pp;
    }

    public static void save(UUID uuid) {
        PlayerPets pp = cache.get(uuid);
        if (pp == null) return;
        String path = uuid.toString();

        data.set(path + ".unlockedPages", pp.unlockedPages);
        data.set(path + ".currentPage", pp.currentPage);

        data.set(path + ".pages", null);
        for (Map.Entry<Integer, Map<Integer, PetInstance>> pe : pp.pages.entrySet()) {
            for (Map.Entry<Integer, PetInstance> se : pe.getValue().entrySet()) {
                String base = path + ".pages." + pe.getKey() + "." + se.getKey();
                data.set(base + ".petId", se.getValue().petId);
                data.set(base + ".level", se.getValue().level);
                data.set(base + ".exp", se.getValue().exp);
            }
        }
        saveFile();
    }

    public static void saveFile() {
        try { data.save(file); } catch (Exception ignored) {}
    }

    public static void unload(UUID uuid) {
        save(uuid);
        cache.remove(uuid);
    }

    // ==================== 便捷 API ====================

    public static Map<Integer, PetInstance> getPage(UUID uuid, int page) {
        return get(uuid).pages.computeIfAbsent(page, k -> new HashMap<>());
    }

    /** 找玩家所有宠物 */
    public static List<PetInstance> getAllPets(UUID uuid) {
        List<PetInstance> result = new ArrayList<>();
        PlayerPets pp = get(uuid);
        for (int p = 0; p < pp.unlockedPages; p++) {
            Map<Integer, PetInstance> page = pp.pages.get(p);
            if (page != null) result.addAll(page.values());
        }
        return result;
    }

    /** 找空闲槽位 */
    public static int findEmptySlot(UUID uuid, int page, int maxSlots) {
        Map<Integer, PetInstance> map = getPage(uuid, page);
        for (int i = 0; i < maxSlots; i++) {
            if (!map.containsKey(i)) return i;
        }
        return -1;
    }

    /** 取出宠物 */
    public static PetInstance getPet(UUID uuid, int page, int slot) {
        Map<Integer, PetInstance> map = getPage(uuid, page);
        return map.get(slot);
    }

    /** 设置宠物 */
    public static void setPet(UUID uuid, int page, int slot, PetInstance pi) {
        Map<Integer, PetInstance> map = getPage(uuid, page);
        if (pi == null) map.remove(slot);
        else map.put(slot, pi);
        save(uuid);
    }

    /** 找宠物在哪个位置 */
    public static int[] findPetPos(UUID uuid, String petId) {
        PlayerPets pp = get(uuid);
        for (int p = 0; p < pp.unlockedPages; p++) {
            Map<Integer, PetInstance> map = pp.pages.get(p);
            if (map == null) continue;
            for (Map.Entry<Integer, PetInstance> e : map.entrySet()) {
                if (e.getValue().petId.equals(petId)) {
                    return new int[]{p, e.getKey()};
                }
            }
        }
        return null;
    }

    public static int getUnlockedPages(UUID uuid) { return get(uuid).unlockedPages; }
    public static void setUnlockedPages(UUID uuid, int n) {
        get(uuid).unlockedPages = Math.max(1, n);
        save(uuid);
    }
    public static int getCurrentPage(UUID uuid) { return get(uuid).currentPage; }
    public static void setCurrentPage(UUID uuid, int page) {
        get(uuid).currentPage = Math.max(0, page);
    }

    public static int getActivePage(UUID uuid) { return get(uuid).activePage; }
    public static int getActiveSlot(UUID uuid) { return get(uuid).activeSlot; }
    public static void setActive(UUID uuid, int page, int slot) {
        get(uuid).activePage = page;
        get(uuid).activeSlot = slot;
        save(uuid);
    }
    public static void clearActive(UUID uuid) {
        get(uuid).activePage = -1;
        get(uuid).activeSlot = -1;
        save(uuid);
    }
}