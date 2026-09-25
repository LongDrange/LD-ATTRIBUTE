package com.longdrange.ldattribute.core.soulring.exchange;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class ExchangeConfig {

    public enum SourceType { BOTH, SOULRING, INVENTORY, VAULT, POINT, VALUE }
    public enum MatchMode { LOOSE, STRICT, LORE, NAME }

    public static class ItemLine {
        public final SourceType source;
        public final Material material;
        public final int amount;
        public final short data;
        public final String valueId;
        public final String raw;
        public final String nameHint;
        public final List<String> lore;

        public ItemLine(SourceType source, Material material, int amount, short data,
                        String valueId, String raw, String nameHint, List<String> lore) {
            this.source = source; this.material = material; this.amount = amount;
            this.data = data; this.valueId = valueId; this.raw = raw;
            this.nameHint = nameHint;
            this.lore = lore == null ? new ArrayList<String>() : lore;
        }

        public String prettyName() {
            if (nameHint != null && !nameHint.isEmpty()) return nameHint;
            if (material != null) {
                String s = material.name();
                String[] parts = s.toLowerCase().split("_");
                StringBuilder sb = new StringBuilder();
                for (String p : parts) {
                    if (p.isEmpty()) continue;
                    sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
                }
                return sb.toString().trim();
            }
            if (valueId != null) return valueId;
            return raw;
        }
    }

    public static class Exchange {
        public final String id;
        public final String name;
        public final String icon;
        public int slot;
        public final List<ItemLine> input;
        public final List<ItemLine> anyOf;
        public final List<ItemLine> output;
        public final double chance;
        public final int dailyLimit;
        public final int totalLimit;
        public final String permission;
        public final MatchMode matchMode;
        public final List<String> matchLore;
        public final String matchName;
        public final List<String> displayLore;
        public final String pageId;

        public Exchange(String id, String name, String icon, int slot,
                        List<ItemLine> input, List<ItemLine> anyOf, List<ItemLine> output,
                        double chance, int dailyLimit, int totalLimit,
                        String permission, MatchMode matchMode,
                        List<String> matchLore, String matchName,
                        List<String> displayLore, String pageId) {
            this.id = id; this.name = name; this.icon = icon; this.slot = slot;
            this.input = input; this.anyOf = anyOf; this.output = output;
            this.chance = chance; this.dailyLimit = dailyLimit; this.totalLimit = totalLimit;
            this.permission = permission; this.matchMode = matchMode;
            this.matchLore = matchLore; this.matchName = matchName;
            this.displayLore = displayLore; this.pageId = pageId;
        }
    }

    public static class Page {
        public final String id;
        public final String title;
        public final String permission;
        public final LinkedHashMap<String, Exchange> exchanges = new LinkedHashMap<>();
        public Page(String id, String title, String permission) {
            this.id = id; this.title = title; this.permission = permission;
        }
    }

    private static final LinkedHashMap<String, Page> pages = new LinkedHashMap<>();

    // ==================== 加载入口 ====================

    public static void load(LDAttribute plugin) {
        pages.clear();

        File root = new File(plugin.getDataFolder(), "配置/兑换商店");
        if (!root.exists()) {
            root.mkdirs();
            createDefaults(root);
        }

        File[] children = root.listFiles();
        if (children == null) children = new File[0];
        Arrays.sort(children, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        int totalEx = 0;
        for (File child : children) {
            String name = child.getName();
            if (name.startsWith(".")) continue;

            try {
                if (child.isDirectory()) {
                    // 文件夹 = 一页
                    Page page = loadPageFromFolder(plugin, child, name);
                    if (page != null && !page.exchanges.isEmpty()) {
                        pages.put(name, page);
                        totalEx += page.exchanges.size();
                    }
                } else if (name.toLowerCase().endsWith(".yml") || name.toLowerCase().endsWith(".yaml")) {
                    // 单文件 = 一页
                    String pageId = name.replaceAll("(?i)\\.(yml|yaml)$", "");
                    Page page = loadPageFromFile(plugin, child, pageId);
                    if (page != null && !page.exchanges.isEmpty()) {
                        pages.put(pageId, page);
                        totalEx += page.exchanges.size();
                    }
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("[Exchange] " + name + " 加载失败: " + t.getMessage());
            }
        }

        // 兼容旧的 exchanges.yml
        if (pages.isEmpty()) {
            File legacy = new File(plugin.getDataFolder(), "exchanges.yml");
            if (legacy.exists()) {
                plugin.getLogger().info("[Exchange] 尝试读旧 exchanges.yml");
                loadLegacy(plugin, legacy);
                totalEx = pages.values().stream().mapToInt(p -> p.exchanges.size()).sum();
            }
        }

        plugin.getLogger().info("[Exchange] 已加载 " + pages.size() + " 页 / " + totalEx + " 个兑换");
    }

    // ==================== 单文件 = 一页 ====================

    private static Page loadPageFromFile(LDAttribute plugin, File file, String pageId) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        String title = ChatColor.translateAlternateColorCodes('&',
                cfg.getString("Title", "&8\u2726 " + pageId));
        String permission = cfg.getString("Permission", "");
        Page page = new Page(pageId, title, permission);

        // 优先读 Exchanges 节点
        ConfigurationSection exSec = cfg.getConfigurationSection("Exchanges");
        if (exSec != null) {
            Set<Integer> usedSlots = new HashSet<>();
            for (String eid : exSec.getKeys(false)) {
                ConfigurationSection es = exSec.getConfigurationSection(eid);
                if (es == null) continue;
                try {
                    Exchange ex = parseExchange(es, eid, pageId, usedSlots);
                    if (ex != null) page.exchanges.put(eid, ex);
                } catch (Throwable t) {
                    plugin.getLogger().warning("[Exchange] " + pageId + "/" + eid + ": " + t.getMessage());
                }
            }
            return page;
        }

        // 没有 Exchanges 节点 → 整文件当一个兑换（文件名 = id）
        try {
            Exchange ex = parseExchange(cfg, pageId, pageId, new HashSet<>());
            if (ex != null) page.exchanges.put(pageId, ex);
        } catch (Throwable ignored) {}
        return page;
    }

    // ==================== 文件夹 = 一页 ====================

    private static Page loadPageFromFolder(LDAttribute plugin, File folder, String pageId) {
        String title = "&8\u2726 " + pageId;
        String permission = "";

        File metaFile = new File(folder, "_page.yml");
        if (!metaFile.exists()) metaFile = new File(folder, "_page.yaml");
        if (metaFile.exists()) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(metaFile);
            title = ChatColor.translateAlternateColorCodes('&', cfg.getString("Title", title));
            permission = cfg.getString("Permission", "");
        }
        Page page = new Page(pageId, title, permission);

        File[] files = folder.listFiles(f -> f.isFile()
                && (f.getName().toLowerCase().endsWith(".yml")
                    || f.getName().toLowerCase().endsWith(".yaml")));
        if (files == null) return page;
        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        Set<Integer> usedSlots = new HashSet<>();
        for (File file : files) {
            String fname = file.getName();
            if (fname.equalsIgnoreCase("_page.yml") || fname.equalsIgnoreCase("_page.yaml")) continue;

            String baseName = fname.replaceAll("(?i)\\.(yml|yaml)$", "");
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

            // 如果文件里有 Exchanges 节点 → 多兑换
            ConfigurationSection exSec = cfg.getConfigurationSection("Exchanges");
            if (exSec != null) {
                for (String eid : exSec.getKeys(false)) {
                    ConfigurationSection es = exSec.getConfigurationSection(eid);
                    if (es == null) continue;
                    try {
                        Exchange ex = parseExchange(es, eid, pageId, usedSlots);
                        if (ex != null) page.exchanges.put(eid, ex);
                    } catch (Throwable t) {
                        plugin.getLogger().warning("[Exchange] " + pageId + "/" + eid + ": " + t.getMessage());
                    }
                }
                continue;
            }

            // 否则 → 单兑换（文件名=id）
            try {
                Exchange ex = parseExchange(cfg, baseName, pageId, usedSlots);
                if (ex != null) page.exchanges.put(baseName, ex);
            } catch (Throwable t) {
                plugin.getLogger().warning("[Exchange] " + pageId + "/" + fname + ": " + t.getMessage());
            }
        }
        return page;
    }

    // ==================== 解析单个兑换 ====================

    private static Exchange parseExchange(ConfigurationSection es, String eid, String pageId, Set<Integer> usedSlots) {
        String name = ChatColor.translateAlternateColorCodes('&', es.getString("Name", eid));
        String icon = es.getString("Icon", "PAPER");
        int slot = es.getInt("Slot", -1);

        List<ItemLine> input = parseLines(es.getStringList("Input"), true);
        List<ItemLine> anyOf = parseLines(es.getStringList("AnyOf"), true);
        List<ItemLine> output = parseLines(es.getStringList("Output"), false);

        if (input.isEmpty() && anyOf.isEmpty()) return null;
        if (output.isEmpty()) return null;

        if (slot < 0 || slot >= 45 || usedSlots.contains(slot)) {
            slot = findFreeSlot(usedSlots);
            if (slot < 0) return null;
        }
        usedSlots.add(slot);

        double chance = es.getDouble("Chance", 100.0);
        int dailyLimit = es.getInt("DailyLimit", -1);
        int totalLimit = es.getInt("TotalLimit", -1);
        String permission = es.getString("Permission", "");

        MatchMode mm = MatchMode.LOOSE;
        try { mm = MatchMode.valueOf(es.getString("Match", "LOOSE").toUpperCase()); }
        catch (Exception ignored) {}

        List<String> matchLore = es.getStringList("MatchLore");
        if (matchLore == null) matchLore = new ArrayList<>();
        String matchName = es.getString("MatchName", "");

        List<String> displayLore = new ArrayList<>();
        for (String s : es.getStringList("Lore"))
            displayLore.add(ChatColor.translateAlternateColorCodes('&', s));

        return new Exchange(eid, name, icon, slot, input, anyOf, output,
                chance, dailyLimit, totalLimit, permission,
                mm, matchLore, matchName, displayLore, pageId);
    }

    private static int findFreeSlot(Set<Integer> used) {
        for (int i = 0; i < 45; i++) if (!used.contains(i)) return i;
        return -1;
    }

    // ==================== 解析 ItemLine ====================

    private static List<ItemLine> parseLines(List<String> list, boolean isInput) {
        List<ItemLine> out = new ArrayList<>();
        if (list == null) return out;

        List<String> normalized = new ArrayList<>();
        for (String raw : list) {
            if (raw == null || raw.trim().isEmpty()) continue;
            if (isItemDefinition(raw)) {
                normalized.add(raw);
            } else {
                if (normalized.isEmpty()) continue;
                int last = normalized.size() - 1;
                normalized.set(last, normalized.get(last) + "|" + raw);
            }
        }

        for (String raw : normalized) {
            ItemLine line = parseLine(raw, isInput);
            if (line != null) out.add(line);
        }
        return out;
    }

    private static boolean isItemDefinition(String raw) {
        String s = raw.trim();
        if (s.isEmpty()) return false;
        int atIdx = s.lastIndexOf('@');
        String head = atIdx >= 0 ? s.substring(0, atIdx) : s;
        int pipeIdx = head.indexOf('|');
        if (pipeIdx >= 0) head = head.substring(0, pipeIdx);

        String[] parts = head.split(":");
        if (parts.length < 2) return false;
        String first = parts[0].toUpperCase();
        if (first.equals("SOULRING") || first.equals("INVENTORY")
                || first.equals("VAULT") || first.equals("POINT")
                || first.equals("VALUE") || first.equals("CMD")) return true;
        Material m = Material.getMaterial(first);
        if (m == null) return false;
        try { Integer.parseInt(parts[1].trim()); return true; }
        catch (Exception e) { return false; }
    }

    private static ItemLine parseLine(String raw, boolean isInput) {
        List<String> lore = new ArrayList<>();
        String line = raw;
        int pipeIdx = raw.indexOf('|');
        if (pipeIdx >= 0) {
            line = raw.substring(0, pipeIdx);
            for (String l : raw.substring(pipeIdx + 1).split("\\|"))
                lore.add(ChatColor.translateAlternateColorCodes('&', l));
        }
        String nameHint = null;
        String head = line;
        int atIdx = line.lastIndexOf('@');
        if (atIdx >= 0) {
            nameHint = line.substring(atIdx + 1).trim();
            head = line.substring(0, atIdx);
        }

        String[] parts = head.split(":");
        if (parts.length == 0) return null;
        String first = parts[0].toUpperCase();

        if (isInput) {
            if (first.equals("SOULRING")) {
                if (parts.length < 3) return null;
                Material m = Material.getMaterial(parts[1].toUpperCase());
                if (m == null) return null;
                return new ItemLine(SourceType.SOULRING, m, parseInt(parts[2], 1),
                        (short) (parts.length >= 4 ? parseInt(parts[3], 0) : 0), null, raw, nameHint, lore);
            }
            if (first.equals("INVENTORY")) {
                if (parts.length < 3) return null;
                Material m = Material.getMaterial(parts[1].toUpperCase());
                if (m == null) return null;
                return new ItemLine(SourceType.INVENTORY, m, parseInt(parts[2], 1),
                        (short) (parts.length >= 4 ? parseInt(parts[3], 0) : 0), null, raw, nameHint, lore);
            }
            if (first.equals("VAULT")) return new ItemLine(SourceType.VAULT, null, parseInt(parts[1], 0), (short) 0, null, raw, nameHint, lore);
            if (first.equals("POINT")) return new ItemLine(SourceType.POINT, null, parseInt(parts[1], 0), (short) 0, null, raw, nameHint, lore);
            if (first.equals("VALUE")) {
                if (parts.length < 3) return null;
                return new ItemLine(SourceType.VALUE, null, parseInt(parts[2], 0), (short) 0, parts[1], raw, nameHint, lore);
            }
            Material m = Material.getMaterial(first);
            if (m != null) {
                return new ItemLine(SourceType.BOTH, m,
                        parts.length >= 2 ? parseInt(parts[1], 1) : 1,
                        (short) (parts.length >= 3 ? parseInt(parts[2], 0) : 0), null, raw, nameHint, lore);
            }
            return null;
        } else {
            if (first.equals("VAULT")) return new ItemLine(SourceType.VAULT, null, parseInt(parts[1], 0), (short) 0, null, raw, nameHint, lore);
            if (first.equals("POINT")) return new ItemLine(SourceType.POINT, null, parseInt(parts[1], 0), (short) 0, null, raw, nameHint, lore);
            if (first.equals("VALUE")) {
                if (parts.length < 3) return null;
                return new ItemLine(SourceType.VALUE, null, parseInt(parts[2], 0), (short) 0, parts[1], raw, nameHint, lore);
            }
            if (first.equals("CMD")) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < parts.length; i++) { if (i > 1) sb.append(':'); sb.append(parts[i]); }
                return new ItemLine(SourceType.VALUE, null, 0, (short) 0, "CMD:" + sb.toString(), raw, nameHint, lore);
            }
            Material m = Material.getMaterial(first);
            if (m != null) {
                return new ItemLine(SourceType.BOTH, m,
                        parts.length >= 2 ? parseInt(parts[1], 1) : 1,
                        (short) (parts.length >= 3 ? parseInt(parts[2], 0) : 0), null, raw, nameHint, lore);
            }
            return null;
        }
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    // ==================== 默认示例 ====================

    private static void createDefaults(File root) {
        try {
            String t1 = "# 页标题（支持 & 颜色码）\r\n" +
                    "Title: \"&8&l\u2726 T1 兑换\"\r\n" +
                    "# 打开此页的权限，留空=所有人\r\n" +
                    "Permission: \"\"\r\n" +
                    "\r\n" +
                    "# 所有兑换写在这里\r\n" +
                    "Exchanges:\r\n" +
                    "\r\n" +
                    "  \u6ce5\u571f\u6362\u94bb\u77f3:\r\n" +
                    "    Name: \"&7\u6ce5\u571f &e\u2192 &b\u94bb\u77f3\"\r\n" +
                    "    Icon: \"DIAMOND\"\r\n" +
                    "    Slot: 10\r\n" +
                    "    Input:\r\n" +
                    "      - \"DIRT:64\"\r\n" +
                    "    Output:\r\n" +
                    "      - \"DIAMOND:1\"\r\n" +
                    "\r\n" +
                    "  \u94bb\u77f3\u6362\u91d1\u5e01:\r\n" +
                    "    Name: \"&b\u94bb\u77f3 &e\u2192 &6\u91d1\u5e01\"\r\n" +
                    "    Icon: \"GOLD_INGOT\"\r\n" +
                    "    Slot: 11\r\n" +
                    "    Input:\r\n" +
                    "      - \"DIAMOND:1\"\r\n" +
                    "    Output:\r\n" +
                    "      - \"VAULT:1000\"\r\n";
            writeFile(new File(root, "T1.yml"), t1);
        } catch (Throwable ignored) {}
    }

    private static void writeFile(File f, String content) {
        try {
            java.io.FileWriter fw = new java.io.FileWriter(f);
            fw.write(content);
            fw.close();
        } catch (Throwable ignored) {}
    }

    // ==================== 兼容旧 exchanges.yml ====================

    private static void loadLegacy(LDAttribute plugin, File f) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection psec = cfg.getConfigurationSection("Pages");
        if (psec == null) return;
        for (String pid : psec.getKeys(false)) {
            ConfigurationSection ps = psec.getConfigurationSection(pid);
            if (ps == null) continue;
            String title = ChatColor.translateAlternateColorCodes('&', ps.getString("Title", "&8兑换"));
            String perm = ps.getString("Permission", "");
            Page page = new Page(pid, title, perm);
            ConfigurationSection esec = ps.getConfigurationSection("Exchanges");
            if (esec != null) {
                Set<Integer> used = new HashSet<>();
                for (String eid : esec.getKeys(false)) {
                    ConfigurationSection es = esec.getConfigurationSection(eid);
                    if (es == null) continue;
                    try {
                        Exchange ex = parseExchange(es, eid, pid, used);
                        if (ex != null) page.exchanges.put(eid, ex);
                    } catch (Throwable ignored) {}
                }
            }
            if (!page.exchanges.isEmpty()) pages.put(pid, page);
        }
    }

    // ==================== 对外 API ====================

    public static Collection<Page> allPages() { return pages.values(); }
    public static Page getPage(String id) { return pages.get(id); }
    public static int pageCount() { return pages.size(); }
    public static Exchange getExchange(String pageId, String exId) {
        Page p = pages.get(pageId);
        return p == null ? null : p.exchanges.get(exId);
    }
    public static Exchange getBySlot(String pageId, int slot) {
        Page p = pages.get(pageId);
        if (p == null) return null;
        for (Exchange ex : p.exchanges.values()) if (ex.slot == slot) return ex;
        return null;
    }
    public static String firstPageId() {
        for (Page p : pages.values()) return p.id;
        return null;
    }
}