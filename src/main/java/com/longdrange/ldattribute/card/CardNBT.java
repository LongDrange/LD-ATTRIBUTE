package com.longdrange.ldattribute.card;

import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

public class CardNBT {

    private static Method asNMSCopy;
    private static Method asCraftMirror;
    private static Method hasTag;
    private static Method getTag;
    private static Method setTag;
    private static Class<?> nbtCls;
    private static Class<?> nmsItemCls;

    private static Method nbtGetInt, nbtSetInt, nbtGetString, nbtSetString;
    private static Method nbtGetCompound, nbtSetCompound, nbtHasKey;

    private static boolean ready = false;

    static {
        try {
            nmsItemCls = Class.forName("net.minecraft.server.v1_12_R1.ItemStack");
            nbtCls = Class.forName("net.minecraft.server.v1_12_R1.NBTTagCompound");
            Class<?> cis = Class.forName("org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack");
            asNMSCopy = cis.getMethod("asNMSCopy", ItemStack.class);
            asCraftMirror = cis.getMethod("asCraftMirror", nmsItemCls);
            hasTag = nmsItemCls.getMethod("hasTag");
            getTag = nmsItemCls.getMethod("getTag");
            setTag = nmsItemCls.getMethod("setTag", nbtCls);
            nbtGetInt = nbtCls.getMethod("getInt", String.class);
            nbtSetInt = nbtCls.getMethod("setInt", String.class, int.class);
            nbtGetString = nbtCls.getMethod("getString", String.class);
            nbtSetString = nbtCls.getMethod("setString", String.class, String.class);
            nbtGetCompound = nbtCls.getMethod("getCompound", String.class);
            nbtSetCompound = nbtCls.getMethod("set", String.class, Class.forName("net.minecraft.server.v1_12_R1.NBTBase"));
            nbtHasKey = nbtCls.getMethod("hasKey", String.class);
            ready = true;
        } catch (Throwable t) {
            System.out.println("[LD-Attribute] CardNBT 反射初始化失敗: " + t.getMessage());
        }
    }

    public static boolean isReady() { return ready; }

    private static Object getCompound(Object nbt) throws Exception {
        if (nbt == null) return null;
        if (!(Boolean) nbtHasKey.invoke(nbt, "LDAttribute")) return null;
        return nbtGetCompound.invoke(nbt, "LDAttribute");
    }

    public static int getInt(ItemStack item, String key, int def) {
        if (!ready || item == null) return def;
        try {
            Object nms = asNMSCopy.invoke(null, item);
            if (nms == null || !(Boolean) hasTag.invoke(nms)) return def;
            Object sub = getCompound(getTag.invoke(nms));
            if (sub == null) return def;
            return (Integer) nbtGetInt.invoke(sub, key);
        } catch (Throwable t) { return def; }
    }

    public static String getString(ItemStack item, String key, String def) {
        if (!ready || item == null) return def;
        try {
            Object nms = asNMSCopy.invoke(null, item);
            if (nms == null || !(Boolean) hasTag.invoke(nms)) return def;
            Object sub = getCompound(getTag.invoke(nms));
            if (sub == null) return def;
            String v = (String) nbtGetString.invoke(sub, key);
            return v == null || v.isEmpty() ? def : v;
        } catch (Throwable t) { return def; }
    }

    public static ItemStack setInt(ItemStack item, String key, int value) {
        if (!ready || item == null) return item;
        try {
            Object nms = asNMSCopy.invoke(null, item);
            Object tag = (Boolean) hasTag.invoke(nms) ? getTag.invoke(nms)
                    : nbtCls.getConstructor().newInstance();
            Object sub = getCompound(tag);
            if (sub == null) sub = nbtCls.getConstructor().newInstance();
            nbtSetInt.invoke(sub, key, value);
            nbtSetCompound.invoke(tag, "LDAttribute", sub);
            setTag.invoke(nms, tag);
            return (ItemStack) asCraftMirror.invoke(null, nms);
        } catch (Throwable t) { return item; }
    }

    public static ItemStack setString(ItemStack item, String key, String value) {
        if (!ready || item == null) return item;
        try {
            Object nms = asNMSCopy.invoke(null, item);
            Object tag = (Boolean) hasTag.invoke(nms) ? getTag.invoke(nms)
                    : nbtCls.getConstructor().newInstance();
            Object sub = getCompound(tag);
            if (sub == null) sub = nbtCls.getConstructor().newInstance();
            nbtSetString.invoke(sub, key, value == null ? "" : value);
            nbtSetCompound.invoke(tag, "LDAttribute", sub);
            setTag.invoke(nms, tag);
            return (ItemStack) asCraftMirror.invoke(null, nms);
        } catch (Throwable t) { return item; }
    }

    // ===== 等級/經驗 =====
    public static int getLevel(ItemStack item) { return getInt(item, "level", 1); }
    public static int getExp(ItemStack item)   { return getInt(item, "exp", 0); }
    public static ItemStack setLevel(ItemStack item, int level) { return setInt(item, "level", level); }
    public static ItemStack setExp(ItemStack item, int exp)     { return setInt(item, "exp", exp); }

    // ===== 鎖定 =====
    public static boolean isLocked(ItemStack item) { return getInt(item, "locked", 0) == 1; }
    public static ItemStack setLocked(ItemStack item, boolean locked) { return setInt(item, "locked", locked ? 1 : 0); }

    // ===== 星級 =====
    public static int getStar(ItemStack item) { return getInt(item, "star", 0); }
    public static ItemStack setStar(ItemStack item, int star) { return setInt(item, "star", Math.max(0, star)); }

    // ===== 法術書 =====
    public static String getSpell(ItemStack item) { return getString(item, "spell", ""); }
    public static ItemStack setSpell(ItemStack item, String spellId) { return setString(item, "spell", spellId); }
    public static int getSpellLevel(ItemStack item) { int v = getInt(item, "spell_level", 1); return v < 1 ? 1 : v; }
    public static ItemStack setSpellLevel(ItemStack item, int level) { return setInt(item, "spell_level", Math.max(1, level)); }

    // ===== 綁定 =====
    public static boolean isBound(ItemStack item) { return !getString(item, "boundUUID", "").isEmpty(); }
    public static String getBoundUUID(ItemStack item) { return getString(item, "boundUUID", ""); }
    public static String getBoundName(ItemStack item) { return getString(item, "boundName", ""); }

    public static ItemStack setBound(ItemStack item, String uuid, String name) {
        ItemStack r = setString(item, "boundUUID", uuid);
        r = setString(r, "boundName", name);
        return r;
    }

    public static ItemStack clearBound(ItemStack item) {
        ItemStack r = setString(item, "boundUUID", "");
        r = setString(r, "boundName", "");
        return r;
    }
    // ===== 符文孔位 =====
    public static boolean isSocketUnlocked(ItemStack item, int index) {
        return (getInt(item, "socket_mask", 0) & (1 << index)) != 0;
    }

    public static ItemStack unlockSocket(ItemStack item, int index) {
        int mask = getInt(item, "socket_mask", 0);
        mask |= (1 << index);
        return setInt(item, "socket_mask", mask);
    }

    public static String getSocketRune(ItemStack item, int index) {
        return getString(item, "socket_" + index, "");
    }

    public static ItemStack setSocketRune(ItemStack item, int index, String runeId) {
        return setString(item, "socket_" + index, runeId == null ? "" : runeId);
    }
    // ===== 符文锁（整卡） =====
    public static boolean isRuneLocked(ItemStack item) {
        return getInt(item, "rune_lock", 0) == 1;
    }

    public static ItemStack setRuneLocked(ItemStack item, boolean locked) {
        return setInt(item, "rune_lock", locked ? 1 : 0);
    }
}
