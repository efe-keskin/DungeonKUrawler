package save;

import engine.GameEngine;
import engine.TowerProgressController;
import save.SaveDtos.SaveDescriptor;

/**
 * Saves the current floor's state into the active scenario playthrough's
 * per-level save (see {@link TowerProgressController#saveLevelState}). It reuses
 * the playthrough's name, so no name prompt is needed.
 */
public final class ScenarioLevelSaveStrategy implements GameSaveStrategy {

    private final TowerProgressController progressController;

    public ScenarioLevelSaveStrategy(TowerProgressController progressController) {
        this.progressController = progressController;
    }

    @Override
    public SaveDescriptor save(GameEngine engine, String saveName) throws SaveGameException {
        if (progressController == null) {
            throw new SaveGameException("No scenario progress is available.");
        }
        return progressController.saveLevelState(engine, engine.getTowerLevelNumber());
    }

    @Override
    public boolean requiresName() {
        return false;
    }
}
