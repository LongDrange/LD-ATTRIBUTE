package com.longdrange.ldattribute.core.ring;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.data.PlayerModuleData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.*;
import java.util.*;

public class RingData {

    public static final String MODULE = "hzring";

    public static class Slot {
        public String ringType;
        public int count;
        public ItemStack template;
        public int maxStack;
        public int level = 1;
        public Slot(String ringType, int count, ItemStack template, int maxStack) {
            this.ringType = ringType; this.count = count;
            this.template = template; this.maxStack = maxStack;
        }
    }

    private final LDAttribute plugin;
    private final UUID uuid;
    private final PlayerModuleData raw;

    private final Map<Integer, Slot> slots = new HashMap<>();
    private final Set<Integer> unlockedSlots = new LinkedHashSet<>();
    private int unlockedPages;

    public RingData(LDAttribute plugin, UUID uuid) {
        this.plugin = plugin;
        this.uuid = uuid;
        this.raw = plugin.getModuleDataManager().get(uuid, MODULE);
        loadFrom(raw);
    }

    private void loadFrom(PlayerModuleData data) {
        unlockedPages = data.getInt("pages", RingConfig.getDefaultPages());
        if (unlockedPages < 1) unlockedPages = 1;

        unlockedSlots.clear();
        String s = data.getString("slotsUnlocked", "");
        if (s != null && !s.isEmpty()) {
            for (String part : s.split(",")) {
                try { unlockedSlots.add(Integer.parseInt(part.trim())); } catch (Exception ignored) {}
            }
        }
        int def = RingSlotConfig.getDefaultUnlocked();
        for (int i = 0; i < def; i++) unlockedSlots.add(i);

        slots.clear();
        for (Map.Entry<String, Object> e : data.getRaw().entrySet()) {
            String key = e.getKey();
            if (!key.startsWith("slot.")) continue;
            String rest = key.substring(5);
            if (rest.contains(".")) continue;
            try {
                int id = Integer.parseInt(rest);
                Slot slot = deserializeSlot(String.valueOf(e.getValue()));
                if (slot != null && slot.count > 0) {
                    slot.level = data.getInt("slot." + id + ".level", 1);
                    if (slot.level < 1) slot.level = 1;
                    slots.put(id, slot);
                }
            } catch (Exception ignored) {}
        }
    }

    public int getUnlockedPages() { return unlockedPages; }
    public Set<Integer> getUnlockedSlots() { return unlockedSlots; }
    public boolean isUnlocked(int gid) { return unlockedSlots.contains(gid); }
    public Slot getSlot(int gid) { return slots.get(gid); }
    public List<Slot> getAllSlots() {
        List<Slot> out = new ArrayList<>();
        for (Slot s : slots.values()) if (s != null && s.count > 0) out.add(s);
        return out;
    }

    public int countType(String type) {
        int c = 0;
        for (Slot s : slots.values()) if (s != null && type.equals(s.ringType)) c += s.count;
        return c;
    }

    public int findSlotOfType(String type) {
        for (Map.Entry<Integer, Slot> e : slots.entrySet())
            if (e.getValue() != null && type.equals(e.getValue().ringType)) return e.getKey();
        return -1;
    }

    public int findEmptySlot() {
        for (Integer id : unlockedSlots) {
            Slot s = slots.get(id);
            if (s == null || s.count <= 0) return id;
        }
        return -1;
    }

    public int addRings(String type, int amount, ItemStack template, int maxStack) {
        if (type == null || amount <= 0) return 0;
        int already = countType(type);
        int canAdd = Math.max(0, maxStack - already);
        int willAdd = Math.min(amount, canAdd);
        if (willAdd <= 0) return 0;

        int existing = findSlotOfType(type);
        if (existing >= 0) {
            Slot s = slots.get(existing);
            s.count += willAdd;
            saveSlot(existing, s);
        } else {
            int slot = findEmptySlot();
            if (slot < 0) return 0;
            Slot s = new Slot(type, willAdd, template.clone(), maxStack);
            slots.put(slot, s);
            saveSlot(slot, s);
            raw.set("slot." + slot + ".level", 1);
        }
        return willAdd;
    }

    public ItemStack takeFromSlot(int slotId, int amount) {
        Slot s = slots.get(slotId);
        if (s == null || s.count <= 0) return null;
        int take = Math.min(amount, s.count);
        ItemStack give = s.template.clone();
        give.setAmount(take);
        s.count -= take;
        if (s.count <= 0) {
            slots.remove(slotId);
            raw.set("slot." + slotId, null);
            raw.set("slot." + slotId + ".level", null);
        } else {
            saveSlot(slotId, s);
        }
        return give;
    }

    /** 从槽位消耗 N 个魂珠，返回实际消耗数量 */
    public int consumeFromSlot(int slotId, int amount) {
        Slot s = slots.get(slotId);
        if (s == null || s.count <= 0 || amount <= 0) return 0;
        int take = Math.min(amount, s.count);
        s.count -= take;
        if (s.count <= 0) {
            slots.remove(slotId);
            raw.set("slot." + slotId, null);
            raw.set("slot." + slotId + ".level", null);
        } else {
            saveSlot(slotId, s);
        }
        return take;
    }

    public void setLevel(int slotId, int level) {
        Slot s = slots.get(slotId);
        if (s == null) return;
        s.level = Math.max(1, level);
        raw.set("slot." + slotId + ".level", s.level);
    }

    public void unlockSlot(int gid) { unlockedSlots.add(gid); saveUnlocked(); }
    public void unlockPage(int page) { if (page + 1 > unlockedPages) unlockedPages = page + 1; raw.set("pages", unlockedPages); }

    private void saveUnlocked() {
        StringBuilder sb = new StringBuilder();
        for (Integer id : unlockedSlots) {
            if (sb.length() > 0) sb.append(',');
            sb.append(id);
        }
        raw.set("slotsUnlocked", sb.toString());
    }

    private void saveSlot(int slotId, Slot s) { raw.set("slot." + slotId, serializeSlot(s)); }

    public void save() {
        saveUnlocked();
        raw.set("pages", unlockedPages);
        for (Map.Entry<Integer, Slot> e : slots.entrySet()) {
            raw.set("slot." + e.getKey() + ".level", e.getValue().level);
        }
    }

    private static String serializeSlot(Slot s) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos);
            boos.writeUTF(s.ringType == null ? "" : s.ringType);
            boos.writeInt(s.count);
            boos.writeInt(s.maxStack);
            boos.writeObject(s.template);
            boos.close();
            return Base64Coder.encodeLines(baos.toByteArray());
        } catch (Exception e) { return null; }
    }

    private static Slot deserializeSlot(String str) {
        if (str == null || str.isEmpty()) return null;
        try {
            byte[] data = Base64Coder.decodeLines(str);
            BukkitObjectInputStream bois = new BukkitObjectInputStream(new ByteArrayInputStream(data));
            String type = bois.readUTF();
            int count = bois.readInt();
            int max = bois.readInt();
            Object obj = bois.readObject();
            bois.close();
            if (obj instanceof ItemStack) return new Slot(type, count, (ItemStack) obj, max);
        } catch (Throwable ignored) {}
        try {
            byte[] data = Base64Coder.decodeLines(str);
            BukkitObjectInputStream bois = new BukkitObjectInputStream(new ByteArrayInputStream(data));
            Object obj = bois.readObject();
            bois.close();
            if (obj instanceof ItemStack) {
                ItemStack it = (ItemStack) obj;
                String type = RingItemUtil.getRingType(it);
                if (type != null) {
                    int max = RingItemUtil.getMaxStack(it, RingConfig.getDefaultMaxStack());
                    return new Slot(type, it.getAmount(), it.clone(), max);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public UUID getUuid() { return uuid; }
}