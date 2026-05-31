package engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.Arch;
import model.BossEnemy;
import model.Chest;
import model.Container;
import model.DungeonLevel;
import model.DungeonMap;
import model.Entity;
import model.GridCell;
import model.Item;
import model.Key;
import model.SearchableObject;
import model.Torch;
import model.TowerScenario;

import org.junit.jupiter.api.Test;

class DungeonLevelFactoryMapTest {

    private final TowerScenario scenario = TowerScenario.defaultScenario();

    @Test
    void packagedTowerMapsLoadAndBecomeProgressivelyMoreConstrained() throws IOException {
        DungeonLevelFactory factory = new DungeonLevelFactory(new java.util.Random(17));
        BuildMapPersistence persistence = new BuildMapPersistence(
                new BuildToolCatalog(), new BuildMapFactory(), new StandardBuildPlacementStrategy());
        int previousObstacleCount = -1;

        for (DungeonLevel level : scenario.getLevels()) {
            DungeonMap packaged = persistence.loadResource(
                    String.format("/maps/tower/floor%02d.json", level.levelNumber()));
            assertNotNull(packaged, level.levelName());

            DungeonMap map = factory.createMapForFloor(level);
            assertEquals(level.levelName(), map.getLevelName());
            assertEquals(level.fogHidden(), map.isFogEnabled(), level.levelName());
            assertTrue(isReachable(map, 1, 1, map.getWidth() - 2, map.getHeight() / 2), level.levelName());
            assertTrue(hasItem(map, Chest.class), level.levelName());
            assertTrue(hasItem(map, SearchableObject.class), level.levelName());
            assertTrue(hasKey(map, "arch-gold"), level.levelName());
            assertTrue(searchablesAreReachable(map), level.levelName());

            int obstacleCount = countInteriorObstacles(map);
            assertTrue(obstacleCount > previousObstacleCount,
                    level.levelName() + " obstacles=" + obstacleCount + ", previous=" + previousObstacleCount);
            previousObstacleCount = obstacleCount;

            if (level.fogHidden()) {
                assertTrue(hasItem(map, Torch.class), level.levelName());
            }
        }
    }

    @Test
    void bossFloorsKeepDesignedMapsAndReceiveExitArchAndBoss() {
        DungeonLevelFactory factory = new DungeonLevelFactory(new java.util.Random(23));

        for (int floorNumber : List.of(5, 10)) {
            GameEngine engine = factory.createEngine(scenario.getLevel(floorNumber), null);
            try {
                assertTrue(hasItem(engine.getDungeonMap(), Arch.class), "floor " + floorNumber);
                assertTrue(hasEntity(engine.getDungeonMap(), BossEnemy.class), "floor " + floorNumber);
            } finally {
                engine.shutdown();
            }
        }
    }

    private int countInteriorObstacles(DungeonMap map) {
        int count = 0;
        for (int y = 1; y < map.getHeight() - 1; y++) {
            for (int x = 1; x < map.getWidth() - 1; x++) {
                if (!map.getCell(x, y).isWalkable()) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean isReachable(DungeonMap map, int startX, int startY, int targetX, int targetY) {
        ArrayDeque<GridCell> queue = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        GridCell start = map.getCell(startX, startY);
        if (start == null || !start.isWalkable()) {
            return false;
        }
        queue.add(start);
        visited.add(id(map, startX, startY));
        while (!queue.isEmpty()) {
            GridCell cell = queue.removeFirst();
            if (cell.getX() == targetX && cell.getY() == targetY) {
                return true;
            }
            addWalkable(map, queue, visited, cell.getX() + 1, cell.getY());
            addWalkable(map, queue, visited, cell.getX() - 1, cell.getY());
            addWalkable(map, queue, visited, cell.getX(), cell.getY() + 1);
            addWalkable(map, queue, visited, cell.getX(), cell.getY() - 1);
        }
        return false;
    }

    private void addWalkable(DungeonMap map, ArrayDeque<GridCell> queue, Set<Integer> visited, int x, int y) {
        GridCell cell = map.getCell(x, y);
        int id = id(map, x, y);
        if (cell != null && cell.isWalkable() && visited.add(id)) {
            queue.addLast(cell);
        }
    }

    private int id(DungeonMap map, int x, int y) {
        return y * map.getWidth() + x;
    }

    private boolean hasItem(DungeonMap map, Class<? extends Item> type) {
        for (GridCell[] column : map.getCells()) {
            for (GridCell cell : column) {
                if (cell.getItemsView().stream().anyMatch(type::isInstance)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasEntity(DungeonMap map, Class<? extends Entity> type) {
        for (GridCell[] column : map.getCells()) {
            for (GridCell cell : column) {
                if (cell.getEntitiesView().stream().anyMatch(type::isInstance)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasKey(DungeonMap map, String keyId) {
        for (GridCell[] column : map.getCells()) {
            for (GridCell cell : column) {
                for (Item item : cell.getItemsView()) {
                    if (matchesKey(item, keyId)) {
                        return true;
                    }
                    if (item instanceof SearchableObject searchableObject
                            && matchesKey(searchableObject.getHiddenItem(), keyId)) {
                        return true;
                    }
                    if (item instanceof Container container
                            && container.getContents().stream().anyMatch(content -> matchesKey(content, keyId))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean matchesKey(Item item, String keyId) {
        return item instanceof Key key && key.matches(keyId);
    }

    private boolean searchablesAreReachable(DungeonMap map) {
        for (GridCell[] column : map.getCells()) {
            for (GridCell cell : column) {
                if (cell.getItemsView().stream().noneMatch(SearchableObject.class::isInstance)) {
                    continue;
                }
                boolean reachable = false;
                for (int dy = -1; dy <= 1 && !reachable; dy++) {
                    for (int dx = -1; dx <= 1 && !reachable; dx++) {
                        reachable = isReachable(map, 1, 1, cell.getX() + dx, cell.getY() + dy);
                    }
                }
                if (!reachable) {
                    return false;
                }
            }
        }
        return true;
    }
}
