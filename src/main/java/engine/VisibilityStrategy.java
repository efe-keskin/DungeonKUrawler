package engine;

import model.DungeonMap;

/**
 * Strategy for "can the hero see this cell?" - the central
 * Fear-of-the-Dark rule that varies. Pure: same inputs produce
 * same output, no mutation, no I/O.
 */
public interface VisibilityStrategy {
    boolean isVisible(DungeonMap map, int heroX, int heroY,
                      int cellX, int cellY);
}
