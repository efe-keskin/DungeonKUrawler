package engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import model.Chest;
import model.Arch;
import model.BreakableObject;
import model.Column;
import model.Container;
import model.DecorativeObject;
import model.DungeonMap;
import model.GridCell;
import model.Key;
import model.KeyColor;
import model.MissingBrick;
import model.SearchableObject;
import model.ValuableItem;

class DesignedMapMissionTest {

    @Test
    void emptyDesignedMapReceivesMissionChestAndWinCondition() {
        DungeonMap map = new BuildMapFactory().createEmptyMap("Designed", 8, 8);
        GameEngine engine = new GameEngine(map);
        try {
            assertTrue(engine.getTargetMission().isStarted());
            assertTrue(hasMissionChest(map));
            assertEquals(1, countValuables(map));
        } finally {
            engine.shutdown();
        }
    }

    @Test
    void searchableFixtureCanReceiveMissionTargetAndCompleteStandaloneGame() {
        DungeonMap map = new BuildMapFactory().createEmptyMap("Designed", 8, 8);
        MissingBrick searchable = new MissingBrick();
        map.getCell(3, 0).getItems().add(searchable);

        GameEngine engine = new GameEngine(map);
        try {
            ValuableItem target = engine.getTargetMission().getTarget();
            assertSame(target, searchable.getHiddenItem());

            assertTrue(engine.search(searchable).getFoundItem() == target);
            assertTrue(map.getCell(3, 0).getItemsView().contains(target));
            assertFalse(engine.isMissionVictory());
            assertFalse(engine.isGameOver());

            assertTrue(engine.takeItem(target, 3, 0));
            assertTrue(engine.isMissionVictory());
            assertTrue(engine.isGameOver());
        } finally {
            engine.shutdown();
        }
    }

    @Test
    void authoredHiddenValuableBecomesMissionTargetWithoutAddingAnotherOne() {
        DungeonMap map = new BuildMapFactory().createEmptyMap("Designed", 8, 8);
        ValuableItem authored = new ValuableItem("Golden Idol", "/items/valuable_items/golden_idol_64x64.png");
        MissingBrick searchable = new MissingBrick(authored);
        map.getCell(3, 0).getItems().add(searchable);

        GameEngine engine = new GameEngine(map);
        try {
            assertSame(authored, engine.getTargetMission().getTarget());
            assertSame(authored, searchable.getHiddenItem());
            assertEquals(1, countValuables(map));
        } finally {
            engine.shutdown();
        }
    }

    @Test
    void breakableFixtureCanReceiveGeneratedMissionTarget() {
        DungeonMap map = new BuildMapFactory().createEmptyMap("Designed", 8, 8);
        Column column = new Column();
        map.getCell(3, 3).getItems().add(column);

        GameEngine engine = new GameEngine(map);
        try {
            assertSame(engine.getTargetMission().getTarget(), column.getHiddenItem());
            assertEquals(1, countValuables(map));
        } finally {
            engine.shutdown();
        }
    }

    @Test
    void launchingDesignedMapRepairsMissingLockedChestKey() {
        DungeonMap map = new BuildMapFactory().createEmptyMap("Designed", 8, 8);
        Chest chest = Chest.locked("Locked Blue Chest", 16, "orange");
        map.getCell(3, 3).getItems().add(chest);

        GameEngine engine = new GameEngine(map);
        try {
            assertTrue(hasAccessibleMatchingKey(map, chest.getRequiredKeyId()));
        } finally {
            engine.shutdown();
        }
    }

    @Test
    void authoredDoorRequiresAssignedKeyAndCollectedValuableBeforeStandaloneVictory() {
        DungeonMap map = new BuildMapFactory().createEmptyMap("Designed", 8, 8);
        ValuableItem authored = new ValuableItem("Golden Idol", "/items/valuable_items/golden_idol_64x64.png");
        MissingBrick searchable = new MissingBrick(authored);
        Key authoredKey = new Key("door-silver", KeyColor.SILVER);
        map.getCell(3, 0).getItems().add(searchable);
        map.getCell(2, 2).getItems().add(authoredKey);
        addClosedBuildDoor(map, 7, 4);

        GameEngine engine = new GameEngine(map);
        try {
            Arch arch = findArch(map);
            assertEquals("door-silver", arch.getRequiredKeyId());
            assertEquals(GameEngine.ExitOpenResult.NO_MATCHING_KEY, engine.tryOpenExit(arch));

            assertTrue(engine.takeItem(authoredKey, 2, 2));
            assertEquals(GameEngine.ExitOpenResult.TREASURE_REQUIRED, engine.tryOpenExit(arch));

            assertSame(authored, engine.search(searchable).getFoundItem());
            assertFalse(engine.getTargetMission().isWon());
            assertTrue(engine.takeItem(authored, 3, 0));
            assertFalse(engine.isMissionVictory());
            assertFalse(engine.isGameOver());

            assertEquals(GameEngine.ExitOpenResult.OPENED, engine.tryOpenExit(arch));
            assertTrue(arch.isOpen());
            assertTrue(engine.isMissionVictory());
            assertTrue(engine.isGameOver());
        } finally {
            engine.shutdown();
        }
    }

    @Test
    void missingDoorKeyIsGeneratedAwayFromValuableFixture() {
        DungeonMap map = new BuildMapFactory().createEmptyMap("Designed", 8, 8);
        ValuableItem authored = new ValuableItem("Golden Idol", "/items/valuable_items/golden_idol_64x64.png");
        MissingBrick valuableFixture = new MissingBrick(authored);
        map.getCell(3, 0).getItems().add(valuableFixture);
        addClosedBuildDoor(map, 7, 4);

        GameEngine engine = new GameEngine(map);
        try {
            Arch arch = findArch(map);
            assertSame(authored, valuableFixture.getHiddenItem());
            assertTrue(hasAccessibleMatchingKey(map, arch.getRequiredKeyId()));
        } finally {
            engine.shutdown();
        }
    }

    private boolean hasMissionChest(DungeonMap map) {
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GridCell cell = map.getCell(x, y);
                if (cell.getItemsView().stream()
                        .anyMatch(item -> item instanceof Chest chest
                                && chest.getName().equals("Blue-Trimmed Chest"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasAccessibleMatchingKey(DungeonMap map, String requiredKeyId) {
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                for (model.Item item : map.getCell(x, y).getItemsView()) {
                    if (item instanceof Key key && key.matches(requiredKeyId)) {
                        return true;
                    }
                    if (item instanceof model.SearchableObject searchable
                            && searchable.getHiddenItem() instanceof Key key
                            && key.matches(requiredKeyId)) {
                        return true;
                    }
                    if (item instanceof BreakableObject breakableObject
                            && breakableObject.getHiddenItem() instanceof Key key
                            && key.matches(requiredKeyId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void addClosedBuildDoor(DungeonMap map, int x, int y) {
        GridCell cell = map.getCell(x, y);
        cell.setPassable(false);
        cell.getItems().add(new DecorativeObject("Door Closed", true,
                BuildToolCatalog.CLOSED_DOOR_SPRITE_RESOURCE));
    }

    private Arch findArch(DungeonMap map) {
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                for (model.Item item : map.getCell(x, y).getItemsView()) {
                    if (item instanceof Arch arch) {
                        return arch;
                    }
                }
            }
        }
        throw new AssertionError("Expected a runtime exit arch.");
    }

    private int countValuables(DungeonMap map) {
        int count = 0;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                for (model.Item item : map.getCell(x, y).getItemsView()) {
                    count += countValuables(item);
                }
            }
        }
        return count;
    }

    private int countValuables(model.Item item) {
        if (item instanceof ValuableItem) {
            return 1;
        }
        if (item instanceof SearchableObject searchableObject) {
            return countValuables(searchableObject.getHiddenItem());
        }
        if (item instanceof BreakableObject breakableObject) {
            return countValuables(breakableObject.getHiddenItem());
        }
        if (item instanceof Container container) {
            return container.getContents().stream().mapToInt(this::countValuables).sum();
        }
        return 0;
    }
}
