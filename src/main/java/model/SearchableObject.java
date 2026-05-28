package model;

import java.util.List;

/**
 * Decorative map fixture that can hide at most one item.
 */
public class SearchableObject extends StaticObject {

    private final String spriteResource;
    private Item hiddenItem;

    public SearchableObject(String name, boolean blocking, String spriteResource) {
        this(name, blocking, spriteResource, null);
    }

    public SearchableObject(String name, boolean blocking, String spriteResource, Item hiddenItem) {
        super(name, blocking);
        this.spriteResource = spriteResource;
        this.hiddenItem = hiddenItem;
    }

    public Item getHiddenItem() {
        return hiddenItem;
    }

    public void setHiddenItem(Item hiddenItem) {
        this.hiddenItem = hiddenItem;
    }

    public Item takeHiddenItem() {
        Item item = hiddenItem;
        hiddenItem = null;
        return item;
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.SEARCH);
    }

    @Override
    public String spriteResource() {
        return spriteResource;
    }
}
