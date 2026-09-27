package com.longdrange.ldattribute.core.ring;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

/**
 * 魂珠类型配置
 * 路径：plugins/LD-Attribute/配置/魂珠/<任意子文件夹>/<文件>.yml
 * 支持：
 *   - 一个文件夹放多个 yml
 *   - 一个 yml 放多个魂珠（顶层 key = 魂珠ID）
 *
 * 格式：
 * fire_ring:
 *   Name: '&c&l火焰魂珠'
 *   Material: BLAZE_POWDER      # 材质名（也支持 ID: 339 数字）
 *   MaxStack: 10
 *   Lore:
 *     - '&6物品类型: &c魂珠'
 *     - '&e魂珠ID: fire_ring'
 *     - '&e上限: 10'
 *     - '&r'
 *     - '&c攻击力: +10'
 *   Enchant: true
 *   Unbreakable: true
 */
public class RingTypeConfig {

    public static class Def {
        public String id;
        public String name;
        public Material material = Material.PAPER;
        public short data = 0;
        public int maxStack = 64;
        public List<String> lore = new ArrayList<>();
        public boolean enchant = false;
        public boolean unbreakable = false;
    }

    private static final Map<String, Def> types = new LinkedHashMap<>();

    public static void load(LDAttribute plugin) {
        types.clear();
        File root = new File(plugin.getDataFolder(), "配置/魂珠");
        if (!root.exists()) {
            root.mkdirs();
            releaseDefaults(plugin);
        } else {
            // 目录已存在，但仍逐个释放缺失的默认文件
            releaseDefaults(plugin);
        }

        File[] dirs = root.listFiles(File::isDirectory);
        if (dirs != null) {
            Arrays.sort(dirs, Comparator.comparing(File::getName));
            for (File dir : dirs) loadFolder(plugin, dir);
        }

        plugin.getLogger().info("[Ring] 已加载魂珠类型 " + types.size() + " 个");
    }

    private static void loadFolder(LDAttribute plugin, File dir) {
        File[] files = dir.listFiles(f -> f.isFile() && f.getName().endsWith(".yml"));
        if (files == null || files.length == 0) return;
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) {
            try {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                for (String id : cfg.getKeys(false)) {
                    ConfigurationSection s = cfg.getConfigurationSection(id);
                    if (s == null) continue;
                    Def d = new Def();
                    d.id = id;
                    d.name = ChatColor.translateAlternateColorCodes((char)38, s.getString("Name", id));

                    Material mat = null;
                    String matName = s.getString("Material", null);
                    if (matName != null && !matName.isEmpty()) {
                        mat = Material.getMaterial(matName.toUpperCase());
                    }
                    if (mat == null) {
                        String idStr = s.getString("ID", null);
                        if (idStr != null) {
                            try { mat = Material.getMaterial(Integer.parseInt(idStr.trim())); } catch (Throwable ignored) {}
                        }
                    }
                    if (mat != null) d.material = mat;

                    d.data = (short) s.getInt("Data", 0);
                    d.maxStack = Math.max(1, s.getInt("MaxStack", 64));
                    List<String> lore = s.getStringList("Lore");
                    if (lore != null) d.lore.addAll(lore);
                    d.enchant = s.getBoolean("Enchant", false);
                    d.unbreakable = s.getBoolean("Unbreakable", false);

                    types.put(id, d);
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("[Ring] 加载 " + file.getName() + " 失败: " + t.getMessage());
            }
        }
    }

    public static Def get(String id) { return types.get(id); }
    public static Collection<Def> all() { return types.values(); }
    public static Set<String> ids() { return types.keySet(); }

    public static ItemStack createItem(String id, int amount) {
        Def d = types.get(id);
        if (d == null) return null;
        ItemStack it = new ItemStack(d.material, Math.max(1, amount), d.data);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(d.name);
            if (!d.lore.isEmpty()) {
                List<String> lore = new ArrayList<>();
                for (String l : d.lore) lore.add(ChatColor.translateAlternateColorCodes((char)38, l));
                meta.setLore(lore);
            }
            if (d.enchant) {
                try { meta.addEnchant(Enchantment.DURABILITY, 1, true); } catch (Throwable ignored) {}
            }
            if (d.unbreakable) meta.setUnbreakable(true);
            it.setItemMeta(meta);
        }
        return it;
    }

    private static final String[] DEFAULTS = {
        "配置/魂珠/T1魂珠/T1魂珠.yml",
        "配置/魂珠/T2魂珠/T2魂珠1.yml",
        "配置/魂珠/T2魂珠/T2魂珠2.yml",
        "配置/魂珠/T2魂珠/T2魂珠3.yml",
        "配置/魂珠/T3魂珠/T3魂珠.yml"
    };

    private static void releaseDefaults(LDAttribute plugin) {
        for (String p : DEFAULTS) {
            File ff = new File(plugin.getDataFolder(), p);
            if (ff.exists()) continue;
            try { plugin.saveResource(p, false); } catch (Throwable ignored) {}
        }
    }

    private static void writeDefault(LDAttribute plugin, File root) {
        File t1 = new File(root, "T1魂珠");
        t1.mkdirs();
        String c = "# ============================================================\n" +
            "# 魂珠类型定义（T1示例）\n" +
            "# ============================================================\n" +
            "# 顶层 key = 魂珠ID（用于 /ldring give <玩家> <ID>）\n" +
            "# 可以一个 yml 放多个魂珠；也可以一个文件夹放多个 yml\n" +
            "#\n" +
            "# 字段：\n" +
            "#   Name:        显示名（& 色码）\n" +
            "#   Material:    材质名（如 BLAZE_POWDER） 或  ID: 339 数字ID\n" +
            "#   Data:        附加值（默认 0）\n" +
            "#   MaxStack:    堆叠上限\n" +
            "#   Lore:        Lore 列表（& 色码）\n" +
            "#   Enchant:     是否有附魔光效\n" +
            "#   Unbreakable: 是否不可破坏\n" +
            "# ============================================================\n" +
            "\n" +
            "fire_ring:\n" +
            "  Name: '&c&l火焰魂珠'\n" +
            "  Material: BLAZE_POWDER\n" +
            "  MaxStack: 10\n" +
            "  Lore:\n" +
            "    - '&6物品类型: &c魂珠'\n" +
            "    - '&e魂珠ID: fire_ring'\n" +
            "    - '&e上限: 10'\n" +
            "    - '&r'\n" +
            "    - '&c攻击力: +10'\n" +
            "    - '&e暴击機率: +2'\n" +
            "  Enchant: true\n" +
            "  Unbreakable: true\n" +
            "\n" +
            "ice_ring:\n" +
            "  Name: '&b&l寒冰魂珠'\n" +
            "  Material: SNOW_BALL\n" +
            "  MaxStack: 20\n" +
            "  Lore:\n" +
            "    - '&6物品类型: &b魂珠'\n" +
            "    - '&e魂珠ID: ice_ring'\n" +
            "    - '&e上限: 20'\n" +
            "    - '&r'\n" +
            "    - '&b防御力: +8'\n" +
            "    - '&b生命上限: +50'\n" +
            "  Enchant: true\n" +
            "  Unbreakable: true\n";
        try {
            java.nio.file.Files.write(new File(t1, "T1魂珠.yml").toPath(),
                    c.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Throwable ignored) {}
    }
}
