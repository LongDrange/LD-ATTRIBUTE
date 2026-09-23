package com.longdrange.ldattribute.rune;

import com.longdrange.ldattribute.points.PointAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RuneRecycleInventory {
    public static final String TITLE_PREFIX = "\u00a78[\u00a7d\u7b26\u6587\u56de\u6536\u00a78] ";
    public static final int PER_PAGE = 45;
    public static int SLOT_BACK = 48;
    public static int SLOT_RECYCLE_ALL = 50;
    public static int SLOT_PREV = 45;
    public static int SLOT_INFO = 49;
    public static int SLOT_NEXT = 53;

    private static final Map<UUID, Integer> lastPage = new HashMap<>();

    public static void open(Player player) { open(player, 0); }

    public static void open(Player player, int page) {
        // 收集背包所有符文（类型 → 数量）
        Map<String, Integer> owned = new LinkedHashMap<>();
        for (ItemStack it : player.getInventory().getContents()) {
            if (it == null) continue;
            String rid = RuneItem.getRuneId(it);
            if (rid == null) continue;
            owned.put(rid, owned.getOrDefault(rid, 0) + it.getAmount());
        }
        List<String> all = new ArrayList<>(owned.keySet());
        int total = all.size();
        if (total == 0) {
            player.sendMessage("\u00a7c\u4f60\u80cc\u5305\u88e1\u6c92\u6709\u7b26\u6587");
            return;
        }
        int maxPage = (total + PER_PAGE - 1) / PER_PAGE - 1;
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;

        long totalValue = 0;
        for (String rid : all) totalValue += getRecycleValue(rid) * owned.get(rid);

        String title = TITLE_PREFIX + "\u5171 " + total + " \u7a2e";
        if (title.replaceAll("\u00a7.", "").length() > 32) title = TITLE_PREFIX + total;
        Inventory inv = Bukkit.createInventory(null, 54, title);
        lastPage.put(player.getUniqueId(), page);

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = start + i;
            if (idx >= total) break;
            String rid = all.get(idx);
            RuneConfig.Rune r = RuneConfig.getRune(rid);
            if (r == null) continue;
            int count = owned.get(rid);
            int unit = getRecycleValue(rid);

            ItemStack show = RuneItem.create(r, 1);
            ItemMeta m = show.getItemMeta();
            List<String> lore = m.hasLore() ? new ArrayList<>(m.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add("\u00a77\u6301\u6709: \u00a7e" + count + " \u500b");
            lore.add("\u00a77\u55ae\u4f4d\u56de\u6536: \u00a76" + unit + " \u9ede\u5238");
            lore.add("\u00a77\u5168\u90e8\u56de\u6536: \u00a76" + (unit * count) + " \u9ede\u5238");
            lore.add("");
            lore.add("\u00a7e\u5de6\u9375 \u56de\u6536 1 \u500b");
            lore.add("\u00a7e\u53f3\u9375 \u56de\u6536\u5168\u90e8");
            m.setLore(lore);
            show.setItemMeta(m);
            inv.setItem(i, show);
        }

        // 底部分隔
        ItemStack sep = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta sepM = sep.getItemMeta(); sepM.setDisplayName(" "); sep.setItemMeta(sepM);
        for (int i = 45; i < 54; i++) {
            if (i == SLOT_BACK || i == SLOT_RECYCLE_ALL || i == SLOT_PREV || i == SLOT_INFO || i == SLOT_NEXT) continue;
            inv.setItem(i, sep);
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta(); bm.setDisplayName("\u00a7c\u00a7l\u2190 \u8fd4\u56de"); back.setItemMeta(bm);
        inv.setItem(SLOT_BACK, back);

        if (page > 0) {
            ItemStack p = new ItemStack(Material.PAPER);
            ItemMeta pm = p.getItemMeta(); pm.setDisplayName("\u00a7e\u4e0a\u4e00\u9801"); p.setItemMeta(pm);
            inv.setItem(SLOT_PREV, p);
        }
        if (page < maxPage) {
            ItemStack p = new ItemStack(Material.PAPER);
            ItemMeta pm = p.getItemMeta(); pm.setDisplayName("\u00a7e\u4e0b\u4e00\u9801"); p.setItemMeta(pm);
            inv.setItem(SLOT_NEXT, p);
        }

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("\u00a7a\u56de\u6536\u7e3d\u50f9\u503c");
        im.setLore(Arrays.asList(
                "\u00a77\u7b26\u6587\u7a2e\u985e: \u00a7e" + total,
                "\u00a77\u7e3d\u50f9\u503c: \u00a76" + totalValue + " \u9ede\u5238"
        ));
        info.setItemMeta(im);
        inv.setItem(SLOT_INFO, info);

        ItemStack all2 = new ItemStack(Material.HOPPER);
        ItemMeta am = all2.getItemMeta();
        am.setDisplayName("\u00a7c\u00a7l\u56de\u6536\u5168\u90e8\u7b26\u6587");
        am.setLore(Arrays.asList(
                "\u00a77\u5c07\u80cc\u5305\u88e1\u6240\u6709\u7b26\u6587\u56de\u6536",
                "\u00a77\u7e3d\u5171\u7372\u5f97: \u00a76" + totalValue + " \u9ede\u5238",
                "",
                "\u00a7c\u2718 \u6b64\u64cd\u4f5c\u4e0d\u53ef\u64a4\u9500"
        ));
        all2.setItemMeta(am);
        inv.setItem(SLOT_RECYCLE_ALL, all2);

        player.openInventory(inv);
    }

    /** 按符文 ID 推断回收价 */
    public static int getRecycleValue(String runeId) {
        if (runeId == null || runeId.isEmpty()) return 0;
        char last = runeId.charAt(runeId.length() - 1);
        if (runeId.startsWith("season_")) {
            if (last == '1') return 8000;
            if (last == '2') return 20000;
            if (last == '3') return 50000;
        }
        if (runeId.startsWith("event_")) return 5000;
        if (runeId.startsWith("fate_")) return 80000;
        if (last >= '1' && last <= '6') {
            switch (last) {
                case '1': return 100;
                case '2': return 300;
                case '3': return 800;
                case '4': return 2000;
                case '5': return 5000;
                case '6': return 12000;
            }
        }
        return 100;
    }

    /** 回收：只扣指定 runeId 的符文，返回实际回收的点券 */
    public static int recycle(Player player, String runeId, int amount) {
        if (amount <= 0) return 0;
        int recycled = 0;
        for (int i = 0; i < player.getInventory().getSize() && recycled < amount; i++) {
            ItemStack it = player.getInventory().getItem(i);
            if (it == null) continue;
            String rid = RuneItem.getRuneId(it);
            if (!runeId.equals(rid)) continue;
            int take = Math.min(it.getAmount(), amount - recycled);
            it.setAmount(it.getAmount() - take);
            recycled += take;
            if (it.getAmount() <= 0) player.getInventory().setItem(i, null);
        }
        if (recycled == 0) return 0;
        int gain = getRecycleValue(runeId) * recycled;
        PointAPI.addPlayerPoints(player.getName(), gain);
        return gain;
    }

    public static int getLastPage(UUID uuid) { return lastPage.getOrDefault(uuid, 0); }

    public static boolean isRecycle(String title) { return title.startsWith(TITLE_PREFIX); }
}