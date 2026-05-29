package save;

import engine.GameEngine;
import model.TowerProgress;

/**
 * Result of loading a save: the restored {@link GameEngine} session plus the
 * player's {@link TowerProgress}. Bundled so a single repository read yields
 * both, instead of reading the save file twice.
 */
public record LoadedGame(GameEngine engine, TowerProgress towerProgress) {
}
