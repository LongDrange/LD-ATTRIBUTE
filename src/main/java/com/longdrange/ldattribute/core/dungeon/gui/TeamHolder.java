package com.longdrange.ldattribute.core.dungeon.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeamHolder implements InventoryHolder {

    public enum Mode { MAIN, INVITE }

    public final Mode mode;
    public final UUID owner;
    private Inventory inv;
    private List<UUID> inviteList = new ArrayList<>();
    private UUID selectedMember = null;

    public TeamHolder(Mode mode, UUID owner) {
        this.mode = mode; this.owner = owner;
    }

    public UUID getOwner() { return owner; }
    public Mode getMode() { return mode; }
    public List<UUID> getInviteList() { return inviteList; }
    public void setInviteList(List<UUID> list) { this.inviteList = list; }
    public UUID getSelectedMember() { return selectedMember; }
    public void setSelectedMember(UUID u) { this.selectedMember = u; }

    @Override public Inventory getInventory() { return inv; }
    public void setInventory(Inventory inv) { this.inv = inv; }
}