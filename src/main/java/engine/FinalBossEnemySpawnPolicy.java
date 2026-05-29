package engine;

import java.util.Objects;
import java.util.Random;

import model.Entity;
import model.Knight;
import model.Sorcerer;

/**
 * Spawn policy for the Level 10 final boss throne room. The Dread King is placed
 * on the map by {@link DungeonLevelFactory}; this policy governs its relentless
 * elite reinforcements — faster and deadlier than the Level 5 guard.
 */
public final class FinalBossEnemySpawnPolicy implements EnemySpawnPolicy {

    private final Random random;

    public FinalBossEnemySpawnPolicy(Random random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    @Override
    public int spawnIntervalMs() {
        return 6000;
    }

    @Override
    public int maxEnemies() {
        return 5;
    }

    @Override
    public Entity createEnemy(int x, int y) {
        // A heavier guard rotation than the Level 5 boss.
        if (random.nextInt(100) < 60) {
            return new Knight(x, y, "Dread Sentinel", 40, 13, 7, 8);
        }
        return new Sorcerer(x, y, "Dread Cultist", 20, 50, 5, true);
    }
}
