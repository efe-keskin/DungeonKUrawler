package view;

import javax.swing.SwingUtilities;

import engine.DungeonLevelFactory;
import engine.GameEngine;
import engine.GameStateSnapshot;
import engine.LevelCompletionResult;
import engine.PetController;
import engine.ShopController;
import engine.TowerProgressController;
import engine.audio.AudioManager;
import model.DungeonLevel;
import model.FullGameInventory;
import model.Hero;
import model.ShopCatalog;
import model.TowerScenario;
import model.ValuableItem;
import save.SaveGameException;

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
    private ShopWindow shopWindow;
    private TowerInventoryWindow inventoryWindow;
    private int currentFloor = 1;

    public TowerSessionController(TowerProgressController progress) {
        this.progress = progress;
    }

    /** Opens (or reopens) the tower map showing current floor statuses. */
    public void openTowerMap() {
        if (progress.getProgress() != null) {
            currentFloor = progress.getProgress().highestUnlockedLevel();
        }
        openTowerMap(currentFloor, -1);
    }

    private void openTowerMap(int heroFloor, int climbToFloor) {
        currentFloor = heroFloor;
        mapWindow = new TowerMapWindow(progress, this::enterLevel, this::returnToMainMenu,
                this::openShop, this::openInventory, heroFloor, climbToFloor,
                this::debugSkipToLevel);
        mapWindow.setVisible(true);
    }

    private void openShop() {
        if (mapWindow != null) {
            mapWindow.dispose();
            mapWindow = null;
        }
        FullGameInventory fullInventory = new FullGameInventory();
        if (progress.getActiveEngine() != null && progress.getActiveEngine().getHero() != null) {
            fullInventory = progress.getActiveEngine().getHero().getFullInventory();
        }
        ShopController shopController = new ShopController(fullInventory, new ShopCatalog());
        shopWindow = new ShopWindow(fullInventory, shopController, () -> {
            shopWindow = null;
            persistProgress();
            openTowerMap(currentFloor, -1);
        });
        shopWindow.setVisible(true);
    }

    /** Opens the persistent full-game inventory from the tower map's inventory icon. */
    private void openInventory() {
        if (mapWindow != null) {
            mapWindow.dispose();
            mapWindow = null;
        }
        FullGameInventory fullInventory = new FullGameInventory();
        PetController petController = null;
        if (progress.getActiveEngine() != null && progress.getActiveEngine().getHero() != null) {
            Hero hero = progress.getActiveEngine().getHero();
            fullInventory = hero.getFullInventory();
            petController = new PetController(hero);
        }
        inventoryWindow = new TowerInventoryWindow(fullInventory, petController, this::persistProgress, () -> {
            inventoryWindow = null;
            openTowerMap(currentFloor, -1);
        });
        inventoryWindow.setVisible(true);
    }

    /** Persists in-memory changes made outside gameplay (shop buy/sell, pet equip). */
    private void persistProgress() {
        try {
            progress.saveActiveProgress();
        } catch (SaveGameException ex) {
            ItemActionMenuDialog.showNotice(null, "Tower", "Save Failed",
                    "Your changes could not be saved.");
        }
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
        currentFloor = levelNumber;
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
        if (levelNumber == 5) {
            showFearOfTheDarkIntro();
        }
        gameWindow.setVisible(true);
    }

    /**
     * Debug entry point: jumps to {@code levelNumber} without
     * consulting progress.canEnter. Used by the SKIP TO LEVEL
     * button on the tower map for testing higher floors without
     * playing through the prerequisites. Logs to stderr so the
     * developer can confirm it fired.
     */
    private void debugSkipToLevel(int levelNumber) {
        if (levelNumber < 1 || levelNumber > TowerScenario.LEVEL_COUNT) {
            return;
        }
        System.err.println("[debug] Skipping to level " + levelNumber);
        currentFloor = levelNumber;
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
        if (levelNumber == 5) {
            showFearOfTheDarkIntro();
        }
        gameWindow.setVisible(true);
    }

    /**
     * Shows the Fear-of-the-Dark introduction popup on the level
     * where the mechanic first appears. Starts the audio cue at the
     * moment the dialog opens; the cue continues whether or not the
     * player has closed the dialog.
     */
    private void showFearOfTheDarkIntro() {
        if (AudioManager.shared() != null) {
            AudioManager.shared().playFearOfTheDark();
        }
        ItemActionMenuDialog.showNotice(
                gameWindow,
                "Floor Briefing",
                "Fear of the Dark",
                "The deeper floors of this tower hold lightless dread.\n"
                        + "Your vision is now limited - explore with care.\n"
                        + "Search for a Torch to push back the darkness.");
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
            openTowerMap(result.levelNumber(), -1);
            return;
        }
        openTowerMap(result.levelNumber(), result.levelNumber() + 1);
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
        if (shopWindow != null) {
            shopWindow.dispose();
            shopWindow = null;
        }
        if (inventoryWindow != null) {
            inventoryWindow.dispose();
            inventoryWindow = null;
        }
        AudioManager.shared().startMenuMusic();
        new MainMenuWindow().setVisible(true);
    }
}
