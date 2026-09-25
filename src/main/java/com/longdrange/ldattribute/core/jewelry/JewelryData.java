package com.longdrange.ldattribute.core.jewelry;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.data.PlayerModuleData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.*;
import java.util.*;

public class JewelryData {

    public static final String MODULE = "jewelry";

    private final LDAttribute plugin;
    private final UUID uuid;
    private final PlayerModuleData raw;

    private final Map<String, ItemStack> items = new LinkedHashMap<>();

    public JewelryData(LDAttribute plugin, UUID uuid) {
        this.plugin = plugin;
        this.uuid = uuid;
        this.raw = plugin.getModuleDataManager().get(uuid, MODULE);
        loadFrom(raw);
    }

    private void loadFrom(PlayerModuleData data) {
        items.clear();
        for (Map.Entry<String, Object> e : data.getRaw().entrySet()) {
            String key = e.getKey();
            if (!key.startsWith("slot.")) continue;
            String slotId = key.substring(5);
            ItemStack it = deserialize(String.valueOf(e.getValue()));
            if (it != null) items.put(slotId, it);
        }
    }

    public ItemStack get(String slotId) { return items.get(slotId); }
    public boolean has(String slotId) { return items.containsKey(slotId); }
    public Map<String, ItemStack> getAllItems() { return items; }

    public void set(String slotId, ItemStack item) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) {
            items.remove(slotId);
            raw.set("slot." + slotId, null);
        } else {
            items.put(slotId, item);
            raw.set("slot." + slotId, serialize(item));
        }
    }

    public void save() {
        for (Map.Entry<String, ItemStack> e : items.entrySet())
            raw.set("slot." + e.getKey(), serialize(e.getValue()));
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