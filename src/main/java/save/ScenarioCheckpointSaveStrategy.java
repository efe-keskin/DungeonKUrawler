package save;

import engine.GameEngine;
import engine.TowerProgressController;
import save.SaveDtos.SaveDescriptor;

/**
 * Saves an in-floor scenario checkpoint together with the current tower
 * progression.
 */
public final class ScenarioCheckpointSaveStrategy implements GameSaveStrategy {

    private final TowerProgressController progressController;

    public ScenarioCheckpointSaveStrategy(TowerProgressController progressController) {
        this.progressController = progressController;
    }

    @Override
    public SaveDescriptor save(GameEngine engine, String saveName) throws SaveGameException {
        if (progressController == null) {
            throw new SaveGameException("No scenario progress is available.");
        }
        return progressController.saveCheckpoint(engine, saveName);
    }
}
