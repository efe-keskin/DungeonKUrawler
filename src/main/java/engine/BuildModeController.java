package engine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import model.DungeonMap;
import model.Chest;
import model.GridCell;
import model.Item;


/**
 * GRASP Controller for build-mode use cases.
 */
public final class BuildModeController {

    public static final int DEFAULT_MAP_WIDTH = 16;
    public static final int DEFAULT_MAP_HEIGHT = 12;
    public static final int MAX_RANDOM_ITEM_ADDS = 3;

    private static final String DEFAULT_LEVEL_NAME = "Designed Map";

    private final BuildToolCatalog toolCatalog;
    private final BuildMapFactory mapFactory;
    private final BuildPlacementStrategy placementStrategy;
    private final BuildMapPersistence mapPersistence;
    private final BuildRandomItemPlacer randomItemPlacer;
    private final LockedChestKeyPlacer lockedChestKeyPlacer;
    private DungeonMap designMap;
    private BuildTool selectedTool;
    private int randomItemAddCount;
    private String lastPlacementMessage;

    public BuildModeController() {
        this(new BuildToolCatalog(), new BuildMapFactory(), new StandardBuildPlacementStrategy(), new Random());
    }

    BuildModeController(BuildToolCatalog toolCatalog, BuildMapFactory mapFactory,
            BuildPlacementStrategy placementStrategy) {
        this(toolCatalog, mapFactory, placementStrategy, new Random());
    }

    BuildModeController(BuildToolCatalog toolCatalog, BuildMapFactory mapFactory,
            BuildPlacementStrategy placementStrategy, Random random) {
        this.toolCatalog = toolCatalog;
        this.mapFactory = mapFactory;
        this.placementStrategy = placementStrategy;
        this.mapPersistence = new BuildMapPersistence(toolCatalog, mapFactory, placementStrategy);
        this.randomItemPlacer = new BuildRandomItemPlacer(toolCatalog, placementStrategy, random);
        this.lockedChestKeyPlacer = new LockedChestKeyPlacer(random);
        this.selectedTool = toolCatalog.defaultTool();
        clearMap();
    }

    public DungeonMap getDesignMap() {
        return designMap;
    }

    public List<BuildTool> getTools() {
        return toolCatalog.tools();
    }

    public BuildTool getSelectedTool() {
        return selectedTool;
    }

    public void selectTool(BuildTool tool) {
        selectedTool = tool == null ? toolCatalog.defaultTool() : tool;
    }

    public BuildTool findTool(String id) {
        return toolCatalog.findById(id);
    }

    public void clearMap() {
        designMap = mapFactory.createEmptyMap(DEFAULT_LEVEL_NAME, DEFAULT_MAP_WIDTH, DEFAULT_MAP_HEIGHT);
        randomItemAddCount = 0;
        lastPlacementMessage = null;
    }

    public void saveMap(Path path) throws IOException {
        mapPersistence.save(designMap, path);
    }

    public void loadMap(Path path) throws IOException {
        designMap = mapPersistence.load(path);
        randomItemAddCount = 0;
        lastPlacementMessage = null;
    }

    public BuildRandomItemPlacer.Result addFiveRandomItems() {
        if (!canAddFiveRandomItems()) {
            return new BuildRandomItemPlacer.Result(0, false);
        }
        randomItemAddCount++;
        return randomItemPlacer.addFiveRandomItemsAndHiddenSearchable(designMap);
    }

    public boolean canAddFiveRandomItems() {
        return randomItemAddCount < MAX_RANDOM_ITEM_ADDS;
    }

    public int getRemainingRandomItemAdds() {
        return Math.max(0, MAX_RANDOM_ITEM_ADDS - randomItemAddCount);
    }

    public boolean placeSelectedToolAt(int x, int y) {
        return placeToolAt(x, y, selectedTool);
    }

    public boolean placeToolAt(int x, int y, BuildTool tool) {
        if (tool == null) {
            return false;
        }
        selectTool(tool);
        boolean placed = placementStrategy.place(designMap, x, y, tool);
        lastPlacementMessage = null;
        if (placed) {
            GridCell cell = designMap.getCell(x, y);
            Item item = cell == null || cell.getItemsView().isEmpty()
                    ? null : cell.getItemsView().get(0);
            if (item instanceof Chest chest && chest.isLocked()) {
                LockedChestKeyPlacer.Placement keyPlacement =
                        lockedChestKeyPlacer.assignAndPlace(designMap, chest);
                lastPlacementMessage = keyPlacement.messageFor(chest);
            }
        }
        return placed;
    }

    public String getLastPlacementMessage() {
        return lastPlacementMessage;
    }

    public boolean eraseAt(int x, int y) {
        return placementStrategy.erase(designMap, x, y);
    }
}
