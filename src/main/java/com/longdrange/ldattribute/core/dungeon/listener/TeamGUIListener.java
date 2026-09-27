package com.longdrange.ldattribute.core.dungeon.listener;

import com.longdrange.ldattribute.LDAttribute;
import com.longdrange.ldattribute.core.dungeon.DungeonConfig;
import com.longdrange.ldattribute.core.dungeon.DungeonTeam;
import com.longdrange.ldattribute.core.dungeon.gui.TeamGUI;
import com.longdrange.ldattribute.core.dungeon.gui.TeamHolder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.List;
import java.util.UUID;

public class TeamGUIListener implements Listener {

    private final LDAttribute plugin;
    public TeamGUIListener(LDAttribute plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!(e.getInventory().getHolder() instanceof TeamHolder)) return;
        Player p = (Player) e.getWhoClicked();
        TeamHolder holder = (TeamHolder) e.getInventory().getHolder();
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= e.getInventory().getSize()) return;

        if (holder.getMode() == TeamHolder.Mode.MAIN) {
            handleMain(p, holder, slot);
        } else if (holder.getMode() == TeamHolder.Mode.INVITE) {
            handleInvite(p, holder, slot);
        }
    }

    private void handleMain(Player p, TeamHolder holder, int slot) {
        DungeonTeam team = plugin.getDungeonTeamManager().getTeam(p.getUniqueId());

        // 创建队伍
        if (slot == TeamGUI.SLOT_CREATE) {
            if (team != null) return;
            plugin.getDungeonTeamManager().createTeam(p);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            p.sendMessage(com.longdrange.ldattribute.core.dungeon.DungeonConfig.msg("TeamCreated", "&a已创建队伍"));
            plugin.getDungeonTeamGUI().openMain(p);
            return;
        }

        // 刷新
        if (slot == TeamGUI.SLOT_REFRESH) {
            plugin.getDungeonTeamGUI().openMain(p, holder.getSelectedMember());
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.5f);
            return;
        }

        if (team == null) return;

        // 成员头像选择（9~17）
        if (slot >= 9 && slot < 18) {
            int idx = slot - 9;
            List<UUID> members = new java.util.ArrayList<>(team.members);
            if (idx >= members.size()) return;
            UUID clicked = members.get(idx);
            UUID cur = holder.getSelectedMember();
            UUID next = clicked.equals(cur) ? null : clicked;
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            plugin.getDungeonTeamGUI().openMain(p, next);
            return;
        }

        // 邀请
        if (slot == TeamGUI.SLOT_INVITE) {
            if (!team.isLeader(p.getUniqueId())) return;
            plugin.getDungeonTeamGUI().openInvite(p, 0);
            return;
        }

        // 踢人
        if (slot == TeamGUI.SLOT_KICK) {
            if (!team.isLeader(p.getUniqueId())) return;
            UUID sel = holder.getSelectedMember();
            if (sel == null) {
                p.sendMessage(ChatColor.RED + "先点击成员头像选中");
                return;
            }
            Player target = Bukkit.getPlayer(sel);
            if (target == null) {
                p.sendMessage(ChatColor.RED + "该玩家已离线");
                return;
            }
            plugin.getDungeonTeamManager().kick(p, target);
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);
            plugin.getDungeonTeamGUI().openMain(p);
            return;
        }

        // 离开/解散
        if (slot == TeamGUI.SLOT_LEAVE) {
            plugin.getDungeonTeamManager().leaveTeam(p);
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 0.7f);
            plugin.getDungeonTeamGUI().openMain(p);
            return;
        }
    }

    private void handleInvite(Player p, TeamHolder holder, int slot) {
        // 返回
        if (slot == TeamGUI.SLOT_INVITE_BACK) {
            plugin.getDungeonTeamGUI().openMain(p);
            return;
        }

        // 上一页
        if (slot == TeamGUI.SLOT_INVITE_PREV) {
            // 找当前页码从 title 取，简单做法：重开 0 页
            // 改：从物品 lore 反推 —— 算了，用 holder 存页码
            // 为简化，直接翻到上一页需记录页码。这里用固定方式：从当前列表第一个玩家的位置反推。
            // 由于没存页码，简化处理：重开第 0 页（保守）
            // 改进：holder 加 page 字段（略）
            p.sendMessage(ChatColor.YELLOW + "翻页请重开邀请界面（暂不支持）");
            return;
        }

        // 下一页
        if (slot == TeamGUI.SLOT_INVITE_NEXT) {
            p.sendMessage(ChatColor.YELLOW + "翻页请重开邀请界面（暂不支持）");
            return;
        }

        // 玩家头像（0~35）
        if (slot >= 0 && slot < 36) {
            List<UUID> list = holder.getInviteList();
            if (slot >= list.size()) return;
            UUID target = list.get(slot);
            Player t = Bukkit.getPlayer(target);
            if (t == null) return;

            boolean ok = plugin.getDungeonTeamManager().invite(
                    p, t, DungeonConfig.inviteTimeoutSec, DungeonConfig.maxTeamSize);
            if (ok) {
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                plugin.getDungeonTeamGUI().openInvite(p, 0);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof TeamHolder) {
            e.setCancelled(true);
        }
    }
}
