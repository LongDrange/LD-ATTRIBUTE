package com.longdrange.ldattribute.core.guide.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class GuideHolder implements InventoryHolder {
    private final UUID owner;
    private final String groupId;
    private final int page;
    private Inventory inv;
    public GuideHolder(UUID owner, String groupId, int page) {
        this.owner = owner; this.groupId = groupId; this.page = page;
    }
    public UUID getOwner() { return owner; }
    public String getGroupId() { return groupId; }
    public int getPage() { return page; }
    @Override public Inventory getInventory() { return inv; }
    public void setInventory(Inventory inv) { this.inv = inv; }
}