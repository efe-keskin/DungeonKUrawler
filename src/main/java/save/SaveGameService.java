package save;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import engine.GameEngine;
import model.Hero;
import model.TowerProgress;
import model.TowerScenario;
import save.SaveDtos.GameStateDto;
import save.SaveDtos.LevelSaveDto;
import save.SaveDtos.SaveDescriptor;
import save.SaveDtos.SaveGameDto;

/**
 * Facade for the save/load use case. Custom games persist a single session;
 * scenario runs persist one named {@code ScenarioSave} per playthrough whose
 * {@link GameStateDto} embeds the long-term progression and the per-level
 * resumable saves (see {@link #saveScenario}).
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

    // ----- Custom games -------------------------------------------------------

    public SaveDescriptor saveGame(GameEngine engine, String saveName) throws SaveGameException {
        return saveCustomGame(engine, saveName);
    }

    public SaveDescriptor saveCustomGame(GameEngine engine, String saveName) throws SaveGameException {
        if (saveName == null || saveName.isBlank()) {
            throw new SaveGameException("Save name is required.");
        }
        if (repository.list().size() >= MAX_SAVE_FILES) {
            throw new SaveLimitExceededException(MAX_SAVE_FILES);
        }
        SaveGameDto dto = new SaveGameDto();
        dto.saveType = SaveGameType.CUSTOM_GAME.name();
        dto.saveName = saveName.trim();
        dto.savedAt = OffsetDateTime.now().toString();
        dto.towerLevelNumber = engine == null ? 0 : engine.getTowerLevelNumber();
        dto.finalTowerLevel = engine != null && engine.isFinalTowerLevel();
        dto.gameState = mapper.toDto(engine);
        return repository.write(dto);
    }

    public GameEngine loadGame(SaveDescriptor descriptor) throws SaveGameException {
        return mapper.toEngine(repository.read(descriptor));
    }

    // ----- Scenario runs ------------------------------------------------------

    /**
     * Persists a named scenario playthrough. With a {@code null} descriptor this
     * creates a fresh save slot (subject to {@link #MAX_SAVE_FILES}); otherwise it
     * overwrites the existing slot in place so the same file is reused across
     * progression and per-level saves.
     */
    public SaveDescriptor saveScenario(SaveDescriptor existing, String saveName, Hero hero,
            TowerProgress progress, List<LevelSaveDto> levelSaves) throws SaveGameException {
        if (saveName == null || saveName.isBlank()) {
            throw new SaveGameException("Save name is required.");
        }
        boolean creatingNew = existing == null || existing.getPath() == null;
        if (creatingNew && repository.list().size() >= MAX_SAVE_FILES) {
            throw new SaveLimitExceededException(MAX_SAVE_FILES);
        }
        SaveGameDto dto = new SaveGameDto();
        dto.saveType = SaveGameType.SCENARIO.name();
        dto.saveName = saveName.trim();
        dto.savedAt = OffsetDateTime.now().toString();
        dto.towerLevelNumber = 0;
        dto.finalTowerLevel = false;
        dto.gameState = mapper.toScenarioState(hero, progress, levelSaves);
        return creatingNew ? repository.write(dto) : repository.overwrite(existing, dto);
    }

    /** Loads a scenario save into its in-memory pieces (hero, progress, level saves). */
    public LoadedScenario loadScenario(SaveDescriptor descriptor) throws SaveGameException {
        SaveGameDto saveGame = repository.read(descriptor);
        if (saveGame == null || saveGame.gameState == null) {
            throw new SaveGameException("Save file does not contain game state.");
        }
        GameStateDto state = saveGame.gameState;
        Hero hero = mapper.toPersistentHero(state.hero);
        TowerProgress progress = mapper.toTowerProgress(state.towerProgress, TowerScenario.LEVEL_COUNT);
        List<LevelSaveDto> levelSaves = state.levelSpecificSaves == null
                ? List.of() : List.copyOf(state.levelSpecificSaves);
        return new LoadedScenario(hero, progress, levelSaves);
    }

    /** Captures the current in-floor session as a resumable level save. */
    public LevelSaveDto captureLevel(GameEngine engine) throws SaveGameException {
        return mapper.toLevelSave(engine);
    }

    /** Rebuilds a playable engine from a resumable level save. */
    public GameEngine restoreLevel(LevelSaveDto levelSave) throws SaveGameException {
        return mapper.fromLevelSave(levelSave);
    }

    // ----- Listing / deletion -------------------------------------------------

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

    public void deleteSave(SaveDescriptor descriptor) throws SaveGameException {
        repository.delete(descriptor);
    }
}
