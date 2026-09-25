package com.longdrange.ldattribute.core.jewelry.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class JewelryHolder implements InventoryHolder {
    private final UUID owner;
    private final String pageId;
    private Inventory inv;
    public JewelryHolder(UUID owner, String pageId) { this.owner = owner; this.pageId = pageId; }
    public UUID getOwner() { return owner; }
    public String getPageId() { return pageId; }
    @Override public Inventory getInventory() { return inv; }
    public void setInventory(Inventory inv) { this.inv = inv; }
}