package engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import model.DungeonMap;
import model.GridCell;
import model.Item;
import model.SearchableObject;

/**
 * Randomizes collectible locations whenever a fresh tower floor is created.
 * Fixtures stay in their designed positions so the room layout remains intact.
 */
final class TowerFloorItemRandomizer {

    private static final int HERO_START_X = 1;
    private static final int HERO_START_Y = 1;

    private final Random random;

    TowerFloorItemRandomizer(Random random) {
        this.random = random;
    }

    void randomize(DungeonMap map) {
        if (map == null) {
            return;
        }
        randomizeLooseItems(map);
        randomizePreparedHiddenItems(map);
    }

    /**
     * Collectible ground items are removed and placed again on random empty
     * floor tiles. The number and type of items stay the same for the level.
     */
    private void randomizeLooseItems(DungeonMap map) {
        List<ItemPlacement> placements = new ArrayList<>();
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                GridCell cell = map.getCell(x, y);
                if (cell == null) {
                    continue;
                }
                for (Item item : new ArrayList<>(cell.getItemsView())) {
                    if (item.isTakable()) {
                        placements.add(new ItemPlacement(item, cell));
                        cell.getItems().remove(item);
                    }
                }
            }
        }
        if (placements.isEmpty()) {
            return;
        }

        List<GridCell> cells = emptyFloorCells(map);
        if (cells.size() < placements.size()) {
            restoreOriginalLocations(placements);
            return;
        }
        Collections.shuffle(cells, random);
        Collections.shuffle(placements, random);
        for (int i = 0; i < placements.size(); i++) {
            cells.get(i).getItems().add(placements.get(i).item());
        }
    }

    /**
     * Prepared hidden loot is shuffled among searchable fixtures. For example,
     * an exit key is not hidden behind the same wall object on every run.
     */
    private void randomizePreparedHiddenItems(DungeonMap map) {
        List<SearchableObject> searchables = new ArrayList<>();
        List<Item> hiddenItems = new ArrayList<>();
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                GridCell cell = map.getCell(x, y);
                if (cell == null) {
                    continue;
                }
                for (Item item : cell.getItemsView()) {
                    if (item instanceof SearchableObject searchable) {
                        searchables.add(searchable);
                        Item hidden = searchable.takeHiddenItem();
                        if (hidden != null) {
                            hiddenItems.add(hidden);
                        }
                    }
                }
            }
        }
        Collections.shuffle(searchables, random);
        Collections.shuffle(hiddenItems, random);
        for (int i = 0; i < hiddenItems.size(); i++) {
            searchables.get(i).setHiddenItem(hiddenItems.get(i));
        }
    }

    private List<GridCell> emptyFloorCells(DungeonMap map) {
        List<GridCell> cells = new ArrayList<>();
        for (int y = 1; y < map.getHeight() - 1; y++) {
            for (int x = 1; x < map.getWidth() - 1; x++) {
                if (x == HERO_START_X && y == HERO_START_Y) {
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

    private void restoreOriginalLocations(List<ItemPlacement> placements) {
        for (ItemPlacement placement : placements) {
            placement.originalCell().getItems().add(placement.item());
        }
    }

    private record ItemPlacement(Item item, GridCell originalCell) {
    }
}
