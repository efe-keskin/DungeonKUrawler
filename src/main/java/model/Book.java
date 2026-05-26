package model;

import java.util.List;

/**
 * Collectible readable object whose contents can be viewed from inventory.
 */
public class Book extends Item {

    private final String text;

    public Book(String name, String text) {
        super(name);
        this.text = text;
    }

    public String read() {
        return text;
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.READ, ItemAction.DISCARD);
    }
}
