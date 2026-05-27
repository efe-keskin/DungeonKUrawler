package engine;

import model.Hero;
import model.Inventory;
import model.Item;
import model.Potion;

/**
 * GRASP Controller for movement in Play Mode.
 * Receives movement input from the UI, checks passability through the map,
 * and asks GameEngine to update state if the move is valid.
 */
public class PlayerModeController {

    private static final int ENERGY_PER_MOVE = 5;

    private final GameEngine engine;

    public PlayerModeController(GameEngine engine) {
        this.engine = engine;
    }

    public void moveHero(Direction direction) {
        if (direction == null || engine.isPaused()) {
            return;
        }

        Hero hero = engine.getHero();
        if (hero.getEnergy() < ENERGY_PER_MOVE) {
            return;
        }

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

        hero.consumeEnergy(ENERGY_PER_MOVE);
        engine.updateHeroPosition(nx, ny);
    }

    /**
     * Consumes the first {@link Potion} in the hero's inventory and applies its effect.
     *
     * @return true if a potion was consumed; false if inventory has no potion.
     */
    public boolean consumePotion() {
        Hero hero = engine.getHero();
        Inventory inv = hero.getInventory();
        for (Item item : inv.getItems()) {
            if (item instanceof Potion potion) {
                engine.consumePotion(potion);
                return true;
            }
        }
        return false;
    }

    /**
     * Drinks a potion lying on the hero's current tile (if any).
     * Does not require the potion to be in the inventory.
     *
     * @return true if a ground potion was drunk.
     */
    public boolean consumePotionOnGround() {
        return engine.consumePotionOnGround();
    }
}