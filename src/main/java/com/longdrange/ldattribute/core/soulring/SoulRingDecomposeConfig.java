package com.longdrange.ldattribute.core.soulring;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

/**
 * 灵魂空间分解配置
 * 路径：plugins/LD-Attribute/配置/灵魂空间/分解/*.yml
 *
 * 支持两种写法：
 * 【单文件】
 * Items:
 *   DIRT:
 *     Points: 10
 *     Vault: 5
 *   "PAPER@孙逊":
 *     Points: 100
 *     Cards:
 *       - "孙逊:1"
 *
 * 匹配 key 格式：
 *   "DIRT"                 完全匹配材质
 *   "PAPER@孙逊"           材质 + 显示名包含"孙逊"
 *   "lore:卡牌"            Lore 包含"卡牌"
 *   "DIRT#dirt"            材质 + 名字包含 dirt（小写不分大小写）
 */
public class SoulRingDecomposeConfig {

    public static class Reward {
        public int points = 0;
        public double vault = 0;
        public Map<String, Double> values = new LinkedHashMap<>();
        public List<String> items = new ArrayList<>();
        public List<String> cards = new ArrayList<>();
        public List<String> commands = new ArrayList<>();

        public boolean isEmpty() {
            return points <= 0 && vault <= 0
                    && values.isEmpty() && items.isEmpty()
                    && cards.isEmpty() && commands.isEmpty();
        }
    }

    /** key -> reward */
    private static final LinkedHashMap<String, Reward> rewards = new LinkedHashMap<>();

    public static void load(LDAttribute plugin) {
        rewards.clear();
        File root = new File(plugin.getDataFolder(), "配置/灵魂空间/分解");
        if (!root.exists()) {
            root.mkdirs();
            writeDefault(plugin, root);
        }
        File[] files = root.listFiles(f -> f.isFile() && f.getName().endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().info("[SoulRingDecompose] 无分解配置");
            return;
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) {
            try {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                ConfigurationSection sec = cfg.getConfigurationSection("Items");
                if (sec == null) continue;
                for (String key : sec.getKeys(false)) {
                    ConfigurationSection s = sec.getConfigurationSection(key);
                    if (s == null) continue;
                    Reward r = new Reward();
                    r.points = s.getInt("Points", 0);
                    r.vault = s.getDouble("Vault", 0);
                    ConfigurationSection vs = s.getConfigurationSection("Values");
                    if (vs != null) {
                        for (String vk : vs.getKeys(false)) {
                            r.values.put(vk, vs.getDouble(vk, 0));
                        }
                    }
                    List<String> its = s.getStringList("Items");
                    if (its != null) r.items.addAll(its);
                    List<String> cards = s.getStringList("Cards");
                    if (cards != null) r.cards.addAll(cards);
                    List<String> cmds = s.getStringList("Commands");
                    if (cmds != null) r.commands.addAll(cmds);
                    rewards.put(key, r);
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("[SoulRingDecompose] 加载 " + file.getName() + " 失败: " + t.getMessage());
            }
        }
        plugin.getLogger().info("[SoulRingDecompose] 已加载 " + rewards.size() + " 条分解规则");
    }

    /** 匹配物品，返回对应的奖励（无匹配返回 null） */
    public static Reward getReward(ItemStack item) {
        if (item == null) return null;
        String matName = item.getType().name();
        String display = "";
        String loreStr = "";
        if (item.hasItemMeta()) {
            ItemMeta m = item.getItemMeta();
            if (m.hasDisplayName()) {
                display = ChatColor.stripColor(m.getDisplayName());
            }
            if (m.hasLore()) {
                StringBuilder sb = new StringBuilder();
                for (String l : m.getLore()) sb.append(ChatColor.stripColor(l)).append("\n");
                loreStr = sb.toString();
            }
        }

        Reward best = null;
        int bestScore = -1;

        for (Map.Entry<String, Reward> en : rewards.entrySet()) {
            String key = en.getKey();
            int score = matchScore(key, matName, display, loreStr);
            if (score > bestScore) {
                bestScore = score;
                best = en.getValue();
            }
        }
        return best;
    }

    /** 返回匹配分数：-1 不匹配；数字越大越优先 */
    private static int matchScore(String key, String matName, String display, String loreStr) {
        try {
            // 格式 1: "lore:xxx"
            if (key.startsWith("lore:")) {
                String need = key.substring(5);
                return loreStr.contains(need) ? 20 : -1;
            }

            // 格式 2: "MATERIAL@名字"
            if (key.contains("@")) {
                int idx = key.indexOf('@');
                String mat = key.substring(0, idx);
                String nameNeed = key.substring(idx + 1);
                if (!matName.equalsIgnoreCase(mat)) return -1;
                return display.contains(nameNeed) ? 100 : -1;
            }

            // 格式 3: "MATERIAL#名字"
            if (key.contains("#")) {
                int idx = key.indexOf('#');
                String mat = key.substring(0, idx);
                String nameNeed = key.substring(idx + 1).toLowerCase();
                if (!matName.equalsIgnoreCase(mat)) return -1;
                return display.toLowerCase().contains(nameNeed) ? 80 : -1;
            }

            // 格式 4: "MATERIAL" 纯材质
            if (matName.equalsIgnoreCase(key)) return 50;
        } catch (Throwable ignored) {}
        return -1;
    }

    public static Collection<String> allKeys() { return rewards.keySet(); }
    public static Reward getByKey(String key) { return rewards.get(key); }

    private static void writeDefault(LDAttribute plugin, File root) {
        String c = "# ============================================================\n" +
                "# 灵魂空间分解规则\n" +
                "# ============================================================\n" +
                "# 匹配 key 格式：\n" +
                "#   \"DIRT\"              完全匹配材质\n" +
                "#   \"PAPER@孙逊\"        材质 + 显示名包含\"孙逊\"\n" +
                "#   \"lore:卡牌\"         Lore 包含\"卡牌\"\n" +
                "#   \"DIRT#dirt\"         材质 + 名字包含（不分大小写）\n" +
                "#\n" +
                "# 奖励字段：\n" +
                "#   Points:   点券\n" +
                "#   Vault:    金币\n" +
                "#   Values:   { 值Id: 数量 }\n" +
                "#   Items:    [ \"材质:数量\" ]\n" +
                "#   Cards:    [ \"卡ID:数量\" ]\n" +
                "#   Commands: [ \"命令（%player% = 玩家名）\" ]\n" +
                "# ============================================================\n" +
                "\n" +
                "Items:\n" +
                "  # 示例：泥土分解为点券\n" +
                "  DIRT:\n" +
                "    Points: 1\n" +
                "\n" +
                "  # 示例：石头分解为金币\n" +
                "  COBBLESTONE:\n" +
                "    Vault: 1\n" +
                "\n" +
                "  # 示例：卡片分解（名字含\"英雄卡\"）\n" +
                "  \"PAPER@英雄卡\":\n" +
                "    Points: 100\n" +
                "    Items:\n" +
                "      - \"DIAMOND:1\"\n";
        try {
            java.nio.file.Files.write(new File(root, "示例.yml").toPath(),
                    c.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Throwable ignored) {}
    }
}