package com.longdrange.ldattribute.pet;

import com.longdrange.ldattribute.LDAttribute;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class PetEquipmentConfig {

    public static class Equip {
        public String id;
        public String name;
        public String slot;          // WEAPON / ARMOR / ACCESSORY
        public Material material;
        public String description;
        public List<String> attributes = new ArrayList<>();
    }

    private static final Map<String, Equip> all = new LinkedHashMap<>();

    public static void load(LDAttribute plugin) {
        all.clear();
        File f = com.longdrange.ldattribute.util.ConfigPaths.resolve(plugin, "pet_equipment.yml");
        if (!f.exists()) { try { plugin.saveResource("pet_equipment.yml", false); } catch (Exception ignored) {} }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection sec = cfg.getConfigurationSection("Equipments");
        if (sec == null) { plugin.getLogger().info("pet_equipment.yml 无 Equipments 区块"); return; }

        for (String key : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) continue;
            Equip e = new Equip();
            e.id = key;
            e.name = org.bukkit.ChatColor.translateAlternateColorCodes((char) 38, s.getString("Name", key));
            e.slot = s.getString("Slot", "WEAPON").toUpperCase();
            String matName = s.getString("Material", "IRON_SWORD");
            Material m = Material.getMaterial(matName.toUpperCase());
            e.material = m != null ? m : Material.IRON_SWORD;
            e.description = org.bukkit.ChatColor.translateAlternateColorCodes((char)38, s.getString("Description", ""));
            List<String> attrs = s.getStringList("Attributes");
            if (attrs != null) e.attributes.addAll(attrs);
            all.put(key, e);
        }
        plugin.getLogger().info("已載入 " + all.size() + " 個寵物裝備");
    }

    public static Equip get(String id) { return all.get(id); }
    public static Collection<Equip> getAll() { return all.values(); }
}