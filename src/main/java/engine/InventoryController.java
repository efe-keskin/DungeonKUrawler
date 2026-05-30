package engine;

import model.Coin;
import model.DungeonMap;
import model.GridCell;
import model.Hero;
import model.Item;
import model.ValuableItem;
import model.Vase;

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
     * requires: No special precondition.
     * modifies: hero inventory, target cell items
     * effects:
     *   If target cell is not adjacent to the hero, returns NOT_ADJACENT.
     *   If target cell does not exist or has no items, returns NO_ITEM.
     *   If the first item is not takable, returns NOT_TAKABLE. Broken vase
     *   remains are skipped so loot dropped by that vase stays collectible.
     *   If the hero inventory is full, returns INVENTORY_FULL.
     *   Otherwise, moves the selected item from the target cell to the hero inventory
     *   and returns SUCCESS.
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
        if (item instanceof Vase vase && vase.isBroken()) {
            item = cell.getItemsView().stream()
                    .filter(Item::isTakable)
                    .findFirst()
                    .orElse(item);
        }
        if (!item.isTakable()) {
            return PickupResult.NOT_TAKABLE;
        }

        if (item instanceof Coin || item instanceof ValuableItem) {
            // Coins go to the gold balance; valuables go to the persistent
            // inventory — neither consumes a per-level bag slot.
            boolean collected = engine.takeItem(item, x, y);
            return collected ? PickupResult.SUCCESS : PickupResult.NO_ITEM;
        }

        if (!hero.getInventory().hasFreeSlot()) {
            return PickupResult.INVENTORY_FULL;
        }

        boolean moved = engine.takeItem(item, x, y);
        return moved ? PickupResult.SUCCESS : PickupResult.INVENTORY_FULL;
    }
}
