package com.longdrange.ldattribute.core.dungeon.gui;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.dungeon.DungeonConfig;
import com.longdrange.ldattribute.core.dungeon.DungeonTeam;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class TeamGUI {

    // 主界面按钮位置
    public static final int SLOT_CREATE    = 22;
    public static final int SLOT_INVITE    = 19;
    public static final int SLOT_KICK      = 21;
    public static final int SLOT_LEAVE     = 23;
    public static final int SLOT_REFRESH   = 49;

    // 邀请界面按钮位置
    public static final int SLOT_INVITE_PREV = 45;
    public static final int SLOT_INVITE_BACK = 49;
    public static final int SLOT_INVITE_NEXT = 53;

    private final LDAttribute plugin;
    public TeamGUI(LDAttribute plugin) { this.plugin = plugin; }

    // ==================== 主界面 ====================
    public void openMain(Player p) {
        openMain(p, null);
    }

    public void openMain(Player p, UUID selected) {
        DungeonTeam team = plugin.getDungeonTeamManager().getTeam(p.getUniqueId());

        String title;
        if (team == null) {
            title = ChatColor.DARK_GRAY + "✦ 组队 " + ChatColor.GRAY + "(未组队)";
        } else {
            title = ChatColor.DARK_GRAY + "✦ 组队 " + ChatColor.GRAY
                    + "(" + team.size() + "/" + DungeonConfig.maxTeamSize + ")";
        }

        TeamHolder holder = new TeamHolder(TeamHolder.Mode.MAIN, p.getUniqueId());
        holder.setSelectedMember(selected);
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        if (team == null) {
            // 未在队伍：显示创建按钮
            inv.setItem(SLOT_CREATE, icon(Material.EMERALD_BLOCK,
                    ChatColor.GREEN + "✔ 创建队伍",
                    ChatColor.GRAY + "点击创建你自己的队伍",
                    ChatColor.GRAY + "然后邀请其他玩家加入"));
        } else {
            // 队伍成员头像（9~17 格）
            int slot = 9;
            for (UUID u : team.members) {
                if (slot >= 18) break;
                Player m = Bukkit.getPlayer(u);
                String name = (m != null) ? m.getName() : Bukkit.getOfflinePlayer(u).getName();
                if (name == null) name = "(离线)";

                boolean isLeader = team.isLeader(u);
                boolean isSelected = u.equals(selected);

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "UUID: " + u.toString().substring(0, 8) + "...");
                if (isLeader) lore.add(ChatColor.GOLD + "★ 队长");
                lore.add("");
                if (isSelected) {
                    lore.add(ChatColor.RED + "▶ 已选中");
                    lore.add(ChatColor.GRAY + "点击取消选择");
                } else {
                    lore.add(ChatColor.GRAY + "左键选中");
                }

                ItemStack head = playerHead(name);
                ItemMeta meta = head.getItemMeta();
                if (meta != null) {
                    String prefix = isSelected ? ChatColor.RED + "▶ " : (isLeader ? ChatColor.GOLD + "★ " : ChatColor.WHITE + "  ");
                    meta.setDisplayName(prefix + name);
                    meta.setLore(lore);
                    head.setItemMeta(meta);
                }
                inv.setItem(slot, head);
                slot++;
            }

            // 按钮
            boolean isLeader = team.isLeader(p.getUniqueId());

            if (isLeader) {
                inv.setItem(SLOT_INVITE, icon(Material.ENDER_PEARL,
                        ChatColor.AQUA + "邀请玩家",
                        ChatColor.GRAY + "点击打开在线玩家列表"));

                if (selected != null && !selected.equals(p.getUniqueId())) {
                    String selName = Bukkit.getOfflinePlayer(selected).getName();
                    inv.setItem(SLOT_KICK, icon(Material.IRON_SWORD,
                            ChatColor.RED + "踢出 " + selName,
                            ChatColor.GRAY + "点击踢出选中的成员"));
                } else {
                    inv.setItem(SLOT_KICK, glass((short) 15,
                            ChatColor.DARK_GRAY + "先选中一个成员"));
                }

                inv.setItem(SLOT_LEAVE, icon(Material.BARRIER,
                        ChatColor.RED + "解散队伍",
                        ChatColor.GRAY + "点击解散队伍（所有成员离开）"));
            } else {
                inv.setItem(SLOT_INVITE, glass((short) 7,
                        ChatColor.DARK_GRAY + "只有队长能邀请"));

                inv.setItem(SLOT_KICK, glass((short) 7,
                        ChatColor.DARK_GRAY + "只有队长能踢人"));

                inv.setItem(SLOT_LEAVE, icon(Material.BARRIER,
                        ChatColor.YELLOW + "离开队伍",
                        ChatColor.GRAY + "点击离开当前队伍"));
            }
        }

        // 刷新
        inv.setItem(SLOT_REFRESH, icon(Material.NETHER_STAR,
                ChatColor.YELLOW + "刷新",
                ChatColor.GRAY + "点击刷新队伍信息"));

        p.openInventory(inv);
    }

    // ==================== 邀请界面 ====================
    public void openInvite(Player p, int page) {
        DungeonTeam team = plugin.getDungeonTeamManager().getTeam(p.getUniqueId());
        if (team == null || !team.isLeader(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "只有队长能邀请");
            return;
        }

        // 收集可邀请的在线玩家
        List<UUID> candidates = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (team.has(online.getUniqueId())) continue;
            if (plugin.getDungeonTeamManager().getTeam(online.getUniqueId()) != null) continue;
            candidates.add(online.getUniqueId());
        }

        int perPage = 36;
        int totalPages = Math.max(1, (candidates.size() + perPage - 1) / perPage);
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        String title = ChatColor.DARK_GRAY + "✦ 邀请玩家 " + ChatColor.GRAY
                + "(" + (page + 1) + "/" + totalPages + ")";

        TeamHolder holder = new TeamHolder(TeamHolder.Mode.INVITE, p.getUniqueId());
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        List<UUID> pageList = new ArrayList<>();
        int start = page * perPage;
        for (int i = 0; i < perPage; i++) {
            int idx = start + i;
            if (idx >= candidates.size()) break;
            UUID u = candidates.get(idx);
            Player m = Bukkit.getPlayer(u);
            String name = (m != null) ? m.getName() : "???";

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "在线玩家");
            lore.add("");
            lore.add(ChatColor.GREEN + "点击邀请加入队伍");

            ItemStack head = playerHead(name);
            ItemMeta meta = head.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.WHITE + name);
                meta.setLore(lore);
                head.setItemMeta(meta);
            }
            inv.setItem(i, head);
            pageList.add(u);
        }
        holder.setInviteList(pageList);

        // 翻页
        if (page > 0) inv.setItem(SLOT_INVITE_PREV, icon(Material.ARROW, ChatColor.GRAY + "← 上一页"));
        else inv.setItem(SLOT_INVITE_PREV, glass((short) 15, ChatColor.DARK_GRAY + "已是首页"));

        if (page < totalPages - 1) inv.setItem(SLOT_INVITE_NEXT, icon(Material.ARROW, ChatColor.GRAY + "下一页 →"));
        else inv.setItem(SLOT_INVITE_NEXT, glass((short) 15, ChatColor.DARK_GRAY + "已是末页"));

        inv.setItem(SLOT_INVITE_BACK, icon(Material.BARRIER, ChatColor.GRAY + "← 返回队伍"));

        p.openInventory(inv);
    }

    // ==================== 工具 ====================
    private ItemStack playerHead(String playerName) {
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null && playerName != null && !playerName.isEmpty()) {
            try {
                meta.setOwner(playerName);
            } catch (Throwable ignored) {}
        }
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack icon(Material mat, String name, String... lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            if (lore != null && lore.length > 0) m.setLore(Arrays.asList(lore));
            it.setItemMeta(m);
        }
        return it;
    }

    private ItemStack glass(short data, String name) {
        ItemStack it = new ItemStack(Material.STAINED_GLASS_PANE, 1, data);
        ItemMeta m = it.getItemMeta();
        if (m != null) { m.setDisplayName(name); it.setItemMeta(m); }
        return it;
    }
}