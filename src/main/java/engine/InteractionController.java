package engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.Coin;
import model.Container;
import model.DungeonMap;
import model.GridCell;
import model.Hero;
import model.Inventory;
import model.Item;
import model.ItemAction;
import model.SearchableObject;

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
            ItemInteraction interaction = buildInteraction(item, targetX, targetY);
            if (!interaction.getActions().isEmpty()) {
                interactions.add(interaction);
            }
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

        String detail = detailFor(item);
        return new ItemInteraction(item, item.getName(), item.isTakable(), detail, x, y, actions);
    }

    private String detailFor(Item item) {
        if (item instanceof Coin coin) {
            return "Reward: " + coin.getValue() + " coins";
        }
        if (item instanceof SearchableObject) {
            return "This location can be searched.";
        }
        return null;
    }

    /**
     * Breaks the first breakable object found in the hero's 3x3 interaction
     * range, scanning adjacent tiles before the hero's own tile. A breakable is
     * any ground item that declares {@link ItemAction#BREAK} (e.g. vase, column,
     * crate, water pipe) or a breakable {@link Container}. Containers also
     * enforce a strength requirement.
     *
     * @return the outcome, or {@code null} when nothing breakable is in reach.
     */
    public BreakResult breakNearestObject() {
        Hero hero = engine.getHero();
        DungeonMap map = engine.getDungeonMap();
        int hx = hero.getX();
        int hy = hero.getY();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                GridCell cell = map.getCell(hx + dx, hy + dy);
                if (cell == null) {
                    continue;
                }
                for (Item item : cell.getItemsView()) {
                    if (!isBreakable(item)) {
                        continue;
                    }
                    int required = item instanceof Container container
                            ? container.getBreakStrengthRequired()
                            : 0;
                    if (hero.getStr() < required) {
                        return new BreakResult(item.getName(), false);
                    }
                    cell.getItems().remove(item);
                    engine.notifyGameStateChanged();
                    return new BreakResult(item.getName(), true);
                }
            }
        }
        return null;
    }

    private boolean isBreakable(Item item) {
        return item.getInventoryActions().contains(ItemAction.BREAK)
                || (item instanceof Container container && container.isBreakable());
    }

    /**
     * Outcome of a break attempt: the object's name and whether it was actually
     * destroyed. {@code broken == false} means the hero lacked the strength.
     */
    public record BreakResult(String objectName, boolean broken) {
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
        if (action == ItemAction.SEARCH && item instanceof SearchableObject searchableObject) {
            return engine.search(searchableObject).getOutcome() != GameEngine.SearchOutcome.NOT_SEARCHABLE;
        }
        if (!engine.takeItem(item, x, y)) {
            return false;
        }
        return engine.performInventoryAction(item, action);
    }

    public GameEngine.SearchResult search(SearchableObject object) {
        return engine.search(object);
    }
}
