package save;

import java.util.List;

import engine.GameEngine;
import model.Hero;
import model.TowerProgress;
import save.SaveDtos.LevelSaveDto;
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

    public SaveDescriptor saveScenario(SaveDescriptor existing, String saveName, Hero hero,
            TowerProgress towerProgress, List<LevelSaveDto> levelSaves) throws SaveGameException {
        return service.saveScenario(existing, saveName, hero, towerProgress, levelSaves);
    }

    public LoadedScenario loadScenario(SaveDescriptor descriptor) throws SaveGameException {
        return service.loadScenario(descriptor);
    }

    public LevelSaveDto captureLevel(GameEngine engine) throws SaveGameException {
        return service.captureLevel(engine);
    }

    public GameEngine restoreLevel(LevelSaveDto levelSave) throws SaveGameException {
        return service.restoreLevel(levelSave);
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

    public void deleteSave(SaveDescriptor descriptor) throws SaveGameException {
        service.deleteSave(descriptor);
    }
}
