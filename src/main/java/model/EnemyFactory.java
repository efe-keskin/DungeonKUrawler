package model;

import java.util.Random;

/**
 * Simple Factory (GoF): centralizes construction of random enemy types so spawn probabilities live in
 * one place, not in the controller.
 *
 * <p>Probabilities: 60% {@link Knight}, 30% {@link Sorcerer}, 10% no enemy ({@code null}).
 */
public class EnemyFactory {

    private final Random random;

    public EnemyFactory(Random random) {
        this.random = random;
    }

    /**
     * Rolls 0–99: 0–59 Knight, 60–89 Sorcerer, 90–99 none.
     *
     * @return a new enemy at {@code (x,y)}, or {@code null} if the roll is “none”
     */
    public Entity createRandomEnemy(int x, int y) {
        int roll = random.nextInt(100);
        if (roll >= 90) {
            return null;
        }
        if (roll < 60) {
            return new Knight(x, y, "Knight", 40, 8, 4, 5);
        }
        return new Sorcerer(x, y, "Sorcerer", 25, 30, 3, random.nextBoolean());
    }
}
