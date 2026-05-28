package model;

import java.util.List;


public class Crate extends SearchableObject {

    public Crate() {
        this(null);
    }

    public Crate(Item hiddenItem) {
        super("Wooden Crate", true, null, hiddenItem);
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.SEARCH, ItemAction.BREAK);
    }
}
