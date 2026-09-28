package com.longdrange.ldattribute.core.relic.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class RelicHolder implements InventoryHolder {

    public final UUID owner;
    private Inventory inv;

    public RelicHolder(UUID owner) { this.owner = owner; }

    @Override public Inventory getInventory() { return inv; }
    public void setInventory(Inventory inv) { this.inv = inv; }
}