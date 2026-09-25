package com.longdrange.ldattribute.core.ring;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.data.attribute.LDAttributeManager;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import org.bukkit.entity.Player;

import java.util.*;

public class RingStatsProvider {

    private static final Map<Character, Character> T2S = new HashMap<>();
    static {
        put('擊','击'); put('機','机'); put('傷','伤'); put('閃','闪');
        put('緩','缓'); put('實','实'); put('經','经'); put('驗','验');
        put('訊','讯'); put('動','动'); put('遠','远'); put('燒','烧');
        put('復','复'); put('護','护'); put('術','术'); put('韌','韧');
        put('擋','挡'); put('幾','几'); put('氣','气'); put('體','体');
        put('數','数'); put('種','种'); put('類','类'); put('範','范');
        put('圍','围'); put('轉','转'); put('變','变'); put('換','换');
        put('強','强'); put('優','优'); put('獲','获'); put('總','总');
        put('減','减'); put('補','补'); put('額','额'); put('級','级');
        put('階','阶'); put('層','层'); put('項','项'); put('裝','装');
        put('備','备'); put('狀','状'); put('態','态'); put('敵','敌');
        put('對','对'); put('無','无'); put('鎖','锁'); put('鑰','钥');
        put('啟','启'); put('開','开'); put('關','关'); put('暫','暂');
        put('時','时'); put('間','间'); put('頻','频'); put('價','价');
        put('點','点'); put('殺','杀'); put('滅','灭'); put('飛','飞');
        put('躍','跃'); put('衝','冲'); put('現','现'); put('準','准');
        put('確','确'); put('運','运'); put('極','极');
    }
    private static void put(char tw, char cn) { T2S.put(tw, cn); }

    private static String toSimplified(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            Character sub = T2S.get(s.charAt(i));
            sb.append(sub == null ? s.charAt(i) : sub);
        }
        return sb.toString();
    }

    /** 属性名归一化：先走映射表，再走简繁归一化，再走已注册表 */
    public static String normalize(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        // 1. 用户配置的别名映射（优先级最高）
        raw = RingConfig.mapAttribute(raw);

        raw = raw.replace("+", "").replace("%", "").trim();
        Map<Integer, LDSubAttribute> registered = getAllRegistered();
        if (registered.isEmpty()) return raw;

        // 2. 精确匹配
        for (LDSubAttribute a : registered.values())
            if (a.getName().equals(raw)) return a.getName();

        // 3. 简繁归一化匹配
        String simp = toSimplified(raw);
        for (LDSubAttribute a : registered.values())
            if (toSimplified(a.getName()).equals(simp)) return a.getName();

        // 4. 去空格匹配
        String nospace = raw.replace(" ", "");
        for (LDSubAttribute a : registered.values())
            if (a.getName().replace(" ", "").equals(nospace)) return a.getName();

        return raw;
    }

    private static Map<Integer, LDSubAttribute> getAllRegistered() {
        try {
            Map<Integer, LDSubAttribute> m = LDAttributeManager.cloneAttributeMap();
            return m == null ? Collections.<Integer, LDSubAttribute>emptyMap() : m;
        } catch (Throwable t) { return Collections.emptyMap(); }
    }

    public static Map<String, Double> getTotal(Player player) {
        if (player == null) return Collections.emptyMap();
        try {
            LDAttribute plugin = LDAttribute.getInstance();
            if (plugin == null || plugin.getCoreManager() == null) return Collections.emptyMap();
            RingManager mgr = plugin.getCoreManager().getRingManager();
            if (mgr == null) return Collections.emptyMap();
            RingData data = mgr.get(player);
            if (data == null) return Collections.emptyMap();
            Map<String, Double> raw = RingAttributeReader.sumAll(data);
            // 套装属性
            try {
                Map<String, Double> setAttrs = RingSetConfig.collectActiveAttributes(data);
                if (!setAttrs.isEmpty()) {
                    for (Map.Entry<String, Double> se : setAttrs.entrySet()) {
                        raw.merge(se.getKey(), se.getValue(), Double::sum);
                    }
                    if (LDAttribute.getInstance().getConfig().getBoolean("debug", false)) {
                        LDAttribute.getInstance().getLogger().info(
                                "[Ring] " + player.getName() + " 触发套装属性: " + setAttrs);
                    }
                }
            } catch (Throwable t) {
                LDAttribute.getInstance().getLogger().warning("[Ring] 套装属性注入失败: " + t.getMessage());
            }
            Map<String, Double> out = new LinkedHashMap<>();
            for (Map.Entry<String, Double> e : raw.entrySet())
                out.merge(normalize(e.getKey()), e.getValue(), Double::sum);
            return out;
        } catch (Throwable t) { return Collections.emptyMap(); }
    }

    public static int count(Player player, String ringType) {
        if (player == null || ringType == null) return 0;
        try {
            RingManager mgr = LDAttribute.getInstance().getCoreManager().getRingManager();
            if (mgr == null) return 0;
            RingData d = mgr.get(player);
            return d == null ? 0 : d.countType(ringType);
        } catch (Throwable t) { return 0; }
    }

    /** 该类型上限（从已放入的槽位读，没放入返回 -1） */
    public static int getMax(Player player, String ringType) {
        try {
            RingData d = LDAttribute.getInstance().getCoreManager().getRingManager().get(player);
            if (d == null) return -1;
            int sid = d.findSlotOfType(ringType);
            if (sid < 0) return -1;
            RingData.Slot s = d.getSlot(sid);
            return s == null ? -1 : s.maxStack;
        } catch (Throwable t) { return -1; }
    }

    /** 还能放多少个 */
    public static int getRemaining(Player player, String ringType) {
        int max = getMax(player, ringType);
        if (max < 0) return -1;
        return Math.max(0, max - count(player, ringType));
    }

    public static int getUnlockedSlotCount(Player player) {
        try { return LDAttribute.getInstance().getCoreManager().getRingManager().get(player).getUnlockedSlots().size(); }
        catch (Throwable t) { return 0; }
    }

    public static int getUnlockedPages(Player player) {
        try { return LDAttribute.getInstance().getCoreManager().getRingManager().get(player).getUnlockedPages(); }
        catch (Throwable t) { return 0; }
    }

    public static int getUsedTypes(Player player) {
        try { return LDAttribute.getInstance().getCoreManager().getRingManager().get(player).getAllSlots().size(); }
        catch (Throwable t) { return 0; }
    }

    public static List<String> getRingLore(Player player) {
        List<String> out = new ArrayList<>();
        Map<String, Double> total = getTotal(player);
        for (Map.Entry<String, Double> e : total.entrySet())
            out.add(e.getKey() + ": " + format(e.getValue()));
        return out;
    }

    public static List<String> listAllRegistered() {
        List<String> out = new ArrayList<>();
        try {
            for (LDSubAttribute a : getAllRegistered().values()) out.add(a.getName());
            Collections.sort(out);
        } catch (Throwable ignored) {}
        return out;
    }

    public static String format(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((long) v);
        return String.format("%.1f", v);
    }
}