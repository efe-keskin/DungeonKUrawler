package engine;

import model.Chest;
import model.GridCell;
import model.HealPotion;
import model.Hero;
import model.Inventory;
import model.Item;
import model.ManaPotion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static engine.InventoryController.PickupResult.INVENTORY_FULL;
import static engine.InventoryController.PickupResult.NOT_ADJACENT;
import static engine.InventoryController.PickupResult.NOT_TAKABLE;
import static engine.InventoryController.PickupResult.NO_ITEM;
import static engine.InventoryController.PickupResult.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link InventoryController#takeFirstItemFromCell(int, int)}.
 */
class InventoryControllerTest {

    private GameEngine engine;
    private InventoryController inventoryController;

    @BeforeEach
    void setUp() {
        engine = new GameEngine();
        inventoryController = new InventoryController(engine);
    }

    @AfterEach
    void tearDown() {
        engine.shutdown();
    }

    @Test
    void takeFirstItemFromCell_whenTargetIsNotAdjacent_returnsNotAdjacent() {
        GridCell targetCell = engine.getDungeonMap().getCell(3, 1);
        int initialItemCount = targetCell.getItems().size();
        int initialInventorySize = engine.getHero().getInventory().size();

        InventoryController.PickupResult result = inventoryController.takeFirstItemFromCell(3, 1);

        assertEquals(NOT_ADJACENT, result);
        assertEquals(initialItemCount, targetCell.getItems().size());
        assertEquals(initialInventorySize, engine.getHero().getInventory().size());
    }

    @Test
    void takeFirstItemFromCell_whenTargetCellDoesNotExist_returnsNoItem() {
        engine.updateHeroPosition(0, 0);

        InventoryController.PickupResult result = inventoryController.takeFirstItemFromCell(-1, 0);

        assertEquals(NO_ITEM, result);
        assertEquals(0, engine.getHero().getInventory().size());
    }

    @Test
    void takeFirstItemFromCell_whenAdjacentCellHasNoItems_returnsNoItem() {
        GridCell emptyCell = engine.getDungeonMap().getCell(2, 1);
        emptyCell.getItems().clear();

        InventoryController.PickupResult result = inventoryController.takeFirstItemFromCell(2, 1);

        assertEquals(NO_ITEM, result);
        assertTrue(emptyCell.getItems().isEmpty());
        assertEquals(0, engine.getHero().getInventory().size());
    }

    @Test
    void takeFirstItemFromCell_whenFirstItemIsNotTakable_returnsNotTakable() {
        GridCell targetCell = engine.getDungeonMap().getCell(2, 1);
        Chest chest = new Chest("Fixed Chest", 2);
        Item potionBehindChest = new HealPotion();
        targetCell.getItems().clear();
        targetCell.getItems().add(chest);
        targetCell.getItems().add(potionBehindChest);

        InventoryController.PickupResult result = inventoryController.takeFirstItemFromCell(2, 1);

        assertEquals(NOT_TAKABLE, result);
        assertSame(chest, targetCell.getItems().get(0));
        assertSame(potionBehindChest, targetCell.getItems().get(1));
        assertEquals(0, engine.getHero().getInventory().size());
    }

    @Test
    void takeFirstItemFromCell_whenInventoryIsFull_returnsInventoryFull() {
        Hero hero = engine.getHero();
        Inventory inventory = hero.getInventory();
        GridCell targetCell = engine.getDungeonMap().getCell(2, 1);
        Item groundItem = new HealPotion();
        targetCell.getItems().clear();
        targetCell.getItems().add(groundItem);
        fillInventory(inventory);

        InventoryController.PickupResult result = inventoryController.takeFirstItemFromCell(2, 1);

        assertEquals(INVENTORY_FULL, result);
        assertTrue(inventory.isFull());
        assertTrue(targetCell.getItems().contains(groundItem));
        assertFalse(inventory.getItems().contains(groundItem));
    }

    @Test
    void takeFirstItemFromCell_whenItemIsTakableAndInventoryHasSpace_movesItemAndReturnsSuccess() {
        Inventory inventory = engine.getHero().getInventory();
        GridCell targetCell = engine.getDungeonMap().getCell(2, 1);
        Item groundItem = new HealPotion();
        targetCell.getItems().clear();
        targetCell.getItems().add(groundItem);

        InventoryController.PickupResult result = inventoryController.takeFirstItemFromCell(2, 1);

        assertEquals(SUCCESS, result);
        assertTrue(inventory.getItems().contains(groundItem));
        assertFalse(targetCell.getItems().contains(groundItem));
        assertEquals(1, inventory.size());
    }

    private void fillInventory(Inventory inventory) {
        while (inventory.hasFreeSlot()) {
            inventory.tryAdd(new ManaPotion());
        }
    }
}
