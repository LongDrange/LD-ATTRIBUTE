package com.longdrange.ldattribute.core.guide.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class GuideOverviewHolder implements InventoryHolder {

    private final UUID owner;
    private int page;
    private Inventory inv;

    public GuideOverviewHolder(UUID owner, int page) {
        this.owner = owner; this.page = page;
    }

    public UUID getOwner() { return owner; }
    public int getPage() { return page; }
    public void setPage(int p) { this.page = p; }

    @Override public Inventory getInventory() { return inv; }
    public void setInventory(Inventory inv) { this.inv = inv; }
}