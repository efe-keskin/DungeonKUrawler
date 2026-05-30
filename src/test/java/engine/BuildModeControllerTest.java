package engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import model.DungeonMap;
import model.GridCell;
import model.HealPotion;
import model.Item;
import model.Chest;
import model.Key;
import model.MissingBrick;
import model.SearchableObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BuildModeControllerTest {

    @TempDir
    Path tempDir;

    @Test
    void eraseClearsItemsAndKeepsBorderCellsWalled() {
        BuildModeController controller = controller();

        assertTrue(controller.placeToolAt(2, 2, controller.findTool("WALL")));
        assertFalse(controller.getDesignMap().getCell(2, 2).isPassable());

        assertTrue(controller.placeToolAt(3, 3, controller.findTool("HEAL")));
        assertEquals(1, controller.getDesignMap().getCell(3, 3).getItemsView().size());

        assertTrue(controller.eraseAt(2, 2));
        assertTrue(controller.getDesignMap().getCell(2, 2).isPassable());
        assertTrue(controller.getDesignMap().getCell(2, 2).getItemsView().isEmpty());

        assertTrue(controller.eraseAt(3, 3));
        assertTrue(controller.getDesignMap().getCell(3, 3).isPassable());
        assertTrue(controller.getDesignMap().getCell(3, 3).getItemsView().isEmpty());

        assertTrue(controller.eraseAt(0, 0));
        assertFalse(controller.getDesignMap().getCell(0, 0).isPassable());
    }

    @Test
    void addFiveRandomItemsAlsoCreatesHiddenSearchableItem() {
        BuildModeController controller = controller();

        BuildRandomItemPlacer.Result result = controller.addFiveRandomItems();

        assertEquals(BuildRandomItemPlacer.VISIBLE_ITEM_COUNT, result.visibleItemsPlaced());
        assertTrue(result.hiddenItemPlaced());
        assertEquals(BuildRandomItemPlacer.VISIBLE_ITEM_COUNT + 1, countCellItems(controller.getDesignMap()));
        assertEquals(1, countSearchablesWithHiddenItems(controller.getDesignMap()));
    }

    @Test
    void randomItemsCanOnlyBeAddedThreeTimesPerDesignMap() {
        BuildModeController controller = controller();

        assertEquals(BuildModeController.MAX_RANDOM_ITEM_ADDS, controller.getRemainingRandomItemAdds());
        for (int i = 0; i < BuildModeController.MAX_RANDOM_ITEM_ADDS; i++) {
            controller.addFiveRandomItems();
        }

        assertFalse(controller.canAddFiveRandomItems());
        assertEquals(0, controller.getRemainingRandomItemAdds());
        int itemCount = countCellItems(controller.getDesignMap());

        BuildRandomItemPlacer.Result rejected = controller.addFiveRandomItems();
        assertEquals(0, rejected.visibleItemsPlaced());
        assertFalse(rejected.hiddenItemPlaced());
        assertEquals(itemCount, countCellItems(controller.getDesignMap()));

        controller.clearMap();
        assertTrue(controller.canAddFiveRandomItems());
        assertEquals(BuildModeController.MAX_RANDOM_ITEM_ADDS, controller.getRemainingRandomItemAdds());
    }

    @Test
    void searchableToolsCanOnlyBePlacedOnTopAndBottomWalls() {
        BuildModeController controller = controller();

        for (BuildTool tool : controller.getTools()) {
            if (tool.previewItem() instanceof SearchableObject) {
                assertHorizontalWallOnly(controller, tool);
            }
        }
        assertHorizontalWallOnly(controller, controller.findTool("POOL"));
    }

    @Test
    void wallToolsCanBePlacedOnEverySideAndInsideTheMap() {
        BuildModeController controller = controller();
        DungeonMap map = controller.getDesignMap();

        for (BuildTool tool : controller.getTools()) {
            if (tool.isWallBrush() || tool.isWallObject()) {
                assertWallPlacement(controller, tool, 0, 3);
                assertWallPlacement(controller, tool, map.getWidth() - 1, 3);
                assertWallPlacement(controller, tool, 3, 0);
                assertWallPlacement(controller, tool, 3, map.getHeight() - 1);
                assertWallPlacement(controller, tool, 3, 3);
            }
        }
    }

    @Test
    void doorToolsCanBePlacedOnEverySideAndInsideTheMap() {
        BuildModeController controller = controller();
        DungeonMap map = controller.getDesignMap();

        for (BuildTool tool : controller.getTools()) {
            if (tool.isDoorObject()) {
                assertDoorPlacement(controller, tool, 0, 3);
                assertDoorPlacement(controller, tool, map.getWidth() - 1, 3);
                assertDoorPlacement(controller, tool, 3, 0);
                assertDoorPlacement(controller, tool, 3, map.getHeight() - 1);
                assertDoorPlacement(controller, tool, 3, 3);
            }
        }
    }

    @Test
    void openChestVariantsCanBePlacedFromTheCatalog() {
        BuildModeController controller = controller();

        assertChestVariant(controller, "CHEST_OPEN_EMPTY_BLUE", 2, 2, true);
        assertChestVariant(controller, "CHEST_OPEN_LOOT_BLUE", 3, 2, false);
        assertChestVariant(controller, "CHEST_ORANGE_OPEN_EMPTY_1", 4, 2, true);
        assertChestVariant(controller, "CHEST_ORANGE_OPEN_LOOT_1", 5, 2, false);
    }

    @Test
    void lockedChestAssignsKeyToEmptySearchableAndReportsPlacement() {
        BuildModeController controller = controller();
        MissingBrick searchable = new MissingBrick();
        controller.getDesignMap().getCell(3, 0).getItems().add(searchable);

        assertTrue(controller.placeToolAt(3, 3, controller.findTool("CHEST")));

        Chest chest = assertInstanceOf(Chest.class,
                controller.getDesignMap().getCell(3, 3).getItemsView().get(0));
        Key key = assertInstanceOf(Key.class, searchable.getHiddenItem());
        assertTrue(key.matches(chest.getRequiredKeyId()));
        assertTrue(controller.getLastPlacementMessage().contains(chest.getName()));
        assertTrue(controller.getLastPlacementMessage().contains(key.getName()));
        assertTrue(controller.getLastPlacementMessage().contains("hidden in Missing Brick"));
    }

    @Test
    void lockedChestAssignsKeyToFloorWhenNoEmptySearchableExists() {
        BuildModeController controller = controller();

        assertTrue(controller.placeToolAt(3, 3, controller.findTool("CHEST")));

        Chest chest = assertInstanceOf(Chest.class,
                controller.getDesignMap().getCell(3, 3).getItemsView().get(0));
        assertTrue(hasGroundKeyMatching(controller.getDesignMap(), chest.getRequiredKeyId()));
        assertTrue(controller.getLastPlacementMessage().contains("placed on floor"));
    }

    @Test
    void saveLoadRoundTripRestoresWallsObjectsAndHiddenItems() throws IOException {
        BuildModeController controller = controller();
        controller.placeToolAt(2, 2, controller.findTool("HEAL"));
        controller.placeToolAt(3, 3, controller.findTool("WALL"));
        controller.placeToolAt(4, 0, controller.findTool("POOL"));
        controller.placeToolAt(5, 5, controller.findTool("LOCKED_CHEST"));

        Path path = tempDir.resolve("roundtrip.dkmap");
        controller.saveMap(path);
        String json = Files.readString(path);
        assertTrue(json.contains("\"schema\": \"DungeonKUrawler.DungeonMap\""));
        assertTrue(json.contains("\"contents\""));
        assertTrue(json.contains("\"locked\": true"));
        controller.clearMap();

        controller.loadMap(path);
        DungeonMap loaded = controller.getDesignMap();

        assertInstanceOf(HealPotion.class, loaded.getCell(2, 2).getItemsView().get(0));
        assertFalse(loaded.getCell(3, 3).isPassable());
        assertTrue(loaded.getCell(3, 3).getItemsView().isEmpty());

        Item wallSearchable = loaded.getCell(4, 0).getItemsView().get(0);
        SearchableObject searchableObject = assertInstanceOf(SearchableObject.class, wallSearchable);
        assertNotNull(searchableObject.getHiddenItem());
    }

    private BuildModeController controller() {
        return new BuildModeController(new BuildToolCatalog(), new BuildMapFactory(),
                new StandardBuildPlacementStrategy(), new Random(4));
    }

    private int countCellItems(DungeonMap map) {
        int count = 0;
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                count += map.getCell(x, y).getItemsView().size();
            }
        }
        return count;
    }

    private void assertHorizontalWallOnly(BuildModeController controller, BuildTool tool) {
        DungeonMap map = controller.getDesignMap();
        assertFalse(controller.placeToolAt(3, 3, tool), tool.id());
        assertTrue(map.getCell(3, 3).getItemsView().isEmpty(), tool.id());

        assertFalse(controller.placeToolAt(0, 3, tool), tool.id());
        assertTrue(map.getCell(0, 3).getItemsView().isEmpty(), tool.id());

        assertTrue(controller.placeToolAt(3, 0, tool), tool.id());
        assertInstanceOf(SearchableObject.class, map.getCell(3, 0).getItemsView().get(0), tool.id());
        controller.eraseAt(3, 0);

        assertTrue(controller.placeToolAt(3, map.getHeight() - 1, tool), tool.id());
        assertInstanceOf(SearchableObject.class,
                map.getCell(3, map.getHeight() - 1).getItemsView().get(0), tool.id());
        controller.eraseAt(3, map.getHeight() - 1);
    }

    private void assertWallPlacement(BuildModeController controller, BuildTool tool, int x, int y) {
        GridCell cell = controller.getDesignMap().getCell(x, y);
        assertTrue(controller.placeToolAt(x, y, tool), tool.id());
        assertFalse(cell.isPassable(), tool.id());
        if (tool.isWallObject()) {
            assertFalse(cell.getItemsView().isEmpty(), tool.id());
        } else {
            assertTrue(cell.getItemsView().isEmpty(), tool.id());
        }
        controller.eraseAt(x, y);
    }

    private void assertDoorPlacement(BuildModeController controller, BuildTool tool, int x, int y) {
        GridCell cell = controller.getDesignMap().getCell(x, y);
        assertTrue(controller.placeToolAt(x, y, tool), tool.id());
        assertFalse(cell.getItemsView().isEmpty(), tool.id());
        assertEquals(tool.id().equals("DOOR_OPEN"), cell.isPassable(), tool.id());
        controller.eraseAt(x, y);
    }

    private void assertChestVariant(BuildModeController controller, String toolId, int x, int y,
            boolean expectEmpty) {
        assertTrue(controller.placeToolAt(x, y, controller.findTool(toolId)), toolId);
        model.Chest chest = assertInstanceOf(model.Chest.class,
                controller.getDesignMap().getCell(x, y).getItemsView().get(0), toolId);
        assertEquals(expectEmpty, chest.getContents().isEmpty(), toolId);
        assertFalse(chest.isLocked(), toolId);
    }

    private boolean hasGroundKeyMatching(DungeonMap map, String requiredKeyId) {
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                for (Item item : map.getCell(x, y).getItemsView()) {
                    if (item instanceof Key key && key.matches(requiredKeyId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int countSearchablesWithHiddenItems(DungeonMap map) {
        int count = 0;
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                GridCell cell = map.getCell(x, y);
                if (!cell.getItemsView().isEmpty()
                        && cell.getItemsView().get(0) instanceof SearchableObject searchableObject
                        && searchableObject.getHiddenItem() != null) {
                    count++;
                }
            }
        }
        return count;
    }
}
