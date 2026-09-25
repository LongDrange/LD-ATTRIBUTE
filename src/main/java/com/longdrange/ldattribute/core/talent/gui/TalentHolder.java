package com.longdrange.ldattribute.core.talent.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class TalentHolder implements InventoryHolder {
    private final UUID owner;
    private final String pageId;
    private Inventory inv;
    public TalentHolder(UUID owner, String pageId) { this.owner = owner; this.pageId = pageId; }
    public UUID getOwner() { return owner; }
    public String getPageId() { return pageId; }
    @Override public Inventory getInventory() { return inv; }
    public void setInventory(Inventory inv) { this.inv = inv; }
}