package com.longdrange.ldattribute.core.dungeon.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DungeonHolder implements InventoryHolder {

    private final UUID owner;
    private int page;
    private Inventory inv;
    private List<String> pageIds = new ArrayList<>();

    public DungeonHolder(UUID owner, int page) {
        this.owner = owner; this.page = page;
    }

    public UUID getOwner() { return owner; }
    public int getPage() { return page; }
    public void setPage(int p) { this.page = p; }
    public List<String> getPageIds() { return pageIds; }
    public void setPageIds(List<String> ids) { this.pageIds = ids; }

    @Override public Inventory getInventory() { return inv; }
    public void setInventory(Inventory inv) { this.inv = inv; }
}