package engine;

import model.Hero;

/**
 * GRASP Controller for movement in Play Mode.
 * Receives movement input from the UI, checks passability through the map,
 * and asks GameEngine to update state if the move is valid.
 */
public class PlayerModeController {

    private final GameEngine engine;

    public PlayerModeController(GameEngine engine) {
        this.engine = engine;
    }

    public void moveHero(Direction direction) {
        if (direction == null) {
            return;
        }

        Hero hero = engine.getHero();

        int nx = hero.getX();
        int ny = hero.getY();

        switch (direction) {
            case UP -> ny--;
            case DOWN -> ny++;
            case LEFT -> nx--;
            case RIGHT -> nx++;
        }

        if (!engine.getDungeonMap().isCellPassable(nx, ny)) {
            return;
        }

        engine.updateHeroPosition(nx, ny);
    }
}