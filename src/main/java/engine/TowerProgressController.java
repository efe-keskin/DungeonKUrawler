package engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import model.DungeonLevel;
import model.Hero;
import model.LevelStatus;
import model.TowerProgress;
import model.TowerScenario;
import save.LoadedScenario;
import save.SaveGameController;
import save.SaveGameException;
import save.SaveDtos.LevelSaveDto;
import save.SaveDtos.SaveDescriptor;

/**
 * GRASP Controller for the tower use cases: own the named scenario save, decide
 * which floors can be entered, carry the persistent hero across floors, and
 * complete a floor (apply rewards, unlock the next floor, persist). Progression
 * rules live in {@link TowerProgress}; this controller orchestrates them with
 * the save facade so that logic stays out of the Swing views.
 *
 * <p>One scenario save = one named playthrough. Its long-term state (carry-over
 * hero + {@link TowerProgress}) and its per-level resumable saves all live in a
 * single file; this controller holds those pieces in memory and writes the whole
 * aggregate back in place, so a per-level save never spawns a new file.
 */
public final class TowerProgressController implements LevelCompletionListener {

    private final TowerScenario scenario;
    private final SaveGameController saveController;
    private final DungeonLevelFactory levelFactory = new DungeonLevelFactory();

    private TowerProgress progress;
    private SaveDescriptor currentSave;
    /** Persistent meta-state (inventory, gold, pets) carried between floors. */
    private Hero persistentHero;
    /** Per-level resumable saves embedded in this playthrough, keyed by floor. */
    private final List<LevelSaveDto> levelSaves = new ArrayList<>();
    /** Carry-over session: holds the hero currently being played on a floor. */
    private GameEngine activeEngine;
    /** Optional view hook fired after a completed floor is persisted (e.g. return to map). */
    private Consumer<LevelCompletionResult> onLevelCompleted;
    /** Last persistence failure during automatic completion handling, if any. */
    private SaveGameException lastSaveError;

    public TowerProgressController(SaveGameController saveController) {
        this(saveController, TowerScenario.defaultScenario());
    }

    public TowerProgressController(SaveGameController saveController, TowerScenario scenario) {
        this.saveController = Objects.requireNonNull(saveController, "saveController");
        this.scenario = Objects.requireNonNull(scenario, "scenario");
    }

    /**
     * UC-T1 (New Game): creates a brand-new named playthrough with default
     * progress (Level 1 unlocked), a fresh hero (no gold, empty inventory, no
     * pets) and no per-level saves, and writes the save file immediately so it
     * appears in the selection screen.
     */
    public TowerProgress startNewRun(String saveName) throws SaveGameException {
        this.progress = TowerProgress.defaultProgress(scenario.size());
        this.persistentHero = levelFactory.defaultHero();
        this.activeEngine = null;
        this.levelSaves.clear();
        this.currentSave = saveController.saveScenario(null, saveName, persistentHero, progress, levelSaves);
        return this.progress;
    }

    /**
     * UC-T1 (Continue): loads the selected scenario save into memory. The
     * progression, carry-over hero and per-level saves are restored; no floor is
     * active until the player enters one from the tower map.
     */
    public TowerProgress loadFromSave(SaveDescriptor descriptor) throws SaveGameException {
        LoadedScenario loaded = saveController.loadScenario(descriptor);
        this.currentSave = descriptor;
        this.progress = loaded.towerProgress();
        this.persistentHero = loaded.hero();
        this.activeEngine = null;
        this.levelSaves.clear();
        this.levelSaves.addAll(loaded.levelSaves());
        return this.progress;
    }

    public TowerScenario getScenario() {
        return scenario;
    }

    public TowerProgress getProgress() {
        return progress;
    }

    public SaveDescriptor getCurrentSave() {
        return currentSave;
    }

    public GameEngine getActiveEngine() {
        return activeEngine;
    }

    /**
     * Replaces the carry-over session with the engine the player is actively
     * playing (set by the session controller when a floor launches).
     */
    public void setActiveEngine(GameEngine engine) {
        this.activeEngine = engine;
    }

    /**
     * The hero/mission to seed the next floor with: the active floor's hero when
     * one is in play, otherwise the persistent carry-over hero (e.g. the first
     * floor of a freshly loaded or new run).
     */
    public GameStateSnapshot getCarryOverSnapshot() {
        return new GameStateSnapshot(getCarryOverHero(), null);
    }

    /**
     * The hero whose persistent state (inventory, gold, pets) the tower map's
     * shop and inventory windows act on: the active floor's hero when one is in
     * play, otherwise the persistent carry-over hero (e.g. right after loading a
     * save, before entering a floor).
     */
    public Hero getCarryOverHero() {
        if (activeEngine != null && activeEngine.getHero() != null) {
            return activeEngine.getHero();
        }
        return persistentHero;
    }

    public LevelStatus statusOf(int levelNumber) {
        return progress.statusOf(levelNumber);
    }

    public boolean canEnter(int levelNumber) {
        return progress != null && progress.canEnter(levelNumber);
    }

    public DungeonLevel getLevel(int levelNumber) {
        return scenario.getLevel(levelNumber);
    }

    /** Whether the given floor has a resumable per-level save in this playthrough. */
    public boolean hasLevelSave(int levelNumber) {
        return findLevelSave(levelNumber) != null;
    }

    /**
     * Rebuilds and adopts the engine for the given floor's resumable save. The
     * restored engine becomes the active session.
     *
     * @throws SaveGameException if the floor has no saved state or it cannot be read
     */
    public GameEngine resumeLevelEngine(int levelNumber) throws SaveGameException {
        LevelSaveDto saved = findLevelSave(levelNumber);
        if (saved == null) {
            throw new SaveGameException("This floor has no saved state.");
        }
        this.activeEngine = saveController.restoreLevel(saved);
        return this.activeEngine;
    }

    /**
     * Discards the resumable save for a floor (e.g. the player chose to start it
     * again) and persists the change. No-op when the floor has no saved state.
     */
    public void clearLevelSave(int levelNumber) throws SaveGameException {
        if (levelSaves.removeIf(save -> save.levelNumber == levelNumber)) {
            persistScenario();
        }
    }

    /**
     * Manual in-floor Save: captures the current floor state into this
     * playthrough's per-level save for {@code levelNumber}, replacing any prior
     * one, and writes the whole scenario aggregate back to the same file. The
     * long-term hero and progression are untouched.
     */
    public SaveDescriptor saveLevelState(GameEngine engine, int levelNumber) throws SaveGameException {
        if (progress == null) {
            throw new SaveGameException("No scenario is loaded.");
        }
        this.activeEngine = engine;
        LevelSaveDto captured = saveController.captureLevel(engine);
        captured.levelNumber = levelNumber;
        levelSaves.removeIf(save -> save.levelNumber == levelNumber);
        levelSaves.add(captured);
        persistScenario();
        return currentSave;
    }

    /**
     * UC-T3 / UC-T4: marks the floor completed, awards its reward gold exactly
     * once, unlocks the next floor, refreshes the carry-over hero, drops this
     * floor's now-stale resumable save, and persists everything back into the
     * scenario file.
     */
    public DungeonLevel completeLevel(int levelNumber) throws SaveGameException {
        requireLoaded();
        DungeonLevel level = scenario.getLevel(levelNumber);

        boolean alreadyCompleted = progress.statusOf(levelNumber) == LevelStatus.COMPLETED;
        progress.completeLevel(levelNumber);
        if (activeEngine != null && activeEngine.getHero() != null) {
            // UC-4: persist this floor's valuables into the full-game inventory
            // before the save write; the per-level bag is discarded next floor.
            activeEngine.getHero().commitLevelLoot();
            if (!alreadyCompleted) {
                activeEngine.getHero().earnCoins(level.rewardGold());
            }
            persistentHero = activeEngine.getHero();
        }
        levelSaves.removeIf(save -> save.levelNumber == levelNumber);
        persistScenario();
        return level;
    }

    /**
     * Persists changes made outside floor completion (e.g. shop buy/sell, pet
     * equip) into the scenario file. No-op when there is no loaded scenario.
     */
    public void saveActiveProgress() throws SaveGameException {
        if (progress == null || currentSave == null) {
            return;
        }
        if (activeEngine != null && activeEngine.getHero() != null) {
            persistentHero = activeEngine.getHero();
        }
        persistScenario();
    }

    private void persistScenario() throws SaveGameException {
        String saveName = currentSave == null || currentSave.getSaveName() == null
                ? "save" : currentSave.getSaveName();
        currentSave = saveController.saveScenario(currentSave, saveName, persistentHero, progress, levelSaves);
    }

    private LevelSaveDto findLevelSave(int levelNumber) {
        for (LevelSaveDto save : levelSaves) {
            if (save.levelNumber == levelNumber) {
                return save;
            }
        }
        return null;
    }

    /**
     * Observer entry point: invoked by {@link GameEngine} when the active floor
     * is completed. Advances and persists progress, recording any save failure
     * (a listener cannot throw checked exceptions), then notifies the optional
     * view callback so it can return to the tower map / show final victory.
     */
    @Override
    public void onLevelCompleted(LevelCompletionResult result) {
        lastSaveError = null;
        try {
            completeLevel(result.levelNumber());
        } catch (SaveGameException ex) {
            lastSaveError = ex;
        }
        if (onLevelCompleted != null) {
            onLevelCompleted.accept(result);
        }
    }

    /** Sets the view hook fired after a completed floor is persisted. */
    public void setOnLevelCompleted(Consumer<LevelCompletionResult> callback) {
        this.onLevelCompleted = callback;
    }

    /** The save failure from the most recent {@link #onLevelCompleted}, or null. */
    public SaveGameException lastSaveError() {
        return lastSaveError;
    }

    private void requireLoaded() {
        if (progress == null) {
            throw new IllegalStateException("No tower progress loaded; call loadFromSave first.");
        }
    }
}
