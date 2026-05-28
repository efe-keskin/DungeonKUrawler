package engine;

import model.DungeonMap;

/**
 * GoF Strategy for applying build mutations to a map cell.
 */
public interface BuildPlacementStrategy {

    boolean place(DungeonMap map, int x, int y, BuildTool tool);

    boolean erase(DungeonMap map, int x, int y);
}

