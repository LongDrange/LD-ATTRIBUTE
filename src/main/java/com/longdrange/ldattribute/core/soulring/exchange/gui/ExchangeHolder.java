package com.longdrange.ldattribute.core.soulring.exchange.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class ExchangeHolder implements InventoryHolder {
    private final UUID owner;
    private final String pageId;
    private Inventory inv;

    public ExchangeHolder(UUID owner, String pageId) { this.owner = owner; this.pageId = pageId; }
    public UUID getOwner() { return owner; }
    public String getPageId() { return pageId; }

    @Override public Inventory getInventory() { return inv; }
    public void setInventory(Inventory inv) { this.inv = inv; }
}