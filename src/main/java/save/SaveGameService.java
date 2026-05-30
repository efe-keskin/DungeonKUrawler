package save;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import engine.GameEngine;
import model.TowerProgress;
import model.TowerScenario;
import save.SaveDtos.SaveDescriptor;
import save.SaveDtos.SaveGameDto;

/**
 * Facade for the save/load use case.
 */
public final class SaveGameService {

    public static final int MAX_SAVE_FILES = 10;

    private final SaveFileRepository repository;
    private final GameStateMapper mapper;

    public SaveGameService() {
        this(new SaveFileRepository(new GsonSaveSerializer()), new GameStateMapper());
    }

    public SaveGameService(SaveFileRepository repository, GameStateMapper mapper) {
        this.repository = Objects.requireNonNull(repository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    public SaveDescriptor saveGame(GameEngine engine, String saveName) throws SaveGameException {
        return saveCustomGame(engine, saveName);
    }

    public SaveDescriptor saveGame(GameEngine engine, TowerProgress towerProgress, String saveName)
            throws SaveGameException {
        SaveGameType type = towerProgress == null ? SaveGameType.CUSTOM_GAME : SaveGameType.SCENARIO_CHECKPOINT;
        return saveGame(engine, towerProgress, saveName, type);
    }

    public SaveDescriptor saveCustomGame(GameEngine engine, String saveName) throws SaveGameException {
        return saveGame(engine, null, saveName, SaveGameType.CUSTOM_GAME);
    }

    public SaveDescriptor saveScenarioCheckpoint(GameEngine engine, TowerProgress towerProgress, String saveName)
            throws SaveGameException {
        return saveGame(engine, towerProgress, saveName, SaveGameType.SCENARIO_CHECKPOINT);
    }

    private SaveDescriptor saveGame(GameEngine engine, TowerProgress towerProgress, String saveName,
            SaveGameType saveType) throws SaveGameException {
        if (saveName == null || saveName.isBlank()) {
            throw new SaveGameException("Save name is required.");
        }
        if (repository.list().size() >= MAX_SAVE_FILES) {
            throw new SaveLimitExceededException(MAX_SAVE_FILES);
        }
        SaveGameDto dto = new SaveGameDto();
        dto.saveType = saveType.name();
        dto.saveName = saveName.trim();
        dto.savedAt = OffsetDateTime.now().toString();
        dto.towerLevelNumber = engine == null ? 0 : engine.getTowerLevelNumber();
        dto.finalTowerLevel = engine != null && engine.isFinalTowerLevel();
        dto.gameState = mapper.toDto(engine, towerProgress);
        return repository.write(dto);
    }

    /**
     * Persists the current session and tower progress back into an existing
     * save slot (overwriting it in place) rather than creating a new file.
     * Used by the tower flow after a level is completed.
     */
    public SaveDescriptor updateSave(SaveDescriptor descriptor, GameEngine engine, TowerProgress towerProgress)
            throws SaveGameException {
        SaveGameDto dto = new SaveGameDto();
        SaveGameType type = towerProgress == null ? SaveGameType.CUSTOM_GAME : SaveGameType.SCENARIO_PROGRESS;
        dto.saveType = type.name();
        dto.saveName = descriptor == null || descriptor.getSaveName() == null
                ? "save" : descriptor.getSaveName();
        dto.savedAt = OffsetDateTime.now().toString();
        dto.towerLevelNumber = engine == null ? 0 : engine.getTowerLevelNumber();
        dto.finalTowerLevel = engine != null && engine.isFinalTowerLevel();
        dto.gameState = mapper.toDto(engine, towerProgress);
        return repository.overwrite(descriptor, dto);
    }

    public List<SaveDescriptor> listSaves() throws SaveGameException {
        return repository.list();
    }

    public List<SaveDescriptor> listSaves(SaveGameType saveType) throws SaveGameException {
        if (saveType == null) {
            return listSaves();
        }
        return repository.list().stream()
                .filter(save -> save.getSaveType() == saveType)
                .toList();
    }

    public GameEngine loadGame(SaveDescriptor descriptor) throws SaveGameException {
        return mapper.toEngine(repository.read(descriptor));
    }

    /**
     * Loads a save as both its game session and tower progress in a single
     * read. Saves predating tower mode yield {@link TowerProgress#defaultProgress}.
     */
    public LoadedGame loadGameWithProgress(SaveDescriptor descriptor) throws SaveGameException {
        SaveGameDto saveGame = repository.read(descriptor);
        GameEngine engine = mapper.toEngine(saveGame);
        TowerProgress progress = mapper.toTowerProgress(
                saveGame.gameState == null ? null : saveGame.gameState.towerProgress,
                TowerScenario.LEVEL_COUNT);
        return new LoadedGame(engine, progress);
    }

    public void deleteSave(SaveDescriptor descriptor) throws SaveGameException {
        repository.delete(descriptor);
    }
}
