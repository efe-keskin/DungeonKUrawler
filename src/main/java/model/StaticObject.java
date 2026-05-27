package model;

import java.util.List;

/**
 * Fixture sitting permanently on a {@link GridCell}. Never picked up; instead
 * the hero interacts with it through actions like {@link ItemAction#SEARCH} or
 * {@link ItemAction#BREAK}. Subclasses declare which actions they support.
 */
public abstract class StaticObject extends Item {

    private final boolean blocking;

    protected StaticObject(String name, boolean blocking) {
        super(name);
        this.blocking = blocking;
    }

    @Override
    public final boolean isTakable() {
        return false;
    }

    @Override
    public boolean isBlocking() {
        return blocking;
    }

    @Override
    public abstract List<ItemAction> getInventoryActions();
}
