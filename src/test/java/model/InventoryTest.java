package model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Inventory#tryAdd(Item)}.
 *
 * requires: No special precondition. item may be null.
 * modifies: this.items
 * effects:
 *   If item is null, returns false and inventory does not change.
 *   If inventory is full, returns false and item is not added.
 *   Otherwise, adds item to inventory and returns true.
 */
class InventoryTest {

    /**
     * Case: normal add into a non-full inventory.
     * Expected: returns true, size grows by one, and the item is present in the inventory's view.
     */
    @Test
    void tryAdd_normalItem_returnsTrueAndGrowsSize() {
        Inventory inventory = new Inventory(3);
        Item potion = new HealPotion();

        boolean added = inventory.tryAdd(potion);

        assertTrue(added);
        assertEquals(1, inventory.size());
        assertTrue(inventory.getItems().contains(potion));
    }

    /**
     * Case: null argument.
     * Expected: returns false and inventory contents are unchanged.
     */
    @Test
    void tryAdd_nullItem_returnsFalseAndDoesNotChangeInventory() {
        Inventory inventory = new Inventory(2);

        boolean added = inventory.tryAdd(null);

        assertFalse(added);
        assertEquals(0, inventory.size());
    }

    /**
     * Case: inventory already at capacity.
     * Expected: returns false, size stays at capacity, and the rejected item is not stored.
     */
    @Test
    void tryAdd_whenFull_rejectsNewItem() {
        Inventory inventory = new Inventory(1);
        inventory.tryAdd(new HealPotion());
        Item overflow = new ManaPotion();

        boolean added = inventory.tryAdd(overflow);

        assertFalse(added);
        assertEquals(1, inventory.size());
        assertFalse(inventory.getItems().contains(overflow));
    }
}
