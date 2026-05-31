package engine;

import java.util.Objects;
import java.util.function.Consumer;

import model.DungeonLevel;
import model.LevelStatus;
import model.TowerProgress;
import model.TowerScenario;
import save.LoadedGame;
import save.SaveGameController;
import save.SaveGameException;
import save.SaveDtos.SaveDescriptor;

/**
 * GRASP Controller for the tower use cases: load a save's progress, decide
 * which floors can be entered, and complete a floor (apply rewards, unlock the
 * next floor, persist). Progression rules live in {@link TowerProgress}; this
 * controller orchestrates them with the save facade and the carry-over game
 * session, keeping that logic out of the Swing views.
 *
 * <p>Per-level dungeon generation and the {@code GameWindow} hand-off are
 * intentionally not here yet; they arrive with {@code DungeonLevelFactory}
 * and the session controller. This class exposes the seams they will use
 * ({@link #setActiveEngine}, {@link #getActiveEngine}).
 */
public final class TowerProgressController implements LevelCompletionListener {

    private final TowerScenario scenario;
    private final SaveGameController saveController;

    private TowerProgress progress;
    private SaveDescriptor currentSave;
    /** Carry-over session: holds the persistent hero/inventory/gold across floors. */
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
     * UC-T1: loads the selected save, initializing default tower progress when
     * the save predates tower mode. The loaded session becomes the carry-over
     * engine. Returns the progress so the caller can open the tower map.
     */
    public TowerProgress loadFromSave(SaveDescriptor descriptor) throws SaveGameException {
        LoadedGame loaded = saveController.loadGameWithProgress(descriptor);
        this.currentSave = descriptor;
        this.activeEngine = loaded.engine();
        this.progress = loaded.towerProgress();
        return this.progress;
    }

    /**
     * Begins a brand-new tower run with default progress (Level 1 unlocked) and
     * no carry-over session; the factory mints a fresh hero on the first floor.
     * The first completed floor creates a new save slot.
     */
    public TowerProgress startNewRun() {
        this.currentSave = null;
        this.activeEngine = null;
        this.progress = TowerProgress.defaultProgress(scenario.size());
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
     * playing (set by the session controller when a floor launches). The hero
     * inside it is the one whose progress gets persisted on completion.
     */
    public void setActiveEngine(GameEngine engine) {
        this.activeEngine = engine;
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

    /**
     * UC-T3 / UC-T4: marks the floor completed, awards its reward gold exactly
     * once, unlocks the next floor, and persists everything back into the save
     * slot the player loaded from.
     *
     * @return the configuration of the completed floor
     * @throws SaveGameException if persistence fails (progress is still updated
     *         in memory so the caller can offer a retry)
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
        }

        currentSave = saveController.updateSave(currentSave, activeEngine, progress);
        return level;
    }

    /**
     * Persists the current carry-over session and progress into the existing
     * save slot. Used for changes made outside floor completion (e.g. shop
     * buy/sell). No-op when there is no loaded save slot yet — the changes stay
     * in memory and are written on the next floor completion.
     */
    public void saveActiveProgress() throws SaveGameException {
        if (progress == null || currentSave == null || activeEngine == null) {
            return;
        }
        currentSave = saveController.updateSave(currentSave, activeEngine, progress);
    }

    /**
     * Persists the current in-floor scenario state as a checkpoint. This is the
     * manual Save Game path while playing a tower floor.
     */
    public SaveDescriptor saveCheckpoint(GameEngine engine, String saveName) throws SaveGameException {
        if (progress == null) {
            progress = TowerProgress.defaultProgress(scenario.size());
        }
        activeEngine = engine;
        currentSave = saveController.saveScenarioCheckpoint(engine, progress, saveName);
        return currentSave;
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
