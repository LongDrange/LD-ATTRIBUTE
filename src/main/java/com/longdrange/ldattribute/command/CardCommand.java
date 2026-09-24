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
import java.util.HashMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.HashMap;

public class CardCommand implements CommandExecutor, TabCompleter {

    private final LDAttribute plugin;

    public CardCommand(LDAttribute plugin) {
        this.plugin = plugin;
    }

    private static final java.util.Map<java.util.UUID, Long> commandCooldown = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long COMMAND_COOLDOWN_MS = 500L;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // 冷却：防止连点刷屏
        if (sender instanceof Player) {
            long _now = System.currentTimeMillis();
            Long _last = commandCooldown.get(((Player) sender).getUniqueId());
            if (_last != null && _now - _last < COMMAND_COOLDOWN_MS) return true;
            commandCooldown.put(((Player) sender).getUniqueId(), _now);
        }

        if (args.length == 0) { sendHelp(sender, label); return true; }
        String sub = args[0];

        if (sub.equalsIgnoreCase("help")) { if (args.length >= 2) return onHelpDetail(sender, args, label); sendHelp(sender, label); return true; }

        String key = CommandConfig.matchSubKey(sub);
        if (key == null) { sendHelp(sender, label); return true; }

        CommandConfig.SubCmd cfg = CommandConfig.getSub(key);
        if (cfg == null) { sendHelp(sender, label); return true; }

        if (cfg.permission != null && !cfg.permission.isEmpty()
                && !sender.hasPermission(cfg.permission)) {
            sender.sendMessage(Message.get("Command.Help.NoPerm"));
            return true;
        }

        try {
            if (sender instanceof Player
                    && sender.hasPermission("ldattribute.command.admin")) {
                com.longdrange.ldattribute.util.AuditLog.write(sender,
                        key, String.join(" ", args));
            }
        } catch (Throwable ignored) {}

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
            case "Give": {
                if (args.length >= 2 && args[1].contains(",")) {
                    for (String _id : args[1].split(",")) {
                        String[] _sub = args.clone();
                        _sub[1] = _id.trim();
                        onGive(sender, _sub);
                    }
                    return true;
                }
                return onGive(sender, args);
            }
            case "Save": return onSave(sender, args);
            case "Damage": return onDamage(sender, args);
            case "Compare": return onCompare(sender, args);
            case "Buff": return onBuff(sender, args);
            case "Points": return onPoints(sender, args);
            case "BackupNow": return onBackupNow(sender, args);
            case "Player": return onPlayer(sender, args);
            case "Find": return onFind(sender, args);
            case "Overview": return onOverview(sender, args);
            case "Export": return onExport(sender, args);
            case "Reload": {
                String _only = (args.length >= 2) ? args[1] : "all";
                plugin.reloadAll(_only);
                sender.sendMessage(Message.get("Command.Reload.Success") + " §7[" + _only + "§7]");
                return true;
            }
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
            case "Achievement": return onAchievement(sender, args);
            case "Gacha": return onGacha(sender, args);
            case "PetEquip": return onPetEquip(sender, args);
            case "ChatItem": return onChatItem(sender, args);
            case "GiveMe":   return onGiveMe(sender, args);
        }
        sendHelp(sender, label);
        return true;
    }

    /**
     * /ldc chatitem <玩家> <卡片ID> [数量]
     * 给玩家发一条聊天消息，消息带可点击的物品链接。
     * 点击会触发 /ldc giveme 指令（那边有权限检查）。
     */
    private boolean onChatItem(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg("&c只能玩家使用")); return true;
        }
        if (!sender.hasPermission("ldattribute.command.admin")) {
            sender.sendMessage(msg("&c无权限")); return true;
        }
        if (args.length < 3) {
            sender.sendMessage(msg("&7用法: /ldc chatitem <玩家> <卡片ID> [数量]"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { sender.sendMessage(msg("&c玩家不在线")); return true; }

        CardData cd = CardDataManager.getCard(args[2]);
        if (cd == null) { sender.sendMessage(msg("&c卡片不存在: " + args[2])); return true; }

        int amount = 1;
        if (args.length >= 4) try { amount = Integer.parseInt(args[3]); } catch (Exception ignored) {}
        final int amt = amount;

        ItemStack preview = cd.getItem();
        preview.setAmount(amt);

        // 给发起者（OP）看
        com.longdrange.ldattribute.util.ChatItemLink.send((Player) sender,
                msg("&7已发送链接给 &e" + target.getName() + " &7→ "),
                preview,
                "/ldc giveme " + target.getName() + " " + args[2] + " " + amt,
                "§a§l[点我发放]");

        // 给目标加一条待领取（10 分钟）
        {
            java.util.List<Pending> q = pendingQueue.computeIfAbsent(
                    target.getUniqueId(), k -> new java.util.ArrayList<>());
            synchronized (q) {
                q.add(new Pending(args[2], amt, System.currentTimeMillis() + 10 * 60 * 1000L));
            }
            savePending();
        }
        // 给目标玩家看
        com.longdrange.ldattribute.util.ChatItemLink.send(target,
                msg("&e" + sender.getName() + " &7发给你一张卡片 "),
                preview,
                "/ldc giveme " + target.getName() + " " + args[2] + " " + amt,
                "§a§l[点击领取]");
        return true;
    }

    /**
     * /ldc giveme <玩家> <卡片ID> [数量]
     * 真正的发放动作。带权限检查，防止玩家自己刷。
     */
    /** 待领取队列：UUID → list。10 分钟有效 */
    public static class Pending {
        public String cardId;
        public int amount;
        public long expireAt;
        public Pending() {}
        public Pending(String id, int n, long t) { cardId = id; amount = n; expireAt = t; }
    }
    private static final java.util.Map<java.util.UUID, java.util.List<Pending>> pendingQueue =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static java.io.File pendingFile;

    public static void loadPending(com.longdrange.ldattribute.LDAttribute plugin) {
        try {
            java.io.File dir = plugin.getDataFolder();
            if (!dir.exists()) dir.mkdirs();
            pendingFile = new java.io.File(dir, "pending.yml");
            if (!pendingFile.exists()) return;
            org.bukkit.configuration.file.YamlConfiguration cfg =
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(pendingFile);
            pendingQueue.clear();
            long now = System.currentTimeMillis();
            for (String uuidStr : cfg.getKeys(false)) {
                try {
                    java.util.UUID uid = java.util.UUID.fromString(uuidStr);
                    java.util.List<java.util.Map<?, ?>> raw = cfg.getMapList(uuidStr);
                    java.util.List<Pending> list = new java.util.ArrayList<>();
                    for (java.util.Map<?, ?> m : raw) {
                        Pending p = new Pending();
                        p.cardId = String.valueOf(m.get("cardId"));
                        Object amtObj = m.get("amount");
                        p.amount = amtObj instanceof Number ? ((Number) amtObj).intValue() : 1;
                        Object expObj = m.get("expireAt");
                        p.expireAt = expObj instanceof Number ? ((Number) expObj).longValue() : 0L;
                        if (p.expireAt > now) list.add(p);
                    }
                    if (!list.isEmpty()) pendingQueue.put(uid, list);
                } catch (Throwable ignored) {}
            }
            plugin.getLogger().info("[待领取] 已恢复 " + pendingQueue.size() + " 个玩家队列");
        } catch (Throwable t) {
            plugin.getLogger().warning("[待领取] 加载失败: " + t.getMessage());
        }
    }

    public static synchronized void savePending() {
        if (pendingFile == null) return;
        try {
            org.bukkit.configuration.file.YamlConfiguration cfg =
                    new org.bukkit.configuration.file.YamlConfiguration();
            long now = System.currentTimeMillis();
            for (java.util.Map.Entry<java.util.UUID, java.util.List<Pending>> e : pendingQueue.entrySet()) {
                java.util.List<java.util.Map<String, Object>> list = new java.util.ArrayList<>();
                synchronized (e.getValue()) {
                    java.util.Iterator<Pending> it = e.getValue().iterator();
                    while (it.hasNext()) {
                        Pending p = it.next();
                        if (p.expireAt < now) { it.remove(); continue; }
                        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                        m.put("cardId", p.cardId);
                        m.put("amount", p.amount);
                        m.put("expireAt", p.expireAt);
                        list.add(m);
                    }
                }
                if (!list.isEmpty()) cfg.set(e.getKey().toString(), list);
            }
            cfg.save(pendingFile);
        } catch (Throwable ignored) {}
    }

    /**
     * /ldc giveme <玩家> <卡片ID> [数量]
     *   OP        → 直接发
     *   本人+待领  → 领一条并消耗
     *   其他      → 拒绝
     */
    private static final java.util.Map<java.util.UUID, Long> lastGiveMeClick = new java.util.concurrent.ConcurrentHashMap<>();

    private boolean onGiveMe(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(msg("&7用法: /ldc giveme <玩家> <卡片ID> [数量]"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { sender.sendMessage(msg("&c玩家不在线")); return true; }
        CardData cd = CardDataManager.getCard(args[2]);
        if (cd == null) { sender.sendMessage(msg("&c卡片不存在: " + args[2])); return true; }

        int amount = 1;
        if (args.length >= 4) try { amount = Integer.parseInt(args[3]); } catch (Exception ignored) {}
        final int amt = amount;

        boolean isAdmin = !(sender instanceof Player)
                || sender.hasPermission("ldattribute.command.admin");
        boolean isSelf = sender instanceof Player
                && ((Player) sender).getUniqueId().equals(target.getUniqueId());

        if (!isAdmin) {
            if (!isSelf) { sender.sendMessage(msg("&c无权限")); return true; }
            // 防刷：3 秒内只能点一次
            long nowMs = System.currentTimeMillis();
            Long lastClick = lastGiveMeClick.get(target.getUniqueId());
            if (lastClick != null && nowMs - lastClick < 3000) {
                sender.sendMessage(msg("&c点击太快了，请稍后再试"));
                return true;
            }
            lastGiveMeClick.put(target.getUniqueId(), nowMs);

            // 本人 → 查待领取队列
            java.util.List<Pending> q = pendingQueue.get(target.getUniqueId());
            boolean consumed = false;
            if (q != null) {
                synchronized (q) {
                    long now = System.currentTimeMillis();
                    java.util.Iterator<Pending> it = q.iterator();
                    while (it.hasNext()) {
                        Pending p = it.next();
                        if (p.expireAt < now) { it.remove(); continue; }
                        if (p.cardId.equals(args[2]) && p.amount == amt) {
                            it.remove(); consumed = true; break;
                        }
                    }
                }
            }
            if (!consumed) {
                sender.sendMessage(msg("&c该链接已过期或已被领取，请让 OP 重新发送"));
                return true;
            }
            savePending();
        }

        ItemStack item = cd.getItem();
        item.setAmount(amt);
        HashMap<Integer, ItemStack> left = target.getInventory().addItem(item);
        for (ItemStack d : left.values())
            target.getWorld().dropItemNaturally(target.getLocation(), d);
        target.sendMessage(msg("&a✦ 你收到了 &e" + args[2] + " &a×" + amt));
        if (!isSelf)
            sender.sendMessage(msg("&a已发放 &e" + args[2] + " ×" + amt + " → " + target.getName()));
        return true;
    }
    private boolean onDamage(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg("&c玩家限定")); return true;
        }
        Player p = (Player) sender;
        com.longdrange.ldattribute.data.attribute.LDAttributeData data;
        try { data = com.longdrange.ldattribute.card.StatsDataRead.loadPlayerStats(p); }
        catch (Throwable t) { sender.sendMessage(msg("&c读取失败: " + t.getMessage())); return true; }

        sender.sendMessage(msg("&8&m-------- &6伤害分解: &e" + p.getName() + " &8&m--------"));
        sender.sendMessage(msg("&7[攻击方属性]"));

        java.util.Map<Integer, com.longdrange.ldattribute.data.attribute.LDSubAttribute> map = data.getAttributeMap();
        java.util.List<String> attackLines = new java.util.ArrayList<>();
        java.util.List<String> defenseLines = new java.util.ArrayList<>();
        java.util.List<String> otherLines = new java.util.ArrayList<>();
        for (com.longdrange.ldattribute.data.attribute.LDSubAttribute a : map.values()) {
            String nm = a.getName();
            double v = a.getValue();
            if (v == 0) continue;
            String line = "  &7- " + pad(nm, 12) + " &e" + fmt(v);
            boolean isAttack = false, isDefense = false;
            try { isAttack = a.containsType(com.longdrange.ldattribute.data.attribute.LDAttributeType.ATTACK); } catch (Throwable ignored) {}
            try { isDefense = a.containsType(com.longdrange.ldattribute.data.attribute.LDAttributeType.DEFENSE); } catch (Throwable ignored) {}
            if (isAttack) attackLines.add(line);
            else if (isDefense) defenseLines.add(line);
            else otherLines.add(line);
        }
        if (attackLines.isEmpty()) sender.sendMessage(msg("  &7(无攻击类属性)"));
        else for (String l : attackLines) sender.sendMessage(msg(l));

        sender.sendMessage(msg("&7[防御方属性]"));
        if (defenseLines.isEmpty()) sender.sendMessage(msg("  &7(无防御类属性)"));
        else for (String l : defenseLines) sender.sendMessage(msg(l));

        sender.sendMessage(msg("&7[其他属性]"));
        if (otherLines.isEmpty()) sender.sendMessage(msg("  &7(无)"));
        else for (String l : otherLines) sender.sendMessage(msg(l));

        sender.sendMessage(msg("&8&m----------------------------"));
        sender.sendMessage(msg("&7§o提示: 主手武器也参与计算，换武器后再看属性会变"));
        return true;
    }
    private boolean onCompare(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(msg("&7用法: /ldc compare <卡片1> <卡片2>"));
            return true;
        }
        com.longdrange.ldattribute.card.CardData a = CardDataManager.getCard(args[1]);
        com.longdrange.ldattribute.card.CardData b = CardDataManager.getCard(args[2]);
        if (a == null) { sender.sendMessage(msg("&c卡片不存在: " + args[1])); return true; }
        if (b == null) { sender.sendMessage(msg("&c卡片不存在: " + args[2])); return true; }

        com.longdrange.ldattribute.card.CardLevelConfig.CardLevel la = com.longdrange.ldattribute.card.CardLevelConfig.get(a.getId());
        com.longdrange.ldattribute.card.CardLevelConfig.CardLevel lb = com.longdrange.ldattribute.card.CardLevelConfig.get(b.getId());

        sender.sendMessage(msg("&8&m-------- &6卡片对比 &8&m--------"));
        sender.sendMessage(msg("&eA: &f" + a.getId() + "  &eB: &f" + b.getId()));
        sender.sendMessage(msg("&7" + pad("属性", 14) + " &eA &7| &eB &7| &a差异"));

        if (la == null || lb == null) {
            sender.sendMessage(msg("&7(有一张卡不可升级，无属性数据)"));
            if (la != null) {
                sender.sendMessage(msg("&7A 属性:"));
                for (com.longdrange.ldattribute.card.CardLevelConfig.AttrConf ac : la.attrs.values())
                    sender.sendMessage(msg("  &7- " + ac.name + ": &e" + ac.base + " &7+&e" + ac.growth + "&7/级"));
            }
            if (lb != null) {
                sender.sendMessage(msg("&7B 属性:"));
                for (com.longdrange.ldattribute.card.CardLevelConfig.AttrConf ac : lb.attrs.values())
                    sender.sendMessage(msg("  &7- " + ac.name + ": &e" + ac.base + " &7+&e" + ac.growth + "&7/级"));
            }
            sender.sendMessage(msg("&8&m----------------------------"));
            return true;
        }

        java.util.Set<String> keys = new java.util.LinkedHashSet<>();
        keys.addAll(la.attrs.keySet());
        keys.addAll(lb.attrs.keySet());
        for (String k : keys) {
            com.longdrange.ldattribute.card.CardLevelConfig.AttrConf ca = la.attrs.get(k);
            com.longdrange.ldattribute.card.CardLevelConfig.AttrConf cb = lb.attrs.get(k);
            String nameA = ca != null ? ca.name : k;
            String nameB = cb != null ? cb.name : k;
            String display = ca != null ? ca.name : (cb != null ? cb.name : k);
            double baseA = ca != null ? ca.base : 0;
            double baseB = cb != null ? cb.base : 0;
            double diff = baseB - baseA;
            String diffStr;
            if (Math.abs(diff) < 0.01) diffStr = "&7=";
            else if (diff > 0) diffStr = "&a+" + fmt(diff);
            else diffStr = "&c" + fmt(diff);
            sender.sendMessage(msg("&7" + pad(display, 14) + " &e" + fmt(baseA) + " &7| &e" + fmt(baseB) + " &7| " + diffStr));
        }
        sender.sendMessage(msg("&8&m----------------------------"));
        return true;
    }

    private static String fmt(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }

    private static String pad(String s, int n) {
        if (s == null) s = "";
        int visible = s.replaceAll("§.", "").length();
        if (visible >= n) return s;
        StringBuilder sb = new StringBuilder(s);
        for (int i = visible; i < n; i++) sb.append(' ');
        return sb.toString();
    }
    private boolean onBuff(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(msg("&7用法: /ldc buff <list|add|clear>"));
            return true;
        }
        String sub = args[1].toLowerCase();
        if (sub.equals("list") || sub.equals("列表")) {
            if (args.length >= 3) {
                String name = args[2];
                org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(name);
                if (op == null || op.getUniqueId() == null) { sender.sendMessage(msg("&c找不到玩家")); return true; }
                java.util.Map<String, Long> m = com.longdrange.ldattribute.card.TempBuffManager.getActiveMap(op.getUniqueId());
                long now = System.currentTimeMillis();
                sender.sendMessage(msg("&8&m-------- &6" + name + " 的 Buff &8&m--------"));
                if (m.isEmpty()) { sender.sendMessage(msg("&7(无)")); return true; }
                for (java.util.Map.Entry<String, Long> e : m.entrySet()) {
                    long left = (e.getValue() - now) / 1000;
                    if (left <= 0) continue;
                    sender.sendMessage(msg("&e" + e.getKey() + " &7剩余: &a" + left + "s"));
                }
                return true;
            }
            sender.sendMessage(msg("&8&m-------- &6所有 Buff &8&m--------"));
            for (com.longdrange.ldattribute.card.TempBuffConfig.Buff b :
                    com.longdrange.ldattribute.card.TempBuffConfig.getAll()) {
                sender.sendMessage(msg("&e" + b.id + " &7触发: &f" + b.event + " &7时长: &f" + b.duration + "s"));
            }
            return true;
        }
        if (!sender.hasPermission("ldattribute.command.admin")) {
            sender.sendMessage(msg("&c无权限")); return true;
        }
        if (sub.equals("add") || sub.equals("给")) {
            if (args.length < 5) { sender.sendMessage(msg("&7用法: /ldc buff add <玩家> <buffId> <秒>")); return true; }
            org.bukkit.entity.Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) { sender.sendMessage(msg("&c玩家不在线")); return true; }
            int sec;
            try { sec = Integer.parseInt(args[4]); } catch (Exception e) { sender.sendMessage(msg("&c秒数必须是数字")); return true; }
            boolean ok = com.longdrange.ldattribute.card.TempBuffManager.addBuff(target.getUniqueId(), args[3], sec);
            if (ok) sender.sendMessage(msg("&a已给 &e" + target.getName() + " &a添加 &e" + args[3] + " &a(" + sec + "秒)"));
            else sender.sendMessage(msg("&cBuff 不存在: " + args[3]));
            return true;
        }
        if (sub.equals("clear") || sub.equals("清除")) {
            if (args.length < 3) { sender.sendMessage(msg("&7用法: /ldc buff clear <玩家>")); return true; }
            org.bukkit.entity.Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) { sender.sendMessage(msg("&c玩家不在线")); return true; }
            com.longdrange.ldattribute.card.TempBuffManager.clearAllActive(target.getUniqueId());
            sender.sendMessage(msg("&a已清除 &e" + target.getName() + " &a的所有 Buff"));
            return true;
        }
        sender.sendMessage(msg("&7用法: /ldc buff <list|add|clear>"));
        return true;
    }
    private boolean onPoints(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ldattribute.command.admin")) {
            sender.sendMessage(msg("&c无权限")); return true;
        }
        if (args.length < 3) {
            sender.sendMessage(msg("&7用法: /ldc points <get|set|add|take> <玩家> [值]"));
            return true;
        }
        String op = args[1].toLowerCase();
        String targetName = args[2];
        // get
        if (op.equals("get") || op.equals("查") || op.equals("查询")) {
            int p = com.longdrange.ldattribute.points.PointAPI.getPlayerPoints(targetName);
            sender.sendMessage(msg("&e" + targetName + " &7的点券: &f" + p));
            return true;
        }
        // 其他需要值
        if (args.length < 4) {
            sender.sendMessage(msg("&7用法: /ldc points " + op + " <玩家> <值>"));
            return true;
        }
        int val;
        try { val = Integer.parseInt(args[3]); } catch (Exception e) {
            sender.sendMessage(msg("&c值必须是数字")); return true;
        }
        if (op.equals("set") || op.equals("设置")) {
            com.longdrange.ldattribute.points.PointAPI.setPlayerPoints(targetName, val);
            sender.sendMessage(msg("&a已设置 &e" + targetName + " &a点券为 &f" + val));
        } else if (op.equals("add") || op.equals("加")) {
            com.longdrange.ldattribute.points.PointAPI.addPlayerPoints(targetName, val);
            int after = com.longdrange.ldattribute.points.PointAPI.getPlayerPoints(targetName);
            sender.sendMessage(msg("&a已给 &e" + targetName + " &a加 &f" + val + " &a点券，现在 &f" + after));
        } else if (op.equals("take") || op.equals("扣")) {
            int cur = com.longdrange.ldattribute.points.PointAPI.getPlayerPoints(targetName);
            if (cur < val) {
                sender.sendMessage(msg("&c点券不足！当前只有 &f" + cur)); return true;
            }
            com.longdrange.ldattribute.points.PointAPI.takePlayerPoints(targetName, val);
            sender.sendMessage(msg("&a已扣 &e" + targetName + " &f" + val + " &a点券"));
        } else {
            sender.sendMessage(msg("&7未知操作: " + op + "，可用: get / set / add / take"));
        }
        return true;
    }
    private boolean onBackupNow(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ldattribute.command.admin")) {
            sender.sendMessage(msg("&c无权限")); return true;
        }
        sender.sendMessage(msg("&e正在备份..."));
        try {
            java.io.File f = com.longdrange.ldattribute.util.BackupManager.runBackup();
            sender.sendMessage(msg("&a已备份到: &e" + f.getName()));
        } catch (Throwable t) {
            sender.sendMessage(msg("&c备份失败: " + t.getMessage()));
        }
        return true;
    }
    private boolean onPlayer(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ldattribute.command.admin")) {
            sender.sendMessage(msg("&c无权限")); return true;
        }
        if (args.length < 2) {
            sender.sendMessage(msg("&7用法: /ldc player <玩家名>")); return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg("&c只有玩家可以使用此命令（GUI 命令）"));
            return true;
        }
        com.longdrange.ldattribute.card.inventory.PlayerInspectInventory.open((Player) sender, args[1]);
        return true;
    }
    private boolean onFind(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ldattribute.command.admin")) {
            sender.sendMessage(msg("&c无权限")); return true;
        }
        if (args.length < 2) {
            sender.sendMessage(msg("&7用法: /ldc find <卡片ID>")); return true;
        }
        String cardId = args[1];
        com.longdrange.ldattribute.card.CardData cd =
                com.longdrange.ldattribute.card.CardDataManager.getCard(cardId);
        if (cd == null) { sender.sendMessage(msg("&c卡片不存在: " + cardId)); return true; }
        sender.sendMessage(msg("&8&m-------- &6卡片查找: &e" + cardId + " &8&m--------"));
        int found = 0;
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            try {
                int count = 0;
                for (org.bukkit.inventory.ItemStack it :
                        com.longdrange.ldattribute.card.PlayerData.getCards(p)) {
                    com.longdrange.ldattribute.card.CardData c2 =
                            com.longdrange.ldattribute.card.CardDataManager.findCard(it);
                    if (c2 != null && c2.getId().equalsIgnoreCase(cardId)) count++;
                }
                if (count > 0) {
                    sender.sendMessage(msg("  &e" + p.getName() + " &7× &f" + count));
                    found++;
                }
            } catch (Throwable ignored) {}
        }
        if (found == 0) sender.sendMessage(msg("  &7(无人在线拥有此卡)"));
        sender.sendMessage(msg("&8&m-------- 共 &f" + found + " &8&m个玩家 --------"));
        return true;
    }
    private boolean onOverview(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ldattribute.command.admin")) {
            sender.sendMessage(msg("&c无权限")); return true;
        }
        sender.sendMessage(msg("&8&m-------- &6服务器总览 &8&m--------"));
        int online = Bukkit.getOnlinePlayers().size();
        sender.sendMessage(msg("&e在线玩家: &f" + online));
        int totalCards = 0, totalPets = 0, totalRunes = 0, totalPoints = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            try { java.util.List<org.bukkit.inventory.ItemStack> cs =
                    com.longdrange.ldattribute.card.PlayerData.getCards(p);
                if (cs != null) totalCards += cs.size(); } catch (Throwable ignored) {}
            try { java.util.List<com.longdrange.ldattribute.pet.PetInstance> ps =
                    com.longdrange.ldattribute.pet.PetData.getAllPets(p.getUniqueId());
                if (ps != null) totalPets += ps.size(); } catch (Throwable ignored) {}
            try { totalRunes += com.longdrange.ldattribute.rune.RuneData.count(p.getUniqueId()); } catch (Throwable ignored) {}
            try { totalPoints += com.longdrange.ldattribute.points.PointAPI.getPlayerPoints(p.getName()); } catch (Throwable ignored) {}
        }
        sender.sendMessage(msg("&e在线卡片总数: &f" + totalCards));
        sender.sendMessage(msg("&e在线宠物总数: &f" + totalPets));
        sender.sendMessage(msg("&e在线符文图鉴数: &f" + totalRunes));
        sender.sendMessage(msg("&e在线点数总和: &f" + totalPoints));
        sender.sendMessage(msg("&8&m----------------------------"));
        return true;
    }
    private boolean onExport(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ldattribute.command.admin")) {
            sender.sendMessage(msg("&c无权限")); return true;
        }
        if (args.length < 2) {
            sender.sendMessage(msg("&7用法: /ldc export <玩家>")); return true;
        }
        org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(args[1]);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage(msg("&c找不到玩家: " + args[1])); return true;
        }
        java.util.UUID uuid = target.getUniqueId();
        String name = target.getName() == null ? args[1] : target.getName();
        StringBuilder sb = new StringBuilder();
        sb.append("# LD-Attribute 数据导出\n");
        sb.append("# 玩家: ").append(name).append("\n");
        sb.append("# UUID: ").append(uuid).append("\n");
        sb.append("# 时间: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())).append("\n\n");
        sb.append("玩家: \"").append(name).append("\"\n");
        sb.append("UUID: \"").append(uuid).append("\"\n\n");
        // 点数
        try { sb.append("点数: ").append(com.longdrange.ldattribute.points.PointAPI.getPlayerPoints(name)).append("\n\n"); } catch (Throwable t) { sb.append("点数: 0\n\n"); }
        // 卡片
        try {
            java.util.List<org.bukkit.inventory.ItemStack> cards = com.longdrange.ldattribute.card.PlayerData.getCards(
                    org.bukkit.Bukkit.getPlayer(uuid) != null ? org.bukkit.Bukkit.getPlayer(uuid) : null);
            sb.append("卡片数: ").append(cards == null ? 0 : cards.size()).append("\n");
            sb.append("卡片:\n");
            if (cards != null) {
                for (org.bukkit.inventory.ItemStack it : cards) {
                    com.longdrange.ldattribute.card.CardData cd = com.longdrange.ldattribute.card.CardDataManager.findCard(it);
                    if (cd == null) continue;
                    sb.append("  - id: \"").append(cd.getId()).append("\"\n");
                    sb.append("    等级: ").append(com.longdrange.ldattribute.card.CardNBT.getLevel(it)).append("\n");
                    sb.append("    经验: ").append(com.longdrange.ldattribute.card.CardNBT.getExp(it)).append("\n");
                    sb.append("    星级: ").append(com.longdrange.ldattribute.card.CardNBT.getStar(it)).append("\n");
                }
            }
            sb.append("\n");
        } catch (Throwable t) { sb.append("卡片: 读取失败 - ").append(t.getMessage()).append("\n\n"); }
        // 宠物
        try {
            java.util.List<com.longdrange.ldattribute.pet.PetInstance> pets = com.longdrange.ldattribute.pet.PetData.getAllPets(uuid);
            sb.append("宠物数: ").append(pets == null ? 0 : pets.size()).append("\n");
            sb.append("宠物:\n");
            if (pets != null) for (com.longdrange.ldattribute.pet.PetInstance pi : pets) {
                sb.append("  - id: \"").append(pi.petId).append("\"\n");
                sb.append("    等级: ").append(pi.level).append("\n");
                sb.append("    经验: ").append(pi.exp).append("\n");
            }
            sb.append("\n");
        } catch (Throwable t) { sb.append("宠物: 读取失败 - ").append(t.getMessage()).append("\n\n"); }
        // 符文
        try {
            java.util.Set<String> runes = com.longdrange.ldattribute.rune.RuneData.get(uuid);
            sb.append("符文数: ").append(runes == null ? 0 : runes.size()).append("\n");
            sb.append("符文: [");
            if (runes != null) sb.append(String.join(", ", runes));
            sb.append("]\n\n");
        } catch (Throwable t) { sb.append("符文: 读取失败\n\n"); }
        // 成就
        try {
            int total = 0;
            for (com.longdrange.ldattribute.achievement.AchievementConfig.Achievement a :
                    com.longdrange.ldattribute.achievement.AchievementConfig.getAll()) {
                int p = com.longdrange.ldattribute.achievement.AchievementData.getProgress(uuid, a.id);
                if (p >= a.target) total++;
            }
            sb.append("成就已达成: ").append(total).append("\n\n");
        } catch (Throwable t) { sb.append("成就: 读取失败\n\n"); }
        // 写文件
        try {
            java.io.File dir = new java.io.File(plugin.getDataFolder(), "exports");
            if (!dir.exists()) dir.mkdirs();
            String ts = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date());
            java.io.File out = new java.io.File(dir, name + "_" + ts + ".yml");
            java.nio.file.Files.write(out.toPath(), sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            sender.sendMessage(msg("&a已导出到: &e" + out.getAbsolutePath()));
        } catch (Throwable t) {
            sender.sendMessage(msg("&c导出失败: " + t.getMessage()));
        }
        return true;
    }
    private boolean onAchievement(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Message.get("Command.Help.PlayerOnly"));
            return true;
        }
        Player p = (Player) sender;
        if (args.length >= 2 && (args[1].equalsIgnoreCase("list") || args[1].equals("列表"))) {
            try {
                com.longdrange.ldattribute.achievement.AchievementChecker.checkSnapshot(p,
                        com.longdrange.ldattribute.achievement.AchievementConfig.Type.CARD_COUNT);
                com.longdrange.ldattribute.achievement.AchievementChecker.checkSnapshot(p,
                        com.longdrange.ldattribute.achievement.AchievementConfig.Type.POINTS);
            } catch (Throwable ignored) {}
            sender.sendMessage(msg("&8&m-------- &6成就列表 &8&m--------"));
            int done = 0, total = 0;
            for (com.longdrange.ldattribute.achievement.AchievementConfig.Achievement a :
                    com.longdrange.ldattribute.achievement.AchievementConfig.getAll()) {
                total++;
                boolean completed = com.longdrange.ldattribute.achievement.AchievementData
                        .isCompleted(p.getUniqueId(), a.id);
                boolean claimed = com.longdrange.ldattribute.achievement.AchievementData
                        .isClaimed(p.getUniqueId(), a.id);
                int progress = com.longdrange.ldattribute.achievement.AchievementData
                        .getProgress(p.getUniqueId(), a.id);
                if (completed) done++;
                if (claimed) {
                    sender.sendMessage(msg("&8[已领] &7" + a.name + " &8[" + a.target + "/" + a.target + "]"));
                } else if (completed) {
                    com.longdrange.ldattribute.util.ClickableList.sendEntry(sender,
                        new com.longdrange.ldattribute.util.ClickableList.Entry(
                            msg("&a[可领] &e" + a.name + " &7[" + a.target + "/" + a.target + "] "),
                            new org.bukkit.inventory.ItemStack(a.icon),
                            "/ldc achievement info " + a.id,
                            "§a§l[查看]"));
                } else {
                    sender.sendMessage(msg("&7[" + progress + "/" + a.target + "] &f" + a.name));
                }
            }
            sender.sendMessage(msg("&8&m-------- &a完成 " + done + "&7/&e" + total + " &8&m--------"));
            return true;
        }
        if (args.length >= 3 && (args[1].equalsIgnoreCase("info") || args[1].equals("详情"))) {
            if (sender instanceof Player) {
                com.longdrange.ldattribute.achievement.AchievementDetailInventory.open((Player) sender, args[2]);
            } else {
                sender.sendMessage(msg("&c玩家限定"));
            }
            return true;
        }
        if (args.length >= 3 && (args[1].equalsIgnoreCase("claim") || args[1].equals("领取"))) {
            boolean ok = com.longdrange.ldattribute.achievement.AchievementManager.claim(p, args[2]);
            if (!ok) sender.sendMessage(msg("&c领取失败：可能未完成或已领取"));
            return true;
        }
        try {
            com.longdrange.ldattribute.achievement.AchievementChecker.checkSnapshot(p,
                    com.longdrange.ldattribute.achievement.AchievementConfig.Type.CARD_COUNT);
            com.longdrange.ldattribute.achievement.AchievementChecker.checkSnapshot(p,
                    com.longdrange.ldattribute.achievement.AchievementConfig.Type.POINTS);
            com.longdrange.ldattribute.achievement.AchievementInventory.open(p);
        } catch (Throwable t) {
            sender.sendMessage("§c成就系统未就绪: " + t.getMessage());
        }
        return true;
    }

    private boolean onGacha(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Message.get("Command.Help.PlayerOnly"));
            return true;
        }
        Player p = (Player) sender;
        try {
            if (args.length >= 2) {
                com.longdrange.ldattribute.gacha.GachaConfig.Gacha g =
                        com.longdrange.ldattribute.gacha.GachaConfig.get(args[1]);
                if (g == null) { sender.sendMessage("§c卡池不存在: " + args[1]); return true; }
                com.longdrange.ldattribute.gacha.GachaManager.Result r =
                        com.longdrange.ldattribute.gacha.GachaManager.draw(p, g);
                sender.sendMessage(r.message);
            } else {
                com.longdrange.ldattribute.gacha.GachaInventory.open(p);
            }
        } catch (Throwable t) {
            sender.sendMessage("§c抽奖系统未就绪: " + t.getMessage());
        }
        return true;
    }

    private boolean onPetEquip(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("list")) {
            sender.sendMessage("§6§l✦ 宠物装备列表 ✦");
            java.util.Collection<com.longdrange.ldattribute.pet.PetEquipmentConfig.Equip> all =
                    com.longdrange.ldattribute.pet.PetEquipmentConfig.getAll();
            if (all.isEmpty()) { sender.sendMessage("§7(空)"); return true; }
            boolean canGiveEquip = sender.hasPermission("ldattribute.command.admin");
            for (com.longdrange.ldattribute.pet.PetEquipmentConfig.Equip e : all) {
                if (canGiveEquip && sender instanceof Player) {
                    com.longdrange.ldattribute.util.ClickableList.sendEntry(sender,
                        new com.longdrange.ldattribute.util.ClickableList.Entry(
                            "§e" + e.id + " §7[" + e.slot + "] §f" + e.name + " ",
                            com.longdrange.ldattribute.pet.PetEquipmentItem.create(e, 1),
                            "/ldc petequip give " + e.id,
                            "§a§l[查看]"));
                } else {
                    sender.sendMessage("§e" + e.id + " §7[" + e.slot + "] §f" + e.name);
                }
            }
            return true;
        }
        if (args[1].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("ldattribute.command.admin")) {
                sender.sendMessage(Message.get("Command.Help.NoPerm"));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage("§c用法: /ldc petequip give <id> [player] [amount]");
                return true;
            }
            com.longdrange.ldattribute.pet.PetEquipmentConfig.Equip e =
                    com.longdrange.ldattribute.pet.PetEquipmentConfig.get(args[2]);
            if (e == null) { sender.sendMessage("§c装备不存在: " + args[2]); return true; }
            Player target;
            if (args.length >= 4) {
                target = Bukkit.getPlayer(args[3]);
                if (target == null) { sender.sendMessage("§c玩家不在线: " + args[3]); return true; }
            } else if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§c请指定玩家");
                return true;
            }
            int amount = 1;
            if (args.length >= 5) {
                try { amount = Integer.parseInt(args[4]); } catch (Exception ignored) {}
            }
            ItemStack item = com.longdrange.ldattribute.pet.PetEquipmentItem.create(e, amount);
            if (item == null) { sender.sendMessage("§c装备生成失败"); return true; }
            target.getInventory().addItem(item);
            sender.sendMessage("§a✓ 已给予 " + target.getName() + " " + amount + " 个 " + e.id);
            return true;
        }
        sender.sendMessage("§c用法: /ldc petequip list | give <id> [player] [amount]");
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
    private boolean onHelpDetail(CommandSender sender, String[] args, String label) {
        String key = CommandConfig.matchSubKey(args[1]);
        if (key == null) { sender.sendMessage(msg("&c未知命令: &e" + args[1])); return true; }
        CommandConfig.SubCmd c = CommandConfig.getSub(key);
        if (c == null) { sender.sendMessage(msg("&c未知命令: &e" + args[1])); return true; }
        sender.sendMessage(msg("&8&m-------- &6命令详情: &e" + c.name + " &8&m--------"));
        sender.sendMessage(msg("&e名称: &f" + c.name));
        if (c.aliases != null && !c.aliases.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String a : c.aliases) {
                if (sb.length() > 0) sb.append("&7, &f");
                sb.append(a);
            }
            sender.sendMessage(msg("&e别名: &f" + sb.toString()));
        }
        if (c.permission != null && !c.permission.isEmpty()) {
            sender.sendMessage(msg("&e权限: &f" + c.permission));
        } else {
            sender.sendMessage(msg("&e权限: &7(无)"));
        }
        sender.sendMessage(msg("&e说明: &f" + (c.description == null ? "" : c.description)));
        sender.sendMessage(msg("&e用法: &f/" + label + " " + c.name));
        sender.sendMessage(msg("&8&m----------------------------"));
        return true;
    }


    private void sendList(CommandSender sender) {
        sender.sendMessage(Message.get("Command.List.Title"));
        boolean canGive = !(sender instanceof Player)
                || sender.hasPermission("ldattribute.command.admin");
        String selfName = sender instanceof Player ? ((Player) sender).getName() : "";

        for (CardData card : CardDataManager.getAllCardOnly()) {
            if (canGive && sender instanceof Player) {
                // OP：可点击领取
                com.longdrange.ldattribute.util.ChatItemLink.send(
                        (Player) sender,
                        msg("&e" + card.getId() + " &7"),
                        card.getItem(),
                        "/ldc giveme " + selfName + " " + card.getId() + " 1",
                        "§a§l[领取]");
            } else {
                sender.sendMessage(msg("&e" + card.getId()));
            }
        }
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
                String lineText = Message.get("Command.Top.Line", i + 1, name, e.getValue(), cnt);
                if (sender instanceof Player) {
                    com.longdrange.ldattribute.util.ClickableList.sendEntry(sender,
                        new com.longdrange.ldattribute.util.ClickableList.Entry(
                            lineText + " ",
                            null,
                            "/ldc player " + name,
                            "§a§l[查看]"));
                } else {
                    sender.sendMessage(lineText);
                }
            }
        }
        if (sender instanceof Player) {
            StringBuilder nav = new StringBuilder();
            if (page > 1) {
                com.longdrange.ldattribute.util.ClickableList.sendEntry(sender,
                    new com.longdrange.ldattribute.util.ClickableList.Entry(
                        msg("&7"),
                        null,
                        "/ldc top " + (page - 1),
                        "§e§l[← 上一页]"));
            }
            sender.sendMessage(Message.get("Command.Top.Footer", page, maxPage));
            if (page < maxPage) {
                com.longdrange.ldattribute.util.ClickableList.sendEntry(sender,
                    new com.longdrange.ldattribute.util.ClickableList.Entry(
                        msg("&7"),
                        null,
                        "/ldc top " + (page + 1),
                        "§e§l[下一页 →]"));
            }
        } else {
            sender.sendMessage(Message.get("Command.Top.Footer", page, maxPage));
        }
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
        // 聊天栏列表
        if (args.length >= 2 && (args[1].equalsIgnoreCase("list") || args[1].equals("列表"))) {
            java.util.Set<String> owned = PlayerData.getOwnedCardIds(player.getUniqueId());
            java.util.Set<String> allIds = CardDataManager.getAllIds();
            sender.sendMessage(msg("&8&m-------- &6卡片图鉴 (" + owned.size() + "/" + allIds.size() + ") &8&m--------"));
            for (String cid : allIds) {
                com.longdrange.ldattribute.card.CardData cd = CardDataManager.getCard(cid);
                if (cd == null) continue;
                if (owned.contains(cid)) {
                    com.longdrange.ldattribute.util.ClickableList.sendEntry(sender,
                        new com.longdrange.ldattribute.util.ClickableList.Entry(
                            msg("&a[✓] &e" + cid + " "),
                            cd.getItem(),
                            "/ldc collection info " + cid,
                            "§a§l[查看]"));
                } else {
                    sender.sendMessage(msg("&7[✗] &7" + cid));
                }
            }
            sender.sendMessage(msg("&8&m----------------------------"));
            return true;
        }
        // 查看详情 GUI
        if (args.length >= 3 && (args[1].equalsIgnoreCase("info") || args[1].equals("详情"))) {
            com.longdrange.ldattribute.card.CardData cd = CardDataManager.getCard(args[2]);
            if (cd == null) { sender.sendMessage(msg("&c卡片不存在: " + args[2])); return true; }
            com.longdrange.ldattribute.card.inventory.CardInfoInventory.open(player, cd, cd.getItem());
            return true;
        }
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
        if (args.length >= 3 && (args[1].equalsIgnoreCase("info") || args[1].equals("查看"))) {
            return onSynergyInfo(sender, args[2]);
        }
        List<org.bukkit.inventory.ItemStack> cards = com.longdrange.ldattribute.card.PlayerData.getCards(p);
        sender.sendMessage(msg("&8&m--------&r &d共鸣/羁绊状态 &8&m--------"));
        for (com.longdrange.ldattribute.card.SynergyData.Synergy s : com.longdrange.ldattribute.card.SynergyData.getAllResonances()) {
            int m = com.longdrange.ldattribute.card.SynergyData.getMatched(cards, s);
            String claim = com.longdrange.ldattribute.card.PlayerData.hasClaimed(p.getUniqueId(), "synergy_" + s.id) ? "§7[已领]" : "§a[未领]";
            com.longdrange.ldattribute.util.ClickableList.sendEntry(sender,
                new com.longdrange.ldattribute.util.ClickableList.Entry(
                    msg("&d共鸣 &f" + s.name + " &7" + m + "/" + s.required + " " + claim + " "),
                    null,
                    "/ldc synergy info " + s.id,
                    "§a§l[查看]"));
        }
        for (com.longdrange.ldattribute.card.SynergyData.Synergy s : com.longdrange.ldattribute.card.SynergyData.getAllBonds()) {
            int m = com.longdrange.ldattribute.card.SynergyData.getMatched(cards, s);
            String claim = com.longdrange.ldattribute.card.PlayerData.hasClaimed(p.getUniqueId(), "synergy_" + s.id) ? "§7[已领]" : "§a[未领]";
            com.longdrange.ldattribute.util.ClickableList.sendEntry(sender,
                new com.longdrange.ldattribute.util.ClickableList.Entry(
                    msg("&a羁绊 &f" + s.name + " &7" + m + "/" + s.required + " " + claim + " "),
                    null,
                    "/ldc synergy info " + s.id,
                    "§a§l[查看]"));
        }
        sender.sendMessage(msg("&8&m------------------------------------"));
        return true;
    }

    private boolean onSynergyInfo(CommandSender sender, String synId) {
        if (sender instanceof Player) {
            com.longdrange.ldattribute.card.inventory.SynergyDetailInventory.open((Player) sender, synId);
            return true;
        }
        if (!(sender instanceof Player)) { sender.sendMessage(msg("&c玩家限定")); return true; }
        Player p = (Player) sender;
        com.longdrange.ldattribute.card.SynergyData.Synergy found = null;
        boolean isBond = false;
        for (com.longdrange.ldattribute.card.SynergyData.Synergy s : com.longdrange.ldattribute.card.SynergyData.getAllResonances()) {
            if (s.id.equalsIgnoreCase(synId)) { found = s; break; }
        }
        if (found == null) {
            for (com.longdrange.ldattribute.card.SynergyData.Synergy s : com.longdrange.ldattribute.card.SynergyData.getAllBonds()) {
                if (s.id.equalsIgnoreCase(synId)) { found = s; isBond = true; break; }
            }
        }
        if (found == null) { sender.sendMessage(msg("&c未找到共鸣/羁绊: " + synId)); return true; }
        List<org.bukkit.inventory.ItemStack> cards = com.longdrange.ldattribute.card.PlayerData.getCards(p);
        int m = com.longdrange.ldattribute.card.SynergyData.getMatched(cards, found);
        boolean active = m >= found.required;
        boolean claimed = com.longdrange.ldattribute.card.PlayerData.hasClaimed(p.getUniqueId(), "synergy_" + found.id);
        sender.sendMessage(msg("&8&m-------- &6详情: " + found.name + " &8&m--------"));
        sender.sendMessage(msg("&e类型: &f" + (isBond ? "羁绊" : "共鸣")));
        sender.sendMessage(msg("&e需要卡片 (&f" + m + "&7/&f" + found.required + "&e):"));
        for (String cid : found.cards) {
            boolean has = false;
            try {
                com.longdrange.ldattribute.card.CardData target = com.longdrange.ldattribute.card.CardDataManager.getCard(cid);
                if (target != null) {
                    for (org.bukkit.inventory.ItemStack it : cards) {
                        if (target.matches(it)) { has = true; break; }
                    }
                }
            } catch (Throwable ignored) {}
            sender.sendMessage(msg("  " + (has ? "&a✓ " : "&c✗ ") + "&f" + cid));
        }
        if (found.attributes != null && !found.attributes.isEmpty()) {
            sender.sendMessage(msg("&e属性加成:"));
            for (String a : found.attributes) {
                sender.sendMessage(msg("  " + a));
            }
        }
        sender.sendMessage(msg("&e状态: " + (active ? "&a✦ 已激活" : "&c未激活")));
        sender.sendMessage(msg("&e奖励: " + (claimed ? "&7已领取" : (active ? "&a可领取" : "&7未达成"))));
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
            case "list": {
                sender.sendMessage(msg("&8&m-------- &d宠物列表 &8&m--------"));
                boolean canGivePet = sender.hasPermission("ldattribute.command.admin");
                for (com.longdrange.ldattribute.pet.PetConfig.Pet def :
                        com.longdrange.ldattribute.pet.PetConfig.getAll()) {
                    if (canGivePet && sender instanceof Player) {
                        com.longdrange.ldattribute.util.ClickableList.sendEntry(sender,
                            new com.longdrange.ldattribute.util.ClickableList.Entry(
                                msg("&e" + def.id + " &7- " + def.name + " &7[" + def.rarity + "] "),
                                com.longdrange.ldattribute.pet.PetManager.createPetEgg(def.id),
                                "/ldc pet give " + def.id,
                                "§a§l[查看]"));
                    } else {
                        sender.sendMessage(msg("&e" + def.id + " &7- " + def.name + " &7[" + def.rarity + "]"));
                    }
                }
                sender.sendMessage(msg("&8&m----------------------------"));
                return true;
            }
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
                boolean canGiveRune = sender.hasPermission("ldattribute.command.admin");
                for (com.longdrange.ldattribute.rune.RuneConfig.Rune r :
                        com.longdrange.ldattribute.rune.RuneConfig.getAllRunes()) {
                    if (canGiveRune && sender instanceof Player) {
                        com.longdrange.ldattribute.util.ClickableList.sendEntry(sender,
                            new com.longdrange.ldattribute.util.ClickableList.Entry(
                                msg("&e" + r.id + " &7- " + r.name + " &7[" + r.type + "] "),
                                com.longdrange.ldattribute.rune.RuneItem.create(r, 1),
                                "/ldc rune give " + r.id,
                                "§a§l[查看]"));
                    } else {
                        sender.sendMessage(msg("&e" + r.id + " &7- " + r.name + " &7[" + r.type + "]"));
                    }
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
            return result;
        }

        String key = CommandConfig.matchSubKey(args[0]);
        if (key == null) return result;

        // ============ 第 2 参数 ============
        if (args.length == 2) {
            String pre = args[1].toLowerCase();

            if ("Cast".equals(key)) {
                for (String id : com.longdrange.ldattribute.spell.SpellConfig.getIds())
                    if (id.toLowerCase().startsWith(pre)) result.add(id);
            } else if ("PetEquip".equals(key)) {
                for (String s : new String[]{"list", "give"})
                    if (s.startsWith(pre)) result.add(s);
            } else if ("Gacha".equals(key)) {
                for (String s : new String[]{"list", "draw", "open", "reload"})
                    if (s.startsWith(pre)) result.add(s);
            } else if ("Rune".equals(key)) {
                for (String s : new String[]{"list", "give", "info"})
                    if (s.startsWith(pre)) result.add(s);
            } else if ("Achievement".equals(key)) {
                for (String s : new String[]{"list", "info", "claim"})
                    if (s.startsWith(pre)) result.add(s);
            } else if ("Pet".equals(key)) {
                for (String s : new String[]{"list", "open", "info"})
                    if (s.startsWith(pre)) result.add(s);
            } else {
                // 默认第 2 参数：玩家名
                for (Player p : Bukkit.getOnlinePlayers())
                    if (p.getName().toLowerCase().startsWith(pre)) result.add(p.getName());
            }
            return result;
        }

        // ============ 第 3 参数 ============
        if (args.length == 3) {
            String pre = args[2].toLowerCase();

            if ("Give".equals(key) || "Remove".equals(key) || "Info".equals(key)
                    || "Star".equals(key) || "Level".equals(key) || "Exp".equals(key)
                    || "ChatItem".equals(key) || "GiveMe".equals(key)) {
                for (String id : CardDataManager.getAllIds())
                    if (id.toLowerCase().startsWith(pre)) result.add(id);
            } else if ("Unlock".equals(key)) {
                int total = PageConfig.getPageCount();
                for (int i = 1; i <= total; i++) {
                    String s = String.valueOf(i);
                    if (s.startsWith(pre)) result.add(s);
                }
            } else if ("PetEquip".equals(key)) {
                for (com.longdrange.ldattribute.pet.PetEquipmentConfig.Equip e :
                        com.longdrange.ldattribute.pet.PetEquipmentConfig.getAll()) {
                    if (e.id.toLowerCase().startsWith(pre)) result.add(e.id);
                }
            } else if ("Gacha".equals(key)) {
                for (com.longdrange.ldattribute.gacha.GachaConfig.Gacha g :
                        com.longdrange.ldattribute.gacha.GachaConfig.getAll()) {
                    if (g.id.toLowerCase().startsWith(pre)) result.add(g.id);
                }
            }
            return result;
        }

        // ============ 第 4 参数 ============
        if (args.length == 4) {
            String pre = args[3].toLowerCase();

            if ("Give".equals(key) || "ChatItem".equals(key) || "GiveMe".equals(key)
                    || "PetEquip".equals(key)) {
                for (Player p : Bukkit.getOnlinePlayers())
                    if (p.getName().toLowerCase().startsWith(pre)) result.add(p.getName());
            }
            return result;
        }

        return result;
    }
}
