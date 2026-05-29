package engine;

import model.Entity;

/**
 * GoF Strategy: encapsulates a floor's enemy cadence and mix so per-level rules
 * (faster spawns, tougher enemies, boss adds) stay out of {@link GameEngine}'s
 * timer logic. {@code DungeonLevelFactory} selects the implementation from a
 * level's difficulty/type.
 */
public interface EnemySpawnPolicy {

    /** Delay between spawn attempts, in milliseconds. */
    int spawnIntervalMs();

    /** Cap on simultaneously living enemies on the floor. */
    int maxEnemies();

    /**
     * Creates the next enemy to spawn at the given cell, or {@code null} when
     * the roll yields no enemy this tick.
     */
    Entity createEnemy(int x, int y);
}
