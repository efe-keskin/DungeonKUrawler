package model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryTest {

    @Test
    void tryAdd_normalItem_returnsTrueAndGrowsSize() {
        Inventory inventory = new Inventory(3);
        Item potion = new HealPotion();

        boolean added = inventory.tryAdd(potion);

        assertTrue(added);
        assertEquals(1, inventory.size());
        assertTrue(inventory.getItems().contains(potion));
    }

    @Test
    void tryAdd_nullItem_returnsFalseAndDoesNotChangeInventory() {
        Inventory inventory = new Inventory(2);

        boolean added = inventory.tryAdd(null);

        assertFalse(added);
        assertEquals(0, inventory.size());
    }

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
