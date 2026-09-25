package com.longdrange.ldattribute.core.soulring.gui;

import com.longdrange.ldattribute.core.soulring.SoulRingData;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.*;

public class SoulRingTrashHolder implements InventoryHolder {

    private final UUID owner;
    private int page = 0;
    private final Set<SoulRingData.Entry> marked = new LinkedHashSet<>();
    private Inventory inv;
    private List<SoulRingData.Entry> pageEntries = new ArrayList<>();

    public SoulRingTrashHolder(UUID owner) { this.owner = owner; }

    public UUID getOwner() { return owner; }
    public int getPage() { return page; }
    public void setPage(int p) { this.page = p; }
    public Set<SoulRingData.Entry> getMarked() { return marked; }
    public List<SoulRingData.Entry> getPageEntries() { return pageEntries; }
    public void setPageEntries(List<SoulRingData.Entry> list) { this.pageEntries = list; }

    @Override public Inventory getInventory() { return inv; }
    public void setInventory(Inventory inv) { this.inv = inv; }
}
