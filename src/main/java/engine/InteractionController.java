package engine;

import model.DungeonMap;
import model.GridCell;
import model.Hero;
import model.Inventory;
import model.Item;


public class InteractionController {

    private static final String TAKE_PREFIX = "TAKE ";

    private final GameEngine engine;

    public InteractionController(GameEngine engine) {
        this.engine = engine;
    }

    // looking action for ActionMenu and rule checker for 3x3 adjacent cell
    public String getPrimaryAction(int targetX, int targetY) {
        Hero hero = engine.getHero();
        DungeonMap map = engine.getDungeonMap();

        // check 3x3 adjacency
        if (!map.isHeroAdjacent(hero, targetX, targetY)) {
            return null;
        }

        GridCell cell = map.getCell(targetX, targetY);
        if (cell == null) return null;

        // check for Items (Key, Gold, Potion, Armour, Book)
        if (!cell.getItemsView().isEmpty()) {
            Item first = cell.getItemsView().get(0);
            if (first.isTakable()) {
                // Returns e.g. "TAKE Potion", "TAKE Key" — shown in the popup menu
                return TAKE_PREFIX + first.getName();
            }
        }

        return null;
    }

    /**
     * Gate between UI and domain logic.
     *
     * <p>Supported actions:
     * <ul>
     *   <li>{@code "TAKE <itemName>"} — picks up the first takable item at {@code (x, y)}
     *       into the hero's {@link Inventory}, then removes it from the map.
     *       GameEngine fires {@code notifyListeners()} → GamePanel repaints automatically.
     * </ul>
     */
    public void executeAction(String action, int x, int y) {
        if (action == null) return;

        if (action.startsWith(TAKE_PREFIX)) {
            executeTake(x, y);
        }
        // future actions (OPEN, FIGHT, …) go here
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Full TAKE pipeline:
     * isTakable → Inventory.hasFreeSlot → Inventory.addItem
     *           → DungeonMap.removeItemFromCell → GameEngine.notifyListeners → repaint
     */
    private void executeTake(int x, int y) {
        DungeonMap map = engine.getDungeonMap();
        GridCell cell = map.getCell(x, y);
        if (cell == null || cell.getItemsView().isEmpty()) return;

        // Grab the first item; GameEngine.takeItem enforces takability + capacity
        Item item = cell.getItemsView().get(0);

        boolean ok = engine.takeItem(item, x, y);

        if (ok) {
            Inventory inv = engine.getHero().getInventory();
            System.out.println("TAKE OK: " + item.getName()
                    + " now in inventory (" + inv.size() + "/" + inv.getCapacity() + ")");
        } else {
            if (!item.isTakable()) {
                System.out.println("TAKE FAILED: " + item.getName() + " is not takable.");
            } else {
                System.out.println("TAKE FAILED: Inventory full ("
                        + engine.getHero().getInventory().getCapacity() + " slots).");
            }
        }
    }
}