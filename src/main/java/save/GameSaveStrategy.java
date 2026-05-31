package save;

import engine.GameEngine;
import save.SaveDtos.SaveDescriptor;

/**
 * GoF Strategy for saving the current gameplay session. The view does not need
 * to know whether the active session belongs to scenario mode or custom maps.
 */
public interface GameSaveStrategy {

    SaveDescriptor save(GameEngine engine, String saveName) throws SaveGameException;
}
