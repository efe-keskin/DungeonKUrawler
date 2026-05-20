package model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GridCellTest {

    @Test
    void passableCellWithNoBlockingItemsIsWalkable() {
        GridCell cell = new GridCell(1, 1, true);
        cell.getItems().add(new HealPotion());

        assertTrue(cell.isWalkable());
    }

    @Test
    void nonPassableCellIsNotWalkable() {
        GridCell cell = new GridCell(1, 1, false);

        assertFalse(cell.isWalkable());
    }

    @Test
    void passableCellWithBlockingItemIsNotWalkable() {
        GridCell cell = new GridCell(1, 1, true);
        cell.getItems().add(new Chest("Wooden Chest", 2));

        assertFalse(cell.isWalkable());
    }
}
