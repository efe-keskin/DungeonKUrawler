package model;

import java.util.List;

/**
 * Collectible readable scroll. Distinct from {@link Book}: scrolls are
 * functional readables (e.g. {@link ShadowCloneScroll}) backed by scroll art,
 * whereas a {@link Book} is the win-condition clue. Both share the
 * {@link Readable} role.
 */
public class Scroll extends Item implements Readable {

    private final String text;

    public Scroll(String name, String text) {
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
