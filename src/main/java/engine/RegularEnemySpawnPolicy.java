package engine;

import java.util.Objects;

import model.EnemyFactory;
import model.Entity;

/**
 * Default spawn policy reproducing the base-game behavior (spec 2.5): a spawn
 * attempt every 9s, at most 5 enemies, with the {@link EnemyFactory} mix of
 * 60% Knight / 30% Sorcerer / 10% none. Used for EASY/MEDIUM floors and as the
 * fallback when no level-specific policy is supplied.
 */
public final class RegularEnemySpawnPolicy implements EnemySpawnPolicy {

    private static final int SPAWN_INTERVAL_MS = 9000;
    private static final int MAX_ENEMIES = 5;

    private final EnemyFactory enemyFactory;

    public RegularEnemySpawnPolicy(EnemyFactory enemyFactory) {
        this.enemyFactory = Objects.requireNonNull(enemyFactory, "enemyFactory");
    }

    @Override
    public int spawnIntervalMs() {
        return SPAWN_INTERVAL_MS;
    }

    @Override
    public int maxEnemies() {
        return MAX_ENEMIES;
    }

    @Override
    public Entity createEnemy(int x, int y) {
        return enemyFactory.createRandomEnemy(x, y);
    }
}
