package com.longdrange.ldattribute;

import com.longdrange.ldattribute.api.LDAttributeAPI;
import com.longdrange.ldattribute.core.CoreManager;
import com.longdrange.ldattribute.core.command.CoreCommand;
import com.longdrange.ldattribute.core.ring.command.RingCommand;
import com.longdrange.ldattribute.core.jewelry.command.JewelryCommand;
import com.longdrange.ldattribute.core.talent.command.TalentCommand;
import com.longdrange.ldattribute.core.guide.command.GuideCommand;
import com.longdrange.ldattribute.core.value.command.ValueCommand;
import com.longdrange.ldattribute.core.scoreboard.command.ScoreboardCommand;
import com.longdrange.ldattribute.core.soulring.command.SoulRingCommand;
import com.longdrange.ldattribute.card.CardDataManager;
import com.longdrange.ldattribute.card.CardExpTask;
import com.longdrange.ldattribute.card.CollectionConfig;
import com.longdrange.ldattribute.card.DecomposeConfig;
import com.longdrange.ldattribute.card.CardLevelConfig;
import com.longdrange.ldattribute.card.StarConfig;
import com.longdrange.ldattribute.card.TempBuffConfig;
import com.longdrange.ldattribute.card.PageConfig;
import com.longdrange.ldattribute.card.RecipeConfig;
import com.longdrange.ldattribute.card.PlayerData;
import com.longdrange.ldattribute.card.SuitData;
import com.longdrange.ldattribute.listener.OnCardListener;
import com.longdrange.ldattribute.points.PointCommand;
import com.longdrange.ldattribute.points.PointData;
import com.longdrange.ldattribute.command.CardCommand;
import com.longdrange.ldattribute.command.CommandConfig;
import com.longdrange.ldattribute.command.LDCommand;
import com.longdrange.ldattribute.data.attribute.LDAttributeManager;
import com.longdrange.ldattribute.data.attribute.sub.damage.*;
import com.longdrange.ldattribute.data.attribute.sub.defence.*;
import com.longdrange.ldattribute.data.attribute.sub.other.*;
import com.longdrange.ldattribute.data.attribute.sub.update.HealthAttribute;
import com.longdrange.ldattribute.data.attribute.sub.update.SpeedAttribute;
import com.longdrange.ldattribute.data.attribute.sub.update.SpeedPercentAttribute;
import com.longdrange.ldattribute.listener.OnDamageListener;
import com.longdrange.ldattribute.listener.OnExpListener;
import com.longdrange.ldattribute.listener.OnUpdateStatsListener;
import com.longdrange.ldattribute.util.Config;
import com.longdrange.ldattribute.util.Message;
import com.longdrange.ldattribute.util.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class LDAttribute extends JavaPlugin {

    private static LDAttribute instance;
    private static final Random RANDOM = new Random();

    private Config configUtil;
    private LDAttributeManager manager;
    private LDAttributeAPI api;
    private CoreManager coreManager;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("=================================");
        getLogger().info("  LD-Attribute 正在啟動...");
        getLogger().info("  版本: " + getDescription().getVersion());
        getLogger().info("=================================");

        this.configUtil = new Config(this);
        this.configUtil.loadConfig();
        com.longdrange.ldattribute.util.LanguageManager.load(this);
        this.manager = new LDAttributeManager(this);
        this.api = new LDAttributeAPI(this);

        registerAllAttributes();

        // 監聽器
        Bukkit.getPluginManager().registerEvents(new OnDamageListener(this), this);
        Bukkit.getPluginManager().registerEvents(new OnUpdateStatsListener(this), this);
        Bukkit.getPluginManager().registerEvents(new OnExpListener(this), this);
        Bukkit.getPluginManager().registerEvents(new com.longdrange.ldattribute.listener.LDExtendedListener(this), this);

        // 指令
        LDCommand cmd = new LDCommand(this);
        getCommand("ldattribute").setExecutor(cmd);
        getCommand("ldattribute").setTabCompleter(cmd);

        // ===== LD-Core 核心模块（批次 1）=====
        this.coreManager = new CoreManager(this);
        this.coreManager.enable();

        CoreCommand coreCmd = new CoreCommand(this);
        getCommand("ldcore").setExecutor(coreCmd);
        getCommand("ldcore").setTabCompleter(coreCmd);

        // ===== 魂珠空间（批次 2-1）=====
        RingCommand ringCmd = new RingCommand(this);
        getCommand("ldring").setExecutor(ringCmd);
        getCommand("ldring").setTabCompleter(ringCmd);

        // ===== 饰品背包（批次3-2）=====
        JewelryCommand jewCmd = new JewelryCommand(this);
        getCommand("ldsp").setExecutor(jewCmd);
        getCommand("ldsp").setTabCompleter(jewCmd);

        // ===== 天赋（批次3-3）=====
        TalentCommand talCmd = new TalentCommand(this);
        getCommand("ldtalent").setExecutor(talCmd);
        getCommand("ldtalent").setTabCompleter(talCmd);

        // ===== 怪物图鉴（批次4）=====
        GuideCommand gdCmd = new GuideCommand(this);
        getCommand("ldguide").setExecutor(gdCmd);
        getCommand("ldguide").setTabCompleter(gdCmd);

        // ===== 自定义值（批次5）=====
        ValueCommand valCmd = new ValueCommand(this);
        getCommand("ldvalue").setExecutor(valCmd);
        getCommand("ldvalue").setTabCompleter(valCmd);

        // ===== 侧边栏（批次7）=====
        ScoreboardCommand sbCmd = new ScoreboardCommand(this);
        getCommand("ldsb").setExecutor(sbCmd);
        getCommand("ldsb").setTabCompleter(sbCmd);

        // ===== 灵魂空间（批次8）=====
        SoulRingCommand srCmd = new SoulRingCommand(this);
        getCommand("ldsr").setExecutor(srCmd);
        getCommand("ldsr").setTabCompleter(srCmd);
        // =========================
        // =========================
        // =========================
        // =========================
        // =========================
        // ===========================
        // ==============================
        // ======================================

        // 卡片系統
        Message.load(this, configUtil.getConfig().getString("Language", "zh_TW"));
        CardLevelConfig.load(this);
        try { com.longdrange.ldattribute.card.UIConfig.load(this); } catch (Throwable t) { getLogger().warning("UIConfig 載入失敗: " + t.getMessage()); }
        try { com.longdrange.ldattribute.card.StatsGUIConfig.load(this); } catch (Throwable t) { getLogger().warning("StatsGUIConfig 載入失敗: " + t.getMessage()); }
        StarConfig.load(this);
        TempBuffConfig.load(this);
        DecomposeConfig.load(this);
        com.longdrange.ldattribute.rune.RuneConfig.load(this);
        com.longdrange.ldattribute.rune.RuneData.init(this);
        com.longdrange.ldattribute.achievement.AchievementConfig.load(this);
        com.longdrange.ldattribute.achievement.AchievementData.init(this);
        com.longdrange.ldattribute.gacha.GachaConfig.load(this);
        com.longdrange.ldattribute.gacha.GachaData.init(this);
        CardDataManager.load(this);
        PlayerData.init(this);
        com.longdrange.ldattribute.card.ManaManager.init(this);
        SuitData.load(this);
        com.longdrange.ldattribute.card.SynergyData.load(this);
        com.longdrange.ldattribute.card.ComboData.load(this);
        com.longdrange.ldattribute.combat.ElementConfig.load(this);
        com.longdrange.ldattribute.combat.StateConfig.load(this);
        com.longdrange.ldattribute.combat.StateManager.init(this);
        PageConfig.load(this);
        CommandConfig.load(this);
        try { com.longdrange.ldattribute.util.AuditLog.init(getDataFolder()); } catch (Throwable ignored) {}
        try { com.longdrange.ldattribute.command.CardCommand.loadPending(this); } catch (Throwable ignored) {}
        CollectionConfig.load(this);
        RecipeConfig.load(this);
        CardCommand cardCmd = new CardCommand(this);
        getCommand("ldcardstats").setExecutor(cardCmd);
        getCommand("ldcardstats").setTabCompleter(cardCmd);

        // 點券系統
        PointData.init(this);
        com.longdrange.ldattribute.pet.PetConfig.load(this);
        com.longdrange.ldattribute.pet.PetEquipmentConfig.load(this);
        com.longdrange.ldattribute.pet.PetData.init(this);
        com.longdrange.ldattribute.pet.PetEntityManager.init(this);
        Bukkit.getPluginManager().registerEvents(new com.longdrange.ldattribute.pet.PetKillListener(this), this);
        try { com.longdrange.ldattribute.compat.MMCompat.init(this); } catch (Throwable t) { getLogger().warning("MM 兼容層載入失敗: " + t.getMessage()); }
        com.longdrange.ldattribute.spell.SpellConfig.load(this);
        com.longdrange.ldattribute.spell.SpellManager.init(this);
        PointCommand pointCmd = new PointCommand(this);
        getCommand("ldpoints").setExecutor(pointCmd);
        getCommand("ldpoints").setTabCompleter(pointCmd);

        // 卡片事件監聽
        Bukkit.getPluginManager().registerEvents(new OnCardListener(this), this);

        // PAPI
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new Placeholders(this).register();
            getLogger().info("已註冊 PlaceholderAPI 佔位符！");
        }

        // 註冊 Bukkit Service（給 TCardStats 等插件用）
        getServer().getServicesManager().register(
                LDAttributeAPI.class, api, this, ServicePriority.Normal);
        getLogger().info("已註冊 LDAttributeAPI 到 Bukkit Services！");

        new CardExpTask(this).runTaskTimer(this, 20L, 20L);
        // 每秒检查 buff 过期
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    if (com.longdrange.ldattribute.card.TempBuffManager.tickExpire(p.getUniqueId())) {
                        try { com.longdrange.ldattribute.card.StatsDataRead.updatePlayer(p); } catch (Throwable ignored) {}
                    }
                }
            }
        }.runTaskTimer(this, 20L, 20L);
        try { com.longdrange.ldattribute.util.BackupManager.init(this); } catch (Throwable t) { getLogger().warning("備份初始化失敗: " + t.getMessage()); }
        getLogger().info("LD-Attribute 啟動完成！");
    }

    private void registerAllAttributes() {
        new DamageAttribute().register(this);
        // new CritAttribute().register(this);  // 舊暴擊已停用
        new ArmorPenAttribute().register(this);
        new CritChanceAttribute().register(this);
        new CritDamageAttribute().register(this);
        new LifeStealAttribute().register(this);
        new IgnitionAttribute().register(this);
        new LightningAttribute().register(this);
        new PoisonAttribute().register(this);
        new WitherAttribute().register(this);
        new BlindnessAttribute().register(this);
        new SlownessAttribute().register(this);
        new RealAttribute().register(this);
        new HitRateAttribute().register(this);
        new DodgeAttribute().register(this);
        new BlockAttribute().register(this);
        new DefenseAttribute().register(this);
        new ToughnessAttribute().register(this);
        new ReflectionAttribute().register(this);
        new ExpAdditionAttribute().register(this);
        new EventMessageAttribute().register(this);
        new HealthAttribute().register(this);
        new SpeedAttribute().register(this);
        new SpeedPercentAttribute().register(this);

        // ==================== 擴展屬性 ====================
        // 攻擊類
        try { new com.longdrange.ldattribute.data.attribute.sub.damage.PvpDamageAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.damage.PveDamageAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.damage.RangedDamageAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.damage.AttackSpeedAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.damage.CritResistAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.damage.BurnChanceAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.damage.BurnDamageAttribute().register(this); } catch (Throwable t) {}

        // 防御類
        try { new com.longdrange.ldattribute.data.attribute.sub.defence.PvpDefenseAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.defence.PveDefenseAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.defence.HealthRegenAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.defence.LifeStealResistAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.defence.KnockbackResistAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.defence.RangedDefenseAttribute().register(this); } catch (Throwable t) {}

        // 功能類
        try { new com.longdrange.ldattribute.data.attribute.sub.other.ManaMaxAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.other.ManaRegenAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.other.CooldownReductionAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.other.LuckAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.other.ArrowSpeedAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.other.ArrowAccuracyAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.other.ArrowPierceAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.other.ShieldReductionAttribute().register(this); } catch (Throwable t) {}

        // 法術類
        try { new com.longdrange.ldattribute.data.attribute.sub.magic.MagicDamageAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.magic.MagicDefenseAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.magic.MagicCritChanceAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.magic.MagicCritDamageAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.magic.MagicPenAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.magic.MagicLifeStealAttribute().register(this); } catch (Throwable t) {}

        // ===== 符文扩展属性 =====
        try { new com.longdrange.ldattribute.data.attribute.sub.damage.CritDamageResistAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.damage.DamageAmplifyAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.damage.ExtraDamageAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.damage.LifeStealChanceAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.damage.LifeStealRatioAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.defence.DefenseAmplifyAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.defence.ReflectRatioAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.defence.ReflectResistAttribute().register(this); } catch (Throwable t) {}
        try { new com.longdrange.ldattribute.data.attribute.sub.update.HealthAmplifyAttribute().register(this); } catch (Throwable t) {}
    }

    public void reload() {
        configUtil.reload();
        LDAttributeManager.getAttributeMap().clear();
        registerAllAttributes();
    }

    @Override
    public void onDisable() {
        if (coreManager != null) coreManager.disable();
        if (manager != null) manager.saveAll();
        try { com.longdrange.ldattribute.card.ManaManager.saveAll(); } catch (Throwable ignored) {}
        getLogger().info("LD-Attribute 已關閉。");
    }

    public static LDAttribute getInstance() { return instance; }
    public static Random getRandom() { return RANDOM; }
    public Config getConfigUtil() { return configUtil; }
    public LDAttributeManager getManager() { return manager; }
    public LDAttributeAPI getApi() { return api; }

    // ==================== 熱加載（所有配置）====================

    /** 重算一个玩家背包里所有卡片的 Lore（用于热重载/动态刷新） */
    public void recalcPlayerCards(org.bukkit.entity.Player p) {
        org.bukkit.inventory.ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            org.bukkit.inventory.ItemStack it = contents[i];
            if (it == null) continue;
            try {
                com.longdrange.ldattribute.card.CardData cd =
                        com.longdrange.ldattribute.card.CardDataManager.findCard(it);
                if (cd == null) continue;
                if (!com.longdrange.ldattribute.card.CardLevelConfig.isUpgradable(cd.getId())) continue;
                org.bukkit.inventory.ItemStack newItem =
                        com.longdrange.ldattribute.card.CardLevel.recalc(it, cd.getId(),
                                com.longdrange.ldattribute.card.CardNBT.getLevel(it),
                                com.longdrange.ldattribute.card.CardNBT.getExp(it));
                p.getInventory().setItem(i, newItem);
            } catch (Throwable ignored) {}
        }
        // 手上那张也重算
        try {
            org.bukkit.inventory.ItemStack held = p.getInventory().getItemInMainHand();
            if (held != null) {
                com.longdrange.ldattribute.card.CardData cd =
                        com.longdrange.ldattribute.card.CardDataManager.findCard(held);
                if (cd != null && com.longdrange.ldattribute.card.CardLevelConfig.isUpgradable(cd.getId())) {
                    org.bukkit.inventory.ItemStack newItem =
                            com.longdrange.ldattribute.card.CardLevel.recalc(held, cd.getId(),
                                    com.longdrange.ldattribute.card.CardNBT.getLevel(held),
                                    com.longdrange.ldattribute.card.CardNBT.getExp(held));
                    p.getInventory().setItemInMainHand(newItem);
                }
            }
        } catch (Throwable ignored) {}
    }
    public void reloadAll() { reloadAll("all"); }

    public void reloadAll(String only) {
        String k = (only == null ? "all" : only.toLowerCase().trim());
        boolean all = "all".equals(k);
        try { com.longdrange.ldattribute.card.CardLevel.clearCache(); } catch (Throwable ignored) {}
        int ok = 0, fail = 0;
        if (all || "config".equals(k)) {
            try { this.configUtil.reload(); ok++; } catch (Throwable t) { fail++; getLogger().warning("config 重載失敗: " + t.getMessage()); }
            try { com.longdrange.ldattribute.util.LanguageManager.load(this); ok++; } catch (Throwable t) { fail++; }
            try { Message.load(this, configUtil.getConfig().getString("Language", "zh_TW")); ok++; } catch (Throwable t) { fail++; }
        }
        if (all || "ui".equals(k))        { try { com.longdrange.ldattribute.card.UIConfig.load(this); ok++; } catch (Throwable t) { fail++; } }
        if (all || "cardlevel".equals(k)) { try { CardLevelConfig.load(this); ok++; } catch (Throwable t) { fail++; } }
        if (all || "star".equals(k))      { try { StarConfig.load(this); ok++; } catch (Throwable t) { fail++; } }
        if (all || "tempbuff".equals(k))  { try { TempBuffConfig.load(this); ok++; } catch (Throwable t) { fail++; } }
        if (all || "decompose".equals(k)) { try { DecomposeConfig.load(this); ok++; } catch (Throwable t) { fail++; } }
        if (all || "rune".equals(k))      { try { com.longdrange.ldattribute.rune.RuneConfig.load(this); ok++; } catch (Throwable t) { fail++; } }
        if (all || "achievement".equals(k)) { try { com.longdrange.ldattribute.achievement.AchievementConfig.load(this); ok++; } catch (Throwable t) { fail++; } }
        if (all || "gacha".equals(k))     { try { com.longdrange.ldattribute.gacha.GachaConfig.load(this); ok++; } catch (Throwable t) { fail++; } }
        if (all || "item".equals(k) || "card".equals(k)) { try { CardDataManager.load(this); ok++; } catch (Throwable t) { fail++; } }
        if (all || "suit".equals(k))      { try { SuitData.load(this); ok++; } catch (Throwable t) { fail++; } }
        if (all || "synergy".equals(k))   { try { com.longdrange.ldattribute.card.SynergyData.load(this); ok++; } catch (Throwable t) { fail++; } }
        if (all || "combo".equals(k))     { try { com.longdrange.ldattribute.card.ComboData.load(this); ok++; } catch (Throwable t) { fail++; } }
        if (all || "element".equals(k))   { try { com.longdrange.ldattribute.combat.ElementConfig.load(this); ok++; } catch (Throwable t) { fail++; } }
        if (all || "state".equals(k)) {
            try { com.longdrange.ldattribute.combat.StateConfig.load(this); ok++; } catch (Throwable t) { fail++; }
            try { com.longdrange.ldattribute.combat.StateManager.init(this); ok++; } catch (Throwable t) { fail++; }
        }
        if (all || "page".equals(k))       { try { PageConfig.load(this); ok++; } catch (Throwable t) { fail++; } }
        if (all || "command".equals(k))    { try { CommandConfig.load(this); ok++; } catch (Throwable t) { fail++; } }
        if (all || "collection".equals(k)) { try { CollectionConfig.load(this); ok++; } catch (Throwable t) { fail++; } }
        if (all || "recipe".equals(k))     { try { RecipeConfig.load(this); ok++; } catch (Throwable t) { fail++; } }
        if (all || "points".equals(k))     { try { PointData.loadData(this); ok++; } catch (Throwable t) { fail++; } }
        if (all || "mm".equals(k))         { try { com.longdrange.ldattribute.compat.MMCompat.loadConfig(); ok++; } catch (Throwable t) { fail++; } }
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            try { recalcPlayerCards(p); } catch (Throwable ignored) {}
        }
        getLogger().info("[reload " + k + "] 熱加載完成: " + ok + " 成功, " + fail + " 失敗");
    }

    public CoreManager getCoreManager() { return coreManager; }

    /** 便捷访问：核心存储管理器 */
    public com.longdrange.ldattribute.core.storage.StorageManager getStorageManager() {
        return coreManager == null ? null : coreManager.getStorageManager();
    }

    /** 便捷访问：模块数据管理器 */
    public com.longdrange.ldattribute.core.data.ModuleDataManager getModuleDataManager() {
        return coreManager == null ? null : coreManager.getModuleDataManager();
    }

    /** 便捷访问：魂珠管理器 */
    public com.longdrange.ldattribute.core.ring.RingManager getRingManager() {
        return coreManager == null ? null : coreManager.getRingManager();
    }

    /** 便捷访问：魂珠GUI */
    public com.longdrange.ldattribute.core.ring.gui.RingGUI getRingGUI() {
        return coreManager == null ? null : coreManager.getRingGUI();
    }

    /** 便捷访问：饰品管理器 */
    public com.longdrange.ldattribute.core.jewelry.JewelryManager getJewelryManager() {
        return coreManager == null ? null : coreManager.getJewelryManager();
    }

    /** 便捷访问：饰品 GUI */
    public com.longdrange.ldattribute.core.jewelry.gui.JewelryGUI getJewelryGUI() {
        return coreManager == null ? null : coreManager.getJewelryGUI();
    }

    public com.longdrange.ldattribute.core.talent.TalentManager getTalentManager() {
        return coreManager == null ? null : coreManager.getTalentManager();
    }

    public com.longdrange.ldattribute.core.talent.gui.TalentGUI getTalentGUI() {
        return coreManager == null ? null : coreManager.getTalentGUI();
    }

    public com.longdrange.ldattribute.core.guide.GuideManager getGuideManager() {
        return coreManager == null ? null : coreManager.getGuideManager();
    }

    public com.longdrange.ldattribute.core.guide.gui.GuideGUI getGuideGUI() {
        return coreManager == null ? null : coreManager.getGuideGUI();
    }

    public com.longdrange.ldattribute.core.value.ValueManager getValueManager() {
        return coreManager == null ? null : coreManager.getValueManager();
    }

    public com.longdrange.ldattribute.core.scoreboard.ScoreboardManager getScoreboardManager() {
        return coreManager == null ? null : coreManager.getScoreboardManager();
    }

    public com.longdrange.ldattribute.core.soulring.SoulRingManager getSoulRingManager() {
        return coreManager == null ? null : coreManager.getSoulRingManager();
    }

    public com.longdrange.ldattribute.core.soulring.gui.SoulRingGUI getSoulRingGUI() {
        return coreManager == null ? null : coreManager.getSoulRingGUI();
    }
    public com.longdrange.ldattribute.core.soulring.gui.SoulRingTrashGUI getSoulRingTrashGUI() { return coreManager == null ? null : coreManager.getSoulRingTrashGUI(); }

    public com.longdrange.ldattribute.core.soulring.rate.RateManager getRateManager() {
        return coreManager == null ? null : coreManager.getRateManager();
    }

    public com.longdrange.ldattribute.core.soulring.exchange.gui.ExchangeGUI getExchangeGUI() {
        return coreManager == null ? null : coreManager.getExchangeGUI();
    }
}
