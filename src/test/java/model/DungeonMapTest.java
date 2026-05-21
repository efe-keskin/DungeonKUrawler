package model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link DungeonMap#isHeroAdjacent(Hero, int, int)}.
 *
 * requires: hero is not null.
 * modifies: nothing.
 * effects:
 *   Returns true if the target coordinates are adjacent
 *   to the hero's current position, including diagonal
 *   neighbors and the same cell.
 *   Returns false otherwise.
 */
class DungeonMapTest {

    /**
     * Case: target cell is the same as the hero's current position.
     * Expected: returns true.
     */
    @Test
    void isHeroAdjacent_sameCell_returnsTrue() {
        DungeonMap map = new DungeonMap("Level1", 10, 10);
        Hero hero = new Hero(5, 5, "Hero", 100, 10, 20, 5, 30);

        boolean result = map.isHeroAdjacent(hero, 5, 5);

        assertTrue(result);
    }

    /**
     * Case: target cell is diagonally adjacent to the hero.
     * Expected: returns true.
     */
    @Test
    void isHeroAdjacent_diagonalCell_returnsTrue() {
        DungeonMap map = new DungeonMap("Level1", 10, 10);
        Hero hero = new Hero(5, 5, "Hero", 100, 10, 20, 5, 30);

        boolean result = map.isHeroAdjacent(hero, 6, 6);

        assertTrue(result);
    }

    /**
     * Case: target cell is farther than one cell away from the hero.
     * Expected: returns false.
     */
    @Test
    void isHeroAdjacent_farCell_returnsFalse() {
        DungeonMap map = new DungeonMap("Level1", 10, 10);
        Hero hero = new Hero(5, 5, "Hero", 100, 10, 20, 5, 30);

        boolean result = map.isHeroAdjacent(hero, 8, 8);

        assertFalse(result);
    }
}