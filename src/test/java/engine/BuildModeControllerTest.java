package engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import model.DungeonMap;
import model.GridCell;
import model.HealPotion;
import model.Item;
import model.ItemAction;
import model.BreakableObject;
import model.Chest;
import model.Column;
import model.Container;
import model.Crate;
import model.Key;
import model.MissingBrick;
import model.SearchableObject;
import model.ValuableItem;
import model.Vase;
import model.WaterPipe;

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
    void randomItemAdditionsNeverCreateValuableObjectives() {
        BuildModeController controller = controller();

        for (int i = 0; i < BuildModeController.MAX_RANDOM_ITEM_ADDS; i++) {
            controller.addFiveRandomItems();
        }

        assertEquals(0, countValuableItems(controller.getDesignMap()));
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
    void movedBreakableToolsCanBePlacedInsideMapWithoutSearchAction() {
        BuildModeController controller = controller();

        assertBreakableOnly(controller, "COLUMN", Column.class, 3, 3);
        assertBreakableOnly(controller, "COLUMN_PURPLE", Column.class, 4, 3);
        assertBreakableOnly(controller, "COLUMN_TOP", Column.class, 5, 3);
        assertBreakableOnly(controller, "VASE", Vase.class, 6, 3);
        assertBreakableOnly(controller, "WATER_PIPE", WaterPipe.class, 7, 3);
    }

    @Test
    void cratesRemainSearchableAndBreakableInBothPaletteGroups() {
        BuildModeController controller = controller();

        for (String id : java.util.List.of(
                "CRATE", "CRATE_WOOD_RIGHT", "CRATE_ORANGE",
                "BREAKABLE_CRATE", "BREAKABLE_CRATE_WOOD_RIGHT", "BREAKABLE_CRATE_ORANGE")) {
            BuildTool tool = controller.findTool(id);
            assertNotNull(tool, id);
            Crate crate = assertInstanceOf(Crate.class, tool.previewItem(), id);
            assertTrue(crate.getInventoryActions().contains(ItemAction.SEARCH), id);
            assertTrue(crate.getInventoryActions().contains(ItemAction.BREAK), id);
        }
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
    void doorToolsCanOnlyBePlacedOnOuterSides() {
        BuildModeController controller = controller();
        DungeonMap map = controller.getDesignMap();

        for (BuildTool tool : controller.getTools()) {
            if (tool.isDoorObject()) {
                assertDoorPlacement(controller, tool, 0, 3);
                assertDoorPlacement(controller, tool, map.getWidth() - 1, 3);
                assertDoorPlacement(controller, tool, 3, 0);
                assertDoorPlacement(controller, tool, 3, map.getHeight() - 1);
                assertFalse(controller.placeToolAt(3, 3, tool), tool.id());
                assertTrue(map.getCell(3, 3).getItemsView().isEmpty(), tool.id());
                assertEquals(BuildModeController.BORDER_DOOR_ONLY_MESSAGE,
                        controller.getLastPlacementMessage(), tool.id());
            }
        }
    }

    @Test
    void playModeRequiresClosedDoorOnOuterSide() {
        BuildModeController controller = controller();
        DungeonMap map = controller.getDesignMap();

        assertEquals(BuildModeController.INVALID_DOOR_COUNT_MESSAGE,
                controller.getPlayModeValidationError());

        assertTrue(controller.placeToolAt(3, 0, controller.findTool("DOOR_OPEN")));
        assertEquals(BuildModeController.INVALID_DOOR_COUNT_MESSAGE,
                controller.getPlayModeValidationError());

        assertTrue(controller.placeToolAt(0, 3, controller.findTool("DOOR_CLOSED")));
        assertTrue(map.getCell(3, 0).getItemsView().isEmpty());
        assertFalse(map.getCell(3, 0).isPassable());
        assertTrue(controller.hasExactlyOneClosedBorderDoor());
        assertNull(controller.getPlayModeValidationError());
    }

    @Test
    void closedDoorRequirementSurvivesSaveLoadRoundTrip() throws IOException {
        BuildModeController controller = controller();
        assertTrue(controller.placeToolAt(0, 3, controller.findTool("DOOR_CLOSED")));

        Path path = tempDir.resolve("closed-door.dkmap");
        controller.saveMap(path);
        controller.clearMap();
        assertEquals(BuildModeController.INVALID_DOOR_COUNT_MESSAGE,
                controller.getPlayModeValidationError());

        controller.loadMap(path);
        assertTrue(controller.hasExactlyOneClosedBorderDoor());
        assertNull(controller.getPlayModeValidationError());
    }

    @Test
    void valuableMustBeHiddenInsideExistingSearchableOrBreakableObject() {
        BuildModeController controller = controller();
        BuildTool valuable = controller.findTool("VALUABLE_CRYSTAL");

        assertFalse(controller.placeToolAt(3, 3, valuable));
        assertEquals(BuildModeController.VALUABLE_HIDING_PLACE_ONLY_MESSAGE,
                controller.getLastPlacementMessage());

        assertTrue(controller.placeToolAt(3, 0, controller.findTool("MISSING_BRICK")));
        SearchableObject searchable = assertInstanceOf(SearchableObject.class,
                controller.getDesignMap().getCell(3, 0).getItemsView().get(0));

        assertTrue(controller.placeToolAt(3, 0, valuable));
        assertInstanceOf(ValuableItem.class, searchable.getHiddenItem());
        assertTrue(controller.getLastPlacementMessage().contains("Crystal Shard hidden in Missing Brick"));
    }

    @Test
    void placingAnotherValuableRelocatesTheSingleAuthoredObjective() {
        BuildModeController controller = controller();

        assertTrue(controller.placeToolAt(3, 0, controller.findTool("MISSING_BRICK")));
        SearchableObject searchable = assertInstanceOf(SearchableObject.class,
                controller.getDesignMap().getCell(3, 0).getItemsView().get(0));
        assertTrue(controller.placeToolAt(4, 4, controller.findTool("COLUMN")));
        BreakableObject breakable = assertInstanceOf(BreakableObject.class,
                controller.getDesignMap().getCell(4, 4).getItemsView().get(0));

        assertTrue(controller.placeToolAt(3, 0, controller.findTool("VALUABLE_CRYSTAL")));
        assertTrue(controller.placeToolAt(4, 4, controller.findTool("VALUABLE_IDOL")));

        assertNull(searchable.getHiddenItem());
        ValuableItem relocated = assertInstanceOf(ValuableItem.class, breakable.getHiddenItem());
        assertEquals("Golden Idol", relocated.getName());
        assertEquals(1, countValuableItems(controller.getDesignMap()));
    }

    @Test
    void valuablePlacementDoesNotOverwriteExistingHiddenLootOrRemoveCurrentObjective() {
        BuildModeController controller = controller();
        assertTrue(controller.placeToolAt(3, 0, controller.findTool("MISSING_BRICK")));
        SearchableObject occupied = assertInstanceOf(SearchableObject.class,
                controller.getDesignMap().getCell(3, 0).getItemsView().get(0));
        HealPotion hiddenLoot = new HealPotion();
        occupied.setHiddenItem(hiddenLoot);
        assertTrue(controller.placeToolAt(4, 4, controller.findTool("COLUMN")));
        BreakableObject current = assertInstanceOf(BreakableObject.class,
                controller.getDesignMap().getCell(4, 4).getItemsView().get(0));
        assertTrue(controller.placeToolAt(4, 4, controller.findTool("VALUABLE_CRYSTAL")));

        assertFalse(controller.placeToolAt(3, 0, controller.findTool("VALUABLE_IDOL")));

        assertEquals(BuildModeController.VALUABLE_HIDING_PLACE_OCCUPIED_MESSAGE,
                controller.getLastPlacementMessage());
        assertEquals("Crystal Shard", current.getHiddenItem().getName());
        assertEquals(hiddenLoot, occupied.getHiddenItem());
        assertEquals(1, countValuableItems(controller.getDesignMap()));
    }

    @Test
    void breakableHiddenValuableSurvivesBuildMapSaveLoadRoundTrip() throws IOException {
        BuildModeController controller = controller();
        assertTrue(controller.placeToolAt(4, 4, controller.findTool("COLUMN")));
        assertTrue(controller.placeToolAt(4, 4, controller.findTool("VALUABLE_IDOL")));

        Path path = tempDir.resolve("breakable-objective.dkmap");
        controller.saveMap(path);
        controller.clearMap();
        controller.loadMap(path);

        BreakableObject breakable = assertInstanceOf(BreakableObject.class,
                controller.getDesignMap().getCell(4, 4).getItemsView().get(0));
        ValuableItem valuable = assertInstanceOf(ValuableItem.class, breakable.getHiddenItem());
        assertEquals("Golden Idol", valuable.getName());
    }

    @Test
    void valuableCanBeHiddenInsideChestWithoutRemovingExistingLoot() {
        BuildModeController controller = controller();
        assertTrue(controller.placeToolAt(4, 4, controller.findTool("CHEST_OPEN_LOOT_BLUE")));
        Chest chest = assertInstanceOf(Chest.class,
                controller.getDesignMap().getCell(4, 4).getItemsView().get(0));
        assertEquals(1, chest.getContents().size());

        assertTrue(controller.placeToolAt(4, 4, controller.findTool("VALUABLE_IDOL")));

        assertEquals(2, chest.getContents().size());
        ValuableItem valuable = assertInstanceOf(ValuableItem.class, chest.getContents().get(1));
        assertEquals("Golden Idol", valuable.getName());
        assertEquals(1, countValuableItems(controller.getDesignMap()));
    }

    @Test
    void keyCanBeHiddenInsideChestWithoutReplacingChest() {
        BuildModeController controller = controller();
        assertTrue(controller.placeToolAt(4, 4, controller.findTool("CHEST_OPEN_EMPTY_BLUE")));
        Chest chest = assertInstanceOf(Chest.class,
                controller.getDesignMap().getCell(4, 4).getItemsView().get(0));

        assertTrue(controller.placeToolAt(4, 4, controller.findTool("KEY_ORANGE")));

        assertEquals(1, controller.getDesignMap().getCell(4, 4).getItemsView().size());
        Key key = assertInstanceOf(Key.class, chest.getContents().get(0));
        assertEquals("orange", key.getKeyId());
        assertTrue(controller.getLastPlacementMessage().contains("hidden in Open Empty Blue Chest"));
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
    void lockedChestUsesSearchableFixturePlacedFromPaletteBeforeFloorFallback() {
        BuildModeController controller = controller();

        assertTrue(controller.placeToolAt(3, 0, controller.findTool("GARGOYLE")));
        SearchableObject searchable = assertInstanceOf(SearchableObject.class,
                controller.getDesignMap().getCell(3, 0).getItemsView().get(0));
        assertTrue(searchable.getHiddenItem() == null);

        assertTrue(controller.placeToolAt(3, 3, controller.findTool("CHEST")));

        Chest chest = assertInstanceOf(Chest.class,
                controller.getDesignMap().getCell(3, 3).getItemsView().get(0));
        Key key = assertInstanceOf(Key.class, searchable.getHiddenItem());
        assertTrue(key.matches(chest.getRequiredKeyId()));
        assertFalse(hasGroundKeyMatching(controller.getDesignMap(), chest.getRequiredKeyId()));
        assertTrue(controller.getLastPlacementMessage().contains("hidden in Stone Gargoyle"));
    }

    @Test
    void lockedChestKeyCanBeHiddenInsideExistingOpenChest() {
        BuildModeController controller = controller();
        assertTrue(controller.placeToolAt(2, 2, controller.findTool("CHEST_OPEN_EMPTY_BLUE")));
        Chest openChest = assertInstanceOf(Chest.class,
                controller.getDesignMap().getCell(2, 2).getItemsView().get(0));

        assertTrue(controller.placeToolAt(3, 3, controller.findTool("CHEST")));

        Chest lockedChest = assertInstanceOf(Chest.class,
                controller.getDesignMap().getCell(3, 3).getItemsView().get(0));
        Key key = assertInstanceOf(Key.class, openChest.getContents().get(0));
        assertTrue(key.matches(lockedChest.getRequiredKeyId()));
        assertTrue(controller.getLastPlacementMessage().contains("hidden in Open Empty Blue Chest"));
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

    @Test
    void saveLoadRoundTripRestoresBreakableAndDualRoleCrateBehavior() throws IOException {
        BuildModeController controller = controller();
        controller.placeToolAt(3, 3, controller.findTool("COLUMN"));
        controller.placeToolAt(4, 0, controller.findTool("CRATE"));

        Path path = tempDir.resolve("breakables-roundtrip.dkmap");
        controller.saveMap(path);
        controller.clearMap();
        controller.loadMap(path);

        Column column = assertInstanceOf(Column.class,
                controller.getDesignMap().getCell(3, 3).getItemsView().get(0));
        assertTrue(column.getInventoryActions().contains(ItemAction.BREAK));
        assertFalse(column.getInventoryActions().contains(ItemAction.SEARCH));

        Crate crate = assertInstanceOf(Crate.class,
                controller.getDesignMap().getCell(4, 0).getItemsView().get(0));
        assertTrue(crate.getInventoryActions().contains(ItemAction.SEARCH));
        assertTrue(crate.getInventoryActions().contains(ItemAction.BREAK));
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

    private void assertBreakableOnly(BuildModeController controller, String toolId,
            Class<? extends BreakableObject> type, int x, int y) {
        BuildTool tool = controller.findTool(toolId);
        assertNotNull(tool, toolId);
        assertTrue(controller.placeToolAt(x, y, tool), toolId);
        BreakableObject object = assertInstanceOf(type,
                controller.getDesignMap().getCell(x, y).getItemsView().get(0), toolId);
        assertTrue(object.getInventoryActions().contains(ItemAction.BREAK), toolId);
        assertFalse(object.getInventoryActions().contains(ItemAction.SEARCH), toolId);
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

    private int countValuableItems(DungeonMap map) {
        int count = 0;
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                for (Item item : map.getCell(x, y).getItemsView()) {
                    count += countValuableItems(item);
                }
            }
        }
        return count;
    }

    private int countValuableItems(Item item) {
        if (item instanceof ValuableItem) {
            return 1;
        }
        if (item instanceof SearchableObject searchableObject) {
            return countValuableItems(searchableObject.getHiddenItem());
        }
        if (item instanceof BreakableObject breakableObject) {
            return countValuableItems(breakableObject.getHiddenItem());
        }
        if (item instanceof Container container) {
            return container.getContents().stream().mapToInt(this::countValuableItems).sum();
        }
        return 0;
    }
}
