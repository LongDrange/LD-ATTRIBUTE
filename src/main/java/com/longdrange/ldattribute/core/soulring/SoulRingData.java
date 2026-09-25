package com.longdrange.ldattribute.core.soulring;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.data.PlayerModuleData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.*;
import java.util.*;

public class SoulRingData {

    public static final String MODULE = "soulring";

    public static class Entry {
        public ItemStack template;
        public long count;
        public Entry(ItemStack t, long c) { this.template = t; this.count = c; }
    }

    private final LDAttribute plugin;
    private final UUID uuid;
    private final PlayerModuleData raw;
    private final List<Entry> entries = new ArrayList<>();

    public SoulRingData(LDAttribute plugin, UUID uuid) {
        this.plugin = plugin;
        this.uuid = uuid;
        this.raw = plugin.getModuleDataManager().get(uuid, MODULE);
        loadFrom();
    }

    private void loadFrom() {
        entries.clear();
        for (Map.Entry<String, Object> e : raw.getRaw().entrySet()) {
            String key = e.getKey();
            if (!key.startsWith("item.")) continue;
            try {
                String rest = key.substring("item.".length());
                int idx = rest.lastIndexOf('.');
                if (idx < 0) continue;
                int id = Integer.parseInt(rest.substring(idx + 1));
                ItemStack tpl = deserialize(String.valueOf(e.getValue()));
                if (tpl == null) continue;
                long count = raw.getLong("count." + id, 0);
                if (count <= 0) continue;
                ensureSize(id);
                entries.set(id, new Entry(tpl, count));
            } catch (Exception ignored) {}
        }
        entries.removeIf(Objects::isNull);
    }

    private void ensureSize(int id) {
        while (entries.size() <= id) entries.add(null);
    }

    public List<Entry> getEntries() {
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries) if (e != null && e.count > 0) out.add(e);
        return out;
    }

    public int size() {
        int n = 0;
        for (Entry e : entries) if (e != null && e.count > 0) n++;
        return n;
    }

    public long totalOf(ItemStack stack) {
        for (Entry e : entries) {
            if (e == null || e.count <= 0) continue;
            if (isSame(e.template, stack)) return e.count;
        }
        return 0;
    }

    public long deposit(ItemStack stack, long amount) {
        if (stack == null || amount <= 0) return 0;
        long max = SoulRingConfig.getMaxStack();

        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (e == null || e.count <= 0) continue;
            if (!isSame(e.template, stack)) continue;
            long can = max < 0 ? amount : Math.min(amount, max - e.count);
            if (can <= 0) return 0;
            e.count += can;
            raw.set("count." + i, e.count);
            return can;
        }

        long can = max < 0 ? amount : Math.min(amount, max);
        int id = entries.size();
        ItemStack tpl = stack.clone();
        tpl.setAmount(1);
        entries.add(new Entry(tpl, can));
        raw.set("item." + id + ".data", serialize(tpl));
        raw.set("count." + id, can);
        return can;
    }

    public long takeByEntry(Entry entry, long amount) {
        if (entry == null || amount <= 0) return 0;
        long take = Math.min(amount, entry.count);
        if (take <= 0) return 0;
        entry.count -= take;
        int i = entries.indexOf(entry);
        if (i < 0) return take;
        if (entry.count <= 0) {
            entries.remove(i);
            raw.set("item." + i + ".data", null);
            raw.set("count." + i, null);
        } else {
            raw.set("count." + i, entry.count);
        }
        return take;
    }

    public long take(ItemStack ref, long amount) {
        if (ref == null || amount <= 0) return 0;
        for (Entry e : entries) {
            if (e == null || e.count <= 0) continue;
            if (!isSame(e.template, ref)) continue;
            return takeByEntry(e, amount);
        }
        return 0;
    }

    public ItemStack getTemplate(ItemStack ref) {
        for (Entry e : entries) {
            if (e == null || e.count <= 0) continue;
            if (isSame(e.template, ref)) return e.template.clone();
        }
        return null;
    }

    // ==================== 自动拾取开关 ====================

    /** 玩家个人自动拾取开关（-1 = 未设置，用默认） */
    public int getAutoPickupState() {
        return raw.getInt("autopickup", -1);
    }

    public void setAutoPickupState(int state) {
        raw.set("autopickup", state);
    }

    public boolean isAutoPickupOn() {
        int s = getAutoPickupState();
        if (s == -1) return SoulRingConfig.isAutoPickupDefaultOn();
        return s == 1;
    }

    public void save() { }

    private static boolean isSame(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        return a.isSimilar(b);
    }

    private static String serialize(ItemStack item) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos);
            boos.writeObject(item);
            boos.close();
            return Base64Coder.encodeLines(baos.toByteArray());
        } catch (Exception e) { return null; }
    }

    private static ItemStack deserialize(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            byte[] data = Base64Coder.decodeLines(s);
            BukkitObjectInputStream bois = new BukkitObjectInputStream(new ByteArrayInputStream(data));
            Object o = bois.readObject();
            bois.close();
            return (ItemStack) o;
        } catch (Exception e) { return null; }
    }

    public UUID getUuid() { return uuid; }
}