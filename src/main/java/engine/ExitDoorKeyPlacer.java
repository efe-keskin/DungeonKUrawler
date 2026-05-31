package engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import model.Arch;
import model.BreakableObject;
import model.Chest;
import model.Container;
import model.DungeonMap;
import model.GridCell;
import model.Item;
import model.ItemAction;
import model.Key;
import model.KeyColor;
import model.SearchableObject;
import model.ValuableItem;

/**
 * Ensures a designed-map exit has one reachable matching key.
 */
final class ExitDoorKeyPlacer {

    private static final List<KeySpec> KEY_SPECS = List.of(
            new KeySpec("olive", KeyColor.OLIVE),
            new KeySpec("bent-silver", KeyColor.BENT_SILVER),
            new KeySpec("long-gold", KeyColor.LONG_GOLD),
            new KeySpec("silver", KeyColor.SILVER),
            new KeySpec("gold", KeyColor.GOLD),
            new KeySpec("orange", KeyColor.ORANGE));

    private final Random random;

    ExitDoorKeyPlacer(Random random) {
        this.random = random == null ? new Random() : random;
    }

    Placement ensureKeyForExit(DungeonMap map, Arch arch) {
        if (map == null || arch == null) {
            return Placement.notPlaced(null, "No exit door was found.");
        }

        List<Key> authoredKeys = availableKeys(map);
        Set<String> lockedChestKeyIds = lockedChestKeyIds(map);
        String requiredKeyId = arch.getRequiredKeyId();
        if (requiredKeyId == null || requiredKeyId.isBlank()
                || lockedChestKeyIds.contains(requiredKeyId.toLowerCase())) {
            List<Key> eligibleKeys = new ArrayList<>(authoredKeys);
            eligibleKeys.removeIf(key -> lockedChestKeyIds.contains(key.getKeyId().toLowerCase()));
            if (!eligibleKeys.isEmpty()) {
                Key key = eligibleKeys.get(random.nextInt(eligibleKeys.size()));
                arch.setRequiredKeyId(key.getKeyId());
                return Placement.reused(key, "using the key already placed on the map");
            }
            KeySpec spec = randomSpecDistinctFrom(lockedChestKeyIds);
            requiredKeyId = "exit-" + spec.id();
            arch.setRequiredKeyId(requiredKeyId);
            return place(map, new Key(requiredKeyId, spec.color()));
        }

        for (Key key : authoredKeys) {
            if (key.matches(requiredKeyId)) {
                return Placement.reused(key, "using the key already placed on the map");
            }
        }
        return place(map, new Key(requiredKeyId, colorFor(requiredKeyId)));
    }

    private Placement place(DungeonMap map, Key key) {
        List<SearchableObject> searchables = emptySearchables(map);
        List<BreakableObject> breakables = emptyBreakables(map);
        List<Container> containers = availableContainers(map);
        int hidingPlaceCount = searchables.size() + breakables.size() + containers.size();
        if (hidingPlaceCount > 0) {
            int pick = random.nextInt(hidingPlaceCount);
            if (pick < searchables.size()) {
                SearchableObject searchable = searchables.get(pick);
                searchable.setHiddenItem(key);
                return Placement.placed(key, "hidden in " + searchable.getName());
            }
            pick -= searchables.size();
            if (pick < breakables.size()) {
                BreakableObject breakable = breakables.get(pick);
                breakable.setHiddenItem(key);
                return Placement.placed(key, "hidden in " + breakable.getName());
            }
            Container container = containers.get(pick - breakables.size());
            if (container.addItem(key)) {
                return Placement.placed(key, "hidden in " + container.getName());
            }
        }

        List<GridCell> floorCells = emptyFloorCells(map);
        if (!floorCells.isEmpty()) {
            GridCell floor = floorCells.get(random.nextInt(floorCells.size()));
            floor.getItems().add(key);
            return Placement.placed(key, "placed on floor (" + floor.getX() + ", " + floor.getY() + ")");
        }
        floorCells = reachableFloorCells(map);
        if (!floorCells.isEmpty()) {
            GridCell floor = floorCells.get(random.nextInt(floorCells.size()));
            floor.getItems().add(key);
            return Placement.placed(key, "placed on floor (" + floor.getX() + ", " + floor.getY() + ")");
        }
        return Placement.notPlaced(key,
                "could not be placed: add an open floor tile or an empty searchable or breakable object");
    }

    private List<Key> availableKeys(DungeonMap map) {
        List<Key> keys = new ArrayList<>();
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GridCell cell = map.getCell(x, y);
                if (cell == null) {
                    continue;
                }
                for (Item item : cell.getItemsView()) {
                    collectAvailableKeys(item, keys);
                }
            }
        }
        return keys;
    }

    private void collectAvailableKeys(Item item, List<Key> keys) {
        if (item instanceof Key key) {
            keys.add(key);
        }
        if (item instanceof SearchableObject searchableObject) {
            collectAvailableKeys(searchableObject.getHiddenItem(), keys);
        }
        if (item instanceof BreakableObject breakableObject) {
            collectAvailableKeys(breakableObject.getHiddenItem(), keys);
        }
        if (item instanceof Container container) {
            for (Item content : container.getContents()) {
                collectAvailableKeys(content, keys);
            }
        }
    }

    private Set<String> lockedChestKeyIds(DungeonMap map) {
        Set<String> ids = new HashSet<>();
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GridCell cell = map.getCell(x, y);
                if (cell == null) {
                    continue;
                }
                for (Item item : cell.getItemsView()) {
                    if (item instanceof Chest chest && chest.isLocked()
                            && chest.getRequiredKeyId() != null) {
                        ids.add(chest.getRequiredKeyId().toLowerCase());
                    }
                }
            }
        }
        return ids;
    }

    private List<SearchableObject> emptySearchables(DungeonMap map) {
        List<SearchableObject> searchables = new ArrayList<>();
        forEachMapItem(map, item -> {
            if (item instanceof SearchableObject searchableObject
                    && searchableObject.getHiddenItem() == null) {
                searchables.add(searchableObject);
            }
        });
        return searchables;
    }

    private List<BreakableObject> emptyBreakables(DungeonMap map) {
        List<BreakableObject> breakables = new ArrayList<>();
        forEachMapItem(map, item -> {
            if (item instanceof BreakableObject breakableObject
                    && breakableObject.getHiddenItem() == null
                    && breakableObject.getInventoryActions().contains(ItemAction.BREAK)) {
                breakables.add(breakableObject);
            }
        });
        return breakables;
    }

    private List<Container> availableContainers(DungeonMap map) {
        List<Container> containers = new ArrayList<>();
        forEachMapItem(map, item -> {
            if (item instanceof Container container
                    && !container.isFull()
                    && !containsValuable(container)) {
                containers.add(container);
            }
        });
        return containers;
    }

    private boolean containsValuable(Container container) {
        for (Item item : container.getContents()) {
            if (item instanceof ValuableItem) {
                return true;
            }
            if (item instanceof Container child && containsValuable(child)) {
                return true;
            }
        }
        return false;
    }

    private List<GridCell> emptyFloorCells(DungeonMap map) {
        List<GridCell> cells = new ArrayList<>();
        for (int y = 1; y < map.getHeight() - 1; y++) {
            for (int x = 1; x < map.getWidth() - 1; x++) {
                if (x == 1 && y == 1) {
                    continue;
                }
                GridCell cell = map.getCell(x, y);
                if (cell != null && cell.isWalkable()
                        && cell.getItemsView().isEmpty()
                        && cell.getEntitiesView().isEmpty()) {
                    cells.add(cell);
                }
            }
        }
        return cells;
    }

    private List<GridCell> reachableFloorCells(DungeonMap map) {
        List<GridCell> cells = new ArrayList<>();
        for (int y = 1; y < map.getHeight() - 1; y++) {
            for (int x = 1; x < map.getWidth() - 1; x++) {
                GridCell cell = map.getCell(x, y);
                if (cell != null && cell.isWalkable() && cell.getEntitiesView().isEmpty()) {
                    cells.add(cell);
                }
            }
        }
        return cells;
    }

    private void forEachMapItem(DungeonMap map, java.util.function.Consumer<Item> consumer) {
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GridCell cell = map.getCell(x, y);
                if (cell == null) {
                    continue;
                }
                for (Item item : cell.getItemsView()) {
                    consumer.accept(item);
                }
            }
        }
    }

    private KeyColor colorFor(String requiredKeyId) {
        for (KeySpec spec : KEY_SPECS) {
            if (requiredKeyId.toLowerCase().endsWith(spec.id())) {
                return spec.color();
            }
        }
        return KeyColor.SILVER;
    }

    private KeySpec randomSpecDistinctFrom(Set<String> lockedChestKeyIds) {
        List<KeySpec> choices = new ArrayList<>(KEY_SPECS);
        choices.removeIf(spec -> lockedChestKeyIds.contains(spec.id()));
        if (choices.isEmpty()) {
            choices = KEY_SPECS;
        }
        return choices.get(random.nextInt(choices.size()));
    }

    record Placement(Key key, boolean placed, boolean reused, String location) {

        static Placement placed(Key key, String location) {
            return new Placement(key, true, false, location);
        }

        static Placement reused(Key key, String location) {
            return new Placement(key, true, true, location);
        }

        static Placement notPlaced(Key key, String location) {
            return new Placement(key, false, false, location);
        }
    }

    private record KeySpec(String id, KeyColor color) {
    }
}
