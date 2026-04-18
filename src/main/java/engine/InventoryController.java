package engine;

import model.DungeonMap;
import model.GridCell;
import model.Hero;
import model.Item;

/**
 * Handles inventory-related use cases that cross map and hero inventory boundaries.
 */
public class InventoryController {

    public enum PickupResult {
        SUCCESS,
        NO_ITEM,
        NOT_ADJACENT,
        NOT_TAKABLE,
        INVENTORY_FULL
    }

    private final GameEngine engine;

    public InventoryController(GameEngine engine) {
        this.engine = engine;
    }

    /**
     * Transfers the first item from map cell to hero inventory when rules allow it.
     */
    public PickupResult takeFirstItemFromCell(int x, int y) {
        DungeonMap map = engine.getDungeonMap();
        Hero hero = engine.getHero();

        if (!map.isHeroAdjacent(hero, x, y)) {
            return PickupResult.NOT_ADJACENT;
        }

        GridCell cell = map.getCell(x, y);
        if (cell == null || cell.getItemsView().isEmpty()) {
            return PickupResult.NO_ITEM;
        }

        Item item = cell.getItemsView().get(0);
        if (!item.isTakable()) {
            return PickupResult.NOT_TAKABLE;
        }

        if (!hero.getInventory().hasFreeSlot()) {
            return PickupResult.INVENTORY_FULL;
        }

        boolean moved = engine.takeItem(item, x, y);
        return moved ? PickupResult.SUCCESS : PickupResult.INVENTORY_FULL;
    }
}
