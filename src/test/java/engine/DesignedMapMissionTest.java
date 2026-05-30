package engine;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import model.Chest;
import model.DungeonMap;
import model.GridCell;
import model.Key;
import model.MissingBrick;
import model.ValuableItem;

class DesignedMapMissionTest {

    @Test
    void emptyDesignedMapReceivesMissionChestAndWinCondition() {
        DungeonMap map = new BuildMapFactory().createEmptyMap("Designed", 8, 8);
        GameEngine engine = new GameEngine(map);
        try {
            assertTrue(engine.getTargetMission().isStarted());
            assertTrue(hasMissionChest(map));
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
            assertTrue(engine.isMissionVictory());
            assertTrue(engine.isGameOver());
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

    private boolean hasMissionChest(DungeonMap map) {
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GridCell cell = map.getCell(x, y);
                if (cell.getItemsView().stream()
                        .anyMatch(item -> item instanceof Chest chest
                                && chest.getName().equals("Mission Chest"))) {
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
                }
            }
        }
        return false;
    }
}
