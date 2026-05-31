package save;

import engine.GameEngine;
import save.SaveDtos.SaveDescriptor;

/**
 * GoF Strategy for saving the current gameplay session. The view does not need
 * to know whether the active session belongs to scenario mode or custom maps.
 */
public interface GameSaveStrategy {

    SaveDescriptor save(GameEngine engine, String saveName) throws SaveGameException;

    /**
     * Whether this strategy needs the player to supply a save name. Scenario
     * floors save into the already-named current playthrough, so they return
     * {@code false} and the view skips the name prompt.
     */
    default boolean requiresName() {
        return true;
    }
}
