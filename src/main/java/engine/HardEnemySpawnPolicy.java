package engine;

import java.util.Objects;
import java.util.Random;

import model.Entity;
import model.Knight;
import model.Sorcerer;

/**
 * Tougher spawn policy for HARD / VERY_HARD floors: faster spawns, a higher
 * enemy cap, sturdier enemies, and no "no-enemy" roll. The {@code veryHard}
 * flag bumps stats and cap further so floors 8-9 outclass floors 6-7.
 */
public final class HardEnemySpawnPolicy implements EnemySpawnPolicy {

    private final Random random;
    private final boolean veryHard;

    public HardEnemySpawnPolicy(Random random, boolean veryHard) {
        this.random = Objects.requireNonNull(random, "random");
        this.veryHard = veryHard;
    }

    @Override
    public int spawnIntervalMs() {
        return veryHard ? 4500 : 6000;
    }

    @Override
    public int maxEnemies() {
        return veryHard ? 8 : 7;
    }

    @Override
    public Entity createEnemy(int x, int y) {
        // 65% Knight, 35% Sorcerer — no empty roll on hard floors.
        if (random.nextInt(100) < 65) {
            int hp = veryHard ? 34 : 28;
            int str = veryHard ? 11 : 10;
            int def = veryHard ? 6 : 5;
            return new Knight(x, y, "Knight", hp, str, def, 6);
        }
        int hp = veryHard ? 16 : 14;
        return new Sorcerer(x, y, "Sorcerer", hp, 40, 4, random.nextBoolean());
    }
}
