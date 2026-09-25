package com.longdrange.ldattribute.core.soulring.gui;

import com.longdrange.ldattribute.core.soulring.SoulRingData;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SoulRingHolder implements InventoryHolder {

    public enum SortMode {
        DEFAULT("默认"),
        NAME_ASC("名称↑"),
        NAME_DESC("名称↓"),
        COUNT_ASC("数量↑"),
        COUNT_DESC("数量↓"),
        TYPE("类型");

        public final String label;
        SortMode(String label) { this.label = label; }
        public SortMode next() {
            SortMode[] arr = values();
            return arr[(ordinal() + 1) % arr.length];
        }
    }

    private final UUID owner;
    private int page;
    private final String filter;
    private int categoryIndex = 0;
    private SortMode sortMode = SortMode.DEFAULT;
    private Inventory inv;
    private List<SoulRingData.Entry> pageEntries = new ArrayList<>();

    public SoulRingHolder(UUID owner, int page, String filter) {
        this.owner = owner; this.page = page; this.filter = filter;
    }

    public UUID getOwner() { return owner; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public String getFilter() { return filter; }
    public int getCategoryIndex() { return categoryIndex; }
    public void setCategoryIndex(int i) { this.categoryIndex = i; }
    public SortMode getSortMode() { return sortMode; }
    public void setSortMode(SortMode s) { this.sortMode = s; }
    public List<SoulRingData.Entry> getPageEntries() { return pageEntries; }
    public void setPageEntries(List<SoulRingData.Entry> list) { this.pageEntries = list; }

    @Override public Inventory getInventory() { return inv; }
    public void setInventory(Inventory inv) { this.inv = inv; }
}