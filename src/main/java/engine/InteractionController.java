package engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.Coin;
import model.DungeonMap;
import model.GridCell;
import model.Hero;
import model.Inventory;
import model.Item;
import model.ItemAction;

public class InteractionController {

    private final GameEngine engine;
    private final InventoryController inventoryController;

    public InteractionController(GameEngine engine) {
        this.engine = engine;
        this.inventoryController = new InventoryController(engine);
    }

    /**
     * One selectable action on a ground item. {@code inventoryAction} is
     * {@code null} for the plain pick-up entry; otherwise it is the action
     * to apply once the item has been collected.
     */
    public static final class ActionOption {
        private final String label;
        private final ItemAction inventoryAction;

        ActionOption(String label, ItemAction inventoryAction) {
            this.label = label;
            this.inventoryAction = inventoryAction;
        }

        public String getLabel() {
            return label;
        }

        public ItemAction getInventoryAction() {
            return inventoryAction;
        }

        public boolean isPickup() {
            return inventoryAction == null;
        }
    }

    /**
     * Lightweight data package for rendering a centered item interaction dialog.
     */
    public static final class ItemInteraction {
        private final Item item;
        private final String itemName;
        private final boolean takable;
        private final String detail;
        private final int x;
        private final int y;
        private final List<ActionOption> actions;

        ItemInteraction(Item item, String itemName, boolean takable, String detail,
                int x, int y, List<ActionOption> actions) {
            this.item = item;
            this.itemName = itemName;
            this.takable = takable;
            this.detail = detail;
            this.x = x;
            this.y = y;
            this.actions = Collections.unmodifiableList(actions);
        }

        public Item getItem() {
            return item;
        }

        public String getItemName() {
            return itemName;
        }

        public boolean isTakable() {
            return takable;
        }

        public String getDetail() {
            return detail;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public List<ActionOption> getActions() {
            return actions;
        }
    }

    /**
     * Returns one {@link ItemInteraction} per item sitting on the clicked cell,
     * each carrying every action the item supports. Empty when the cell is out
     * of reach or has no items.
     */
    public List<ItemInteraction> getItemInteractions(int targetX, int targetY) {
        Hero hero = engine.getHero();
        DungeonMap map = engine.getDungeonMap();

        if (!map.isHeroAdjacent(hero, targetX, targetY)) {
            return List.of();
        }

        GridCell cell = map.getCell(targetX, targetY);
        if (cell == null || cell.getItemsView().isEmpty()) {
            return List.of();
        }

        List<ItemInteraction> interactions = new ArrayList<>(cell.getItemsView().size());
        for (Item item : cell.getItemsView()) {
            interactions.add(buildInteraction(item, targetX, targetY));
        }
        return interactions;
    }

    private ItemInteraction buildInteraction(Item item, int x, int y) {
        List<ActionOption> actions = new ArrayList<>();
        if (item.isTakable()) {
            String pickupLabel = item instanceof Coin ? "Collect" : "Take";
            actions.add(new ActionOption(pickupLabel, null));
        }
        for (ItemAction action : item.getInventoryActions()) {
            actions.add(new ActionOption(action.getLabel(), action));
        }

        String detail = item instanceof Coin coin
                ? "Reward: " + coin.getValue() + " coins"
                : null;
        return new ItemInteraction(item, item.getName(), item.isTakable(), detail, x, y, actions);
    }

    /**
     * Executes item pickup through {@link InventoryController} and returns a user-facing outcome.
     */
    public InventoryController.PickupResult takeItemAt(int x, int y) {
        InventoryController.PickupResult result = inventoryController.takeFirstItemFromCell(x, y);
        if (result == InventoryController.PickupResult.SUCCESS) {
            Inventory inv = engine.getHero().getInventory();
            System.out.println("COLLECT OK: coins=" + engine.getHero().getCoinBalance()
                    + ", inventory=(" + inv.size() + "/" + inv.getCapacity() + ")");
        }
        return result;
    }

    /**
     * Picks up {@code item} from cell {@code (x, y)} and applies {@code action}
     * through {@link GameEngine#performInventoryAction}. Used by the ground
     * interaction menu so the player can drink/read/equip in one click.
     *
     * @return {@code true} when the item was collected and the action applied.
     */
    public boolean applyGroundAction(Item item, int x, int y, ItemAction action) {
        if (item == null || action == null) {
            return false;
        }
        if (!engine.takeItem(item, x, y)) {
            return false;
        }
        return engine.performInventoryAction(item, action);
    }
}
