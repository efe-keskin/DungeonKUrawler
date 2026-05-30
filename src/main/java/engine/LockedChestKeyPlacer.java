package engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import model.Chest;
import model.Container;
import model.DungeonMap;
import model.GridCell;
import model.Item;
import model.Key;
import model.KeyColor;
import model.SearchableObject;

/**
 * Assigns and places keys for locked chests in designed maps.
 */
public final class LockedChestKeyPlacer {

    private static final List<KeySpec> KEY_SPECS = List.of(
            new KeySpec("olive", KeyColor.OLIVE),
            new KeySpec("silver", KeyColor.SILVER),
            new KeySpec("gold", KeyColor.GOLD),
            new KeySpec("orange", KeyColor.ORANGE),
            new KeySpec("bent-silver", KeyColor.BENT_SILVER),
            new KeySpec("long-gold", KeyColor.LONG_GOLD));

    private final Random random;

    public LockedChestKeyPlacer(Random random) {
        this.random = random == null ? new Random() : random;
    }

    public Placement assignAndPlace(DungeonMap map, Chest chest) {
        if (map == null || chest == null || !chest.isLocked()) {
            return Placement.notPlaced(null, "No locked chest was placed.");
        }
        KeySpec spec = KEY_SPECS.get(random.nextInt(KEY_SPECS.size()));
        chest.setRequiresKey(true);
        chest.setRequiredKeyId(spec.id());
        return place(map, new Key(spec.id(), spec.color()));
    }

    /**
     * Repairs designed maps at launch: every locked chest receives a matching
     * key on the map when the builder removed or never placed one.
     */
    public void ensureKeysForLockedChests(DungeonMap map) {
        if (map == null) {
            return;
        }
        for (Chest chest : lockedChests(map)) {
            String requiredKeyId = chest.getRequiredKeyId();
            if (requiredKeyId == null || requiredKeyId.isBlank()) {
                KeySpec spec = KEY_SPECS.get(random.nextInt(KEY_SPECS.size()));
                chest.setRequiresKey(true);
                chest.setRequiredKeyId(spec.id());
                requiredKeyId = spec.id();
            }
            if (!hasAccessibleMatchingKey(map, requiredKeyId)) {
                place(map, new Key(requiredKeyId, colorFor(requiredKeyId)));
            }
        }
    }

    private Placement place(DungeonMap map, Key key) {
        List<SearchableObject> searchables = emptySearchables(map);
        if (!searchables.isEmpty()) {
            SearchableObject searchable = searchables.get(random.nextInt(searchables.size()));
            searchable.setHiddenItem(key);
            return Placement.placed(key, "hidden in " + searchable.getName());
        }

        List<GridCell> floorCells = emptyFloorCells(map);
        if (!floorCells.isEmpty()) {
            GridCell floor = floorCells.get(random.nextInt(floorCells.size()));
            floor.getItems().add(key);
            return Placement.placed(key, "placed on floor (" + floor.getX() + ", " + floor.getY() + ")");
        }
        return Placement.notPlaced(key, "could not be placed: add an open floor tile or searchable fixture");
    }

    private List<Chest> lockedChests(DungeonMap map) {
        List<Chest> chests = new ArrayList<>();
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GridCell cell = map.getCell(x, y);
                if (cell == null) {
                    continue;
                }
                for (Item item : cell.getItemsView()) {
                    if (item instanceof Chest chest && chest.isLocked()) {
                        chests.add(chest);
                    }
                }
            }
        }
        return chests;
    }

    private List<SearchableObject> emptySearchables(DungeonMap map) {
        List<SearchableObject> searchables = new ArrayList<>();
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GridCell cell = map.getCell(x, y);
                if (cell == null) {
                    continue;
                }
                for (Item item : cell.getItemsView()) {
                    if (item instanceof SearchableObject searchableObject
                            && searchableObject.getHiddenItem() == null) {
                        searchables.add(searchableObject);
                    }
                }
            }
        }
        return searchables;
    }

    private List<GridCell> emptyFloorCells(DungeonMap map) {
        List<GridCell> floorCells = new ArrayList<>();
        for (int y = 1; y < map.getHeight() - 1; y++) {
            for (int x = 1; x < map.getWidth() - 1; x++) {
                if (x == 1 && y == 1) {
                    continue;
                }
                GridCell cell = map.getCell(x, y);
                if (cell != null && cell.isWalkable()
                        && cell.getItemsView().isEmpty()
                        && cell.getEntitiesView().isEmpty()) {
                    floorCells.add(cell);
                }
            }
        }
        return floorCells;
    }

    private boolean hasAccessibleMatchingKey(DungeonMap map, String requiredKeyId) {
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GridCell cell = map.getCell(x, y);
                if (cell == null) {
                    continue;
                }
                for (Item item : cell.getItemsView()) {
                    if (matches(item, requiredKeyId)) {
                        return true;
                    }
                    if (item instanceof SearchableObject searchableObject
                            && matches(searchableObject.getHiddenItem(), requiredKeyId)) {
                        return true;
                    }
                    if (item instanceof Container container && !container.isLocked()) {
                        for (Item content : container.getContents()) {
                            if (matches(content, requiredKeyId)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean matches(Item item, String requiredKeyId) {
        return item instanceof Key key && key.matches(requiredKeyId);
    }

    private KeyColor colorFor(String requiredKeyId) {
        for (KeySpec spec : KEY_SPECS) {
            if (spec.id().equalsIgnoreCase(requiredKeyId)) {
                return spec.color();
            }
        }
        return KeyColor.SILVER;
    }

    public record Placement(Key key, boolean placed, String location) {

        static Placement placed(Key key, String location) {
            return new Placement(key, true, location);
        }

        static Placement notPlaced(Key key, String location) {
            return new Placement(key, false, location);
        }

        public String messageFor(Chest chest) {
            String keyName = key == null ? "Key" : key.getName();
            return chest.getName() + ": assigned " + keyName + ", " + location;
        }
    }

    private record KeySpec(String id, KeyColor color) {
    }
}
