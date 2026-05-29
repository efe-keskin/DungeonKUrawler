package engine;

import java.util.Objects;
import java.util.Random;

import model.Entity;
import model.Knight;
import model.Sorcerer;

/**
 * Spawn policy for the Level 5 boss arena. The boss itself is placed on the map
 * by {@link DungeonLevelFactory}; this policy governs the elite adds that
 * reinforce it — infrequent, few at a time, but sturdy.
 */
public final class BossEnemySpawnPolicy implements EnemySpawnPolicy {

    private final Random random;

    public BossEnemySpawnPolicy(Random random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    @Override
    public int spawnIntervalMs() {
        return 8000;
    }

    @Override
    public int maxEnemies() {
        // Boss + a small honor guard.
        return 3;
    }

    @Override
    public Entity createEnemy(int x, int y) {
        // Mostly armored guards, with the occasional support caster.
        if (random.nextInt(100) < 75) {
            return new Knight(x, y, "Warden's Guard", 32, 11, 6, 7);
        }
        return new Sorcerer(x, y, "Warden's Acolyte", 16, 45, 4, random.nextBoolean());
    }
}
