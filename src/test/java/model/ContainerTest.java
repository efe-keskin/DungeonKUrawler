package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ContainerTest {

    /**
     * Checks that a newly created container starts in a valid representation state.
     */
    @Test
    void newContainerIsValid() {
        Container container = new Container("Wooden Chest", false, false, 2);

        assertTrue(container.repOk());
    }

    /**
     * Checks that a container can accept items until it reaches its declared capacity.
     */
    @Test
    void containerCanStoreItemsUpToCapacity() {
        Container container = new Container("Wooden Chest", false, false, 2);
        Item healPotion = new HealPotion();
        Item energyPotion = new EnergyPotion();

        assertTrue(container.addItem(healPotion));
        assertTrue(container.addItem(energyPotion));

        assertEquals(2, container.size());
        assertTrue(container.isFull());
        assertTrue(container.getContents().contains(healPotion));
        assertTrue(container.getContents().contains(energyPotion));
        assertTrue(container.repOk());
    }

    /**
     * Checks that a full container rejects extra items without changing its contents.
     */
    @Test
    void fullContainerRejectsAdditionalItem() {
        Container container = new Container("Small Chest", false, false, 1);
        Item storedItem = new HealPotion();
        Item rejectedItem = new EnergyPotion();

        assertTrue(container.addItem(storedItem));
        assertFalse(container.addItem(rejectedItem));

        assertEquals(1, container.size());
        assertTrue(container.isFull());
        assertTrue(container.getContents().contains(storedItem));
        assertFalse(container.getContents().contains(rejectedItem));
        assertTrue(container.repOk());
    }

    /**
     * Checks that null cannot be added as a stored item.
     */
    @Test
    void nullItemIsRejected() {
        Container container = new Container("Wooden Chest", false, false, 2);

        assertFalse(container.addItem(null));

        assertEquals(0, container.size());
        assertFalse(container.getContents().contains(null));
    }

    /**
     * Checks that a key-requiring container is invalid when no required key id is set.
     */
    @Test
    void keyRequiredContainerWithoutRequiredKeyIdIsInvalid() {
        Container container = new Container("Locked Chest", false, true, 2);

        assertFalse(container.repOk());
    }

    /**
     * Checks that rejecting a null item does not corrupt an otherwise valid container.
     */
    @Test
    void rejectedNullItemDoesNotBreakValidContainerState() {
        Container container = new Container("Wooden Chest", false, false, 2);
        Item storedItem = new HealPotion();

        assertTrue(container.addItem(storedItem));
        assertFalse(container.addItem(null));

        assertEquals(1, container.size());
        assertTrue(container.getContents().contains(storedItem));
        assertTrue(container.repOk());
    }
}
