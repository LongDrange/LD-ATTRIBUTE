package com.longdrange.ldattribute.card;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class ComboData {

    public static class Combo {
        public String id;
        public String name;
        public String description;
        public List<String> cards = new ArrayList<>();
        public int required;
        public List<String> attributes = new ArrayList<>();
        public String effect = "";
        public double effectValue = 0;
        public String message = "";
    }

    private static final Map<String, Combo> all = new LinkedHashMap<>();
    private static final Map<UUID, Set<String>> activeCache = new HashMap<>();

    public static void load(LDAttribute plugin) {
        all.clear();
        File f = com.longdrange.ldattribute.util.ConfigPaths.resolve(plugin, "combo.yml");
        if (!f.exists()) { try { plugin.saveResource("combo.yml", false); } catch (Exception ignored) {} }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection sec = cfg.getConfigurationSection("Combos");
        if (sec == null) { plugin.getLogger().info("combo.yml 无 Combos 区块"); return; }

        for (String key : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) continue;
            Combo c = new Combo();
            c.id = key;
            c.name = org.bukkit.ChatColor.translateAlternateColorCodes((char) 38, s.getString("Name", key));
            c.description = org.bukkit.ChatColor.translateAlternateColorCodes((char)38, s.getString("Description", ""));
            List<String> cards = s.getStringList("Cards");
            if (cards != null) c.cards.addAll(cards);
            c.required = s.getInt("Required", c.cards.size());
            List<String> attrs = s.getStringList("Attributes");
            if (attrs != null) c.attributes.addAll(attrs);
            c.effect = s.getString("Effect", "").toUpperCase();
            c.effectValue = s.getDouble("EffectValue", 0);
            c.message = org.bukkit.ChatColor.translateAlternateColorCodes((char)38, s.getString("Message", ""));
            all.put(key, c);
        }
        plugin.getLogger().info("已載入 " + all.size() + " 個卡片組合");
    }

    /** 应用组合的属性 + 记录激活状态 */
    public static void apply(Player player, List<ItemStack> cards, LDAttributeData statsData) {
        Set<String> active = new HashSet<>();
        Set<String> was = activeCache.getOrDefault(player.getUniqueId(), new HashSet<>());

        for (Combo c : all.values()) {
            int matched = 0;
            for (String cardId : c.cards) if (hasCard(cards, cardId)) matched++;
            if (matched < c.required) continue;

            // 激活
            active.add(c.id);

            // 属性
            if (!c.attributes.isEmpty()) {
                LDAttributeData d = LDAttribute.getInstance().getApi().getLoreData(player, null, c.attributes);
                statsData.add(d);
            }

            // 首次激活消息
            if (!was.contains(c.id) && c.message != null && !c.message.isEmpty()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes((char) 38, c.message));
            }
        }

        // 失效消息
        for (String id : was) {
            if (!active.contains(id)) {
                Combo c = all.get(id);
                if (c != null) {
                    player.sendMessage("§8[§6组合§8] §c" + c.name + " §c已失效");
                }
            }
        }

        activeCache.put(player.getUniqueId(), active);
    }

    /** 检查某组合是否激活 */
    public static boolean isActive(Player player, String comboId) {
        Set<String> active = activeCache.get(player.getUniqueId());
        return active != null && active.contains(comboId);
    }

    public static Collection<Combo> getAll() { return all.values(); }
    public static Combo get(String id) { return all.get(id); }

    private static boolean hasCard(List<ItemStack> cards, String cardId) {
        CardData target = CardDataManager.getCard(cardId);
        if (target == null) return false;
        for (ItemStack card : cards) if (target.matches(card)) return true;
        return false;
    }

    public static void unload(UUID uuid) {
        activeCache.remove(uuid);
    }
}