package view;

import javax.swing.SwingUtilities;

import engine.DungeonLevelFactory;
import engine.GameEngine;
import engine.GameStateSnapshot;
import engine.LevelCompletionResult;
import engine.TowerProgressController;
import engine.audio.AudioManager;
import model.DungeonLevel;
import model.ValuableItem;

/**
 * GRASP Controller / Indirection for the map to gameplay transition. Lives in the
 * view layer because it opens and disposes Swing windows; the progression rules
 * and persistence stay in {@link TowerProgressController}. It builds a floor's
 * {@link GameEngine} via {@link DungeonLevelFactory}, wires completion handling,
 * and routes the player back to the tower map when a floor is cleared.
 */
public final class TowerSessionController {

    private final TowerProgressController progress;
    private final DungeonLevelFactory levelFactory = new DungeonLevelFactory();

    private TowerMapWindow mapWindow;
    private GameWindow gameWindow;

    public TowerSessionController(TowerProgressController progress) {
        this.progress = progress;
    }

    /** Opens (or reopens) the tower map showing current floor statuses. */
    public void openTowerMap() {
        mapWindow = new TowerMapWindow(progress, this::enterLevel, this::returnToMainMenu);
        mapWindow.setVisible(true);
    }

    /**
     * UC-T2: enters the chosen floor. The map already gates locked/hidden floors
     * (no ENTER button), but we re-check here as the authority before building a
     * session. The persistent hero carries over from the active session.
     */
    private void enterLevel(int levelNumber) {
        if (!progress.canEnter(levelNumber)) {
            return;
        }
        DungeonLevel level = progress.getLevel(levelNumber);
        GameStateSnapshot snapshot = GameStateSnapshot.of(progress.getActiveEngine());
        GameEngine engine = levelFactory.createEngine(level, snapshot);

        progress.setActiveEngine(engine);
        engine.setLevelCompletionListener(progress);
        progress.setOnLevelCompleted(result ->
                SwingUtilities.invokeLater(() -> onFloorCleared(result)));

        AudioManager.shared().stopMenuMusic();
        if (mapWindow != null) {
            mapWindow.dispose();
            mapWindow = null;
        }
        gameWindow = new GameWindow(engine);
        gameWindow.setVisible(true);
    }

    /**
     * UC-T3/T4: the floor was cleared (progress already advanced and persisted by
     * {@link TowerProgressController}). Close the gameplay window and return to
     * the tower map, surfacing a save warning or final-victory notice.
     */
    private void onFloorCleared(LevelCompletionResult result) {
        GameEngine completedEngine = progress.getActiveEngine();
        ValuableItem target = completedEngine == null || completedEngine.getTargetMission() == null
                ? null : completedEngine.getTargetMission().getTarget();
        AudioManager.shared().play("victory");
        MissionSplashDialog.showVictory(gameWindow, target);

        if (gameWindow != null) {
            gameWindow.dispose();
            gameWindow = null;
        }

        if (progress.lastSaveError() != null) {
            ItemActionMenuDialog.showNotice(null, "Tower", "Save Failed",
                    "Your progress for this floor could not be saved. "
                            + "It will not persist if you quit now.");
        }

        if (result.finalLevel()) {
            ItemActionMenuDialog.showNotice(null, "Tower", "Tower Cleared",
                    "You have conquered the final floor. The tower is yours!");
        }
        openTowerMap();
    }

    /** Convenience for the menu: build a session around loaded progress and show the map. */
    public static void startFrom(TowerProgressController progress) {
        new TowerSessionController(progress).openTowerMap();
    }

    private void returnToMainMenu() {
        if (mapWindow != null) {
            mapWindow.dispose();
            mapWindow = null;
        }
        AudioManager.shared().startMenuMusic();
        new MainMenuWindow().setVisible(true);
    }
}
