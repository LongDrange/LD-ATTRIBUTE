package com.longdrange.ldattribute.command;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.card.CardData;
import com.longdrange.ldattribute.card.CardDataManager;
import com.longdrange.ldattribute.card.PageConfig;
import com.longdrange.ldattribute.card.CardNBT;
import com.longdrange.ldattribute.card.CardLevel;
import com.longdrange.ldattribute.card.CardLevelConfig;
import com.longdrange.ldattribute.card.PlayerData;
import com.longdrange.ldattribute.card.RecipeConfig;
import com.longdrange.ldattribute.card.StatsDataRead;
import com.longdrange.ldattribute.card.StarConfig;
import com.longdrange.ldattribute.data.attribute.LDAttributeData;
import com.longdrange.ldattribute.data.attribute.LDSubAttribute;
import com.longdrange.ldattribute.card.RecipeEngine;
import com.longdrange.ldattribute.card.inventory.CardInventory;
import com.longdrange.ldattribute.card.inventory.CollectionInventory;
import com.longdrange.ldattribute.card.inventory.MergeInventory;
import com.longdrange.ldattribute.util.Message;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CardCommand implements CommandExecutor, TabCompleter {

    private final LDAttribute plugin;

    public CardCommand(LDAttribute plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender, label); return true; }
        String sub = args[0];

        if (sub.equalsIgnoreCase("help")) { sendHelp(sender, label); return true; }

        String key = CommandConfig.matchSubKey(sub);
        if (key == null) { sendHelp(sender, label); return true; }

        CommandConfig.SubCmd cfg = CommandConfig.getSub(key);
        if (cfg == null) { sendHelp(sender, label); return true; }

        if (cfg.permission != null && !cfg.permission.isEmpty()
                && !sender.hasPermission(cfg.permission)) {
            sender.sendMessage(Message.get("Command.Help.NoPerm"));
            return true;
        }

        switch (key) {
            case "Open":
                if (sender instanceof Player) CardInventory.open((Player) sender);
                else sender.sendMessage(Message.get("Command.Help.PlayerOnly"));
                return true;
            case "Collection": return onCollection(sender, args);
            case "Top": return onTop(sender, args);
            case "Stats": return onStats(sender, args);
            case "Merge": return onMerge(sender, args);
            case "List": sendList(sender); return true;
            case "Give": return onGive(sender, args);
            case "Save": return onSave(sender, args);
            case "Reload":
                plugin.reloadAll();
                sender.sendMessage(Message.get("Command.Reload.Success"));
                return true;
            case "Version":
                sender.sendMessage(msg("&bLD-CardStats &e" + plugin.getDescription().getVersion()));
                return true;
            case "Unlock": return onUnlock(sender, args);
            case "Reset": return onReset(sender, args);
            case "Remove": return onRemove(sender, args);
            case "Star": return onSetStat(sender, args, "star");
            case "Level": return onSetStat(sender, args, "level");
            case "Exp": return onSetStat(sender, args, "exp");
            case "Info": return onInfo(sender, args);
            case "Msg": return onMsg(sender, args);
            case "Log": return onLog(sender, args);
            case "Mana": return onMana(sender, args);
            case "Lang": return onLang(sender, args);
            case "Pet": return onPet(sender, args);
            case "Backup": return onBackup(sender, args);
            case "Debug": return onDebug(sender, args);
            case "Rune": return onRune(sender, args);
            case "Mm": return onMm(sender, args);
            case "Synergy": return onSynergy(sender, args);
            case "Cast": return onCast(sender, args);
            case "GiveBook": return onGiveBook(sender, args);
        }
        sendHelp(sender, label);
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(Message.get("Command.Help.Title"));
        for (Map.Entry<String, CommandConfig.SubCmd> e : CommandConfig.getAllSubs().entrySet()) {
            CommandConfig.SubCmd c = e.getValue();
            if (c.permission != null && !c.permission.isEmpty()
                    && !sender.hasPermission(c.permission)) continue;
            sender.sendMessage(msg("&e/" + label + " " + c.name + " &7- " + c.description));
        }
        sender.sendMessage(Message.get("Command.Help.Bottom"));
    }

    private void sendList(CommandSender sender) {
        sender.sendMessage(Message.get("Command.List.Title"));
        for (CardData card : CardDataManager.getAllCardOnly())
            sender.sendMessage(msg("&e" + card.getId()));
        sender.sendMessage(Message.get("Command.Help.Bottom"));
    }

    private boolean onGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Message.get("Command.Give.Usage"));
            return true;
        }
        CardData card = CardDataManager.getCard(args[1]);
        if (card == null) {
            sender.sendMessage(Message.get("Command.Give.NoCard", args[1]));
            return true;
        }
        Player target;
        int amount = 1;
        if (args.length >= 3) {
            target = plugin.getServer().getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(Message.get("Command.Give.NoPlayer", args[2]));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(Message.get("Command.Give.NoTarget"));
            return true;
        }
        if (args.length >= 4) {
            try { amount = Integer.parseInt(args[3]); } catch (NumberFormatException ignored) {}
        }
        for (int i = 0; i < amount; i++) target.getInventory().addItem(card.getItem());
        sender.sendMessage(Message.get("Command.Give.Success", target.getName(), args[1], amount));
        return true;
    }

    private boolean onTop(CommandSender sender, String[] args) {
        int page = 1;
        if (args.length >= 2) {
            try { page = Integer.parseInt(args[1]); } catch (Exception ignored) {}
        }
        if (page < 1) page = 1;
        int perPage = 10;

        java.util.List<String> uuids = PlayerData.getAllPlayerUUIDs();
        java.util.Map<String, Integer> totals = new java.util.HashMap<>();
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();

        for (String uuid : uuids) {
            int unlocked = PlayerData.getUnlockedPagesFromFile(uuid);
            int total = 0;
            int count = 0;
            for (int p = 0; p < unlocked; p++) {
                org.bukkit.inventory.ItemStack[] items = PlayerData.getPageInventoryFromFile(uuid, p);
                for (org.bukkit.inventory.ItemStack it : items) {
                    if (it == null) continue;
                    com.longdrange.ldattribute.card.CardData cd = CardDataManager.findCard(it);
                    if (cd == null) continue;
                    if (!CardLevelConfig.isUpgradable(cd.getId())) continue;
                    total += com.longdrange.ldattribute.card.CardNBT.getLevel(it);
                    count++;
                }
            }
            totals.put(uuid, total);
            counts.put(uuid, count);
        }

        java.util.List<java.util.Map.Entry<String, Integer>> sorted = new java.util.ArrayList<>(totals.entrySet());
        sorted.removeIf(e -> e.getValue() == 0);
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        int total_players = sorted.size();
        int maxPage = Math.max(1, (total_players + perPage - 1) / perPage);
        if (page > maxPage) page = maxPage;
        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, total_players);

        sender.sendMessage(Message.get("Command.Top.Title"));
        if (sorted.isEmpty()) {
            sender.sendMessage(Message.get("Command.Top.Empty"));
        } else {
            for (int i = start; i < end; i++) {
                java.util.Map.Entry<String, Integer> e = sorted.get(i);
                String name = getPlayerName(e.getKey());
                int cnt = counts.getOrDefault(e.getKey(), 0);
                sender.sendMessage(Message.get("Command.Top.Line", i + 1, name, e.getValue(), cnt));
            }
        }
        sender.sendMessage(Message.get("Command.Top.Footer", page, maxPage));
        return true;
    }

    private String getPlayerName(String uuidStr) {
        try {
            org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuidStr));
            if (op != null && op.getName() != null) return op.getName();
        } catch (Throwable ignored) {}
        return uuidStr.substring(0, 8) + "...";
    }

    private boolean onStats(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Message.get("Command.Help.PlayerOnly"));
            return true;
        }
        Player player = (Player) sender;

        if (args.length >= 2) {
            CardData card = CardDataManager.getCard(args[1]);
            if (card == null) {
                player.sendMessage(Message.get("Command.Stats.NoCard", args[1]));
                return true;
            }
            List<String> lore = StatsDataRead.filterNormalLore(card.getItem());
            LDAttributeData data = plugin.getApi().getLoreData(player, null, lore);
            player.sendMessage(Message.get("Command.Stats.TitleCard", card.getId()));
            printAttributes(player, data);
            player.sendMessage(Message.get("Command.Stats.Bottom"));
            return true;
        }

        // 无参数：打开属性面板 GUI
        com.longdrange.ldattribute.card.inventory.StatsInventory.open(player);
        return true;
    }

    private void printAttributes(Player player, LDAttributeData data) {
        if (data == null) { player.sendMessage(Message.get("Command.Stats.NoAttr")); return; }
        java.util.Map<Integer, LDSubAttribute> map = data.getAttributeMap();
        boolean any = false;
        for (java.util.Map.Entry<Integer, LDSubAttribute> e : map.entrySet()) {
            LDSubAttribute attr = e.getValue();
            double v = attr.getValue();
            if (v == 0) continue;
            any = true;
            String num = LDSubAttribute.getDf().format(v);
            player.sendMessage(Message.get("Command.Stats.AttrLine", attr.getName(), num));
        }
        if (!any) player.sendMessage(Message.get("Command.Stats.NoBonus"));
    }

    private boolean onCollection(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Message.get("Command.Help.PlayerOnly"));
            return true;
        }
        Player player = (Player) sender;
        if (args.length < 2) {
            CollectionInventory.open(player);
            return true;
        }
        String arg = args[1];
        if (arg.matches("\\d+")) {
            int page = Integer.parseInt(arg) - 1;
            if (page < 0) page = 0;
            if (page > CollectionInventory.getMaxPage()) page = CollectionInventory.getMaxPage();
            CollectionInventory.open(player, page);
            return true;
        }
        int p = CollectionInventory.findPageByKeyword(arg);
        if (p < 0) {
            player.sendMessage(Message.get("Command.Collection.NoCard", arg));
            return true;
        }
        player.sendMessage(Message.get("Command.Collection.Jumped", arg));
        CollectionInventory.open(player, p);
        return true;
    }

    private boolean onMerge(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Message.get("Command.Help.PlayerOnly"));
            return true;
        }
        Player player = (Player) sender;
        if (args.length < 2) {
            MergeInventory.open(player);
            return true;
        }
        RecipeConfig.Recipe r = RecipeConfig.get(args[1]);
        if (r == null) { player.sendMessage(Message.get("Command.Merge.NoRecipe", args[1])); return true; }
        player.sendMessage(msg(RecipeEngine.execute(player, r)));
        return true;
    }

    private boolean onSave(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Message.get("Command.Help.PlayerOnly"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(msg("&c用法：/ldc save <ID>"));
            return true;
        }
        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().toString().contains("AIR")) {
            sender.sendMessage(Message.get("Command.Save.NoItem"));
            return true;
        }
        sender.sendMessage(Message.get("Command.Save.Success", args[1]));
        sender.sendMessage(Message.get("Command.Save.NameLine", item.getItemMeta().getDisplayName()));
        sender.sendMessage(Message.get("Command.Save.IdLine", item.getTypeId()));
        if (item.getItemMeta().hasLore())
            for (String line : item.getItemMeta().getLore())
                sender.sendMessage("  " + line);
        return true;
    }

    // ==================== 管理員指令 ====================

    private boolean onUnlock(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Message.get("Command.Unlock.Usage"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Message.get("Command.Unlock.NoPlayer", args[1]));
            return true;
        }
        int page;
        try { page = Integer.parseInt(args[2]); } catch (Exception e) {
            sender.sendMessage(Message.get("Command.Unlock.NoNumber"));
            return true;
        }
        if (page < 1) page = 1;
        int total = PageConfig.getPageCount();
        if (page > total) {
            sender.sendMessage(Message.get("Command.Unlock.OutOfRange", total));
            return true;
        }
        int cur = PlayerData.getUnlockedPages(target.getUniqueId());
        if (page <= cur) {
            sender.sendMessage(Message.get("Command.Unlock.Already", target.getName(), cur));
            return true;
        }
        PlayerData.setUnlockedPages(target.getUniqueId(), page);
        PlayerData.savePlayer(target.getUniqueId());
        sender.sendMessage(Message.get("Command.Unlock.Success", target.getName(), page));
        target.sendMessage(Message.get("Command.Unlock.Notify", page));
        return true;
    }

    private boolean onReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Message.get("Command.Reset.Usage"));
            return true;
        }
        String name = args[1];
        UUID uuid;
        Player target = Bukkit.getPlayerExact(name);
        if (target != null) {
            uuid = target.getUniqueId();
        } else {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(name);
            if (op == null || op.getUniqueId() == null) {
                sender.sendMessage(Message.get("Command.Reset.NoPlayer", name));
                return true;
            }
            uuid = op.getUniqueId();
        }
        int pages = PlayerData.getUnlockedPages(uuid);
        for (int p = 0; p < pages; p++) {
            PlayerData.setPageInventory(uuid, p, new ItemStack[45]);
        }
        PlayerData.setUnlockedPages(uuid, 1);
        PlayerData.savePlayer(uuid);
        if (target != null) StatsDataRead.updatePlayer(target);
        sender.sendMessage(Message.get("Command.Reset.Success", name));
        return true;
    }

    private boolean onRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Message.get("Command.Remove.Usage"));
            return true;
        }
        String cardId = args[1];
        CardData card = CardDataManager.getCard(cardId);
        if (card == null) {
            sender.sendMessage(Message.get("Command.Remove.NoCard", cardId));
            return true;
        }
        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(Message.get("Command.Give.NoPlayer", args[2]));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(Message.get("Command.Give.NoTarget"));
            return true;
        }
        int before = PlayerData.countCard(target.getUniqueId(), cardId);
        if (before == 0) {
            sender.sendMessage(Message.get("Command.Remove.NotFound", target.getName(), cardId));
            return true;
        }
        PlayerData.removeCard(target.getUniqueId(), cardId, -1);
        StatsDataRead.updatePlayer(target);
        sender.sendMessage(Message.get("Command.Remove.Success", target.getName(), cardId, before));
        return true;
    }

    private boolean onSetStat(CommandSender sender, String[] args, String type) {
        if (args.length < 4) {
            sender.sendMessage(Message.get("Command.SetStat.Usage", type));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Message.get("Command.Unlock.NoPlayer", args[1]));
            return true;
        }
        String cardId = args[2];
        CardData card = CardDataManager.getCard(cardId);
        if (card == null) {
            sender.sendMessage(Message.get("Command.Remove.NoCard", cardId));
            return true;
        }
        int value;
        try { value = Integer.parseInt(args[3]); } catch (Exception e) {
            sender.sendMessage(Message.get("Command.SetStat.NoNumber"));
            return true;
        }
        if (value < 0) value = 0;

        UUID uuid = target.getUniqueId();
        int pages = PlayerData.getUnlockedPages(uuid);
        boolean found = false;
        int totalLevelsGained = 0;

        for (int p = 0; p < pages; p++) {
            ItemStack[] items = PlayerData.getPageInventory(uuid, p);
            for (int i = 0; i < items.length; i++) {
                ItemStack it = items[i];
                if (it == null) continue;
                CardData cd = CardDataManager.findCard(it);
                if (cd == null || !cd.getId().equals(cardId)) continue;

                if (type.equals("exp")) {
                    // 累加經驗，自動升級
                    CardLevel.Result res = CardLevel.addExp(it, cardId, value);
                    it = res.item;
                    totalLevelsGained += res.levelsGained;
                } else if (type.equals("star")) {
                    it = CardNBT.setStar(it, value);
                    it = CardLevel.recalc(it, cardId, CardNBT.getLevel(it), CardNBT.getExp(it));
                } else if (type.equals("level")) {
                    it = CardNBT.setLevel(it, value);
                    it = CardLevel.recalc(it, cardId, CardNBT.getLevel(it), CardNBT.getExp(it));
                }
                items[i] = it;
                found = true;
            }
            PlayerData.setPageInventory(uuid, p, items);
        }
        if (!found) {
            sender.sendMessage(Message.get("Command.SetStat.NotFound", cardId));
            return true;
        }
        PlayerData.savePlayer(uuid);
        StatsDataRead.updatePlayer(target);

        if (type.equals("exp")) {
            if (totalLevelsGained > 0) {
                sender.sendMessage(Message.get("Command.SetStat.ExpLevelUp", target.getName(), cardId, value, totalLevelsGained));
                target.sendMessage(Message.get("Command.SetStat.ExpNotify", cardId, totalLevelsGained));
            } else {
                sender.sendMessage(Message.get("Command.SetStat.ExpSuccess", target.getName(), cardId, value));
            }
        } else {
            sender.sendMessage(Message.get("Command.SetStat.Success", target.getName(), cardId, type, value));
        }
        return true;
    }

    private boolean onInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Message.get("Command.Info.Usage"));
            return true;
        }
        CardData card = CardDataManager.getCard(args[1]);
        if (card == null) {
            sender.sendMessage(Message.get("Command.Give.NoCard", args[1]));
            return true;
        }
        sender.sendMessage(Message.get("Command.Info.Title", card.getId()));
        sender.sendMessage(Message.get("Command.Info.Id", card.getId()));
        try { sender.sendMessage(Message.get("Command.Info.Name", card.getItem().getItemMeta().getDisplayName())); } catch (Throwable ignored) {}
        CardLevelConfig.CardLevel cl = CardLevelConfig.get(card.getId());
        if (cl != null) {
            sender.sendMessage(Message.get("Command.Info.MaxLevel", cl.maxLevel));
            sender.sendMessage(Message.get("Command.Info.MaxStar", (cl.maxStar > 0 ? cl.maxStar : StarConfig.getMaxStar())));
            sender.sendMessage(Message.get("Command.Info.Attrs"));
            for (CardLevelConfig.AttrConf a : cl.attrs.values()) {
                sender.sendMessage(Message.get("Command.Info.AttrLine", a.name, a.base, a.growth, a.max, a.mode));
            }
        } else {
            sender.sendMessage(Message.get("Command.Info.NoUpgrade"));
        }
        sender.sendMessage(msg("&8&m----------------------------"));
        return true;
    }

    private boolean onMana(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(msg("&c用法：/ldc mana <玩家> <set|add|take> <值>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(msg("&c玩家不在線上：&e" + args[1]));
            return true;
        }
        String op = args[2].toLowerCase();
        int value = 0;
        if (args.length >= 4) {
            try { value = Integer.parseInt(args[3]); } catch (Exception e) {
                sender.sendMessage(msg("&c值必須是數字！"));
                return true;
            }
        }
        com.longdrange.ldattribute.card.ManaManager mgr = null;
        switch (op) {
            case "set":
                com.longdrange.ldattribute.card.ManaManager.set(target, value);
                sender.sendMessage(msg("&a已設定 &e" + target.getName() + " &a的法力 = &e" + value));
                break;
            case "add":
                int newVal = com.longdrange.ldattribute.card.ManaManager.add(target, value);
                sender.sendMessage(msg("&a已為 &e" + target.getName() + " &a增加 &e" + value + " &a法力（目前 &e" + newVal + "&a）"));
                break;
            case "take":
                if (!com.longdrange.ldattribute.card.ManaManager.take(target, value)) {
                    sender.sendMessage(msg("&c玩家法力不足！"));
                    return true;
                }
                sender.sendMessage(msg("&a已扣 &e" + target.getName() + " &a的 &e" + value + " &a法力"));
                break;
            default:
                sender.sendMessage(msg("&c未知操作：&e" + op + " &7(可用：set/add/take)"));
        }
        return true;
    }
    private boolean onCast(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg("&c此指令只能由玩家執行！"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(msg("&c用法：/ldc cast <法術ID>"));
            sender.sendMessage(msg("&7可用法術：&e" + String.join(", ", com.longdrange.ldattribute.spell.SpellConfig.getIds())));
            return true;
        }
        Player p = (Player) sender;
        com.longdrange.ldattribute.spell.SpellConfig.Spell sp =
                com.longdrange.ldattribute.spell.SpellConfig.get(args[1]);
        if (sp == null) {
            sender.sendMessage(msg("&c找不到法術：&e" + args[1]));
            return true;
        }
        String err = com.longdrange.ldattribute.spell.SpellManager.cast(p, sp);
        if (err == null) {
            p.sendMessage(msg("&a✦ 施放 &r" + sp.name));
        } else {
            p.sendMessage(msg(err));
        }
        return true;
    }
    private boolean onGiveBook(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(msg("&c用法：/ldc givebook <法術ID> [玩家]"));
            sender.sendMessage(msg("&7可用：&e" + String.join(", ", com.longdrange.ldattribute.spell.SpellConfig.getIds())));
            return true;
        }
        String spellId = args[1];
        if (com.longdrange.ldattribute.spell.SpellConfig.get(spellId) == null) {
            sender.sendMessage(msg("&c找不到法術：&e" + spellId));
            return true;
        }
        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) { sender.sendMessage(msg("&c玩家不在線上")); return true; }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(msg("&c請指定玩家"));
            return true;
        }
        int lv = 1;
        if (args.length >= 4) {
            try { lv = Integer.parseInt(args[3]); } catch (Exception ignored) {}
        }
        org.bukkit.inventory.ItemStack book = com.longdrange.ldattribute.spell.SpellBookItem.create(spellId, lv);
        if (book == null) { sender.sendMessage(msg("&c建立失敗")); return true; }
        target.getInventory().addItem(book);
        sender.sendMessage(msg("&a已給予 &e" + target.getName() + " &a法術書 &e" + spellId + " &aLv." + lv));
        return true;
    }
    private boolean onSynergy(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(msg("&c玩家限定")); return true; }
        Player p = (Player) sender;
        List<org.bukkit.inventory.ItemStack> cards = com.longdrange.ldattribute.card.PlayerData.getCards(p);
        sender.sendMessage(msg("&8&m--------&r &d共鳴/羈絆狀態 &8&m--------"));
        for (com.longdrange.ldattribute.card.SynergyData.Synergy s : com.longdrange.ldattribute.card.SynergyData.getAllResonances()) {
            int m = com.longdrange.ldattribute.card.SynergyData.getMatched(cards, s);
            String claim = com.longdrange.ldattribute.card.PlayerData.hasClaimed(p.getUniqueId(), "synergy_" + s.id) ? "§7[已領]" : "§a[未領]";
            sender.sendMessage(msg("&d共鳴 &f" + s.name + " &7" + m + "/" + s.required + " " + claim));
        }
        for (com.longdrange.ldattribute.card.SynergyData.Synergy s : com.longdrange.ldattribute.card.SynergyData.getAllBonds()) {
            int m = com.longdrange.ldattribute.card.SynergyData.getMatched(cards, s);
            String claim = com.longdrange.ldattribute.card.PlayerData.hasClaimed(p.getUniqueId(), "synergy_" + s.id) ? "§7[已領]" : "§a[未領]";
            sender.sendMessage(msg("&a羈絆 &f" + s.name + " &7" + m + "/" + s.required + " " + claim));
        }
        sender.sendMessage(msg("&8&m------------------------------------"));
        return true;
    }
    private boolean onLog(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg("&c此指令只能由玩家執行！"));
            return true;
        }
        Player p = (Player) sender;
        java.util.List<String> log = com.longdrange.ldattribute.combat.CombatLog.get(p.getUniqueId());
        sender.sendMessage(msg("&8&m-------- &6戰鬥日誌 &8&m--------"));
        if (log.isEmpty()) {
            sender.sendMessage(msg("&7暫無記錄"));
        } else {
            for (String line : log) sender.sendMessage(msg(line));
        }
        sender.sendMessage(msg("&8&m----------------------------"));
        return true;
    }
    private boolean onLang(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(msg("&7用法：/ldc lang <zh_TW|zh_CN|en_US>"));
            sender.sendMessage(msg("&7当前：&e" + com.longdrange.ldattribute.util.Message.getCurrentLang()));
            return true;
        }
        String lang = args[1];
        java.io.File f = new java.io.File(plugin.getDataFolder(), "message/" + lang + ".yml");
        if (!f.exists()) {
            sender.sendMessage(msg("&c找不到语言文件：&e" + lang + ".yml"));
            return true;
        }
        // 更新 config.yml 的 Language
        try {
            plugin.getConfig().set("Language", lang);
            plugin.saveConfig();
        } catch (Throwable ignored) {}
        // 立即加载
        com.longdrange.ldattribute.util.Message.load(plugin, lang);
        sender.sendMessage(msg("&a已切换语言：&e" + lang));
        return true;
    }
    private boolean onMm(CommandSender sender, String[] args) {
        boolean hooked = com.longdrange.ldattribute.compat.MMCompat.isHooked();
        sender.sendMessage(msg("&8&m-------- &bMythicMobs 兼容 &8&m--------"));
        if (hooked) {
            sender.sendMessage(msg("&a✔ MythicMobs 已连接"));
            sender.sendMessage(msg("&7掉落配置: &e編輯 plugins/LD-Attribute/mm.yml"));
        } else {
            sender.sendMessage(msg("&c✘ MythicMobs 未检测到"));
            sender.sendMessage(msg("&7需要安装 MythicMobs 4.4.0"));
        }
        sender.sendMessage(msg("&8&m-------------------------------"));
        return true;
    }
    private boolean onPet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(msg("&c玩家限定")); return true; }
        Player p = (Player) sender;
        if (args.length < 2) {
            com.longdrange.ldattribute.pet.inventory.PetInventory.open(p);
            return true;
        }
        String sub = args[1].toLowerCase();
        switch (sub) {
            case "open":
                com.longdrange.ldattribute.pet.inventory.PetInventory.open(p);
                return true;
            case "list":
                sender.sendMessage(msg("&8&m-------- &d宠物列表 &8&m--------"));
                for (com.longdrange.ldattribute.pet.PetConfig.Pet def :
                        com.longdrange.ldattribute.pet.PetConfig.getAll()) {
                    sender.sendMessage(msg("&e" + def.id + " &7- " + def.name + " &7[" + def.rarity + "]"));
                }
                sender.sendMessage(msg("&8&m----------------------------"));
                return true;
            case "give":
                if (!sender.hasPermission("ldattribute.command.admin")) {
                    sender.sendMessage(msg("&c无权限"));
                    return true;
                }
                if (args.length < 3) { sender.sendMessage(msg("&c用法：/ldc pet give <宠物ID> [玩家]")); return true; }
                String petId = args[2];
                if (com.longdrange.ldattribute.pet.PetConfig.get(petId) == null) {
                    sender.sendMessage(msg("&c找不到宠物：&e" + petId));
                    return true;
                }
                Player target = p;
                if (args.length >= 4) {
                    target = Bukkit.getPlayerExact(args[3]);
                    if (target == null) { sender.sendMessage(msg("&c玩家不在线")); return true; }
                }
                org.bukkit.inventory.ItemStack egg = com.longdrange.ldattribute.pet.PetManager.createPetEgg(petId);
                if (egg == null) { sender.sendMessage(msg("&c生成失败")); return true; }
                target.getInventory().addItem(egg);
                sender.sendMessage(msg("&a已给予 &e" + target.getName() + " &a宠物蛋 &e" + petId));
                return true;
            case "evolve":
                if (args.length < 3) { sender.sendMessage(msg("&c用法：/ldc pet evolve <宠物ID>")); return true; }
                int[] pos = com.longdrange.ldattribute.pet.PetData.findPetPos(p.getUniqueId(), args[2]);
                if (pos == null) { sender.sendMessage(msg("&c你还没有此宠物")); return true; }
                com.longdrange.ldattribute.pet.PetManager.evolve(p, pos[0], pos[1]);
                com.longdrange.ldattribute.pet.inventory.PetInventory.open(p, pos[0]);
                return true;
            case "level":
                if (!sender.hasPermission("ldattribute.command.admin")) { sender.sendMessage(msg("&c无权限")); return true; }
                if (args.length < 5) { sender.sendMessage(msg("&c用法：/ldc pet level <玩家> <宠物ID> <值>")); return true; }
                Player t2 = Bukkit.getPlayerExact(args[2]);
                if (t2 == null) { sender.sendMessage(msg("&c玩家不在线")); return true; }
                int[] pos2 = com.longdrange.ldattribute.pet.PetData.findPetPos(t2.getUniqueId(), args[3]);
                if (pos2 == null) { sender.sendMessage(msg("&c该玩家没有此宠物")); return true; }
                int lv;
                try { lv = Integer.parseInt(args[4]); } catch (Exception e) { sender.sendMessage(msg("&c值必须是数字")); return true; }
                com.longdrange.ldattribute.pet.PetInstance pi2 =
                        com.longdrange.ldattribute.pet.PetData.getPet(t2.getUniqueId(), pos2[0], pos2[1]);
                if (pi2 != null) { pi2.level = Math.max(1, lv); pi2.exp = 0; }
                com.longdrange.ldattribute.pet.PetData.save(t2.getUniqueId());
                try { com.longdrange.ldattribute.card.StatsDataRead.updatePlayer(t2); } catch (Throwable ignored) {}
                sender.sendMessage(msg("&a已设置 " + t2.getName() + " 的 " + args[3] + " 为 Lv." + lv));
                return true;
            case "unlock":
                if (!sender.hasPermission("ldattribute.command.admin")) { sender.sendMessage(msg("&c无权限")); return true; }
                if (args.length < 4) { sender.sendMessage(msg("&c用法：/ldc pet unlock <玩家> <页数>")); return true; }
                Player t3 = Bukkit.getPlayerExact(args[2]);
                if (t3 == null) { sender.sendMessage(msg("&c玩家不在线")); return true; }
                int pg;
                try { pg = Integer.parseInt(args[3]); } catch (Exception e) { sender.sendMessage(msg("&c值必须是数字")); return true; }
                com.longdrange.ldattribute.pet.PetData.setUnlockedPages(t3.getUniqueId(), pg);
                sender.sendMessage(msg("&a已解锁 " + t3.getName() + " 到第 " + pg + " 页"));
                return true;
        }
        sender.sendMessage(msg("&7用法：/ldc pet [open|list|give|evolve|level|unlock]"));
        return true;
    }
    private boolean hasEnoughItemCmd(Player p, String spec) {
        String[] parts = spec.split(":");
        org.bukkit.Material mat = org.bukkit.Material.getMaterial(parts[0].toUpperCase());
        if (mat == null) return false;
        int need = parts.length >= 2 ? Integer.parseInt(parts[1]) : 1;
        int have = 0;
        for (org.bukkit.inventory.ItemStack it : p.getInventory().getContents()) {
            if (it != null && it.getType() == mat) have += it.getAmount();
        }
        return have >= need;
    }

    private void takeItemCmd(Player p, String spec) {
        String[] parts = spec.split(":");
        org.bukkit.Material mat = org.bukkit.Material.getMaterial(parts[0].toUpperCase());
        if (mat == null) return;
        int need = parts.length >= 2 ? Integer.parseInt(parts[1]) : 1;
        int removed = 0;
        for (int i = 0; i < p.getInventory().getSize() && removed < need; i++) {
            org.bukkit.inventory.ItemStack it = p.getInventory().getItem(i);
            if (it == null || it.getType() != mat) continue;
            int take = Math.min(it.getAmount(), need - removed);
            it.setAmount(it.getAmount() - take);
            removed += take;
            if (it.getAmount() <= 0) p.getInventory().setItem(i, null);
        }
    }

    private boolean onDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ldattribute.command.admin")) {
            sender.sendMessage(msg("&c无权限")); return true;
        }
        sender.sendMessage(msg("&8&m-------- &bLD-Attribute Debug &8&m--------"));
        // 属性
        try {
            int attrCount = com.longdrange.ldattribute.data.attribute.LDAttributeManager.getAttributeMap().size();
            sender.sendMessage(msg("&7属性注册: &e" + attrCount));
        } catch (Throwable t) { sender.sendMessage(msg("&c属性读失败: " + t.getMessage())); }
        // 符文
        try {
            int runeCount = com.longdrange.ldattribute.rune.RuneConfig.getAllRunes().size();
            int socketCount = com.longdrange.ldattribute.rune.RuneConfig.getAllSockets().size();
            sender.sendMessage(msg("&7符文: &e" + runeCount + " &7孔位类型: &e" + socketCount));
        } catch (Throwable t) { sender.sendMessage(msg("&c符文读失败: " + t.getMessage())); }
        // 卡片
        try {
            int cardCount = com.longdrange.ldattribute.card.CardDataManager.getAllCardOnly().size();
            sender.sendMessage(msg("&7卡片种类: &e" + cardCount));
        } catch (Throwable ignored) {}
        // 法术
        try {
            int spellCount = com.longdrange.ldattribute.spell.SpellConfig.getAll().size();
            sender.sendMessage(msg("&7法术: &e" + spellCount));
        } catch (Throwable ignored) {}
        // 宠物
        try {
            int petCount = com.longdrange.ldattribute.pet.PetConfig.getAll().size();
            sender.sendMessage(msg("&7宠物: &e" + petCount));
        } catch (Throwable ignored) {}
        // Lore 缓存
        try {
            int cacheSize = com.longdrange.ldattribute.card.CardLevel.getCacheSize();
            sender.sendMessage(msg("&7Lore 缓存: &e" + cacheSize + " &7条"));
        } catch (Throwable ignored) {}
        // MM 挂钩
        try {
            boolean hooked = com.longdrange.ldattribute.compat.MMCompat.isHooked();
            sender.sendMessage(msg("&7MythicMobs: " + (hooked ? "&a已挂钩" : "&c未挂钩")));
        } catch (Throwable ignored) {}
        // 在线玩家
        sender.sendMessage(msg("&7在线玩家: &e" + org.bukkit.Bukkit.getOnlinePlayers().size()));
        // 内存
        try {
            Runtime rt = Runtime.getRuntime();
            long usedMb = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
            long maxMb = rt.maxMemory() / 1024 / 1024;
            sender.sendMessage(msg("&7内存: &e" + usedMb + " MB &7/ &e" + maxMb + " MB"));
        } catch (Throwable ignored) {}
        // 备份目录
        try {
            java.io.File bd = com.longdrange.ldattribute.util.BackupManager.getBackupDir();
            int count = 0;
            if (bd != null && bd.exists()) {
                java.io.File[] fs = bd.listFiles();
                if (fs != null) count = fs.length;
            }
            sender.sendMessage(msg("&7备份: &e" + count + " &7份 (最多保留 &e"
                    + com.longdrange.ldattribute.util.BackupManager.getMaxBackups() + "&7)"));
        } catch (Throwable ignored) {}
        sender.sendMessage(msg("&8&m------------------------------------"));
        return true;
    }
    private boolean onBackup(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ldattribute.command.admin")) {
            sender.sendMessage(msg("&c无权限"));
            return true;
        }
        try {
            java.io.File dir = com.longdrange.ldattribute.util.BackupManager.runBackup();
            sender.sendMessage(msg("&a✦ 备份完成: &e" + (dir != null ? dir.getName() : "?")));
            sender.sendMessage(msg("&7 备份目录: &fplugins/LD-Attribute/backups/"));
            sender.sendMessage(msg("&7 保留: &f" + com.longdrange.ldattribute.util.BackupManager.getMaxBackups()
                    + " &7份，每 &f" + com.longdrange.ldattribute.util.BackupManager.getIntervalMinutes() + " &7分钟一次"));
        } catch (Throwable t) {
            sender.sendMessage(msg("&c备份失败: " + t.getMessage()));
        }
        return true;
    }
    private boolean onRune(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(msg("&7用法：/ldc rune [list|give|socket|clear]"));
            return true;
        }
        String sub = args[1].toLowerCase();
        switch (sub) {
            case "recycle": {
                if (!(sender instanceof Player)) { sender.sendMessage(msg("&c玩家限定")); return true; }
                com.longdrange.ldattribute.rune.RuneRecycleInventory.open((Player) sender);
                return true;
            }            case "craft": {
                if (!(sender instanceof Player)) { sender.sendMessage(msg("&c玩家限定")); return true; }
                if (args.length < 3) { sender.sendMessage(msg("&c用法：/ldc rune craft <符文ID>")); return true; }
                Player p = (Player) sender;
                com.longdrange.ldattribute.rune.RuneConfig.Rune r =
                        com.longdrange.ldattribute.rune.RuneConfig.getRune(args[2]);
                if (r == null) { sender.sendMessage(msg("&c符文不存在")); return true; }
                if (r.costPoints <= 0 && r.costItems.isEmpty()) {
                    sender.sendMessage(msg("&c此符文无合成配方")); return true;
                }
                // 检查点券
                if (r.costPoints > 0) {
                    int pp = com.longdrange.ldattribute.points.PointAPI.getPlayerPoints(p.getName());
                    if (pp < r.costPoints) {
                        sender.sendMessage(msg("&c点券不足！需要 &e" + r.costPoints + " &c你有 &e" + pp));
                        return true;
                    }
                }
                // 检查材料
                for (String spec : r.costItems) {
                    if (!hasEnoughItemCmd(p, spec)) {
                        sender.sendMessage(msg("&c缺少物品: &e" + spec));
                        return true;
                    }
                }
                // 扣点券
                if (r.costPoints > 0) com.longdrange.ldattribute.points.PointAPI.takePlayerPoints(p.getName(), r.costPoints);
                // 扣材料
                for (String spec : r.costItems) takeItemCmd(p, spec);

                // 发符文
                org.bukkit.inventory.ItemStack rune =
                        com.longdrange.ldattribute.rune.RuneItem.create(r, 1);
                java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> left =
                        p.getInventory().addItem(rune);
                for (org.bukkit.inventory.ItemStack drop : left.values())
                    p.getWorld().dropItemNaturally(p.getLocation(), drop);
                sender.sendMessage(msg("&a✦ 合成成功！获得 &e" + r.name));
                try { p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f); } catch (Throwable ignored) {}
                return true;
            }            case "upgrade": {
                if (!(sender instanceof Player)) { sender.sendMessage(msg("&c玩家限定")); return true; }
                if (args.length < 3) { sender.sendMessage(msg("&c用法：/ldc rune upgrade <符文ID>")); return true; }
                if (com.longdrange.ldattribute.rune.RuneConfig.getRune(args[2]) == null) {
                    sender.sendMessage(msg("&c符文不存在")); return true;
                }
                com.longdrange.ldattribute.rune.RuneUpgradeInventory.open((Player) sender, args[2]);
                return true;
            }            case "collection": {
                if (!(sender instanceof Player)) { sender.sendMessage(msg("&c玩家限定")); return true; }
                com.longdrange.ldattribute.rune.RuneCollectionInventory.open((Player) sender);
                return true;
            }            case "list": {
                sender.sendMessage(msg("&8&m-------- &d符文列表 &8&m--------"));
                for (com.longdrange.ldattribute.rune.RuneConfig.Rune r :
                        com.longdrange.ldattribute.rune.RuneConfig.getAllRunes()) {
                    sender.sendMessage(msg("&e" + r.id + " &7- " + r.name + " &7[" + r.type + "]"));
                }
                sender.sendMessage(msg("&8&m----------------------------"));
                return true;
            }
            case "give": {
                if (!sender.hasPermission("ldattribute.command.admin")) {
                    sender.sendMessage(msg("&c无权限"));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(msg("&c用法：/ldc rune give <符文ID> [玩家] [数量]"));
                    return true;
                }
                String runeId = args[2];
                com.longdrange.ldattribute.rune.RuneConfig.Rune r =
                        com.longdrange.ldattribute.rune.RuneConfig.getRune(runeId);
                if (r == null) {
                    sender.sendMessage(msg("&c找不到符文：&e" + runeId));
                    return true;
                }
                Player target;
                if (args.length >= 4) {
                    target = Bukkit.getPlayerExact(args[3]);
                    if (target == null) { sender.sendMessage(msg("&c玩家不在线")); return true; }
                } else {
                    if (!(sender instanceof Player)) { sender.sendMessage(msg("&c请指定玩家")); return true; }
                    target = (Player) sender;
                }
                int amount = 1;
                if (args.length >= 5) {
                    try { amount = Integer.parseInt(args[4]); } catch (Exception ignored) {}
                }
                org.bukkit.inventory.ItemStack item =
                        com.longdrange.ldattribute.rune.RuneItem.create(r, amount);
                java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> left =
                        target.getInventory().addItem(item);
                for (org.bukkit.inventory.ItemStack drop : left.values()) {
                    target.getWorld().dropItemNaturally(target.getLocation(), drop);
                }
                sender.sendMessage(msg("&a已给予 &e" + target.getName() + " &a符文 &e" + runeId + " &7×" + amount));
                return true;
            }
            case "socket": {
                if (!sender.hasPermission("ldattribute.command.admin")) {
                    sender.sendMessage(msg("&c无权限")); return true;
                }
                if (!(sender instanceof Player)) { sender.sendMessage(msg("&c玩家限定")); return true; }
                Player p = (Player) sender;
                if (args.length < 5) {
                    sender.sendMessage(msg("&c用法：/ldc rune socket <孔位序号> <符文ID> [卡槽0-35]"));
                    return true;
                }
                int idx;
                try { idx = Integer.parseInt(args[2]); } catch (Exception e) {
                    sender.sendMessage(msg("&c孔位序号必须是数字")); return true;
                }
                String runeId = args[3];
                com.longdrange.ldattribute.rune.RuneConfig.Rune r =
                        com.longdrange.ldattribute.rune.RuneConfig.getRune(runeId);
                if (r == null) { sender.sendMessage(msg("&c找不到符文")); return true; }
                int slot = p.getInventory().getHeldItemSlot();
                if (args.length >= 5) {
                    try { slot = Integer.parseInt(args[4]); } catch (Exception ignored) {}
                }
                org.bukkit.inventory.ItemStack card = p.getInventory().getItem(slot);
                if (card == null) { sender.sendMessage(msg("&c该槽位没有物品")); return true; }
                com.longdrange.ldattribute.card.CardData cd =
                        com.longdrange.ldattribute.card.CardDataManager.findCard(card);
                if (cd == null) { sender.sendMessage(msg("&c该物品不是卡片")); return true; }
                org.bukkit.inventory.ItemStack newCard =
                        com.longdrange.ldattribute.card.CardNBT.setSocketRune(card, idx, runeId);
                newCard = com.longdrange.ldattribute.card.CardNBT.unlockSocket(newCard, idx);
                newCard = com.longdrange.ldattribute.card.CardLevel.recalc(newCard, cd.getId(),
                        com.longdrange.ldattribute.card.CardNBT.getLevel(newCard),
                        com.longdrange.ldattribute.card.CardNBT.getExp(newCard));
                p.getInventory().setItem(slot, newCard);
                sender.sendMessage(msg("&a已在孔位 &e" + idx + " &a镶嵌 &e" + runeId));
                return true;
            }
            case "clear": {
                if (!sender.hasPermission("ldattribute.command.admin")) {
                    sender.sendMessage(msg("&c无权限")); return true;
                }
                if (!(sender instanceof Player)) { sender.sendMessage(msg("&c玩家限定")); return true; }
                Player p = (Player) sender;
                int slot = args.length >= 3 ? Integer.parseInt(args[2]) : p.getInventory().getHeldItemSlot();
                org.bukkit.inventory.ItemStack card = p.getInventory().getItem(slot);
                if (card == null) { sender.sendMessage(msg("&c该槽位没有物品")); return true; }
                com.longdrange.ldattribute.card.CardData cd =
                        com.longdrange.ldattribute.card.CardDataManager.findCard(card);
                if (cd == null) { sender.sendMessage(msg("&c该物品不是卡片")); return true; }
                int max = 16;
                for (int i = 0; i < max; i++) {
                    card = com.longdrange.ldattribute.card.CardNBT.setSocketRune(card, i, "");
                }
                card = com.longdrange.ldattribute.card.CardLevel.recalc(card, cd.getId(),
                        com.longdrange.ldattribute.card.CardNBT.getLevel(card),
                        com.longdrange.ldattribute.card.CardNBT.getExp(card));
                p.getInventory().setItem(slot, card);
                sender.sendMessage(msg("&a已清空该卡所有符文"));
                return true;
            }
        }
        sender.sendMessage(msg("&7用法：/ldc rune [list|give|socket|clear]"));
        return true;
    }
    private boolean onMsg(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Message.get("Command.Msg.Usage"));
            return true;
        }
        String key = args[1];
        Object[] params = Arrays.copyOfRange(args, 2, args.length);
        String result = Message.get(key, params);
        sender.sendMessage(Message.get("Command.Msg.Result", key, result));
        return true;
    }

    private String msg(String s) {
        return ChatColor.translateAlternateColorCodes((char) 38, s);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            String pre = args[0].toLowerCase();
            for (Map.Entry<String, CommandConfig.SubCmd> e : CommandConfig.getAllSubs().entrySet()) {
                CommandConfig.SubCmd c = e.getValue();
                if (c.permission != null && !c.permission.isEmpty()
                        && !sender.hasPermission(c.permission)) continue;
                if (c.name.toLowerCase().startsWith(pre)) result.add(c.name);
                for (String a : c.aliases)
                    if (a.toLowerCase().startsWith(pre)) result.add(a);
            }
            if ("help".startsWith(pre)) result.add("help");
        } else if (args.length == 2) {
            String key = CommandConfig.matchSubKey(args[0]);
            if (key == null) return result;
            String pre = args[1];
            if ("Cast".equals(key)) {
                for (String id : com.longdrange.ldattribute.spell.SpellConfig.getIds())
                    if (id.startsWith(pre.toLowerCase())) result.add(id);
            } else if ("Give".equals(key) || "Info".equals(key) || "Remove".equals(key)) {
                for (String id : CardDataManager.getAllIds())
                    if (id.startsWith(pre)) result.add(id);
            } else if ("Star".equals(key) || "Level".equals(key) || "Exp".equals(key)
                    || "Unlock".equals(key) || "Reset".equals(key)) {
                for (Player p : Bukkit.getOnlinePlayers())
                    if (p.getName().startsWith(pre)) result.add(p.getName());
            }
        } else if (args.length == 3) {
            String key = CommandConfig.matchSubKey(args[0]);
            if (key == null) return result;
            String pre = args[2];
            if ("Remove".equals(key)) {
                for (Player p : Bukkit.getOnlinePlayers())
                    if (p.getName().startsWith(pre)) result.add(p.getName());
            } else if ("Star".equals(key) || "Level".equals(key) || "Exp".equals(key)) {
                for (String id : CardDataManager.getAllIds())
                    if (id.startsWith(pre)) result.add(id);
            } else if ("Unlock".equals(key)) {
                int total = PageConfig.getPageCount();
                for (int i = 1; i <= total; i++) {
                    String s = String.valueOf(i);
                    if (s.startsWith(pre)) result.add(s);
                }
            }
        }
        return result;
    }
}
