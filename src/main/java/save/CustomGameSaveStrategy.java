package save;

import engine.GameEngine;
import save.SaveDtos.SaveDescriptor;

/** Saves a custom-map gameplay session independently from scenario progress. */
public final class CustomGameSaveStrategy implements GameSaveStrategy {

    private final SaveGameController controller;

    public CustomGameSaveStrategy() {
        this(new SaveGameController());
    }

    public CustomGameSaveStrategy(SaveGameController controller) {
        this.controller = controller == null ? new SaveGameController() : controller;
    }

    @Override
    public SaveDescriptor save(GameEngine engine, String saveName) throws SaveGameException {
        return controller.saveCustomGame(engine, saveName);
    }
}
