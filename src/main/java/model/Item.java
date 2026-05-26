package model;

import java.util.List;

/**
 * Base type for anything that can sit on a {@link GridCell} or inside an {@link Inventory}.
 */
public abstract class Item {

    protected String name;

    public Item(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Whether this item can be picked up by the hero.
     * Subclasses may override to return {@code false} for fixtures (e.g. bolted chests).
     */
    public boolean isTakable() {
        return true;
    }

    /**
     * Whether this item physically blocks the hero from entering its tile.
     * Default {@code false} — loose loot is walk-through. Fixtures like
     * chests/crates override this.
     */
    public boolean isBlocking() {
        return false;
    }

    /**
     * Inventory actions supported by this item. Loose collectibles can always
     * be discarded; specialized items add their own actions.
     */
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.DISCARD);
    }
}
