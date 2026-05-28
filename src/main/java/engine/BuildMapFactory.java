package engine;

import model.DungeonMap;
import model.GridCell;

/**
 * Creator for fresh build-mode maps.
 */
public final class BuildMapFactory {

    public DungeonMap createEmptyMap(String levelName, int width, int height) {
        DungeonMap map = new DungeonMap(levelName, width, height);
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GridCell cell = map.getCell(x, y);
                boolean border = x == 0 || y == 0 || x == map.getWidth() - 1 || y == map.getHeight() - 1;
                cell.setPassable(!border);
                cell.getItems().clear();
                cell.getEntities().clear();
            }
        }
        return map;
    }
}

