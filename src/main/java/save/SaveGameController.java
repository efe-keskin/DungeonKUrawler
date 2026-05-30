package save;

import java.util.List;

import engine.GameEngine;
import model.TowerProgress;
import save.SaveDtos.SaveDescriptor;

/**
 * GRASP Controller for the Save/Load Game use case.
 */
public final class SaveGameController {

    private final SaveGameService service;

    public SaveGameController() {
        this(new SaveGameService());
    }

    public SaveGameController(SaveGameService service) {
        this.service = service;
    }

    public SaveDescriptor saveGame(GameEngine engine, String saveName) throws SaveGameException {
        return service.saveGame(engine, saveName);
    }

    public SaveDescriptor saveCustomGame(GameEngine engine, String saveName) throws SaveGameException {
        return service.saveCustomGame(engine, saveName);
    }

    public SaveDescriptor saveGame(GameEngine engine, TowerProgress towerProgress, String saveName)
            throws SaveGameException {
        return service.saveGame(engine, towerProgress, saveName);
    }

    public SaveDescriptor saveScenarioCheckpoint(GameEngine engine, TowerProgress towerProgress, String saveName)
            throws SaveGameException {
        return service.saveScenarioCheckpoint(engine, towerProgress, saveName);
    }

    public List<SaveDescriptor> listSaves() throws SaveGameException {
        return service.listSaves();
    }

    public List<SaveDescriptor> listSaves(SaveGameType saveType) throws SaveGameException {
        return service.listSaves(saveType);
    }

    public GameEngine loadGame(SaveDescriptor descriptor) throws SaveGameException {
        return service.loadGame(descriptor);
    }

    public LoadedGame loadGameWithProgress(SaveDescriptor descriptor) throws SaveGameException {
        return service.loadGameWithProgress(descriptor);
    }

    public SaveDescriptor updateSave(SaveDescriptor descriptor, GameEngine engine, TowerProgress towerProgress)
            throws SaveGameException {
        return service.updateSave(descriptor, engine, towerProgress);
    }

    public void deleteSave(SaveDescriptor descriptor) throws SaveGameException {
        service.deleteSave(descriptor);
    }
}
