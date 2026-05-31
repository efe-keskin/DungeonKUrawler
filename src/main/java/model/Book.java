package model;

import java.util.List;

/**
 * Collectible readable book. Used as the win-condition clue: a book is hidden
 * on the map and its text points the hero toward where the target valuable is
 * stashed. Distinct from {@link Scroll}; both share the {@link Readable} role.
 */
public class Book extends Item implements Readable {

    private final String text;

    public Book(String name, String text) {
        super(name);
        this.text = text;
    }

    @Override
    public String read() {
        return text;
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.READ, ItemAction.DISCARD);
    }
}
