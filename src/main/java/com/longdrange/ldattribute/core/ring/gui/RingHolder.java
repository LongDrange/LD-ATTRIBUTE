package com.longdrange.ldattribute.core.ring.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class RingHolder implements InventoryHolder {

    private final UUID owner;
    private final int page;
    private Inventory inventory;

    public RingHolder(UUID owner, int page) {
        this.owner = owner;
        this.page = page;
    }

    public UUID getOwner() { return owner; }
    public int getPage() { return page; }

    @Override
    public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inv) { this.inventory = inv; }
}