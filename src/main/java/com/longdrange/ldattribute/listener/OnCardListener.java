package com.longdrange.ldattribute.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.card.CardData;
import com.longdrange.ldattribute.card.CardDataManager;
import com.longdrange.ldattribute.card.CardNBT;
import com.longdrange.ldattribute.card.CardExpTask;
import com.longdrange.ldattribute.card.CooldownSkipData;
import com.longdrange.ldattribute.card.DecomposeConfig;
import com.longdrange.ldattribute.card.CardLevel;
import com.longdrange.ldattribute.card.CardLevelConfig;
import com.longdrange.ldattribute.card.ExpStoneData;
import com.longdrange.ldattribute.card.PageConfig;
import com.longdrange.ldattribute.card.PlayerData;
import com.longdrange.ldattribute.card.StatsDataRead;
import com.longdrange.ldattribute.card.CollectionChecker;
import com.longdrange.ldattribute.card.inventory.CardInfoInventory;
import com.longdrange.ldattribute.card.inventory.CardInventory;
import com.longdrange.ldattribute.card.inventory.CollectionInventory;
import com.longdrange.ldattribute.card.inventory.DecomposeConfirmInventory;
import com.longdrange.ldattribute.card.inventory.ExpStoneSelectInventory;
import com.longdrange.ldattribute.card.inventory.MergeInventory;
import com.longdrange.ldattribute.card.inventory.SellInventory;
import com.longdrange.ldattribute.card.inventory.SuitInventory;
import com.longdrange.ldattribute.points.PointAPI;
import com.longdrange.ldattribute.util.Message;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.entity.Item;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class OnCardListener implements Listener {

    private final LDAttribute plugin;
    private final java.util.Map<java.util.UUID, int[]> playerViewPos = new java.util.HashMap<>();

    public OnCardListener(LDAttribute plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemHeldRefresh(org.bukkit.event.player.PlayerItemHeldEvent event) {
        final org.bukkit.entity.Player p = event.getPlayer();
        final int slot = event.getNewSlot();
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                org.bukkit.inventory.ItemStack held = p.getInventory().getItem(slot);
                if (held == null) return;
                com.longdrange.ldattribute.card.CardData cd =
                        com.longdrange.ldattribute.card.CardDataManager.findCard(held);
                if (cd == null) return;
                if (!com.longdrange.ldattribute.card.CardLevelConfig.isUpgradable(cd.getId())) return;
                org.bukkit.inventory.ItemStack newItem =
                        com.longdrange.ldattribute.card.CardLevel.recalc(held, cd.getId(),
                                com.longdrange.ldattribute.card.CardNBT.getLevel(held),
                                com.longdrange.ldattribute.card.CardNBT.getExp(held));
                p.getInventory().setItem(slot, newItem);
            } catch (Throwable ignored) {}
        }, 1L);
    }
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (!com.longdrange.ldattribute.card.CardNBT.isBound(item)) return;
        String boundUUID = com.longdrange.ldattribute.card.CardNBT.getBoundUUID(item);
        if (boundUUID.equals(event.getPlayer().getUniqueId().toString())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c這張卡已綁定你自己，無法丟棄！");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (!com.longdrange.ldattribute.card.CardNBT.isBound(item)) return;
        String boundUUID = com.longdrange.ldattribute.card.CardNBT.getBoundUUID(item);
        if (!boundUUID.equals(event.getPlayer().getUniqueId().toString())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c這張卡綁定的是其他玩家，無法撿取！");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();
        if (!com.longdrange.ldattribute.card.CardNBT.isBound(item)) return;
        // 只允許進入卡片背包相關的容器（無此類容器時直接取消）
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // 攻擊方觸發
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            com.longdrange.ldattribute.card.TempBuffManager.tryTrigger(attacker, "PLAYER_ATTACK");
            // 暴擊判定
            if (com.longdrange.ldattribute.card.TempBuffManager.isCritical(attacker)) {
                com.longdrange.ldattribute.card.TempBuffManager.tryTrigger(attacker, "PLAYER_CRITICAL");
                // 受害者被暴擊
                if (event.getEntity() instanceof Player) {
                    com.longdrange.ldattribute.card.TempBuffManager.tryTrigger((Player) event.getEntity(), "PLAYER_BE_CRITICAL");
                }
            }
        }
        // 受傷方觸發
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            com.longdrange.ldattribute.card.TempBuffManager.tryTrigger(victim, "PLAYER_DAMAGED");
            // 計算受傷後 HP 百分比（延遲 1 tick 因為此時 HP 還未扣）
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    double hp = victim.getHealth();
                    double max = victim.getMaxHealth();
                    if (max <= 0) return;
                    int percent = (int) (hp / max * 100);
                    com.longdrange.ldattribute.card.TempBuffManager.tryTrigger(victim, "PLAYER_LOW_HP", percent);
                } catch (Throwable ignored) {}
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        com.longdrange.ldattribute.card.TempBuffManager.tryTrigger(killer, "PLAYER_KILL");
        com.longdrange.ldattribute.card.TempBuffManager.addKillStreak(killer);
    }
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        processOfflineCardExp(event.getPlayer());
        StatsDataRead.scheduleUpdate(event.getPlayer());
    }
    private void scanRunesOnJoin(Player p) {
        try {
            for (org.bukkit.inventory.ItemStack it : p.getInventory().getContents()) {
                if (it == null) continue;
                String rid = com.longdrange.ldattribute.rune.RuneItem.getRuneId(it);
                if (rid != null) com.longdrange.ldattribute.rune.RuneData.collect(p.getUniqueId(), rid);
            }
        } catch (Throwable ignored) {}
    }


    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PlayerData.setLastSeen(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        CardExpTask.clearPlayer(event.getPlayer().getUniqueId());
        com.longdrange.ldattribute.card.TempBuffManager.clear(event.getPlayer().getUniqueId());
        PlayerData.unload(event.getPlayer().getUniqueId());
        try { com.longdrange.ldattribute.pet.PetEntityManager.onPlayerQuit(event.getPlayer()); } catch (Throwable ignored) {}
        com.longdrange.ldattribute.card.SynergyData.unload(event.getPlayer().getUniqueId());
        com.longdrange.ldattribute.card.ManaManager.unload(event.getPlayer().getUniqueId());
        CollectionChecker.unload(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        if (CardInventory.isCardInventory(event.getView().getTitle())) {
            int page = PlayerData.getCurrentPage(player.getUniqueId());
            if (CardInventory.isPageUnlocked(player, page)) {
                CardInventory.save(player, event.getInventory(), page);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) return;
        ExpStoneData.ExpStone stone = ExpStoneData.parse(hand);
        if (stone != null) {
            event.setCancelled(true);
            ExpStoneSelectInventory.open(player, stone, hand.clone());
            return;
        }
        CooldownSkipData skip = CooldownSkipData.parse(hand);
        if (skip != null) {
            event.setCancelled(true);
            useCooldownSkip(player, hand);
        }
    }
    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = event.getView().getTitle();
        if (CardInventory.isCardInventory(title) || SellInventory.isSellInventory(title)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onContainerClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (CardInventory.isCardInventory(event.getView().getTitle())) return;
        if (event.getClickedInventory() == null) return;
        String top = event.getView().getTopInventory().getType().name();
        boolean isContainer = top.contains("CHEST") || top.contains("BARREL")
                || top.contains("SHULKER") || top.contains("ENDER")
                || top.contains("HOPPER") || top.contains("DROPPER")
                || top.contains("DISPENSER") || top.contains("FURNACE");
        if (!isContainer) return;
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;
        if (!com.longdrange.ldattribute.card.CardNBT.isBound(item)) return;
        // 玩家背包→容器 或 容器→玩家背包，只要涉及绑定卡就取消
        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
            event.setCancelled(true);
            player.sendMessage("§c這張卡已綁定，無法放入他人容器！");
        }
    }
    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        if (CardInventory.isCardInventory(title)) handleCardInventory(player, event);
        else if (SellInventory.isSellInventory(title)) handleSellInventory(player, event);
        else if (SuitInventory.isSuitInventory(title)) handleSuitInventory(player, event);
        else if (CollectionInventory.isCollection(title)) handleCollection(player, event);
        else if (MergeInventory.isMerge(title)) handleMerge(player, event);
        else if (CardInfoInventory.isCardInfo(title)) handleCardInfo(player, event);
        else if (com.longdrange.ldattribute.rune.RuneInventory.isRune(title)) handleRune(player, event);
        else if (com.longdrange.ldattribute.rune.RuneSelectInventory.isRuneSelect(title)) handleRuneSelect(player, event);
        else if (com.longdrange.ldattribute.rune.RuneCollectionInventory.isCollection(title)) handleRuneCollection(player, event);
        else if (com.longdrange.ldattribute.rune.RuneUpgradeInventory.isUpgrade(title)) handleRuneUpgrade(player, event);
        else if (com.longdrange.ldattribute.rune.RuneRecycleInventory.isRecycle(title)) handleRuneRecycle(player, event);
        else if (ExpStoneSelectInventory.isSelect(title)) handleExpStoneSelect(player, event);
        else if (DecomposeConfirmInventory.isConfirm(title)) handleDecomposeConfirm(player, event);
        else if (com.longdrange.ldattribute.pet.inventory.PetInventory.isPetInventory(title)) handlePetInventory(player, event);
        else if (com.longdrange.ldattribute.pet.inventory.PetDetailInventory.isDetail(title)) handlePetDetail(player, event);
        else if (com.longdrange.ldattribute.card.inventory.StatsInventory.isStatsInventory(title)) handleStats(player, event);
        else if (com.longdrange.ldattribute.card.inventory.RankInventory.isRankInventory(title)) handleRank(player, event, title);
    }

    private void handlePetInventory(Player player, InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        String title = event.getView().getTitle();

        // 提取 page
        int page = com.longdrange.ldattribute.pet.PetData.getCurrentPage(player.getUniqueId());
        int maxPages = com.longdrange.ldattribute.pet.PetConfig.getMaxPages();

        // 翻页
        if (slot == com.longdrange.ldattribute.pet.inventory.PetInventory.SLOT_PREV) {
            if (page > 0) com.longdrange.ldattribute.pet.inventory.PetInventory.open(player, page - 1);
            return;
        }
        if (slot == com.longdrange.ldattribute.pet.inventory.PetInventory.SLOT_NEXT) {
            if (page < maxPages - 1) com.longdrange.ldattribute.pet.inventory.PetInventory.open(player, page + 1);
            return;
        }
        if (slot == com.longdrange.ldattribute.pet.inventory.PetInventory.SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == com.longdrange.ldattribute.pet.inventory.PetInventory.SLOT_PAGE) {
            return;
        }
        if (slot == com.longdrange.ldattribute.pet.inventory.PetInventory.SLOT_STATS) {
            // 打开属性面板显示出战宠物加成
            try {
                com.longdrange.ldattribute.card.inventory.StatsInventory.open(player);
            } catch (Throwable ignored) {}
            return;
        }

        // 点宠物槽
        int petIdx = com.longdrange.ldattribute.pet.inventory.PetInventory.getPetSlotIndex(slot);
        if (petIdx < 0) return;

        boolean unlocked = page < com.longdrange.ldattribute.pet.PetData.getUnlockedPages(player.getUniqueId());
        if (!unlocked) return;

        com.longdrange.ldattribute.pet.PetInstance pi =
                com.longdrange.ldattribute.pet.PetData.getPet(player.getUniqueId(), page, petIdx);

        // 空槽 + 手上有宠物蛋 → 放入
        if (pi == null) {
            org.bukkit.inventory.ItemStack hand = player.getInventory().getItemInMainHand();
            String eggId = com.longdrange.ldattribute.pet.PetManager.getPetEggId(hand);
            player.sendMessage("§7[调试] 手物品: " + (hand == null ? "null" : hand.getType()) + " eggId=" + eggId);
            if (eggId != null) {
                // 检查槽位是否超出玩家上限
                int maxSlots = com.longdrange.ldattribute.pet.inventory.PetInventory.getMaxSlots(player);
                if (petIdx >= maxSlots) {
                    player.sendMessage("§c你的宠物槽位不足（上限 " + maxSlots + "）");
                    return;
                }
                // 放入
                int eggLv = com.longdrange.ldattribute.pet.PetManager.getPetEggLevel(hand);
                int eggExp = com.longdrange.ldattribute.pet.PetManager.getPetEggExp(hand);
                com.longdrange.ldattribute.pet.PetInstance newPi =
                        new com.longdrange.ldattribute.pet.PetInstance(eggId, eggLv, eggExp);
                com.longdrange.ldattribute.pet.PetData.setPet(player.getUniqueId(), page, petIdx, newPi);
                if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
                else player.getInventory().setItemInMainHand(null);
                com.longdrange.ldattribute.pet.PetConfig.Pet def =
                        com.longdrange.ldattribute.pet.PetConfig.get(eggId);
                player.sendMessage("§a✦ 成功放入宠物 " + (def != null ? def.name : eggId));
                com.longdrange.ldattribute.pet.inventory.PetInventory.open(player, page);
            }
            return;
        }

        if (event.getClick() == ClickType.RIGHT) {
            // 右键 → 详情
            playerViewPos.put(player.getUniqueId(), new int[]{page, petIdx});
            com.longdrange.ldattribute.pet.inventory.PetDetailInventory.open(player, page, petIdx);
        } else {
            // 左键 → 出战/取消
            int ap = com.longdrange.ldattribute.pet.PetData.getActivePage(player.getUniqueId());
            int as = com.longdrange.ldattribute.pet.PetData.getActiveSlot(player.getUniqueId());
            if (ap == page && as == petIdx) {
                com.longdrange.ldattribute.pet.PetData.clearActive(player.getUniqueId());
                com.longdrange.ldattribute.pet.PetEntityManager.removePet(player);
                player.sendMessage("§a✦ 宠物已取消出战");
            } else {
                com.longdrange.ldattribute.pet.PetData.setActive(player.getUniqueId(), page, petIdx);
                com.longdrange.ldattribute.pet.PetEntityManager.spawnPet(player, pi);
                player.sendMessage("§a✦ 宠物已出战");
            }
            try { com.longdrange.ldattribute.card.StatsDataRead.updatePlayer(player); } catch (Throwable ignored) {}
            com.longdrange.ldattribute.pet.inventory.PetInventory.open(player, page);
        }
    }
    private void handlePetDetail(Player player, InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        int page = com.longdrange.ldattribute.pet.PetData.getCurrentPage(player.getUniqueId());
        int[] viewPos = playerViewPos.get(player.getUniqueId());

        if (slot == com.longdrange.ldattribute.pet.inventory.PetDetailInventory.SLOT_BACK) {
            com.longdrange.ldattribute.pet.inventory.PetInventory.open(player, page);
            return;
        }
        if (slot == com.longdrange.ldattribute.pet.inventory.PetDetailInventory.SLOT_EXTRACT) {
            if (viewPos != null) {
                boolean ok = com.longdrange.ldattribute.pet.PetManager.extractPet(player, viewPos[0], viewPos[1]);
                if (ok) {
                    playerViewPos.remove(player.getUniqueId());
                    com.longdrange.ldattribute.pet.inventory.PetInventory.open(player, viewPos[0]);
                }
            }
            return;
        }
        if (slot == com.longdrange.ldattribute.pet.inventory.PetDetailInventory.SLOT_EVOLVE) {
            if (viewPos != null) {
                com.longdrange.ldattribute.pet.PetManager.evolve(player, viewPos[0], viewPos[1]);
                com.longdrange.ldattribute.pet.inventory.PetDetailInventory.open(player, viewPos[0], viewPos[1]);
            }
            return;
        }
        if (slot == com.longdrange.ldattribute.pet.inventory.PetDetailInventory.SLOT_FEED) {
            if (viewPos != null) {
                org.bukkit.inventory.ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand == null || hand.getType() == org.bukkit.Material.AIR) {
                    player.sendMessage("§c请把经验石/钻石拿在主手");
                    return;
                }
                int expGain = getFeedExp(hand);
                if (expGain <= 0) {
                    player.sendMessage("§c此物品不能喂食（可用: 钻石/绿宝石/金锭/铁锭/经验瓶/下界之星）");
                    return;
                }
                if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
                else player.getInventory().setItemInMainHand(null);
                com.longdrange.ldattribute.pet.PetManager.addExp(player, viewPos[0], viewPos[1], expGain);
                player.sendMessage("§a✦ 喂食成功！宠物获得 §e" + expGain + " §a经验");
                try { player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_BURP, 1f, 1f); } catch (Throwable ignored) {}
                com.longdrange.ldattribute.pet.inventory.PetDetailInventory.open(player, viewPos[0], viewPos[1]);
            }
            return;
        }
    }
    private int getFeedExp(org.bukkit.inventory.ItemStack hand) {
        if (hand == null) return 0;
        switch (hand.getType()) {
            case DIAMOND: return 50;
            case EMERALD: return 30;
            case GOLD_INGOT: return 20;
            case IRON_INGOT: return 15;
            case EXP_BOTTLE: return 100;
            case NETHER_STAR: return 500;
            default: return 0;
        }
    }
    private void processOfflineCardExp(Player player) {
        long now = System.currentTimeMillis();
        long lastSeen = PlayerData.getLastSeen(player.getUniqueId());
        if (lastSeen <= 0) return;
        long offlineSec = (now - lastSeen) / 1000L;
        if (offlineSec < 60) return;

        int totalExp = 0;
        int unlocked = PlayerData.getUnlockedPages(player.getUniqueId());
        for (int page = 0; page < unlocked; page++) {
            ItemStack[] items = PlayerData.getPageInventory(player.getUniqueId(), page);
            boolean dirty = false;
            for (int i = 0; i < items.length; i++) {
                ItemStack card = items[i];
                if (card == null) continue;
                CardData cd = CardDataManager.findCard(card);
                if (cd == null) continue;
                CardLevelConfig.CardLevel cl = CardLevelConfig.get(cd.getId());
                if (cl == null || cl.timeOfflineInterval <= 0) continue;
                if (CardNBT.isBound(card) && !CardNBT.getBoundUUID(card).equals(player.getUniqueId().toString())) continue;
                if (CardLevel.isMaxLevel(cd.getId(), CardNBT.getLevel(card))) continue;

                long calcSec = Math.min(offlineSec, cl.timeOfflineMaxSeconds);
                int times = (int) (calcSec / cl.timeOfflineInterval);
                if (times <= 0) continue;
                int exp = times * cl.timeOfflineExp;
                CardLevel.Result r = CardLevel.addExp(card, cd.getId(), exp);
                items[i] = r.item;
                dirty = true;
                totalExp += exp;
            }
            if (dirty) PlayerData.setPageInventory(player.getUniqueId(), page, items);
        }
        if (totalExp > 0) {
            PlayerData.savePlayer(player.getUniqueId());
            player.sendMessage("§8[§d卡片§8] §a離線期間卡片獲得 §e" + totalExp + " §a經驗");
        }
    }
    private void handleCardInventory(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();

        if (slot >= 0 && slot < 54) {
            event.setCancelled(true);
            int page = PlayerData.getCurrentPage(player.getUniqueId());
            boolean unlocked = CardInventory.isPageUnlocked(player, page);

            if (slot == CardInventory.SLOT_PREV) {
                if (page > 0) CardInventory.open(player, page - 1);
                return;
            }
            if (slot == CardInventory.SLOT_NEXT) {
                if (page < PageConfig.getPageCount() - 1) CardInventory.open(player, page + 1);
                return;
            }
            if (slot == CardInventory.SLOT_COLLECTION) { CollectionInventory.open(player); return; }
            if (slot == CardInventory.SLOT_SELL) { SellInventory.open(player); return; }
            if (slot == CardInventory.SLOT_SUIT) { SuitInventory.open(player); return; }
            if (slot == CardInventory.SLOT_RANK) {
                com.longdrange.ldattribute.card.inventory.RankInventory.open(player, "CARD", 1);
                return;
            }
            if (slot == CardInventory.SLOT_INFO) {
                if (!unlocked) unlockPage(player, page);
                return;
            }
            if (slot >= 45) return;
            if (!unlocked) return;

            // 逐格解锁判断
            PageConfig.Page pageCfg = PageConfig.getPage(page);
            if (pageCfg != null) {
                int unlockedSlots = PlayerData.getOrInitPageUnlockedSlots(
                        player.getUniqueId(), page, pageCfg.defaultSlots);
                if (slot >= unlockedSlots) {
                    tryUnlockSlots(player, page, pageCfg, unlockedSlots);
                    return;
                }
            }
            if (clicked == null || clicked.getType() == Material.AIR) return;
            if (event.getClick() == ClickType.RIGHT) {
                CardData cd = CardDataManager.findCard(clicked);
                if (cd != null) { CardInfoInventory.open(player, cd, clicked); return; }
            }
            takeCard(player, slot, clicked, page);
            return;
        }

        if (slot >= 54 && clicked != null && clicked.getType() != Material.AIR) {
            int page = PlayerData.getCurrentPage(player.getUniqueId());
            if (!CardInventory.isPageUnlocked(player, page)) return;
            if (com.longdrange.ldattribute.card.CardNBT.isBound(clicked)) {
                String bu = com.longdrange.ldattribute.card.CardNBT.getBoundUUID(clicked);
                if (!bu.equals(player.getUniqueId().toString())) {
                    event.setCancelled(true);
                    player.sendMessage("\u00a7c\u9019\u5f35\u5361\u7d81\u5b9a\u7684\u662f\u5176\u4ed6\u73a9\u5bb6\uff0c\u7121\u6cd5\u653e\u5165\u5361\u7247\u80cc\u5305\uff01");
                    return;
                }
            }
            if (ExpStoneData.parse(clicked) != null) {
                event.setCancelled(true);
                player.sendMessage("§c經驗石不能放入卡片背包！");
                return;
            }
            CardData card = CardDataManager.findCard(clicked);
            if (card == null) return;
            event.setCancelled(true);
            putCard(player, slot, clicked, card, page);
        }
    }

    private void tryUnlockSlots(Player player, int page, PageConfig.Page cfg, int currentUnlocked) {
        PageConfig.SlotTier tier = cfg.getNextTier(currentUnlocked);
        if (tier == null) {
            player.sendMessage("§c所有格子已解鎖！");
            return;
        }
        if (tier.permission != null && !tier.permission.isEmpty()
                && !player.hasPermission(tier.permission)) {
            player.sendMessage("§c你沒有權限解鎖！需要權限: §e" + tier.permission);
            return;
        }
        if (tier.costPoints > 0) {
            int points = PointAPI.getPlayerPoints(player.getName());
            if (points < tier.costPoints) {
                player.sendMessage("§c點券不足！需要 §e" + tier.costPoints + " §c你有 §e" + points);
                return;
            }
        }
        net.milkbowl.vault.economy.Economy eco = null;
        if (tier.costVault > 0) {
            try {
                org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
                        Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                if (rsp != null) eco = rsp.getProvider();
            } catch (Throwable ignored) {}
            if (eco == null) {
                player.sendMessage("§c未安裝 Vault 經濟");
                return;
            }
            if (!eco.has(player, tier.costVault)) {
                player.sendMessage("§c金幣不足！需要 §e" + (int) tier.costVault);
                return;
            }
        }
        for (String itemStr : tier.items) {
            if (!hasEnoughItem(player, itemStr)) {
                player.sendMessage("§c缺少物品: §e" + itemStr);
                return;
            }
        }
        if (tier.costPoints > 0) PointAPI.takePlayerPoints(player.getName(), tier.costPoints);
        if (tier.costVault > 0 && eco != null) eco.withdrawPlayer(player, tier.costVault);
        for (String itemStr : tier.items) takeItem(player, itemStr);

        int newUnlocked = Math.min(cfg.slots, currentUnlocked + tier.count);
        PlayerData.setPageUnlockedSlots(player.getUniqueId(), page, newUnlocked);
        PlayerData.savePlayer(player.getUniqueId());

        for (String cmd : tier.commands) {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        cmd.replace("{player}", player.getName()));
            } catch (Throwable ignored) {}
        }
        player.sendMessage("§a✦ 成功解鎖 §e" + tier.count + " §a格！(共 " + newUnlocked + "/" + cfg.slots + ")");
        try { player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f); } catch (Throwable ignored) {}

        CardInventory.open(player, page);
    }

    private void takeCard(Player player, int slot, ItemStack card, int page) {
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(Message.get("Player.InventoryFull"));
            return;
        }
        Inventory top = player.getOpenInventory().getTopInventory();
        CardData data = CardDataManager.findCard(card);
        top.setItem(slot, null);
        player.getInventory().addItem(card);
        CardInventory.save(player, top, page);
        if (data != null) player.sendMessage(Message.get("Player.CardTaken", data.getId()));
        CollectionChecker.check(player);
    }

    private void putCard(Player player, int playerSlot, ItemStack card, CardData cardData, int page) {
        Inventory top = player.getOpenInventory().getTopInventory();
        PageConfig.Page cfg = PageConfig.getPage(page);
        if (cfg == null) return;
        int slots = Math.min(cfg.slots, 45);

        int unlocked = PlayerData.getUnlockedPages(player.getUniqueId());
        for (int p = 0; p < unlocked; p++) {
            ItemStack[] items = PlayerData.getPageInventory(player.getUniqueId(), p);
            for (ItemStack ex : items) {
                if (ex == null || ex.getType() == Material.AIR) continue;
                CardData ed = CardDataManager.findCard(ex);
                if (ed != null && ed.getId().equals(cardData.getId())) {
                    player.sendMessage(Message.get("Player.DuplicateCard"));
                    return;
                }
            }
        }

        int empty = -1;
        for (int i = 0; i < slots; i++) {
            ItemStack ex = top.getItem(i);
            if (ex == null || ex.getType() == Material.AIR) { empty = i; break; }
        }
        if (empty == -1) {
            player.sendMessage(Message.get("Player.NoSlot"));
            return;
        }

        ItemStack copy = card.clone();
        copy.setAmount(1);
        top.setItem(empty, copy);

        if (card.getAmount() > 1) card.setAmount(card.getAmount() - 1);
        else card.setAmount(0);

        CardInventory.save(player, top, page);
        CollectionChecker.check(player);
    }

    private void unlockPage(Player player, int page) {
        PageConfig.Page cfg = PageConfig.getPage(page);
        if (cfg == null) return;

        if (cfg.permission != null && !cfg.permission.isEmpty()
                && !player.hasPermission(cfg.permission)) {
            player.sendMessage(Message.get("Prefix") + "§c你沒有權限解鎖此頁！需要: §e" + cfg.permission);
            return;
        }

        int points = PointAPI.getPlayerPoints(player.getName());
        if (cfg.costPoints > 0 && points < cfg.costPoints) {
            player.sendMessage(Message.get("Player.NoPoints", cfg.costPoints, points));
            return;
        }

        if (cfg.costVault > 0) {
            Economy eco = getEconomy();
            if (eco == null) {
                player.sendMessage(Message.get("Prefix") + "§c未安裝 Vault 經濟！");
                return;
            }
            if (!eco.has(player, cfg.costVault)) {
                player.sendMessage(Message.get("Prefix") + "§c金幣不足！需要 §6" + (int) cfg.costVault);
                return;
            }
        }

        for (String s : cfg.items) {
            if (!hasItem(player, s)) {
                player.sendMessage(Message.get("Prefix") + "§c缺少物品: §e" + s);
                return;
            }
        }

        if (cfg.costPoints > 0) PointAPI.takePlayerPoints(player.getName(), cfg.costPoints);
        if (cfg.costVault > 0) {
            Economy eco = getEconomy();
            if (eco != null) eco.withdrawPlayer(player, cfg.costVault);
        }
        for (String s : cfg.items) removeItem(player, s);
        for (String cmd : cfg.commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", player.getName()));
        }

        int cur = PlayerData.getUnlockedPages(player.getUniqueId());
        if (cur <= page) PlayerData.setUnlockedPages(player.getUniqueId(), page + 1);
        PlayerData.savePlayer(player.getUniqueId());

        player.sendMessage(Message.get("Prefix") + "§a成功解鎖第 §e" + (page + 1) + " §a頁！");
        CardInventory.open(player, page);
    }

    private boolean hasItem(Player player, String s) {
        String[] p = s.split(":");
        Material mat = Material.getMaterial(p[0].toUpperCase());
        if (mat == null) return false;
        int need = p.length >= 2 ? Integer.parseInt(p[1]) : 1;
        int c = 0;
        for (ItemStack is : player.getInventory().getContents())
            if (is != null && is.getType() == mat) c += is.getAmount();
        return c >= need;
    }

    private void removeItem(Player player, String s) {
        String[] p = s.split(":");
        Material mat = Material.getMaterial(p[0].toUpperCase());
        if (mat == null) return;
        int need = p.length >= 2 ? Integer.parseInt(p[1]) : 1;
        int removed = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack is = contents[i];
            if (is == null || is.getType() != mat) continue;
            int take = Math.min(is.getAmount(), need - removed);
            is.setAmount(is.getAmount() - take);
            removed += take;
            if (is.getAmount() <= 0) player.getInventory().setItem(i, null);
            if (removed >= need) break;
        }
    }

    private Economy getEconomy() {
        try {
            org.bukkit.plugin.RegisteredServiceProvider<Economy> rsp =
                    Bukkit.getServicesManager().getRegistration(Economy.class);
            return rsp != null ? rsp.getProvider() : null;
        } catch (Throwable t) { return null; }
    }

    private void handleSellInventory(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        event.setCancelled(true);
        if (slot == SellInventory.SLOT_CANCEL) { CardInventory.open(player); return; }
        if (slot < 0 || slot >= 45) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        CardData card = CardDataManager.findCard(clicked);
        if (card == null) return;
        double rate = com.longdrange.ldattribute.util.Config.getDouble("SellCard.Rate", 1.0);
        int value = (int) (card.getValue() * rate);
        int amount = event.getClick() == ClickType.RIGHT
                ? PlayerData.countCard(player.getUniqueId(), card.getId()) : 1;
        int total = value * amount;
        PlayerData.removeCard(player.getUniqueId(), card.getId(),
                event.getClick() == ClickType.RIGHT ? -1 : 1);
        PointAPI.addPlayerPoints(player.getName(), total);
        player.sendMessage(Message.get("Player.SellCard", total));
        SellInventory.open(player);
    }

    private void handleRuneRecycle(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        event.setCancelled(true);

        if (slot == com.longdrange.ldattribute.rune.RuneRecycleInventory.SLOT_BACK) {
            player.closeInventory();
            return;
        }
        int cur = com.longdrange.ldattribute.rune.RuneRecycleInventory.getLastPage(player.getUniqueId());
        if (slot == com.longdrange.ldattribute.rune.RuneRecycleInventory.SLOT_PREV) {
            com.longdrange.ldattribute.rune.RuneRecycleInventory.open(player, cur - 1);
            return;
        }
        if (slot == com.longdrange.ldattribute.rune.RuneRecycleInventory.SLOT_NEXT) {
            com.longdrange.ldattribute.rune.RuneRecycleInventory.open(player, cur + 1);
            return;
        }
        if (slot == com.longdrange.ldattribute.rune.RuneRecycleInventory.SLOT_RECYCLE_ALL) {
            // 全部回收
            java.util.Map<String, Integer> owned = new java.util.LinkedHashMap<>();
            for (org.bukkit.inventory.ItemStack it : player.getInventory().getContents()) {
                if (it == null) continue;
                String rid = com.longdrange.ldattribute.rune.RuneItem.getRuneId(it);
                if (rid == null) continue;
                owned.put(rid, owned.getOrDefault(rid, 0) + it.getAmount());
            }
            if (owned.isEmpty()) { player.sendMessage("§c背包里没有符文"); return; }
            int total = 0;
            for (java.util.Map.Entry<String, Integer> e : owned.entrySet()) {
                total += com.longdrange.ldattribute.rune.RuneRecycleInventory.recycle(player, e.getKey(), e.getValue());
            }
            player.sendMessage("§a✦ 批量回收完成！获得 §6" + total + " §a点券");
            try { player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f); } catch (Throwable ignored) {}
            player.closeInventory();
            return;
        }
        if (slot >= 45) return;  // 底部其它槽位忽略

        // 单个回收
        org.bukkit.inventory.ItemStack clicked = event.getInventory().getItem(slot);
        if (clicked == null) return;
        String runeId = com.longdrange.ldattribute.rune.RuneItem.getRuneId(clicked);
        if (runeId == null) return;
        int amount = event.isRightClick() ? Integer.MAX_VALUE : 1;
        int gain = com.longdrange.ldattribute.rune.RuneRecycleInventory.recycle(player, runeId, amount);
        if (gain <= 0) { player.sendMessage("§c背包里没有此符文"); return; }
        player.sendMessage("§a✦ 回收 " + clicked.getItemMeta().getDisplayName() + " §a→ §6" + gain + " §a点券");
        try { player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f); } catch (Throwable ignored) {}
        // 刷新界面
        com.longdrange.ldattribute.rune.RuneRecycleInventory.open(player, cur);
    }
    private void handleRuneUpgrade(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        event.setCancelled(true);
        if (slot == com.longdrange.ldattribute.rune.RuneUpgradeInventory.SLOT_BACK) {
            player.closeInventory();
            return;
        }
        if (slot == com.longdrange.ldattribute.rune.RuneUpgradeInventory.SLOT_CONFIRM) {
            ItemStack cur = event.getInventory().getItem(com.longdrange.ldattribute.rune.RuneUpgradeInventory.SLOT_CURRENT);
            if (cur == null) return;
            String runeId = com.longdrange.ldattribute.rune.RuneItem.getRuneId(cur);
            if (runeId == null) return;
            com.longdrange.ldattribute.rune.RuneConfig.Rune r =
                    com.longdrange.ldattribute.rune.RuneConfig.getRune(runeId);
            if (r == null || r.upgradeTo == null || r.upgradeTo.isEmpty()) return;
            com.longdrange.ldattribute.rune.RuneConfig.Rune next =
                    com.longdrange.ldattribute.rune.RuneConfig.getRune(r.upgradeTo);
            if (next == null) return;

            int have = com.longdrange.ldattribute.rune.RuneUpgradeInventory.countRunes(player, runeId);
            if (have < 3) {
                player.sendMessage("§c符文不足！需要 §e3 §c个 " + r.name);
                return;
            }
            // 扣 3 个
            int need = 3;
            for (int i = 0; i < player.getInventory().getSize() && need > 0; i++) {
                ItemStack it = player.getInventory().getItem(i);
                if (it == null) continue;
                String rid = com.longdrange.ldattribute.rune.RuneItem.getRuneId(it);
                if (!runeId.equals(rid)) continue;
                int take = Math.min(it.getAmount(), need);
                it.setAmount(it.getAmount() - take);
                need -= take;
                if (it.getAmount() <= 0) player.getInventory().setItem(i, null);
            }
            // 给 1 个升级版
            ItemStack upgraded = com.longdrange.ldattribute.rune.RuneItem.create(next, 1);
            java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(upgraded);
            for (ItemStack drop : leftover.values())
                player.getWorld().dropItemNaturally(player.getLocation(), drop);

            player.sendMessage("§a✦ 升级成功！§e" + r.name + " §a×3 → §e" + next.name);
            try { player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f); } catch (Throwable ignored) {}
            com.longdrange.ldattribute.rune.RuneUpgradeInventory.open(player, runeId);
            return;
        }
    }
    private void handleRuneCollection(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        event.setCancelled(true);
        if (slot == com.longdrange.ldattribute.rune.RuneCollectionInventory.SLOT_BACK) {
            player.closeInventory();
            return;
        }
        int cur = com.longdrange.ldattribute.rune.RuneCollectionInventory.getLastPage(player.getUniqueId());
        if (slot == com.longdrange.ldattribute.rune.RuneCollectionInventory.SLOT_PREV) {
            com.longdrange.ldattribute.rune.RuneCollectionInventory.open(player, cur - 1);
        } else if (slot == com.longdrange.ldattribute.rune.RuneCollectionInventory.SLOT_NEXT) {
            com.longdrange.ldattribute.rune.RuneCollectionInventory.open(player, cur + 1);
        }
    }
    private void handleRuneSelect(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        event.setCancelled(true);

        ItemStack cur = event.getInventory().getItem(com.longdrange.ldattribute.rune.RuneSelectInventory.SLOT_CARD);
        if (cur == null) { player.closeInventory(); return; }
        CardData cd = CardDataManager.findCard(cur);
        if (cd == null) { player.closeInventory(); return; }

        if (slot == com.longdrange.ldattribute.rune.RuneSelectInventory.SLOT_BACK) {
            com.longdrange.ldattribute.rune.RuneInventory.open(player, cd, cur);
            return;
        }

        if (!com.longdrange.ldattribute.rune.RuneSelectInventory.isListSlot(slot)) return;

        Integer socketIndex = com.longdrange.ldattribute.rune.RuneInventory.pendingSocket.get(player.getUniqueId());
        if (socketIndex == null) { player.closeInventory(); return; }

        ItemStack clicked = event.getInventory().getItem(slot);
        String runeId = com.longdrange.ldattribute.rune.RuneItem.getRuneId(clicked);
        if (runeId == null) return;

        java.util.List<String> socketIds = com.longdrange.ldattribute.rune.RuneConfig.getCardSockets(cd.getId());
        if (socketIndex >= socketIds.size()) return;
        String socketId = socketIds.get(socketIndex);
        com.longdrange.ldattribute.rune.RuneConfig.Socket sk =
                com.longdrange.ldattribute.rune.RuneConfig.getSocket(socketId);
        com.longdrange.ldattribute.rune.RuneConfig.Rune rn =
                com.longdrange.ldattribute.rune.RuneConfig.getRune(runeId);
        if (sk == null || rn == null) return;
        if (!sk.allowedTypes.contains(rn.type)) {
            player.sendMessage("§c此孔位不能放此類型符文");
            return;
        }

        // 按 NBT 精确扣一个符文
        if (findRuneSlot(player, runeId) < 0) {
            player.sendMessage("§c背包裡沒有此符文");
            return;
        }
        if (!takeRune(player, runeId, 1)) {
            player.sendMessage("§c扣除符文失敗");
            return;
        }

        ItemStack newCard = com.longdrange.ldattribute.card.CardNBT.setSocketRune(cur, socketIndex, runeId);
        newCard = CardLevel.recalc(newCard, cd.getId(),
                com.longdrange.ldattribute.card.CardNBT.getLevel(newCard),
                com.longdrange.ldattribute.card.CardNBT.getExp(newCard));

        if (replaceCardInBag(player, cur, newCard, cd.getId())) {
            player.sendMessage("§a✦ 鑲嵌成功！§f" + rn.name);
            try { player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.5f); } catch (Throwable ignored) {}
            com.longdrange.ldattribute.rune.RuneInventory.open(player, cd, newCard);
        } else {
            // 卡片找不到 → 把符文还回去
            player.getInventory().addItem(com.longdrange.ldattribute.rune.RuneItem.create(rn, 1));
            player.sendMessage("§c卡片背包裡找不到這張卡");
        }
    }
    private void handleRune(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        event.setCancelled(true);

        ItemStack cur = event.getInventory().getItem(com.longdrange.ldattribute.rune.RuneInventory.SLOT_CARD);
        if (cur == null) { player.closeInventory(); return; }
        CardData cd = CardDataManager.findCard(cur);
        if (cd == null) { player.closeInventory(); return; }

        if (slot == com.longdrange.ldattribute.rune.RuneInventory.SLOT_BACK) {
            CardInfoInventory.open(player, cd, cur);
            return;
        }

        int idx = com.longdrange.ldattribute.rune.RuneInventory.getSocketIndex(slot);
        if (idx < 0) return;

        java.util.List<String> socketIds = com.longdrange.ldattribute.rune.RuneConfig.getCardSockets(cd.getId());
        if (idx >= socketIds.size()) return;
        String socketId = socketIds.get(idx);
        com.longdrange.ldattribute.rune.RuneConfig.Socket sk =
                com.longdrange.ldattribute.rune.RuneConfig.getSocket(socketId);
        if (sk == null) return;

        boolean unlocked = com.longdrange.ldattribute.card.CardNBT.isSocketUnlocked(cur, idx);

        if (!unlocked) {
            // ============ 打孔 ============
            if (sk.costPoints > 0) {
                int p = PointAPI.getPlayerPoints(player.getName());
                if (p < sk.costPoints) {
                    player.sendMessage("§c點券不足！需要 §e" + sk.costPoints + " §c你有 §e" + p);
                    return;
                }
            }
            if (sk.costVault > 0) {
                net.milkbowl.vault.economy.Economy eco = null;
                try {
                    org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
                            org.bukkit.Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                    if (rsp != null) eco = rsp.getProvider();
                } catch (Throwable ignored) {}
                if (eco == null) { player.sendMessage("§c未安裝 Vault"); return; }
                if (!eco.has(player, sk.costVault)) {
                    player.sendMessage("§c金幣不足！需要 §e" + (int) sk.costVault);
                    return;
                }
            }
            for (String spec : sk.costItems) {
                if (!hasEnoughItem(player, spec)) {
                    player.sendMessage("§c缺少物品: §e" + spec);
                    return;
                }
            }
            if (sk.costPoints > 0) PointAPI.takePlayerPoints(player.getName(), sk.costPoints);
            if (sk.costVault > 0) {
                try {
                    org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
                            org.bukkit.Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                    if (rsp != null) rsp.getProvider().withdrawPlayer(player, sk.costVault);
                } catch (Throwable ignored) {}
            }
            for (String spec : sk.costItems) takeItem(player, spec);

            ItemStack newCard = com.longdrange.ldattribute.card.CardNBT.unlockSocket(cur, idx);
            newCard = CardLevel.recalc(newCard, cd.getId(),
                    com.longdrange.ldattribute.card.CardNBT.getLevel(newCard),
                    com.longdrange.ldattribute.card.CardNBT.getExp(newCard));

            if (replaceCardInBag(player, cur, newCard, cd.getId())) {
                player.sendMessage("§a✦ 打孔成功！§7" + sk.name);
                try { player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 1f, 1.5f); } catch (Throwable ignored) {}
                com.longdrange.ldattribute.rune.RuneInventory.open(player, cd, newCard);
            } else {
                player.sendMessage("§c卡片背包裡找不到這張卡");
            }
            return;
        }

        // ============ 已打孔 ============
        String runeId = com.longdrange.ldattribute.card.CardNBT.getSocketRune(cur, idx);
        if (runeId == null || runeId.isEmpty()) {
            // 空孔 → 开选择界面
            com.longdrange.ldattribute.rune.RuneInventory.pendingSocket.put(player.getUniqueId(), idx);
            com.longdrange.ldattribute.rune.RuneSelectInventory.open(player, cd, cur, idx);
            return;
        }

        // 已镶嵌 → 取出：按 runeId 完整重建符文物品（名称/材质/属性/Lore 一致）
        com.longdrange.ldattribute.rune.RuneConfig.Rune r = com.longdrange.ldattribute.rune.RuneConfig.getRune(runeId);
        if (r == null) {
            player.sendMessage("§c符文配置不存在: §e" + runeId);
            return;
        }
        ItemStack runeItem = com.longdrange.ldattribute.rune.RuneItem.create(r, 1);
        ItemStack newCard = com.longdrange.ldattribute.card.CardNBT.setSocketRune(cur, idx, "");
        newCard = CardLevel.recalc(newCard, cd.getId(),
                com.longdrange.ldattribute.card.CardNBT.getLevel(newCard),
                com.longdrange.ldattribute.card.CardNBT.getExp(newCard));

        if (!replaceCardInBag(player, cur, newCard, cd.getId())) {
            player.sendMessage("§c卡片背包裡找不到這張卡");
            return;
        }

        // 先给玩家，若背包满了就掉地上
        java.util.HashMap<Integer, ItemStack> left = player.getInventory().addItem(runeItem);
        if (!left.isEmpty()) {
            for (ItemStack drop : left.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            player.sendMessage("§e背包已滿，符文掉落在你腳下");
        }
        player.sendMessage("§a✦ 已取出符文 §f" + r.name);
        try { player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CLOTH_BREAK, 1f, 1f); } catch (Throwable ignored) {}
        com.longdrange.ldattribute.rune.RuneInventory.open(player, cd, newCard);
    }
    private void handleCardInfo(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        event.setCancelled(true);
        if (slot == CardInfoInventory.SLOT_RUNE) {
            ItemStack cur = event.getInventory().getItem(CardInfoInventory.SLOT_CARD);
            if (cur == null) return;
            CardData cd = CardDataManager.findCard(cur);
            if (cd == null) return;
            java.util.List<String> socketIds =
                    com.longdrange.ldattribute.rune.RuneConfig.getCardSockets(cd.getId());
            if (socketIds.isEmpty()) {
                player.sendMessage("§c此卡片不支援符文孔位");
                return;
            }
            com.longdrange.ldattribute.rune.RuneInventory.open(player, cd, cur);
            return;
        }        if (slot == CardInfoInventory.SLOT_STAR) {
            ItemStack cur = event.getInventory().getItem(CardInfoInventory.SLOT_CARD);
            if (cur == null) return;
            CardData cd = CardDataManager.findCard(cur);
            if (cd == null) return;
            int star = com.longdrange.ldattribute.card.CardNBT.getStar(cur);
            int maxStar = com.longdrange.ldattribute.card.StarConfig.getMaxStarFor(cd.getId());
            if (star >= maxStar) {
                player.sendMessage("§c已達最大星級");
                return;
            }
            com.longdrange.ldattribute.card.StarConfig.Cost cost =
                    com.longdrange.ldattribute.card.StarConfig.getCost(star + 1);
            if (cost == null) {
                player.sendMessage("§c找不到升星配置");
                return;
            }
            // 檢查點券
            if (cost.points > 0) {
                int points = PointAPI.getPlayerPoints(player.getName());
                if (points < cost.points) {
                    player.sendMessage("§c點券不足！需要 §e" + cost.points + " §c你有 §e" + points);
                    return;
                }
            }
            // 檢查金幣
            if (cost.vault > 0) {
                net.milkbowl.vault.economy.Economy eco = null;
                try {
                    org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
                            org.bukkit.Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                    if (rsp != null) eco = rsp.getProvider();
                } catch (Throwable ignored) {}
                if (eco == null) {
                    player.sendMessage("§c未安裝 Vault 經濟");
                    return;
                }
                if (!eco.has(player, cost.vault)) {
                    player.sendMessage("§c金幣不足！需要 §e" + (int) cost.vault);
                    return;
                }
            }
            // 檢查物品
            for (String spec : cost.items) {
                if (!hasEnoughItem(player, spec)) {
                    player.sendMessage("§c缺少物品: §e" + spec);
                    return;
                }
            }
            // 扣款
            if (cost.points > 0) PointAPI.takePlayerPoints(player.getName(), cost.points);
            if (cost.vault > 0) {
                try {
                    org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
                            org.bukkit.Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                    if (rsp != null) rsp.getProvider().withdrawPlayer(player, cost.vault);
                } catch (Throwable ignored) {}
            }
            for (String spec : cost.items) takeItem(player, spec);

            // 成功率判定
            if (Math.random() > cost.successRate) {
                player.sendMessage("§c升星失敗！§7(成功率 §e" + (int)(cost.successRate * 100) + "%§7，材料已消耗)");
                try {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                } catch (Throwable ignored) {}
                CardInfoInventory.open(player, cd, cur);
                return;
            }

            // 升星
            int newStar = star + 1;
            ItemStack newCard = com.longdrange.ldattribute.card.CardNBT.setStar(cur, newStar);
            // 重算 Lore
            newCard = CardLevel.recalc(newCard, cd.getId(),
                    com.longdrange.ldattribute.card.CardNBT.getLevel(newCard),
                    com.longdrange.ldattribute.card.CardNBT.getExp(newCard));
            newCard = com.longdrange.ldattribute.card.CardNBT.setStar(newCard, newStar);
            // 再重算一次以确保星级计入
            newCard = CardLevel.recalc(newCard, cd.getId(),
                    com.longdrange.ldattribute.card.CardNBT.getLevel(newCard),
                    com.longdrange.ldattribute.card.CardNBT.getExp(newCard));
            if (replaceCardInBag(player, cur, newCard, cd.getId())) {
                player.sendMessage("§6✦ 升星成功！§e" + cd.getId() + " §7→ §6" + newStar + " 星");
                try { player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f); } catch (Throwable ignored) {}
                try {
                    org.bukkit.Location loc = player.getLocation().add(0, 1, 0);
                    player.getWorld().spawnParticle(org.bukkit.Particle.FIREWORKS_SPARK, loc, 40, 1, 1, 1, 0.1);
                } catch (Throwable ignored) {}
                CardInfoInventory.open(player, cd, newCard);
            } else {
                player.sendMessage("§c卡片背包裡找不到這張卡");
            }
            return;
        }
        if (slot == CardInfoInventory.SLOT_BIND) {
            ItemStack cur = event.getInventory().getItem(CardInfoInventory.SLOT_CARD);
            if (cur != null) {
                CardData cd = CardDataManager.findCard(cur);
                if (cd != null) {
                    String boundUUID = com.longdrange.ldattribute.card.CardNBT.getBoundUUID(cur);
                    if (boundUUID.isEmpty()) {
                        ItemStack newCard = com.longdrange.ldattribute.card.CardNBT.setBound(
                                cur, player.getUniqueId().toString(), player.getName());
                        newCard = CardLevel.recalc(newCard, cd.getId(),
                                com.longdrange.ldattribute.card.CardNBT.getLevel(newCard),
                                com.longdrange.ldattribute.card.CardNBT.getExp(newCard));
                        if (replaceCardInBag(player, cur, newCard, cd.getId())) {
                            player.sendMessage("\u00a7a\u5df2\u7d81\u5b9a\u5361\u7247 \u00a7e" + cd.getId() + " \u00a7a\u5230 \u00a7e" + player.getName());
                            try { player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f); } catch (Throwable ignored) {}
                            CardInfoInventory.open(player, cd, newCard);
                        } else {
                            player.sendMessage("\u00a7c\u5361\u7247\u80cc\u5305\u88e1\u627e\u4e0d\u5230\u9019\u5f35\u5361");
                        }
                    } else if (boundUUID.equals(player.getUniqueId().toString())) {
                        if (!cd.allowUnbind) {
                            player.sendMessage("\u00a7c\u9019\u5f35\u5361\u7121\u6cd5\u89e3\u7d81");
                            return;
                        }
                        int cost = cd.unbindCost;
                        if (cost > 0) {
                            int points = PointAPI.getPlayerPoints(player.getName());
                            if (points < cost) {
                                player.sendMessage("\u00a7c\u89e3\u7d81\u9700\u8981 \u00a7e" + cost + " \u00a7c\u9ede\u5238\uff0c\u4f60\u6709 \u00a7e" + points);
                                return;
                            }
                            PointAPI.takePlayerPoints(player.getName(), cost);
                        }
                        ItemStack newCard = com.longdrange.ldattribute.card.CardNBT.clearBound(cur);
                        newCard = CardLevel.recalc(newCard, cd.getId(),
                                com.longdrange.ldattribute.card.CardNBT.getLevel(newCard),
                                com.longdrange.ldattribute.card.CardNBT.getExp(newCard));
                        if (replaceCardInBag(player, cur, newCard, cd.getId())) {
                            player.sendMessage("\u00a7a\u5df2\u89e3\u7d81\u5361\u7247 \u00a7e" + cd.getId());
                            CardInfoInventory.open(player, cd, newCard);
                        }
                    } else {
                        player.sendMessage("\u00a7c\u9019\u5f35\u5361\u7d81\u5b9a\u7684\u662f\u5176\u4ed6\u73a9\u5bb6 \u00a7e" + com.longdrange.ldattribute.card.CardNBT.getBoundName(cur));
                    }
                }
            }
            return;
        }
        if (slot == CardInfoInventory.SLOT_LOCK) {
            ItemStack cur = event.getInventory().getItem(CardInfoInventory.SLOT_CARD);
            if (cur != null) {
                CardData cd = CardDataManager.findCard(cur);
                if (cd != null) {
                    boolean locked = com.longdrange.ldattribute.card.CardNBT.isLocked(cur);
                    ItemStack newCard = com.longdrange.ldattribute.card.CardNBT.setLocked(cur, !locked);
                        newCard = CardLevel.recalc(newCard, cd.getId(),
                                com.longdrange.ldattribute.card.CardNBT.getLevel(newCard),
                                com.longdrange.ldattribute.card.CardNBT.getExp(newCard));
                    if (replaceCardInBag(player, cur, newCard, cd.getId())) {
                        player.sendMessage(locked ? "\u00a7a\u5df2\u89e3\u9396\u5361\u7247 \u00a7e" + cd.getId() : "\u00a7a\u5df2\u9396\u5b9a\u5361\u7247 \u00a7e" + cd.getId());
                        CardInfoInventory.open(player, cd, newCard);
                    } else {
                        player.sendMessage("\u00a7c\u5361\u7247\u80cc\u5305\u88e1\u627e\u4e0d\u5230\u9019\u5f35\u5361");
                    }
                }
            }
            return;
        }
        if (slot == CardInfoInventory.SLOT_CLOSE) {
            CardInventory.open(player);
            return;
        }
        if (slot == CardInfoInventory.SLOT_DECOMPOSE) {
            CardData cd = null;
            ItemStack ct = null;
            // 從當前卡資訊界面拿原始卡
            // CardInfoInventory.open 傳進來的 ItemStack 就是玩家卡片背包裡的，暫存一下
            // 簡單做法：直接把當前顯示的卡片拿出來當原卡
            ItemStack cur = event.getInventory().getItem(CardInfoInventory.SLOT_CARD);
            if (cur != null) {
                cd = CardDataManager.findCard(cur);
                ct = cur;
            }
            if (cd != null) {
                DecomposeConfirmInventory.open(player, cd, ct);
            }
            return;
        }
        if (slot == CardInfoInventory.SLOT_USE_EXPSTONE) {
            player.sendMessage("§8[§d經驗石§8] §7經驗石系統 §e第 6 批 §7才會實裝");
            player.closeInventory();
        }
    }

    private void handleDecomposeConfirm(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        event.setCancelled(true);
        CardData card = DecomposeConfirmInventory.getPendingCard(player.getUniqueId());
        ItemStack cardItem = DecomposeConfirmInventory.getPendingItem(player.getUniqueId());
        if (card == null || cardItem == null) { player.closeInventory(); return; }
        if (slot == DecomposeConfirmInventory.SLOT_CANCEL) {
            DecomposeConfirmInventory.clear(player.getUniqueId());
            CardInfoInventory.open(player, card, cardItem);
            return;
        }
        if (slot == DecomposeConfirmInventory.SLOT_CONFIRM) {
            DecomposeConfig.Entry e = DecomposeConfig.get(card.getId());
            // 扣卡
            if (!removeCardFromBag(player, card.getId())) {
                player.sendMessage("§c卡片背包裡找不到這張卡");
                DecomposeConfirmInventory.clear(player.getUniqueId());
                CardInventory.open(player);
                return;
            }
            // 給獎勵
            StringBuilder got = new StringBuilder();
            if (e.points > 0) {
                PointAPI.addPlayerPoints(player.getName(), e.points);
                got.append("點券x").append(e.points).append(" ");
            }
            if (e.vault > 0) {
                try {
                    org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
                            Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                    if (rsp != null) rsp.getProvider().depositPlayer(player, e.vault);
                } catch (Throwable ignored) {}
                got.append("金幣x").append((int) e.vault).append(" ");
            }
            for (String it : e.items) {
                giveItem(player, it);
                got.append(it).append(" ");
            }
            // 隨機池
            for (java.util.Map.Entry<String, Integer> r : e.random.entrySet()) {
                if (Math.random() * 100 < r.getValue()) {
                    CardData c2 = CardDataManager.getCard(r.getKey());
                    if (c2 != null) {
                        PlayerData.addCardToBag(player.getUniqueId(), c2.getItem());
                        got.append(r.getKey()).append(" ");
                    }
                }
            }
            player.sendMessage("§a分解成功！獲得: §e" + got.toString().trim());
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 1f, 1.5f);
            DecomposeConfirmInventory.clear(player.getUniqueId());
            CardInventory.open(player);
        }
    }

    private boolean removeCardFromBag(Player player, String cardId) {
        int unlocked = PlayerData.getUnlockedPages(player.getUniqueId());
        for (int page = 0; page < unlocked; page++) {
            ItemStack[] items = PlayerData.getPageInventory(player.getUniqueId(), page);
            for (int i = 0; i < items.length; i++) {
                if (items[i] == null) continue;
                CardData cd = CardDataManager.findCard(items[i]);
                if (cd != null && cd.getId().equals(cardId)) {
                    items[i] = null;
                    PlayerData.setPageInventory(player.getUniqueId(), page, items);
                    PlayerData.savePlayer(player.getUniqueId());
                    return true;
                }
            }
        }
        return false;
    }

    private void giveItem(Player player, String spec) {
        String[] p = spec.split(":");
        Material mat = Material.getMaterial(p[0].toUpperCase());
        if (mat == null) return;
        int n = p.length >= 2 ? Integer.parseInt(p[1]) : 1;
        player.getInventory().addItem(new ItemStack(mat, n));
    }
    private void handleExpStoneSelect(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        System.out.println("[ExpStone] handleExpStoneSelect called, slot=" + slot);
        event.setCancelled(true);
        if (slot == ExpStoneSelectInventory.SLOT_BACK) {
            ExpStoneSelectInventory.clearPending(player.getUniqueId());
            CardInventory.open(player);
            return;
        }
        if (slot < 0 || slot >= 45) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ExpStoneData.ExpStone stone = ExpStoneData.parse(ExpStoneSelectInventory.getPendingStone(player.getUniqueId()));
        if (stone == null) { player.sendMessage("§c經驗石資料遺失"); return; }
        CardData cd = CardDataManager.findCard(clicked);
        if (cd == null) { player.sendMessage("§c這不是卡片"); return; }

        if (!CardLevelConfig.isUpgradable(cd.getId())) {
            player.sendMessage("§c這張卡不可升級"); return;
        }
        if (CardLevel.isMaxLevel(cd.getId(), CardLevel.getLevel(clicked))) {
            player.sendMessage("§c這張卡已達最大等級"); return;
        }

        // ===== 冷卻 / 每日限制檢查 =====
        CardLevelConfig.CardLevel clc = CardLevelConfig.get(cd.getId());
        long nowTime = System.currentTimeMillis();
        PlayerData.CardCooldown cc = PlayerData.getCooldown(player.getUniqueId(), cd.getId());
        if (clc.cooldownSeconds > 0 && cc.lastUpgrade > 0) {
            long elapsed = nowTime - cc.lastUpgrade;
            long need = clc.cooldownSeconds * 1000L;
            if (elapsed < need) {
                long remain = (need - elapsed) / 1000L;
                player.sendMessage("§c冷卻中！剩餘 §e" + formatTime(remain) + " §c才能再次升級");
                return;
            }
        }
        if (clc.dailyLimit > 0) {
            String today = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
            if (!today.equals(cc.todayDate)) {
                cc.todayDate = today;
                cc.todayCount = 0;
            }
            if (cc.todayCount >= clc.dailyLimit) {
                player.sendMessage("§c今日升級次數已達上限 §e" + clc.dailyLimit);
                return;
            }
        }

        // 扣石頭（一次，不管成敗）
        int have = countExpStone(player, stone);
        if (have < stone.needCount) {
            player.sendMessage("§c經驗石不足，需要 §e" + stone.needCount + " §c個，你有 §e" + have);
            return;
        }
        takeExpStone(player, stone, stone.needCount);

        // 更新冷卻 + 每日計數（不管成敗）
        cc.lastUpgrade = nowTime;
        cc.todayCount++;
        PlayerData.savePlayer(player.getUniqueId());

        // 機率判定
        if (Math.random() > stone.successRate) {
            player.sendMessage("§c使用經驗石失敗！§7(今日已用 §e" + cc.todayCount + "§7/§e" + (clc.dailyLimit > 0 ? clc.dailyLimit : "∞") + "§7)");
            CardInventory.open(player);
            return;
        }

        // 成功
        ItemStack original = ExpStoneSelectInventory.getOriginalCard(player.getUniqueId(), slot);
        if (original == null) original = clicked;
        CardLevel.Result r = CardLevel.addExp(original, cd.getId(), stone.exp);
        if (!replaceCardInBag(player, original, r.item, cd.getId())) {
            player.getInventory().addItem(r.item);
        }
        player.sendMessage("§a使用經驗石成功！獲得 §e" + stone.exp + " §a經驗");
        if (r.levelsGained > 0) {
            player.sendMessage("§6✦ 卡片升級！§e" + cd.getId() + " §7→ §eLv." + CardLevel.getLevel(r.item));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            try {
                org.bukkit.Location loc = player.getLocation().add(0, 1, 0);
                player.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, loc, 30, 0.8, 0.8, 0.8, 0.05);
                player.getWorld().spawnParticle(org.bukkit.Particle.FLAME, loc, 20, 0.5, 0.5, 0.5, 0.02);
            } catch (Throwable ignored) {}
        }
        CardInventory.open(player);
    }

    private void useCooldownSkip(Player player, ItemStack scroll) {
        int cleared = 0;
        int unlocked = PlayerData.getUnlockedPages(player.getUniqueId());
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int page = 0; page < unlocked; page++) {
            ItemStack[] items = PlayerData.getPageInventory(player.getUniqueId(), page);
            for (ItemStack it : items) {
                if (it == null) continue;
                CardData cd = CardDataManager.findCard(it);
                if (cd == null) continue;
                if (!seen.add(cd.getId())) continue;
                PlayerData.CardCooldown cc = PlayerData.getCooldown(player.getUniqueId(), cd.getId());
                if (cc.lastUpgrade <= 0) continue;
                cc.lastUpgrade = 0;
                cleared++;
            }
        }
        PlayerData.savePlayer(player.getUniqueId());

        // 扣卷轴
        scroll.setAmount(scroll.getAmount() - 1);
        if (scroll.getAmount() <= 0) player.getInventory().setItemInMainHand(null);

        if (cleared == 0) {
            player.sendMessage("§7[§b跳冷卻卷軸§7] §e沒有需要冷卻清除的卡片");
        } else {
            player.sendMessage("§7[§b跳冷卻卷軸§7] §a已跳過 §e" + cleared + " §a張卡的冷卻");
        }
    }
    /** 找背包里指定 runeId 的符文所在槽位，找不到 -1 */
    private int findRuneSlot(Player player, String runeId) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack it = player.getInventory().getItem(i);
            if (it == null) continue;
            if (runeId.equals(com.longdrange.ldattribute.rune.RuneItem.getRuneId(it))) return i;
        }
        return -1;
    }

    /** 按 runeId 精确扣 count 个符文（NBT 匹配，不依赖 Material） */
    private boolean takeRune(Player player, String runeId, int count) {
        int remaining = count;
        for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
            ItemStack it = player.getInventory().getItem(i);
            if (it == null) continue;
            if (!runeId.equals(com.longdrange.ldattribute.rune.RuneItem.getRuneId(it))) continue;
            int take = Math.min(it.getAmount(), remaining);
            it.setAmount(it.getAmount() - take);
            remaining -= take;
            if (it.getAmount() <= 0) player.getInventory().setItem(i, null);
        }
        return remaining == 0;
    }

    private boolean hasEnoughItem(Player player, String spec) {
        String[] p = spec.split(":");
        Material mat = Material.getMaterial(p[0].toUpperCase());
        if (mat == null) return false;
        int need = p.length >= 2 ? Integer.parseInt(p[1]) : 1;
        int have = 0;
        for (ItemStack it : player.getInventory().getContents()) {
            if (it != null && it.getType() == mat) have += it.getAmount();
        }
        return have >= need;
    }

    private void takeItem(Player player, String spec) {
        String[] p = spec.split(":");
        Material mat = Material.getMaterial(p[0].toUpperCase());
        if (mat == null) return;
        int need = p.length >= 2 ? Integer.parseInt(p[1]) : 1;
        int removed = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && removed < need; i++) {
            ItemStack it = contents[i];
            if (it == null || it.getType() != mat) continue;
            int take = Math.min(it.getAmount(), need - removed);
            it.setAmount(it.getAmount() - take);
            removed += take;
            if (it.getAmount() <= 0) player.getInventory().setItem(i, null);
        }
    }
    private String formatTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append("時");
        if (m > 0) sb.append(m).append("分");
        if (s > 0 || sb.length() == 0) sb.append(s).append("秒");
        return sb.toString();
    }
    private int countExpStone(Player player, ExpStoneData.ExpStone stone) {
        int c = 0;
        for (ItemStack it : player.getInventory().getContents()) {
            if (it == null) continue;
            ExpStoneData.ExpStone s = ExpStoneData.parse(it);
            if (s == null) continue;
            if (!s.target.equals(stone.target)) continue;
            if (s.exp != stone.exp) continue;
            c += it.getAmount();
        }
        return c;
    }

    private void takeExpStone(Player player, ExpStoneData.ExpStone stone, int n) {
        int removed = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && removed < n; i++) {
            ItemStack it = contents[i];
            if (it == null) continue;
            ExpStoneData.ExpStone s = ExpStoneData.parse(it);
            if (s == null) continue;
            if (!s.target.equals(stone.target)) continue;
            if (s.exp != stone.exp) continue;
            int take = Math.min(it.getAmount(), n - removed);
            it.setAmount(it.getAmount() - take);
            removed += take;
            if (it.getAmount() <= 0) player.getInventory().setItem(i, null);
        }
    }

    /** 用 newCard 替換玩家卡片背包裡第一張 matches 的卡 */
    private boolean replaceCardInBag(Player player, ItemStack oldCard, ItemStack newCard, String cardId) {
        int unlocked = PlayerData.getUnlockedPages(player.getUniqueId());
        for (int page = 0; page < unlocked; page++) {
            ItemStack[] items = PlayerData.getPageInventory(player.getUniqueId(), page);
            for (int i = 0; i < items.length; i++) {
                ItemStack it = items[i];
                if (it == null) continue;
                CardData icd = CardDataManager.findCard(it);
                if (icd == null || !icd.getId().equals(cardId)) continue;
                items[i] = newCard;
                PlayerData.setPageInventory(player.getUniqueId(), page, items);
                PlayerData.savePlayer(player.getUniqueId());
                return true;
            }
        }
        return false;
    }

    private void handleCollection(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        event.setCancelled(true);
        if (slot == CollectionInventory.SLOT_BACK) { CardInventory.open(player); return; }
        int cur = CollectionInventory.getLastPage(player.getUniqueId());
        if (slot == CollectionInventory.SLOT_PREV) {
            if (cur > 0) CollectionInventory.open(player, cur - 1);
            return;
        }
        if (slot == CollectionInventory.SLOT_NEXT) {
            if (cur < CollectionInventory.getMaxPage()) CollectionInventory.open(player, cur + 1);
            return;
        }
        // 點擊圖鑑卡片無操作
    }

    private void handleMerge(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        event.setCancelled(true);
        if (slot == MergeInventory.SLOT_BACK) { CardInventory.open(player); return; }
        if (slot < 0 || slot >= 45) return;
        com.longdrange.ldattribute.card.RecipeConfig.Recipe r = MergeInventory.getRecipeAt(slot);
        if (r == null) return;
        String result = com.longdrange.ldattribute.card.RecipeEngine.execute(player, r);
        player.sendMessage("§8[§b合成§8] " + result);
        // 刷新 GUI
        MergeInventory.open(player);
    }

    private void handleRankInventory(Player player, InventoryClickEvent event, String title) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        String type = com.longdrange.ldattribute.card.inventory.RankInventory.getType(title);
        if (slot == com.longdrange.ldattribute.card.inventory.RankInventory.SLOT_BACK) {
            CardInventory.open(player);
            return;
        }
        if (slot == com.longdrange.ldattribute.card.inventory.RankInventory.SLOT_TAB_CARD) {
            com.longdrange.ldattribute.card.inventory.RankInventory.open(player, "CARD", 1);
            return;
        }
        if (slot == com.longdrange.ldattribute.card.inventory.RankInventory.SLOT_TAB_POINTS) {
            com.longdrange.ldattribute.card.inventory.RankInventory.open(player, "POINTS", 1);
            return;
        }
        if (slot == com.longdrange.ldattribute.card.inventory.RankInventory.SLOT_PREV) {
            com.longdrange.ldattribute.card.inventory.RankInventory.open(player, type, 1);
            return;
        }
        if (slot == com.longdrange.ldattribute.card.inventory.RankInventory.SLOT_NEXT) {
            com.longdrange.ldattribute.card.inventory.RankInventory.open(player, type, 2);
            return;
        }
    }
    private void handleStats(Player player, InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == com.longdrange.ldattribute.card.inventory.StatsInventory.SLOT_BACK) {
            CardInventory.open(player);
        }
        if (slot == com.longdrange.ldattribute.card.inventory.StatsInventory.SLOT_PREV) {
            int cur = com.longdrange.ldattribute.card.inventory.StatsInventory.getLastPage(player.getUniqueId());
            com.longdrange.ldattribute.card.inventory.StatsInventory.open(player, cur - 1);
            return;
        }
        if (slot == com.longdrange.ldattribute.card.inventory.StatsInventory.SLOT_NEXT) {
            int cur = com.longdrange.ldattribute.card.inventory.StatsInventory.getLastPage(player.getUniqueId());
            com.longdrange.ldattribute.card.inventory.StatsInventory.open(player, cur + 1);
            return;
        }
    }

    private void handleRank(Player player, InventoryClickEvent event, String title) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        String type = com.longdrange.ldattribute.card.inventory.RankInventory.getType(title);
        if (slot == com.longdrange.ldattribute.card.inventory.RankInventory.SLOT_BACK) {
            CardInventory.open(player);
            return;
        }
        if (slot == com.longdrange.ldattribute.card.inventory.RankInventory.SLOT_TAB_CARD) {
            com.longdrange.ldattribute.card.inventory.RankInventory.open(player, "CARD", 1);
            return;
        }
        if (slot == com.longdrange.ldattribute.card.inventory.RankInventory.SLOT_TAB_POINTS) {
            com.longdrange.ldattribute.card.inventory.RankInventory.open(player, "POINTS", 1);
            return;
        }
    }

    private void handleSuitInventory(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        event.setCancelled(true);
        if (slot == SuitInventory.SLOT_BACK) CardInventory.open(player);
    }
}
