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
