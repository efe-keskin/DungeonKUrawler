package engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import model.DungeonMap;
import model.GridCell;
import model.Item;
import model.Key;
import model.SearchableObject;


/**
 * Pure Fabrication for the build-mode "Add 5 Random Items" use case.
 *
 * <p>The controller delegates here so the random placement policy stays testable
 * and independent from Swing.
 */
public final class BuildRandomItemPlacer {

    public static final int VISIBLE_ITEM_COUNT = 5;

    private static final List<String> VISIBLE_ITEM_TOOL_IDS = List.of(
            "HEAL", "ENERGY", "MANA", "KEY", "WEAPON", "ARMOR", "RING");
    private static final List<String> HIDDEN_ITEM_TOOL_IDS = VISIBLE_ITEM_TOOL_IDS;
    private static final List<String> WALL_SEARCHABLE_TOOL_IDS = List.of(
            "CRATE", "CRATE_WOOD_RIGHT", "CRATE_ORANGE", "HOLE", "HOLE_2", "HOLE_3",
            "GRILL", "GRILL_2", "GARGOYLE", "GARGOYLE_GREEN",
            "GARGOYLE_BLUE", "MISSING_BRICK", "MISSING_BRICK_2");

    private final BuildToolCatalog catalog;
    private final BuildPlacementStrategy placementStrategy;
    private final Random random;

    public BuildRandomItemPlacer(BuildToolCatalog catalog, BuildPlacementStrategy placementStrategy, Random random) {
        this.catalog = catalog;
        this.placementStrategy = placementStrategy;
        this.random = random == null ? new Random() : random;
    }

    public Result addFiveRandomItemsAndHiddenSearchable(DungeonMap map) {
        boolean hiddenPlaced = placeHiddenItemInSearchableLocation(map);
        int visiblePlaced = placeVisibleRandomItems(map, VISIBLE_ITEM_COUNT);
        return new Result(visiblePlaced, hiddenPlaced);
    }

    private int placeVisibleRandomItems(DungeonMap map, int count) {
        if (map == null) {
            return 0;
        }

        List<CellSpot> candidates = collectEmptyInteriorFloorSpots(map);
        int placed = 0;
        while (placed < count && !candidates.isEmpty()) {
            CellSpot spot = candidates.remove(random.nextInt(candidates.size()));
            BuildTool tool = randomTool(VISIBLE_ITEM_TOOL_IDS);
            if (tool != null && placementStrategy.place(map, spot.x(), spot.y(), tool)) {
                placed++;
            }
        }
        return placed;
    }

    private boolean placeHiddenItemInSearchableLocation(DungeonMap map) {
        if (map == null) {
            return false;
        }

        BuildTool hiddenTool = randomTool(HIDDEN_ITEM_TOOL_IDS);
        Item hiddenItem = hiddenTool == null ? null : hiddenTool.createItem();
        if (hiddenItem == null) {
            return false;
        }

        SearchableObject emptySearchable = randomHorizontalWallSearchable(map, true);
        if (emptySearchable != null) {
            emptySearchable.setHiddenItem(hiddenItem);
            return true;
        }

        if (placeNewSearchableWithHiddenItem(map, hiddenItem)) {
            return true;
        }

        SearchableObject anySearchable = randomHorizontalWallSearchable(map, false);
        if (anySearchable != null) {
            anySearchable.setHiddenItem(hiddenItem);
            return true;
        }
        return false;
    }

    private boolean placeNewSearchableWithHiddenItem(DungeonMap map, Item hiddenItem) {
        List<SearchablePlacement> candidates = new ArrayList<>();

        for (int x = 1; x < map.getWidth() - 1; x++) {
            collectHorizontalWallSearchableCandidate(map, candidates, x, 0);
            collectHorizontalWallSearchableCandidate(map, candidates, x, map.getHeight() - 1);
        }

        while (!candidates.isEmpty()) {
            SearchablePlacement pick = candidates.remove(random.nextInt(candidates.size()));
            if (!placementStrategy.place(map, pick.x(), pick.y(), pick.tool())) {
                continue;
            }
            SearchableObject searchableObject = searchableAt(map, pick.x(), pick.y());
            if (searchableObject != null) {
                searchableObject.setHiddenItem(hiddenItem);
                return true;
            }
        }
        return false;
    }

    private void collectHorizontalWallSearchableCandidate(DungeonMap map,
            List<SearchablePlacement> candidates, int x, int y) {
        GridCell cell = map.getCell(x, y);
        if (cell == null || !cell.getItemsView().isEmpty()) {
            return;
        }
        for (String toolId : WALL_SEARCHABLE_TOOL_IDS) {
            BuildTool tool = catalog.findById(toolId);
            if (tool != null) {
                candidates.add(new SearchablePlacement(x, y, tool));
            }
        }
    }

    private List<CellSpot> collectEmptyInteriorFloorSpots(DungeonMap map) {
        List<CellSpot> spots = new ArrayList<>();
        for (int y = 1; y < map.getHeight() - 1; y++) {
            for (int x = 1; x < map.getWidth() - 1; x++) {
                if (x == 1 && y == 1) {
                    continue;
                }
                GridCell cell = map.getCell(x, y);
                if (cell != null && cell.isPassable() && cell.getItemsView().isEmpty()) {
                    spots.add(new CellSpot(x, y));
                }
            }
        }
        return spots;
    }

    private SearchableObject randomHorizontalWallSearchable(DungeonMap map, boolean requireEmptyHiddenSlot) {
        List<SearchableObject> searchables = new ArrayList<>();
        for (int y : List.of(0, map.getHeight() - 1)) {
            for (int x = 1; x < map.getWidth() - 1; x++) {
                SearchableObject searchableObject = searchableAt(map, x, y);
                if (searchableObject != null
                        && (!requireEmptyHiddenSlot || searchableObject.getHiddenItem() == null)
                        && !(searchableObject.getHiddenItem() instanceof Key)) {
                    searchables.add(searchableObject);
                }
            }
        }
        if (searchables.isEmpty()) {
            return null;
        }
        return searchables.get(random.nextInt(searchables.size()));
    }

    private SearchableObject searchableAt(DungeonMap map, int x, int y) {
        GridCell cell = map.getCell(x, y);
        if (cell == null || cell.getItemsView().isEmpty()) {
            return null;
        }
        Item item = cell.getItemsView().get(0);
        return item instanceof SearchableObject searchableObject ? searchableObject : null;
    }

    private BuildTool randomTool(List<String> toolIds) {
        if (toolIds.isEmpty()) {
            return null;
        }
        for (int attempts = 0; attempts < toolIds.size() * 2; attempts++) {
            BuildTool tool = catalog.findById(toolIds.get(random.nextInt(toolIds.size())));
            if (tool != null) {
                return tool;
            }
        }
        return null;
    }

    public record Result(int visibleItemsPlaced, boolean hiddenItemPlaced) {
    }

    private record CellSpot(int x, int y) {
    }

    private record SearchablePlacement(int x, int y, BuildTool tool) {
    }
}
