package com.longdrange.ldattribute.achievement;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class AchievementDetailInventory {
    public static final String TITLE_PREFIX = "§8[§6成就详情§8] ";
    public static final int SLOT_BACK = 49;

    public static void open(Player player, String achId) {
        AchievementConfig.Achievement a = AchievementConfig.get(achId);
        if (a == null) { player.sendMessage("§c未找到成就: " + achId); return; }

        UUID uuid = player.getUniqueId();
        int progress = AchievementData.getProgress(uuid, achId);
        boolean completed = AchievementData.isCompleted(uuid, achId);
        boolean claimed = AchievementData.isClaimed(uuid, achId);

        String title = TITLE_PREFIX + a.name;
        if (title.replaceAll("§.", "").length() > 32) title = TITLE_PREFIX + a.id;
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // 第1排：标题书
        ItemStack titleBook = new ItemStack(Material.BOOK);
        ItemMeta tm = titleBook.getItemMeta();
        if (tm != null) {
            tm.setDisplayName("§b【理想龙猫】§c伺服器");
            List<String> tl = new ArrayList<>();
            tl.add("§7名称: §f" + a.name);
            tl.add("§7类型: §f" + a.type);
            tl.add("§7分类: §f" + (a.category == null ? "MISC" : a.category));
            tl.add("§7进度: §e" + progress + "§7/§e" + a.target);
            tl.add("§7状态: " + (claimed ? "§8已发放" : (completed ? "§a可发放" : "§c未达成")));
            tm.setLore(tl);
            titleBook.setItemMeta(tm);
        }
        for (int i = 0; i < 9; i++) inv.setItem(i, titleBook.clone());

        // 第2排：奖励预览
        List<ItemStack> rewards = new ArrayList<>();
        if (a.rewardPoints > 0) {
            ItemStack p = new ItemStack(Material.GOLD_INGOT);
            ItemMeta pm = p.getItemMeta();
            if (pm != null) {
                pm.setDisplayName("§6点券奖励");
                pm.setLore(Arrays.asList("§7数量: §e" + a.rewardPoints));
                p.setItemMeta(pm);
            }
            rewards.add(p);
        }
        if (a.rewardCards != null) {
            for (String spec : a.rewardCards) {
                String cid = spec.contains(":") ? spec.substring(0, spec.indexOf(':')).trim() : spec;
                int cnt = 1;
                try { cnt = Integer.parseInt(spec.substring(spec.indexOf(':') + 1).trim()); } catch (Exception ignored) {}
                com.longdrange.ldattribute.card.CardData cd =
                        com.longdrange.ldattribute.card.CardDataManager.getCard(cid);
                ItemStack card = cd != null ? cd.getItem() : new ItemStack(Material.PAPER);
                if (card != null && cnt > 1) card.setAmount(Math.min(64, cnt));
                ItemMeta cm = card.getItemMeta();
                if (cm != null) {
                    List<String> lore = cm.getLore() == null ? new ArrayList<>() : new ArrayList<>(cm.getLore());
                    lore.add("");
                    lore.add("§6卡片奖励");
                    cm.setLore(lore);
                    card.setItemMeta(cm);
                }
                rewards.add(card);
            }
        }
        if (a.rewardRunes != null) {
            for (String spec : a.rewardRunes) {
                String rid = spec.contains(":") ? spec.substring(0, spec.indexOf(':')).trim() : spec;
                int cnt = 1;
                try { cnt = Integer.parseInt(spec.substring(spec.indexOf(':') + 1).trim()); } catch (Exception ignored) {}
                com.longdrange.ldattribute.rune.RuneConfig.Rune r =
                        com.longdrange.ldattribute.rune.RuneConfig.getRune(rid);
                ItemStack rune = r != null ? com.longdrange.ldattribute.rune.RuneItem.create(r, 1)
                        : new ItemStack(Material.REDSTONE);
                if (rune != null && cnt > 1) rune.setAmount(Math.min(64, cnt));
                ItemMeta rm = rune.getItemMeta();
                if (rm != null) {
                    List<String> lore = rm.getLore() == null ? new ArrayList<>() : new ArrayList<>(rm.getLore());
                    lore.add("");
                    lore.add("§6符文奖励");
                    rm.setLore(lore);
                    rune.setItemMeta(rm);
                }
                rewards.add(rune);
            }
        }
        if (a.rewardCommands != null) {
            for (String cmd : a.rewardCommands) {
                ItemStack cmdBlock = new ItemStack(Material.COMMAND);
                ItemMeta cm = cmdBlock.getItemMeta();
                if (cm != null) {
                    cm.setDisplayName("§6命令奖励");
                    cm.setLore(Arrays.asList("§7命令: §f" + cmd));
                    cmdBlock.setItemMeta(cm);
                }
                rewards.add(cmdBlock);
            }
        }
        for (int i = 0; i < 9 && i < rewards.size(); i++) {
            inv.setItem(9 + i, rewards.get(i));
        }

        // 第3排：状态玻璃
        for (int i = 0; i < 9; i++) {
            ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) (completed ? 5 : 14));
            ItemMeta gm = glass.getItemMeta();
            if (gm != null) {
                gm.setDisplayName(completed ? "§a✓ 已达成" : "§c✗ 未达成");
                gm.setLore(Arrays.asList("§7进度: §e" + progress + "§7/§e" + a.target));
                glass.setItemMeta(gm);
            }
            inv.setItem(18 + i, glass);
        }

        // 第4排：进度书
        ItemStack progressBook = new ItemStack(Material.BOOK);
        ItemMeta pm2 = progressBook.getItemMeta();
        if (pm2 != null) {
            pm2.setDisplayName("§e进度详情");
            int barLen = 20;
            int filled = a.target <= 0 ? 0 : Math.min(barLen, progress * barLen / a.target);
            StringBuilder bar = new StringBuilder("§a");
            for (int i = 0; i < filled; i++) bar.append("■");
            bar.append("§7");
            for (int i = filled; i < barLen; i++) bar.append("■");
            pm2.setLore(Arrays.asList(
                    "§7类型: §f" + a.type,
                    "§7目标: §e" + a.target,
                    "§7当前: §e" + progress,
                    bar.toString()
            ));
            progressBook.setItemMeta(pm2);
        }
        inv.setItem(27, progressBook);
        inv.setItem(28, progressBook.clone());

        // 第5排：汇总书，lore 列出所有奖励
        ItemStack summaryBook = new ItemStack(Material.BOOK);
        ItemMeta sm = summaryBook.getItemMeta();
        if (sm != null) {
            sm.setDisplayName("§b【理想龙猫】§c伺服器");
            List<String> sl = new ArrayList<>();
            sl.add("§7成就: §f" + a.name);
            sl.add("§7类型: §f" + a.type);
            sl.add("§7进度: §e" + progress + "§7/§e" + a.target);
            sl.add("");
            sl.add("§e✦ 完整奖励列表:");
            if (a.rewardPoints > 0) sl.add("  §7- 点券: §6" + a.rewardPoints);
            if (a.rewardCards != null) for (String c : a.rewardCards) sl.add("  §7- 卡片: §e" + c);
            if (a.rewardRunes != null) for (String r : a.rewardRunes) sl.add("  §7- 符文: §e" + r);
            if (a.rewardCommands != null) for (String cmd : a.rewardCommands) sl.add("  §7- 命令: §f" + cmd);
            if ((a.rewardPoints <= 0) && (a.rewardCards == null || a.rewardCards.isEmpty())
                    && (a.rewardRunes == null || a.rewardRunes.isEmpty())
                    && (a.rewardCommands == null || a.rewardCommands.isEmpty())) {
                sl.add("  §7(无奖励)");
            }
            sl.add("");
            sl.add("§7说明: §f" + (a.description == null ? "" : a.description));
            sl.add("§7状态: " + (claimed ? "§8已自动发放" : (completed ? "§a发放中..." : "§c未达成")));
            sm.setLore(sl);
            summaryBook.setItemMeta(sm);
        }
        for (int i = 0; i < 9; i++) inv.setItem(36 + i, summaryBook.clone());

        // 第6排：返回
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta bkm = back.getItemMeta();
        if (bkm != null) bkm.setDisplayName("§c§l← 返回");
        back.setItemMeta(bkm);
        inv.setItem(SLOT_BACK, back);

        player.openInventory(inv);
    }

    public static boolean isDetail(String title) { return title.startsWith(TITLE_PREFIX); }
}