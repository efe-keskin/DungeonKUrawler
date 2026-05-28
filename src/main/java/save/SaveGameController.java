package save;

import java.util.List;

import engine.GameEngine;
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

    public List<SaveDescriptor> listSaves() throws SaveGameException {
        return service.listSaves();
    }

    public GameEngine loadGame(SaveDescriptor descriptor) throws SaveGameException {
        return service.loadGame(descriptor);
    }

    public void deleteSave(SaveDescriptor descriptor) throws SaveGameException {
        service.deleteSave(descriptor);
    }
}
