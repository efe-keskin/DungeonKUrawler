package model;

import java.util.List;

/**
 * Static scenery that can be destroyed. It may hide one item revealed by the
 * break action, but it is not directly searchable.
 */
public class BreakableObject extends StaticObject {

    private final String spriteResource;
    private Item hiddenItem;

    public BreakableObject(String name, boolean blocking, String spriteResource) {
        this(name, blocking, spriteResource, null);
    }

    public BreakableObject(String name, boolean blocking, String spriteResource, Item hiddenItem) {
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
        return List.of(ItemAction.BREAK);
    }

    @Override
    public String spriteResource() {
        return spriteResource;
    }
}
