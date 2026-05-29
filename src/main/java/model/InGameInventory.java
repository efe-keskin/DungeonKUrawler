package model;

import java.util.ArrayList;
import java.util.List;

/**
 * The per-level bag carried during a single floor session (GRASP Information
 * Expert). Inherits the fixed-capacity rules of {@link Inventory}; what makes it
 * "in-game" is its lifetime: it is recreated empty on every floor, so temporary
 * items (potions, keys, level-use objects) do not persist.
 *
 * <p>On floor completion the bag decides which of its contents survive into the
 * run-wide {@link FullGameInventory}; see {@link #drainValuables()} and
 * {@link Hero#commitLevelLoot()}.
 */
public class InGameInventory extends Inventory {

    public InGameInventory(int capacity) {
        super(capacity);
    }

    /**
     * Removes and returns the {@link ValuableItem}s held in this bag — the loot
     * that persists past the level. Coins are not stored here (they go straight
     * to the gold balance), so only valuables are drained.
     *
     * <p>modifies: this — the returned valuables are removed.
     */
    public List<ValuableItem> drainValuables() {
        List<ValuableItem> drained = new ArrayList<>();
        for (Item item : new ArrayList<>(getItems())) {
            if (item instanceof ValuableItem valuable) {
                drained.add(valuable);
                remove(item);
            }
        }
        return drained;
    }
}
