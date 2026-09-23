package com.longdrange.ldattribute.card.inventory;

import com.longdrange.ldattribute.card.StatsDataRead;
import com.longdrange.ldattribute.card.StatsGUIConfig;
import com.longdrange.ldattribute.card.StatsSourceTracker;
import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import com.longdrange.ldattribute.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class StatsInventory {

    public static int SLOT_PREV = 45;
    public static int SLOT_INFO = 48;
    public static int SLOT_BACK = 49;
    public static int SLOT_NEXT = 53;
    public static final int PER_PAGE = 45;

    private static final Map<UUID, Integer> lastPage = new HashMap<>();

    public static void open(Player player) { open(player, 0); }

    public static void open(Player player, int page) {
        LDAttributeData data = StatsDataRead.loadPlayerStats(player);
        Map<String, Double> attrValues = new LinkedHashMap<>();
        for (LDSubAttribute a : data.getAttributeMap().values()) {
            attrValues.put(a.getName(), a.getValue());
        }
        boolean hideZero = StatsGUIConfig.isHideZero();

        List<String> displayList;
        if (StatsGUIConfig.isDynamic()) displayList = buildDynamicList(attrValues, hideZero);
        else displayList = buildStaticList();

        Map<String, List<StatsSourceTracker.Line>> sourceMap = new HashMap<>();
        try { sourceMap = StatsSourceTracker.track(player); } catch (Throwable ignored) {}

        int total = displayList.size();
        int maxPage = Math.max(0, (total + PER_PAGE - 1) / PER_PAGE - 1);
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;
        lastPage.put(player.getUniqueId(), page);

        String base = StatsGUIConfig.getTitle();
        String plain = base.replaceAll("\u00a7.", "");
        String title = (plain + " (X/Y)").length() <= 32
                ? base + " \u00a77(" + (page + 1) + "/" + (maxPage + 1) + ")"
                : base;

        int rows = Math.max(StatsGUIConfig.getRows(), 6);
        Inventory inv = Bukkit.createInventory(null, rows * 9, title);

        int start = page * PER_PAGE;
        int slot = 0;
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = start + i;
            if (idx >= total) break;
            String key = displayList.get(idx);
            if (key == null || key.isEmpty()) continue;

            String displayValue;
            if (key.startsWith("%") && key.endsWith("%")) {
                displayValue = resolvePAPI(player, key);
                if (displayValue == null || displayValue.isEmpty()) continue;
            } else {
                Double v = attrValues.get(key);
                if (v == null) v = 0.0;
                if (hideZero && v == 0) continue;
                displayValue = LDSubAttribute.getDf().format(v);
            }
            inv.setItem(slot, buildIcon(player, key, displayValue, sourceMap.get(key)));
            slot++;
        }

        // 底部分隔
        ItemStack sep = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta sepMeta = sep.getItemMeta();
        sepMeta.setDisplayName(" ");
        sep.setItemMeta(sepMeta);
        int bottomStart = (rows - 1) * 9;
        for (int i = bottomStart; i < rows * 9; i++) {
            if (i == SLOT_BACK || i == SLOT_PREV || i == SLOT_NEXT || i == SLOT_INFO) continue;
            inv.setItem(i, sep);
        }

        // 上一页
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pm = prev.getItemMeta();
            pm.setDisplayName("\u00a7e\u00a7l\u2190 \u4e0a\u4e00\u9875");
            pm.setLore(Arrays.asList("\u00a77\u8fd4\u56de\u7b2c \u00a7e" + page + " \u00a77\u9875"));
            prev.setItemMeta(pm);
            inv.setItem(SLOT_PREV, prev);
        }

        // 下一页
        if (page < maxPage) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nm = next.getItemMeta();
            nm.setDisplayName("\u00a7e\u00a7l\u4e0b\u4e00\u9875 \u2192");
            nm.setLore(Arrays.asList("\u00a77\u524d\u5f80\u7b2c \u00a7e" + (page + 2) + " \u00a77\u9875"));
            next.setItemMeta(nm);
            inv.setItem(SLOT_NEXT, next);
        }

        // 属性统计（列出所有非零属性）
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("\u00a7a\u00a7l\u5c5e\u6027\u7edf\u8ba1");
        List<String> infoLore = new ArrayList<>();
        infoLore.add("\u00a77\u5c5e\u6027\u79cd\u7c7b: \u00a7e" + total);
        infoLore.add("\u00a77\u5f53\u524d\u9875: \u00a7e\u7b2c " + (page + 1) + " \u00a77/ \u00a7e" + (maxPage + 1) + " \u9875");
        infoLore.add("\u00a78\u00a7m--------");
        infoLore.add("\u00a77\u258c \u00a7f\u6240\u6709\u5c5e\u6027:");
        int listed = 0;
        for (String k : displayList) {
            if (k.startsWith("%") && k.endsWith("%")) continue;
            Double v = attrValues.get(k);
            if (v == null || v == 0) continue;
            infoLore.add("  \u00a78\u00b7 \u00a7f" + k + " \u00a77= \u00a7e" + LDSubAttribute.getDf().format(v));
            listed++;
        }
        if (listed == 0) infoLore.add("  \u00a78(\u7121\u975e\u96f6\u5c5e\u6027)");
        im.setLore(infoLore);
        info.setItemMeta(im);
        inv.setItem(SLOT_INFO, info);

        // 返回
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("\u00a7c\u00a7l\u2190 \u8fd4\u56de");
        backMeta.setLore(Arrays.asList("\u00a77\u8fd4\u56de\u5361\u7247\u80cc\u5305"));
        back.setItemMeta(backMeta);
        inv.setItem(SLOT_BACK, back);

        player.openInventory(inv);
    }

    public static int getLastPage(UUID uuid) { return lastPage.getOrDefault(uuid, 0); }

    private static List<String> buildDynamicList(Map<String, Double> attrs, boolean hideZero) {
        List<String> result = new ArrayList<>();
        List<String> order = StatsGUIConfig.getOrder();
        Set<String> used = new HashSet<>();
        for (String k : order) {
            if (attrs.containsKey(k)) {
                Double v = attrs.get(k);
                if (hideZero && (v == null || v == 0)) continue;
                result.add(k);
                used.add(k);
            }
        }
        for (Map.Entry<String, Double> e : attrs.entrySet()) {
            if (used.contains(e.getKey())) continue;
            if (hideZero && (e.getValue() == null || e.getValue() == 0)) continue;
            result.add(e.getKey());
        }
        return result;
    }

    private static List<String> buildStaticList() {
        List<String> result = new ArrayList<>();
        String[][] layout = StatsGUIConfig.getLayout();
        for (String[] row : layout) {
            for (String s : row) result.add(s == null ? "" : s);
        }
        return result;
    }

    private static ItemStack buildIcon(Player player, String key, String displayValue,
                                        java.util.List<StatsSourceTracker.Line> sources) {
        StatsGUIConfig.IconDef def = StatsGUIConfig.getIcon(key);
        Material mat = def != null ? def.material : pickMaterial(key);

        String name;
        List<String> lore = new ArrayList<>();
        String localized = Message.getOrNull("Attr." + key);
        boolean hasI18n = localized != null && !localized.isEmpty();

        if (def != null) {
            name = hasI18n ? localized : def.name;
            lore.addAll(def.lore);
        } else {
            name = hasI18n ? localized : "\u00a7b" + key;
            String sub = Message.getOrNull("AttrDesc." + key);
            if (sub != null && !sub.isEmpty()) lore.add(sub);
            else lore.add("\u00a77\u5c5e\u6027");
            lore.add("\u00a77\u5f53\u524d: \u00a7e%value%");
        }

        name = ChatColor.translateAlternateColorCodes('&', name.replace("%value%", displayValue));
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(ChatColor.translateAlternateColorCodes('&',
                    line.replace("%value%", displayValue)));
        }

        if (sources != null && !sources.isEmpty()) {
            coloredLore.add("");
            coloredLore.add("\u00a78\u00a7m--------");
            coloredLore.add("\u00a77\u258c \u00a7f\u5c5e\u6027\u6765\u6e90");
            double sum = 0;
            for (StatsSourceTracker.Line ln : sources) {
                coloredLore.add("  \u00a78\u00b7 \u00a7f" + ln.source + " \u00a77+\u00a7e"
                        + StatsSourceTracker.fmt(ln.value));
                sum += ln.value;
            }
            coloredLore.add("\u00a78\u00a7m--------");
            coloredLore.add("\u00a77\u258c \u00a7f\u5408\u8ba1: \u00a7e" + StatsSourceTracker.fmt(sum));
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (!coloredLore.isEmpty()) meta.setLore(coloredLore);
        item.setItemMeta(meta);
        return item;
    }

    private static Material pickMaterial(String name) {
        String n = name.toLowerCase();
        if (n.contains("\u653b\u51fb") || n.contains("\u4f24\u5bb3") || n.contains("\u7834\u7532") || n.contains("\u7a7f\u900f")) return Material.DIAMOND_SWORD;
        if (n.contains("\u9632\u5fa1") || n.contains("\u62a4\u7532") || n.contains("\u97e7") || n.contains("\u6297")) return Material.DIAMOND_CHESTPLATE;
        if (n.contains("\u751f\u547d") || n.contains("\u56de\u8840") || n.contains("\u6062\u590d")) return Material.GOLDEN_APPLE;
        if (n.contains("\u66b4\u51fb") || n.contains("\u66b4\u4f24")) return Material.BLAZE_POWDER;
        if (n.contains("\u901f\u5ea6")) return Material.FEATHER;
        if (n.contains("\u5438\u8840") || n.contains("\u71c3\u70e7")) return Material.REDSTONE;
        if (n.contains("\u96f7\u970d") || n.contains("\u95ea\u7535")) return Material.NETHER_STAR;
        if (n.contains("\u6cd5\u672f") || n.contains("\u6cd5\u529b")) return Material.ENDER_PEARL;
        if (n.contains("\u95ea\u907f") || n.contains("\u683c\u6321") || n.contains("\u53cd\u5c04")) return Material.SHIELD;
        if (n.contains("\u5e78\u8fd0")) return Material.RABBIT_FOOT;
        if (n.contains("\u7ecf\u9a8c")) return Material.EXP_BOTTLE;
        if (n.contains("\u547d\u4e2d")) return Material.BOW;
        return Material.PAPER;
    }

    private static String resolvePAPI(Player player, String placeholder) {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, placeholder);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static boolean isStatsInventory(String title) {
        return title.equals(StatsGUIConfig.getTitle()) || title.startsWith(StatsGUIConfig.getTitle());
    }
}