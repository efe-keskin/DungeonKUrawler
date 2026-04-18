package engine;

import model.DungeonMap;
import model.GridCell;
import model.Hero;
import model.Inventory;
import model.Item;

public class InteractionController {

    private final GameEngine engine;
    private final InventoryController inventoryController;

    public InteractionController(GameEngine engine) {
        this.engine = engine;
        this.inventoryController = new InventoryController(engine);
    }

    /**
     * Lightweight data package for rendering a centered item interaction dialog.
     */
    public static final class ItemInteraction {
        private final String itemName;
        private final boolean takable;
        private final int x;
        private final int y;

        ItemInteraction(String itemName, boolean takable, int x, int y) {
            this.itemName = itemName;
            this.takable = takable;
            this.x = x;
            this.y = y;
        }

        public String getItemName() {
            return itemName;
        }

        public boolean isTakable() {
            return takable;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    // Returns the clicked item details when there is an interactable item in range.
    public ItemInteraction getItemInteraction(int targetX, int targetY) {
        Hero hero = engine.getHero();
        DungeonMap map = engine.getDungeonMap();

        // check 3x3 adjacency
        if (!map.isHeroAdjacent(hero, targetX, targetY)) {
            return null;
        }

        GridCell cell = map.getCell(targetX, targetY);
        if (cell == null) {
            return null;
        }

        // check for Items (Key, Gold, Potion, Armour, Book)
        if (!cell.getItemsView().isEmpty()) {
            Item first = cell.getItemsView().get(0);
            return new ItemInteraction(first.getName(), first.isTakable(), targetX, targetY);
        }

        return null;
    }

    /**
     * Executes item pickup through {@link InventoryController} and returns a user-facing outcome.
     */
    public InventoryController.PickupResult takeItemAt(int x, int y) {
        InventoryController.PickupResult result = inventoryController.takeFirstItemFromCell(x, y);
        if (result == InventoryController.PickupResult.SUCCESS) {
            Inventory inv = engine.getHero().getInventory();
            System.out.println("TAKE OK: inventory now (" + inv.size() + "/" + inv.getCapacity() + ")");
        }
        return result;
    }
}