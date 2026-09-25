package com.longdrange.ldattribute.core.guide;

import org.bukkit.entity.Entity;

import java.lang.reflect.Method;

/**
 * MythicMobs 兼容工具
 *
 * 兼容的版本：
 *   4.x (io.lumine.xikage.mythicmobs.MythicMobs)
 *   5.x (io.lumine.mythic.bukkit.MythicBukkit)
 *
 * 通过多重反射尝试以下路径：
 *   A) inst().getMobManager().getMythicMobInstance(entity)
 *   B) inst().getAPIHelper().getMythicMobInstance(entity)
 * 再从 ActiveMob 拿 getType().getInternalName()
 */
public class MythicMobsHelper {

    private static final String[] API_CLASSES = {
            // 5.x
            "io.lumine.mythic.bukkit.MythicBukkit",
            "io.lumine.mythic.core.MythicMobs",
            // 4.x
            "io.lumine.xikage.mythicmobs.MythicMobs"
    };

    /** 拿到 MM 怪物内部名；不是 MM 怪物返回 null */
    public static String getMobId(Entity entity) {
        if (entity == null) return null;
        for (String cls : API_CLASSES) {
            String id = tryByClass(entity, cls);
            if (id != null) return id;
        }
        return null;
    }

    public static boolean isMythicMob(Entity entity) {
        return getMobId(entity) != null;
    }

    // ==================== 内部 ====================

    private static String tryByClass(Entity entity, String className) {
        try {
            Class<?> api = Class.forName(className);
            Object inst = invokeStatic(api, "inst");
            if (inst == null) return null;

            Object activeMob = null;

            // 路径 A: getMobManager().getMythicMobInstance(entity)
            Object mobManager = invokeNoArgs(inst, "getMobManager");
            if (mobManager != null) {
                activeMob = invokeOneArg(mobManager, "getMythicMobInstance", entity);
            }

            // 路径 B: getAPIHelper().getMythicMobInstance(entity)
            if (activeMob == null) {
                Object apiHelper = invokeNoArgs(inst, "getAPIHelper");
                if (apiHelper != null) {
                    activeMob = invokeOneArg(apiHelper, "getMythicMobInstance", entity);
                }
            }

            if (activeMob == null) return null;

            // 拿 type
            Object mobType = invokeNoArgs(activeMob, "getType");
            if (mobType == null) mobType = invokeNoArgs(activeMob, "getMobType");
            if (mobType == null) return null;

            // 拿 name（内部名）
            Object name = invokeNoArgs(mobType, "getInternalName");
            if (name == null) name = invokeNoArgs(mobType, "getName");
            return name == null ? null : name.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object invokeStatic(Class<?> cls, String method) {
        try {
            Method m = cls.getMethod(method);
            return m.invoke(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object invokeNoArgs(Object obj, String method) {
        if (obj == null) return null;
        try {
            Method m = obj.getClass().getMethod(method);
            return m.invoke(obj);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object invokeOneArg(Object obj, String method, Object arg) {
        if (obj == null || arg == null) return null;
        try {
            for (Method m : obj.getClass().getMethods()) {
                if (!m.getName().equals(method)) continue;
                if (m.getParameterCount() != 1) continue;
                Class<?> p = m.getParameterTypes()[0];
                if (!p.isAssignableFrom(arg.getClass())) continue;
                return m.invoke(obj, arg);
            }
        } catch (Throwable ignored) {}
        return null;
    }
}