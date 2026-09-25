package com.longdrange.ldattribute.core;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.api.AttributeSourceRegistry;
import com.longdrange.ldattribute.core.data.ModuleDataManager;
import com.longdrange.ldattribute.core.guide.GuideConfig;
import com.longdrange.ldattribute.core.guide.GuideManager;
import com.longdrange.ldattribute.core.guide.gui.GuideGUI;
import com.longdrange.ldattribute.core.guide.listener.GuideGUIListener;
import com.longdrange.ldattribute.core.guide.listener.GuideKillListener;
import com.longdrange.ldattribute.core.guide.listener.GuideUnlockItemListener;
import com.longdrange.ldattribute.core.jewelry.JewelryConfig;
import com.longdrange.ldattribute.core.jewelry.JewelryManager;
import com.longdrange.ldattribute.core.jewelry.gui.JewelryGUI;
import com.longdrange.ldattribute.core.jewelry.listener.JewelryGUIListener;
import com.longdrange.ldattribute.core.listener.CorePlayerListener;
import com.longdrange.ldattribute.core.listener.NameTagListener;
import com.longdrange.ldattribute.core.util.JoinConfig;
import com.longdrange.ldattribute.core.util.PapiUtil;
import com.longdrange.ldattribute.core.ring.RingConfig;
import com.longdrange.ldattribute.core.ring.RingManager;
import com.longdrange.ldattribute.core.ring.RingPAPI;
import com.longdrange.ldattribute.core.ring.RingSetConfig;
import com.longdrange.ldattribute.core.ring.RingSlotConfig;
import com.longdrange.ldattribute.core.ring.RingUpgradeConfig;
import com.longdrange.ldattribute.core.ring.gui.RingGUI;
import com.longdrange.ldattribute.core.ring.listener.RingGUIListener;
import com.longdrange.ldattribute.core.source.CardSource;
import com.longdrange.ldattribute.core.source.LoreSource;
import com.longdrange.ldattribute.core.source.RuneSource;
import com.longdrange.ldattribute.core.source.SuitSource;
import com.longdrange.ldattribute.core.source.SynergySource;
import com.longdrange.ldattribute.core.storage.StorageManager;
import com.longdrange.ldattribute.core.talent.TalentConfig;
import com.longdrange.ldattribute.core.talent.TalentManager;
import com.longdrange.ldattribute.core.talent.gui.TalentGUI;
import com.longdrange.ldattribute.core.talent.listener.TalentGUIListener;
import com.longdrange.ldattribute.core.value.ValueConfig;
import com.longdrange.ldattribute.core.scoreboard.ScoreboardConfig;
import com.longdrange.ldattribute.core.scoreboard.ScoreboardManager;
import com.longdrange.ldattribute.core.soulring.SoulRingConfig;
import com.longdrange.ldattribute.core.soulring.SoulRingManager;
import com.longdrange.ldattribute.core.soulring.rate.RateConfig;
import com.longdrange.ldattribute.core.soulring.rate.RateManager;
import com.longdrange.ldattribute.core.soulring.rate.MythicMobsRateListener;
import com.longdrange.ldattribute.core.soulring.gui.SoulRingGUI;
import com.longdrange.ldattribute.core.soulring.gui.SoulRingTrashGUI;
import com.longdrange.ldattribute.core.soulring.listener.SoulRingTrashListener;
import com.longdrange.ldattribute.core.soulring.listener.SoulRingListener;
import com.longdrange.ldattribute.core.soulring.exchange.ExchangeConfig;
import com.longdrange.ldattribute.core.soulring.exchange.gui.ExchangeGUI;
import com.longdrange.ldattribute.core.soulring.exchange.gui.ExchangeListener;
import com.longdrange.ldattribute.core.soulring.listener.AutoPickupListener;
import com.longdrange.ldattribute.core.value.ValueManager;
import com.longdrange.ldattribute.core.value.ValuePAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class CoreManager {

    private final LDAttribute plugin;
    private FileConfiguration coreConfig;
    private StorageManager storageManager;
    private ModuleDataManager moduleDataManager;

    private RingManager ringManager;
    private RingGUI ringGUI;

    private JewelryManager jewelryManager;
    private JewelryGUI jewelryGUI;

    private TalentManager talentManager;
    private TalentGUI talentGUI;

    private GuideManager guideManager;
    private GuideGUI guideGUI;

    private ValueManager valueManager;
    private ScoreboardManager scoreboardManager;
    private SoulRingManager soulRingManager;
    private SoulRingGUI soulRingGUI;
    private SoulRingTrashGUI soulRingTrashGUI;
    private ExchangeGUI exchangeGUI;
    private RateManager rateManager;

    public CoreManager(LDAttribute plugin) { this.plugin = plugin; }

    public void enable() {
        loadCoreConfig();
        PapiUtil.init();
        JoinConfig.load(plugin);

        this.storageManager = new StorageManager(plugin);
        try {
            this.storageManager.init();
        } catch (Exception e) {
            plugin.getLogger().severe("[Core] 存储初始化失败: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        this.moduleDataManager = new ModuleDataManager(plugin);
        this.moduleDataManager.startAutoSave();

        Bukkit.getPluginManager().registerEvents(new CorePlayerListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new NameTagListener(plugin), plugin);

        enableRing();
        enableJewelry();
        enableTalent();
        enableGuide();
        enableValue();
        enableScoreboard();
        enableSoulRing();
        enableRate();

        registerAttributeSources();

        plugin.getLogger().info("[Core] 核心模块已启用 | 存储: " + storageManager.getType());
    }

    public void disable() {
        if (valueManager != null) valueManager.stopRegen();
        if (scoreboardManager != null) scoreboardManager.stop();
        if (soulRingManager != null) soulRingManager.saveAll();
        if (guideManager != null) guideManager.saveAll();
        if (talentManager != null) talentManager.saveAll();
        if (jewelryManager != null) jewelryManager.saveAll();
        if (ringManager != null) ringManager.saveAll();
        if (moduleDataManager != null) {
            moduleDataManager.stopAutoSave();
            moduleDataManager.saveAll();
        }
        if (storageManager != null) storageManager.shutdown();
        plugin.getLogger().info("[Core] 核心模块已关闭");
    }

    public void reload() throws Exception {
        loadCoreConfig();
        PapiUtil.init();
        JoinConfig.load(plugin);
        if (moduleDataManager != null) moduleDataManager.saveAll();
        if (storageManager != null) storageManager.reload();
        if (moduleDataManager != null) {
            moduleDataManager.stopAutoSave();
            moduleDataManager.startAutoSave();
        }
        if (ringManager != null) ringManager.reload();
        if (jewelryManager != null) jewelryManager.reload();
        if (talentManager != null) talentManager.reload();
        if (guideManager != null) guideManager.reload();
        if (valueManager != null) {
            valueManager.stopRegen();
            valueManager.reload();
            valueManager.startRegenTimers();
        }
        if (scoreboardManager != null) scoreboardManager.reload();
        if (soulRingManager != null) soulRingManager.reload();
        if (rateManager != null) rateManager.reload();
        registerAttributeSources();
    }

    private void loadCoreConfig() {
        File f = new File(plugin.getDataFolder(), "core.yml");
        if (!f.exists()) {
            try { plugin.saveResource("core.yml", false); } catch (Throwable ignored) {}
        }
        coreConfig = YamlConfiguration.loadConfiguration(f);
    }

    public FileConfiguration getCoreConfig() { return coreConfig; }
    public StorageManager getStorageManager() { return storageManager; }
    public ModuleDataManager getModuleDataManager() { return moduleDataManager; }
    public RingManager getRingManager() { return ringManager; }
    public RingGUI getRingGUI() { return ringGUI; }
    public JewelryManager getJewelryManager() { return jewelryManager; }
    public JewelryGUI getJewelryGUI() { return jewelryGUI; }
    public TalentManager getTalentManager() { return talentManager; }
    public TalentGUI getTalentGUI() { return talentGUI; }
    public GuideManager getGuideManager() { return guideManager; }
    public GuideGUI getGuideGUI() { return guideGUI; }
    public ValueManager getValueManager() { return valueManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public SoulRingManager getSoulRingManager() { return soulRingManager; }
    public SoulRingGUI getSoulRingGUI() { return soulRingGUI; }
    public SoulRingTrashGUI getSoulRingTrashGUI() { return soulRingTrashGUI; }
    public ExchangeGUI getExchangeGUI() { return exchangeGUI; }
    public RateManager getRateManager() { return rateManager; }

    public String msg(String key) {
        String s = coreConfig.getString("messages." + key, "");
        return ChatColor.translateAlternateColorCodes('&', s);
    }
    public String msg(String key, String placeholder, String value) {
        return msg(key).replace(placeholder, value);
    }
    public String prefix() { return msg("prefix"); }

    private void enableRing() {
        RingConfig.load(plugin);
        RingSlotConfig.load(plugin);
        RingSetConfig.load(plugin);
        RingUpgradeConfig.load(plugin);
        this.ringManager = new RingManager(plugin);
        this.ringGUI = new RingGUI(plugin);
        Bukkit.getPluginManager().registerEvents(new RingGUIListener(plugin), plugin);
        plugin.getLogger().info("[Ring] 魂珠空间已启用");
        RingPAPI.register(plugin);
    }

    private void enableJewelry() {
        JewelryConfig.load(plugin);
        this.jewelryManager = new JewelryManager(plugin);
        this.jewelryGUI = new JewelryGUI(plugin);
        Bukkit.getPluginManager().registerEvents(new JewelryGUIListener(plugin), plugin);
        plugin.getLogger().info("[Jewelry] 饰品背包已启用");
    }

    private void enableTalent() {
        TalentConfig.load(plugin);
        this.talentManager = new TalentManager(plugin);
        this.talentGUI = new TalentGUI(plugin);
        Bukkit.getPluginManager().registerEvents(new TalentGUIListener(plugin), plugin);
        plugin.getLogger().info("[Talent] 天赋系统已启用");
    }

    private void enableGuide() {
        GuideConfig.load(plugin);
        this.guideManager = new GuideManager(plugin);
        this.guideGUI = new GuideGUI(plugin);
        Bukkit.getPluginManager().registerEvents(new GuideGUIListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new GuideKillListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new GuideUnlockItemListener(plugin), plugin);
        plugin.getLogger().info("[Guide] 怪物图鉴已启用");
    }

    private void enableValue() {
        ValueConfig.load(plugin);
        this.valueManager = new ValueManager(plugin);
        ValuePAPI.register(plugin);
        this.valueManager.startRegenTimers();
        plugin.getLogger().info("[Value] 自定义值已启用");
    }

    private void registerAttributeSources() {
        AttributeSourceRegistry.clear();

        AttributeSourceRegistry.register(new LoreSource("魂珠空间",
            p -> com.longdrange.ldattribute.core.ring.RingStatsProvider.getRingLore(p)));
        AttributeSourceRegistry.register(new LoreSource("饰品背包",
            p -> com.longdrange.ldattribute.core.jewelry.JewelryStatsProvider.getJewelryLore(p)));
        AttributeSourceRegistry.register(new LoreSource("天赋",
            p -> com.longdrange.ldattribute.core.talent.TalentStatsProvider.getTalentLore(p)));
        AttributeSourceRegistry.register(new LoreSource("宠物",
            p -> com.longdrange.ldattribute.pet.PetManager.getAllPetsAttributes(p)));
        AttributeSourceRegistry.register(new LoreSource("战斗状态",
            p -> com.longdrange.ldattribute.combat.StateManager.getActiveAttributes(p)));
        AttributeSourceRegistry.register(new LoreSource("限时Buff",
            p -> com.longdrange.ldattribute.card.TempBuffManager.getActiveEffects(p.getUniqueId())));
        AttributeSourceRegistry.register(new LoreSource("怪物图鉴",
            p -> com.longdrange.ldattribute.core.guide.GuideStatsProvider.getGuideLore(p)));

        AttributeSourceRegistry.register(new CardSource());
        AttributeSourceRegistry.register(new RuneSource());
        AttributeSourceRegistry.register(new SuitSource());
        AttributeSourceRegistry.register(new SynergySource());

        AttributeSourceRegistry.loadFromConfig(plugin);

        plugin.getLogger().info("[Core] 共注册 " + AttributeSourceRegistry.getAll().size() + " 个属性来源");
    }

    private void enableScoreboard() {
        ScoreboardConfig.load(plugin);
        this.scoreboardManager = new ScoreboardManager(plugin);
        this.scoreboardManager.start();
        plugin.getLogger().info("[Scoreboard] 内置侧边栏已启用");
    }

    private void enableSoulRing() {
        SoulRingConfig.load(plugin);
        this.soulRingManager = new SoulRingManager(plugin);
        this.soulRingGUI = new SoulRingGUI(plugin);
        this.soulRingTrashGUI = new SoulRingTrashGUI(plugin);
        Bukkit.getPluginManager().registerEvents(new SoulRingTrashListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new SoulRingListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new AutoPickupListener(plugin), plugin);
        ExchangeConfig.load(plugin);
        this.exchangeGUI = new ExchangeGUI(plugin);
        Bukkit.getPluginManager().registerEvents(new ExchangeListener(plugin), plugin);
        plugin.getLogger().info("[SoulRing] 灵魂空间已启用");
    }

    private void enableRate() {
        RateConfig.load(plugin);
        this.rateManager = new RateManager(plugin);
        plugin.getLogger().info("[Rate] 倍率系统已启用");
        MythicMobsRateListener mmListener = new MythicMobsRateListener(plugin);
        mmListener.register();
    }
}
