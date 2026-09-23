package com.longdrange.ldattribute.data.attribute;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.data.eventdata.LDEventData;
import com.longdrange.ldattribute.util.Config;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 所有屬性的抽象基類
 *
 * 每個屬性（攻擊力、暴擊、防禦力…）都繼承這個類，
 * 並實作：
 *   - eventMethod()    屬性被觸發時要做什麼
 *   - loadAttribute()  從 Lore 讀取數值
 *   - getValue()       回傳屬性代表值
 *   - getPlaceholder() PAPI 佔位符
 */
public abstract class LDSubAttribute {

    /** 數值格式化器（顯示用） */
    private static final DecimalFormat DF = new DecimalFormat("0.##");

    /** 屬性名稱（如「攻擊力」） */
    private final String name;

    /** 屬性類型（可多個，如 ATTACK） */
    private final LDAttributeType[] attributeTypes;

    /** 數值陣列（有些屬性需要多個數值） */
    private final double[] doubles;

    /** 註冊這個屬性的插件 */
    private JavaPlugin plugin;

    public LDSubAttribute(String name, int doublesLength, LDAttributeType... attributeTypes) {
        this.name = name;
        this.attributeTypes = attributeTypes;
        this.doubles = new double[doublesLength];
    }

    // ==================== 靜態工具 ====================

    /**
     * 機率判定
     * @param d 機率百分比（0-100）
     * @return 是否觸發
     */
    public static boolean probability(double d) {
        return d > 0.0 && d / 100.0 > LDAttribute.getRandom().nextDouble();
    }

    /**
     * 從 Lore 字串中提取數字
     * 會自動去除色碼與非數字字元
     */
    public static String getNumber(String lore) {
        String str = lore.replaceAll("\u00a7+[a-z0-9]", "").replaceAll("[^-0-9.]", "");
        return str.length() == 0 || str.replaceAll("[^.]", "").length() > 1 ? "0" : str;
    }

    public static DecimalFormat getDf() {
        return DF;
    }

    // ==================== 註冊 ====================

    /**
     * 將此屬性註冊到全局屬性表
     * 通常由插件在 onEnable() 中呼叫
     */
    public final void register(JavaPlugin plugin) {
        if (plugin == null) {
            Bukkit.getConsoleSender().sendMessage("\u00a7c[LD-Attribute] " + this.getName() + " 註冊失敗：Plugin 為 null");
            return;
        }
        if (this.getPriority() < 0) {
            Bukkit.getConsoleSender().sendMessage("[" + plugin.getName() + "] \u00a77屬性 \u00a7c" + this.getName() + " \u00a77已停用（優先級 < 0）");
            return;
        }
        if (this.attributeTypes.length == 0) {
            Bukkit.getConsoleSender().sendMessage("[" + plugin.getName() + "] \u00a7c屬性 " + this.getName() + " 沒有設定類型！");
            return;
        }
        this.plugin = plugin;
        LDSubAttribute old = LDAttributeManager.getAttributeMap().put(this.getPriority(), this);
        if (old == null) {
            Bukkit.getConsoleSender().sendMessage("[" + plugin.getName() + "] \u00a7a註冊屬性 \u00a7e" + this.getName() + " \u00a7a到優先級 \u00a7e" + this.getPriority());
        } else {
            Bukkit.getConsoleSender().sendMessage("[" + plugin.getName() + "] \u00a7e屬性 " + this.getName() + " \u00a77覆蓋了 \u00a7c" + old.getName());
        }
    }

    // ==================== 抽象方法（子類必須實作） ====================

    /**
     * 屬性被觸發時的處理邏輯
     * 例如攻擊力屬性會在 ATTACK 時把數值加到傷害上
     */
    public abstract void eventMethod(LDEventData data);

    /**
     * PAPI 佔位符
     * %ldattr_攻擊力%  會呼叫此方法
     */
    public abstract String getPlaceholder(Player player, String params);

    /**
     * 此屬性支援的佔位符列表
     */
    public abstract List<String> getPlaceholders();

    /**
     * 從 Lore 字串載入屬性
     * 格式由各子類定義，例如「攻擊力: 100」
     *
     * @return 是否有成功載入
     */
    public abstract boolean loadAttribute(String str);

    /**
     * 取得屬性的代表值（用於排序、顯示、總值計算）
     */
    public abstract double getValue();

    // ==================== 通用方法 ====================

    public final LDAttributeType[] getType() {
        return this.attributeTypes.clone();
    }

    public final boolean containsType(LDAttributeType type) {
        return Arrays.stream(this.attributeTypes).anyMatch(t -> t.equals(type));
    }

    /**
     * 取得此屬性的優先級（從 config.yml 讀取）
     */
    public final int getPriority() {
        return Config.getConfig().getInt("AttributePriority." + this.getName(), -1);
    }

    public final double[] getAttributes() {
        return this.doubles;
    }

    public final void setAttributes(Double... doubles) {
        IntStream.range(0, this.doubles.length).forEach(i -> this.doubles[i] = doubles[i]);
    }

    public final void addAttribute(double[] doubles) {
        IntStream.range(0, this.doubles.length).forEach(i -> this.doubles[i] += doubles[i]);
    }

    /**
     * 從序列化字串載入
     * 格式：屬性名#數值1/數值2
     */
    public final void loadFromString(String attributeString) {
        String[] args = attributeString.split("#");
        if (args.length < 2) return;
        if (args[0].equals(this.getName())) {
            List<String> list = args[1].contains("/")
                    ? Arrays.asList(args[1].split("/"))
                    : Collections.singletonList(args[1]);
            this.setAttributes(list.stream()
                    .filter(s -> s.length() > 0)
                    .map(Double::valueOf)
                    .toArray(Double[]::new));
        }
    }

    /**
     * 序列化為字串
     * 全部數值為 0 時回傳 null
     */
    public final String saveToString() {
        if (Arrays.stream(this.doubles).anyMatch(d -> d != 0.0)) {
            return this.getName() + "#" + IntStream.range(0, this.doubles.length)
                    .mapToObj(i -> String.valueOf(i == this.doubles.length - 1 ? this.doubles[i] : this.doubles[i] + "/"))
                    .collect(Collectors.joining());
        }
        return null;
    }

    /**
     * 修正數值（負數歸零）
     */
    public void correct() {
        IntStream.range(0, this.doubles.length).filter(i -> this.doubles[i] < 0).forEach(i -> this.doubles[i] = 0);
    }

    /**
     * 建立一個新實例（用於建立預設屬性表）
     */
    public LDSubAttribute newAttribute() {
        try {
            return this.getClass().newInstance();
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("\u00a7c[LD-Attribute] " + this.getName() + " 建構失敗！");
            return null;
        }
    }

    public List<String> introduction() {
        return new ArrayList<>();
    }

    public void onEnable() {}
    public void onDisable() {}

    // ==================== Getter ====================

    public final String getName() { return name; }
    public final JavaPlugin getPlugin() { return plugin; }
}