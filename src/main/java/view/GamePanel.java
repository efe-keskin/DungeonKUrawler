package view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import engine.CombatController;
import engine.CombatManager;
import engine.Direction;
import engine.FogOfWarEngine;
import engine.GameEngine;
import engine.GameMode;
import engine.InventoryController;
import engine.LockController;
import engine.PlayerModeController;
import engine.GameStateListener;
import engine.InteractionController;
import model.BossEnemy;
import model.Container;
import model.DefeatedEnemyMarker;
import model.DungeonMap;
import model.Entity;
import model.GridCell;
import model.Hero;
import model.HeroProjectileStyle;
import model.Item;
import model.Knight;
import model.PetEntity;
import model.Potion;
import model.Projectile;
import model.SearchableObject;
import model.Sorcerer;
import model.Weapon;
import save.SaveGameController;
import save.SaveGameException;
import save.SaveLimitExceededException;
import view.assets.SpriteRegistry;
import view.render.AmbienceRenderer;

/**
 * Observer: implements {@link GameStateListener} and repaints when the engine
 * notifies; no direct
 * model mutation. Input is forwarded to {@link GameEngine#moveHero(Direction)}
 * only; movement rules
 * stay in the controller.
 */
public class GamePanel extends JPanel implements GameStateListener {

    private static final int BASE_CELL = 28;

    /** Visible grid lines (retro tile border). */
    private static final Color GRID_LINE = new Color(55, 55, 62);
    private static final Color HERO = new Color(50, 130, 255);
    private static final Color KNIGHT = new Color(220, 55, 55);
    private static final Color SORCERER = new Color(160, 70, 220);
    private static final Color ITEM = new Color(255, 200, 40);
    private static final Color HUD_HP = new Color(220, 80, 80);
    private static final Color HUD_ENERGY = new Color(230, 200, 60);
    private static final Color HUD_COINS = new Color(235, 178, 45);
    private static final Color HUD_STR = new Color(205, 90, 70);
    private static final Color HUD_DEF = new Color(90, 140, 225);
    private static final Color HUD_MANA = new Color(140, 90, 220);
    private static final Color HUD_STONE_OUTLINE = new Color(5, 5, 9);
    private static final Color HUD_STONE_BORDER = new Color(103, 91, 75);
    private static final Color HUD_STONE_HIGHLIGHT = new Color(156, 131, 85);
    private static final Color HUD_PANEL_FILL = new Color(18, 17, 22);
    private static final Color HUD_PANEL_INSET = new Color(28, 25, 27);
    private static final Color HUD_GOLD = new Color(214, 170, 70);
    private static final Color HUD_TITLE = new Color(240, 222, 180);
    private static final Color HUD_TEXT = new Color(198, 190, 170);
    private static final Color ARENA_BACKDROP_TOP = new Color(18, 16, 20);
    private static final Color ARENA_BACKDROP_BOTTOM = new Color(9, 9, 13);
    private static final Color ARENA_FRAME_OUTLINE = new Color(5, 5, 9);
    private static final Color ARENA_FRAME_BORDER = new Color(71, 62, 54);
    private static final Color ARENA_FRAME_HIGHLIGHT = new Color(125, 103, 65);
    private static final Color ARENA_GOLD = new Color(164, 127, 53);
    private static final int ARENA_SHIFT_X = 30;
    private static final int ARENA_FRAME_PADDING = 12;
    private static final int ARENA_EDGE_MARGIN = 10;

    private static final int HERO_ANIM_INTERVAL_MS = 100;
    private static final int ENERGY_REFILL_INTERVAL_MS = 300;
    private static final int FOG_SHIMMER_INTERVAL_MS = 100;
    private static final float HERO_ANIM_STEP = 0.20f;
    private static final float ENEMY_ANIM_STEP = 0.25f;
    private static final float HERO_SPRITE_SCALE = 1.15f;
    private static final int KNIGHT_MAX_HP = 20;
    private static final int SORCERER_MAX_HP = 10;

    private final GameEngine engine;
    private final PlayerModeController playerModeController;
    private final InteractionController interactionController;
    private final CombatController combatController;
    private final SaveGameController saveGameController = new SaveGameController();
    private final AmbienceRenderer ambienceRenderer = new AmbienceRenderer();
    private final Timer heroAnimTimer;
    private final Timer energyRefillTimer;
    private final Timer fogShimmerTimer;
    private final long playStartTime = System.currentTimeMillis();
    private Timer continuousMoveTimer;
    private Timer transientWarningTimer;
    private Direction currentMovementDirection = null;
    private String transientWarningTitle;
    private String transientWarningMessage;
    private boolean lastPausedState;
    private int heroFrame = 0;
    private int heroLastX = Integer.MIN_VALUE;
    private int heroLastY = Integer.MIN_VALUE;
    private int heroAnimDx = 0;
    private int heroAnimDy = 0;
    private float heroAnimProgress = 1f;
    private float heroPixelOffsetX = 0f;
    private float heroPixelOffsetY = 0f;
    private boolean heroFacingLeft = false;
    private final Map<Entity, GridPosition> lastEnemyPositions = new IdentityHashMap<>();
    private final Map<Entity, EnemyMoveAnimation> enemyMoveAnimations = new IdentityHashMap<>();

    public GamePanel(GameEngine engine, PlayerModeController playerModeController,
            InteractionController interactionController) {
        this.engine = engine;
        this.playerModeController = playerModeController;
        this.interactionController = interactionController;
        this.combatController = new CombatController(engine);

        Hero hero = engine.getHero();
        if (hero != null) {
            heroLastX = hero.getX();
            heroLastY = hero.getY();
        }

        engine.addGameStateListener(this);
        setBackground(ARENA_BACKDROP_BOTTOM);
        setOpaque(true);
        setFocusable(true);

        heroAnimTimer = new Timer(HERO_ANIM_INTERVAL_MS, e -> {
            boolean repaintNeeded = false;
            if (heroAnimProgress < 1f) {
                heroAnimProgress = Math.min(1f, heroAnimProgress + HERO_ANIM_STEP);
                heroFrame = (heroFrame + 1) % SpriteRegistry.heroFrameCount();
                if (heroAnimProgress >= 1f) {
                    heroFrame = 0;
                    heroAnimDx = 0;
                    heroAnimDy = 0;
                    heroPixelOffsetX = 0f;
                    heroPixelOffsetY = 0f;
                }
                repaintNeeded = true;
            }
            if (advanceEnemyAnimations()) {
                repaintNeeded = true;
            }
            if (repaintNeeded) {
                repaint();
            }
        });
        heroAnimTimer.start();

        energyRefillTimer = new Timer(ENERGY_REFILL_INTERVAL_MS, e -> engine.tickEnergyRefill());
        energyRefillTimer.start();

        fogShimmerTimer = new Timer(FOG_SHIMMER_INTERVAL_MS, e -> {
            DungeonMap map = engine.getDungeonMap();
            if (map != null && map.isFogEnabled()) {
                repaint();
            }
        });
        fogShimmerTimer.start();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    showInGameMenu();
                    return;
                }

                if (e.getKeyCode() == KeyEvent.VK_R) {
                    engine.togglePause();
                    applyPauseState();
                    return;
                }
                if (engine.isGameOver() || engine.isPaused()) {
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_T) {
                    handleTakeKeyPress();
                    return;
                }

                if (e.getKeyCode() == KeyEvent.VK_O) {
                    handleOpenKeyPress();
                    return;
                }

                if (e.getKeyCode() == KeyEvent.VK_P) {
                    handleHitKeyPress();
                    return;
                }

                if (e.getKeyCode() == KeyEvent.VK_I) {
                    handleInventoryKeyPress();
                    return;
                }

                Direction d = Direction.fromKeyCode(e.getKeyCode());
                if (d != null) {                    
                    currentMovementDirection = d;
                    if (!continuousMoveTimer.isRunning()) {
                        GamePanel.this.playerModeController.moveHero(currentMovementDirection);
                        continuousMoveTimer.start();
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (engine.isGameOver() || engine.isPaused()) {
                    return;
                }
                Direction d = Direction.fromKeyCode(e.getKeyCode());                
                if (d != null && d == currentMovementDirection) {
                    currentMovementDirection = null;
                    continuousMoveTimer.stop();
                }
            }
        });

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (engine.isGameOver() || engine.isPaused()) {
                    return;
                }
                DungeonMap map = GamePanel.this.engine.getDungeonMap();
                if (map == null || map.getWidth() <= 0 || map.getHeight() <= 0) {
                    return;
                }

                int tileSize = getTileSize(map);
                int mapPixelW = map.getWidth() * tileSize;
                int mapPixelH = map.getHeight() * tileSize;
                int offsetX = getArenaOffsetX(mapPixelW);
                int offsetY = getArenaOffsetY(mapPixelH);

                // Ignore clicks outside the shifted arena map.
                if (e.getX() < offsetX || e.getX() >= offsetX + mapPixelW
                        || e.getY() < offsetY || e.getY() >= offsetY + mapPixelH) {
                    return;
                }

                int gridX = (e.getX() - offsetX) / tileSize;
                int gridY = (e.getY() - offsetY) / tileSize;
                if (!isContentVisible(map, gridX, gridY)) {
                    return;
                }
                Window parent = SwingUtilities.getWindowAncestor(GamePanel.this);

                CombatManager.AttackResult attackResult = GamePanel.this.combatController.attackAt(gridX, gridY);
                if (attackResult != null) {
                    if (attackResult.isDefenderDefeated()) {
                        leaveDefeatMarker(gridX, gridY);
                    }
                    GamePanel.this.requestFocusInWindow();
                    return;
                }

                List<InteractionController.ItemInteraction> interactions = GamePanel.this.interactionController
                        .getItemInteractions(gridX, gridY);
                if (interactions.isEmpty()) {
                    return;
                }

                for (int i = 0; i < interactions.size(); i++) {
                    InteractionController.ItemInteraction interaction = interactions.get(i);
                    boolean handled = presentItemInteraction(parent, interaction, i, interactions.size());
                    if (handled) {
                        break;
                    }
                }

                GamePanel.this.requestFocusInWindow();
            }
        });

        continuousMoveTimer = new Timer(300, e -> {
            if (currentMovementDirection != null) {
                GamePanel.this.playerModeController.moveHero(currentMovementDirection);
            }
        });



    }

    /**
     * Shows the action menu for one ground item and runs the user's choice.
     *
     * @return {@code true} when the user picked a real action (caller should
     *         stop iterating remaining items); {@code false} when the menu was
     *         skipped/cancelled so the caller can move on to the next item.
     */
    private boolean presentItemInteraction(Window parent,
            InteractionController.ItemInteraction interaction, int index, int total) {
        List<InteractionController.ActionOption> actions = interaction.getActions();
        String dismissLabel = (index < total - 1) ? "Next Item" : "Close";
        String[] labels = new String[actions.size() + 1];
        for (int i = 0; i < actions.size(); i++) {
            labels[i] = actions.get(i).getLabel();
        }
        labels[labels.length - 1] = dismissLabel;

        String message = interaction.isTakable()
                ? "This object is within reach."
                : "This object cannot be taken.";
        if (interaction.getDetail() != null) {
            message += "\n" + interaction.getDetail();
        }
        if (total > 1) {
            message += "\n(Item " + (index + 1) + " of " + total + ")";
        }

        int choice = ItemActionMenuDialog.show(parent, "Nearby Object",
                interaction.getItemName(), message, labels);
        if (choice < 0 || choice == labels.length - 1) {
            return false;
        }

        InteractionController.ActionOption picked = actions.get(choice);
        if (picked.isPickup()) {
            InventoryController.PickupResult result = interactionController.takeItemAt(
                    interaction.getX(), interaction.getY());
            if (result != InventoryController.PickupResult.SUCCESS) {
                showTransientWarning("Cannot Take Item", getPickupFailureMessage(result));
            }
        } else if (picked.getInventoryAction() == model.ItemAction.SEARCH
                && interaction.getItem() instanceof SearchableObject searchableObject) {
            showSearchResult(parent, interactionController.search(searchableObject));
        } else if (picked.getInventoryAction() == model.ItemAction.BREAK) {
            showBreakResult(parent, interactionController.breakObjectAt(interaction.getItem(),
                    interaction.getX(), interaction.getY()));
        } else if (!interactionController.applyGroundAction(interaction.getItem(),
                interaction.getX(), interaction.getY(), picked.getInventoryAction())) {
            showTransientWarning("Cannot Use Item",
                    "That action could not be performed on " + interaction.getItemName() + ".");
        }
        return true;
    }

    public void showInGameMenu() {
        currentMovementDirection = null;
        if (continuousMoveTimer != null) {
            continuousMoveTimer.stop();
        }

        Window parent = SwingUtilities.getWindowAncestor(this);
        int choice = ItemActionMenuDialog.show(parent, "Menu", "Game Menu",
                "Choose an action.", "Continue", "Save Game", "Menu");
        switch (choice) {
            case 1 -> handleSaveGame(parent);
            case 2 -> returnToMainMenu(parent);
            default -> requestFocusInWindow();
        }
    }

    private void handleSaveGame(Window parent) {
        SaveGameDialog.Result result = SaveGameDialog.show(parent);
        if (result.action() == SaveGameDialog.Action.CANCEL) {
            requestFocusInWindow();
            return;
        }
        try {
            saveGameController.saveGame(engine, result.saveName());
            if (result.action() == SaveGameDialog.Action.SAVE_AND_EXIT) {
                returnToMainMenu(parent);
            } else {
                ItemActionMenuDialog.showNotice(parent, "Save Game", "Saved",
                        "Game saved successfully.");
                requestFocusInWindow();
            }
        } catch (SaveLimitExceededException ex) {
            ItemActionMenuDialog.showNotice(parent, "Save Game", "Save Limit",
                    "You can keep at most 10 saved games. Delete an old save first.");
            requestFocusInWindow();
        } catch (SaveGameException ex) {
            ItemActionMenuDialog.showNotice(parent, "Save Game", "Save Failed",
                    "Game could not be saved. Please try again.");
            requestFocusInWindow();
        }
    }

    private void returnToMainMenu(Window parent) {
        if (parent != null) {
            parent.dispose();
        }
        SwingUtilities.invokeLater(() -> new MainMenuWindow().setVisible(true));
    }

    private void showSearchResult(Window parent, GameEngine.SearchResult result) {
        switch (result.getOutcome()) {
            case FOUND -> ItemActionMenuDialog.showNotice(parent, "Search", "Found",
                    "You have found a " + result.getFoundItem().getName() + ".");
            case NOTHING_FOUND -> ItemActionMenuDialog.showNotice(parent, "Search", "Nothing Found",
                    "you couldn't found anything");
            case INVENTORY_FULL -> ItemActionMenuDialog.showNotice(parent, "Search", "Inventory Full",
                    "You have found a " + result.getFoundItem().getName()
                            + ", but your inventory is full.");
            case NOT_SEARCHABLE -> ItemActionMenuDialog.showNotice(parent, "Search", "Cannot Search",
                    "This location cannot be searched.");
        }
    }

    private void leaveDefeatMarker(int gridX, int gridY) {
        GridCell cell = engine.getDungeonMap().getCell(gridX, gridY);
        if (cell != null) {
            cell.getItems().add(new DefeatedEnemyMarker());
            repaint();
        }
    }

    private void handleOpenKeyPress() {
        Window parent = SwingUtilities.getWindowAncestor(this);

        model.Arch arch = engine.findArchNearHero();
        if (arch != null) {
            boolean hasKey = engine.heroHasGoldKey();
            boolean foundTreasure = engine.getTargetMission().isWon();
            if (!hasKey) {
                showTransientWarning("Cannot Open Exit",
                        "The exit is locked. Find the gold key before opening the arch.");
            } else if (!foundTreasure) {
                showTransientWarning("Treasure Required",
                        "You have the gold key, but you must claim the floor's hidden treasure before leaving.");
            } else {
                engine.openArch(arch);
            }
            requestFocusInWindow();
            return;
        }

        Container container = engine.findContainerNearHero();
        if (container == null) {
            // No container in reach; stay silent rather than nagging.
            requestFocusInWindow();
            return;
        }

        if (container.isLocked()) {
            model.Key match = engine.getHero().getInventory().findKey(container.getRequiredKeyId());
            if (match == null) {
                ItemActionMenuDialog.showNotice(parent, "Locked Container", "Locked",
                        container.getName() + " is locked.");
                requestFocusInWindow();
                return;
            }
            int choice = ItemActionMenuDialog.show(parent,
                    "Locked Container",
                    container.getName(),
                    "Open with " + match.getName() + "?",
                    "Open",
                    "Leave");
            if (choice != 0) {
                requestFocusInWindow();
                return;
            }
            LockController.UnlockResult result = engine.tryUnlock(container);
            if (result == LockController.UnlockResult.NO_MATCHING_KEY) {
                ItemActionMenuDialog.showNotice(parent, "Locked Container", "Locked",
                        container.getName() + " is locked.");
                requestFocusInWindow();
                return;
            }
        }

        ChestDialog dialog = new ChestDialog(parent, engine, container);
        dialog.setVisible(true);
        requestFocusInWindow();
    }

    private void handleHitKeyPress() {
        Hero hero = engine.getHero();
        Weapon weapon = hero.getEquippedWeapon();
        CombatController.TargetedAttack attack = weapon != null && weapon.isRanged()
                ? combatController.autoAimRangedAttack()
                : combatController.attackNearestEnemy();
        if (attack != null) {
            if (attack.result().isDefenderDefeated()) {
                leaveDefeatMarker(attack.x(), attack.y());
            }
            requestFocusInWindow();
            return;
        }

        // No enemy in reach; try to break a nearby breakable object instead.
        InteractionController.BreakResult breakResult = interactionController.breakNearestObject();
        // Nothing breakable in reach (breakResult == null): stay silent.
        showBreakResult(SwingUtilities.getWindowAncestor(this), breakResult);
        requestFocusInWindow();
    }

    private void showBreakResult(Window parent, InteractionController.BreakResult result) {
        if (result == null || result.broken()) {
            return;
        }
        if (result.outcome() == InteractionController.BreakOutcome.NOT_ENOUGH_ENERGY) {
            ItemActionMenuDialog.showNotice(parent, "Break", "Not Enough Energy",
                    "Breaking the " + result.objectName() + " requires "
                            + result.energyCost() + " energy.");
            return;
        }
        int chancePercent = Math.round((float) (result.successChance() * 100));
        ItemActionMenuDialog.showNotice(parent, "Break", "Break Failed",
                "You failed to break the " + result.objectName()
                        + ". Success chance was " + chancePercent + "%.");
    }

    private void handleTakeKeyPress() {
        // Only warn when an item is in reach but the inventory is full; staying
        // silent when there is simply nothing takable nearby.
        if (!engine.takeItemOnGround() && engine.getHero().getInventory().isFull()) {
            showTransientWarning("Cannot Take Item",
                    getPickupFailureMessage(InventoryController.PickupResult.INVENTORY_FULL));
        }

        requestFocusInWindow();
    }

private void handleInventoryKeyPress() {
        Window parent = SwingUtilities.getWindowAncestor(this);
        view.InventoryDialog dialog = null;

        
        if (parent instanceof java.awt.Frame) {
            dialog = new view.InventoryDialog((java.awt.Frame) parent, engine);
        } else if (parent instanceof javax.swing.JDialog) {
            dialog = new view.InventoryDialog((javax.swing.JDialog) parent, engine);
        }

        
        if (dialog != null) {
            dialog.setVisible(true);
        }

        requestFocusInWindow(); 
    }

    @Override
    public void removeNotify() {
        heroAnimTimer.stop();
        energyRefillTimer.stop();
        fogShimmerTimer.stop();
        engine.removeGameStateListener(this);
        super.removeNotify();
    }

    @Override
    public void onGameStateChanged() {
        applyPauseState();
        Hero hero = engine.getHero();
        if (hero != null) {
            DungeonMap map = engine.getDungeonMap();
            int tileSize = map != null ? getTileSize(map) : BASE_CELL;
            int dx = hero.getX() - heroLastX;
            int dy = hero.getY() - heroLastY;
            if (heroLastX != Integer.MIN_VALUE
                    && (dx != 0 || dy != 0)
                    && Math.abs(dx) + Math.abs(dy) == 1) {
                if (heroAnimProgress < 1f) {
                    float remaining = 1f - heroAnimProgress;
                    heroPixelOffsetX = -heroAnimDx * remaining * tileSize;
                    heroPixelOffsetY = -heroAnimDy * remaining * tileSize;
                } else {
                    heroPixelOffsetX = 0f;
                    heroPixelOffsetY = 0f;
                }
                heroPixelOffsetX += -dx * tileSize;
                heroPixelOffsetY += -dy * tileSize;
                heroAnimDx = dx;
                heroAnimDy = dy;
                heroAnimProgress = 0f;
                heroFrame = 0;
                if (dx != 0) {
                    heroFacingLeft = dx < 0;
                }
            }
            heroLastX = hero.getX();
            heroLastY = hero.getY();
        }
        updateEnemyAnimationState();
        SwingUtilities.invokeLater(this::repaint);
    }

    private void applyPauseState() {
        boolean paused = engine.isPaused();
        if (paused == lastPausedState) {
            return;
        }
        lastPausedState = paused;

        if (paused) {
            if (continuousMoveTimer != null) {
                continuousMoveTimer.stop();
            }
            currentMovementDirection = null;
            heroAnimTimer.stop();
            energyRefillTimer.stop();
        } else {
            if (!heroAnimTimer.isRunning()) {
                heroAnimTimer.start();
            }
            if (!energyRefillTimer.isRunning()) {
                energyRefillTimer.start();
            }
        }
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        DungeonMap map = engine.getDungeonMap();
        return new Dimension(map.getWidth() * BASE_CELL + 1, map.getHeight() * BASE_CELL + 1);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            DungeonMap map = engine.getDungeonMap();
            int mapW = Math.max(1, map.getWidth());
            int mapH = Math.max(1, map.getHeight());
            int tileSize = getTileSize(map);
            int mapPixelW = mapW * tileSize;
            int mapPixelH = mapH * tileSize;
            int offsetX = getArenaOffsetX(mapPixelW);
            int offsetY = getArenaOffsetY(mapPixelH);
            drawArenaBackdrop(g2, offsetX, offsetY, mapPixelW, mapPixelH);
            // Pass 1: floor sprite under every passable cell.
            for (int x = 0; x < map.getWidth(); x++) {
                for (int y = 0; y < map.getHeight(); y++) {
                    GridCell cell = map.getCell(x, y);
                    if (cell == null || !cell.isPassable()) {
                        continue;
                    }
                    int px = offsetX + x * tileSize;
                    int py = offsetY + y * tileSize;
                    ambienceRenderer.drawFloor(g2, px, py, tileSize);
                }
            }

            // Pass 2: walls (renderer owns the layout; multi-cell sprites span
            // their CSV-derived number of cells).
            ambienceRenderer.drawWalls(g2, map, tileSize, offsetX, offsetY);

            // Pass 3: items, entities, and AI labels on top of the ambience.
            for (int x = 0; x < map.getWidth(); x++) {
                for (int y = 0; y < map.getHeight(); y++) {
                    GridCell cell = map.getCell(x, y);
                    if (cell == null) {
                        continue;
                    }
                    int px = offsetX + x * tileSize;
                    int py = offsetY + y * tileSize;
                    int cellW = tileSize;
                    int cellH = tileSize;
                    boolean contentVisible = isContentVisible(map, x, y);

                    if (contentVisible && !cell.getItemsView().isEmpty()) {
                        Item first = cell.getItemsView().get(0);
                        if (first instanceof Weapon weapon && isWoodenBow(weapon)) {
                            drawGroundBowPixelArt(g2, px, py, cellW, cellH);
                        } else if (first instanceof Weapon weapon && isMagicWand(weapon)) {
                            drawGroundWandPixelArt(g2, px, py, cellW, cellH);
                        } else if (first instanceof model.Armor) {
                            drawGroundArmorPixelArt(g2, px, py, cellW, cellH);
                        } else {
                            BufferedImage sprite = spriteFor(first);
                            if (sprite != null) {
                                drawItemSprite(g2, first, sprite, px, py, cellW, cellH, y == map.getHeight() - 1);
                            } else {
                                Color itemColor = first instanceof Potion p ? p.getColor() : ITEM;
                                drawItemMarker(g2, px, py, cellW, cellH, itemColor);
                            }
                        }
                    }

                    if (!contentVisible) {
                        continue;
                    }
                    for (Entity ent : cell.getEntitiesView()) {
                        if (ent instanceof Hero) {
                            continue;
                        }
                        EnemyDrawPosition enemyDrawPosition = enemyDrawPosition(ent, px, py, tileSize);
                        BufferedImage enemySprite = enemySpriteFor(ent);
                        if (enemySprite != null) {    
                            int spriteW = Math.round(enemySprite.getWidth() * HERO_SPRITE_SCALE);
                            int spriteH = Math.round(enemySprite.getHeight() * HERO_SPRITE_SCALE);    
                            int drawX = enemyDrawPosition.x + (cellW - spriteW) / 2;
                            int drawY = enemyDrawPosition.y + (cellH - spriteH) / 2;
    
                            g2.drawImage(enemySprite, drawX, drawY, spriteW, spriteH, null);
                        } else {
                            g2.setColor(ent instanceof Knight ? KNIGHT
                                    : ent instanceof Sorcerer || ent instanceof BossEnemy ? SORCERER
                                            : Color.LIGHT_GRAY);
                            int inset = Math.max(1, Math.min(cellW, cellH) / 5);
                            int entityW = Math.max(1, cellW - inset * 2);
                            int entityH = Math.max(1, cellH - inset * 2);
                            g2.fillRect(enemyDrawPosition.x + inset, enemyDrawPosition.y + inset, entityW, entityH);
                        }
                        drawAiStateLabel(g2, ent, enemyDrawPosition.x, enemyDrawPosition.y, cellW);
                        drawEnemyHpBar(g2, ent, enemyDrawPosition.x, enemyDrawPosition.y, cellW, cellH);
                    }
                }
            }
            drawProjectiles(g2, tileSize, offsetX, offsetY);
            drawHero(g2, map, tileSize, offsetX, offsetY);
            drawHud(g2);
            drawTransientWarning(g2);
            drawFogOverlay(g2, tileSize, offsetX, offsetY);
        } finally {
            g2.dispose();
        }
    }

    private int getTileSize(DungeonMap map) {
        int mapW = Math.max(1, map.getWidth());
        int mapH = Math.max(1, map.getHeight());
        int frameSpace = (ARENA_FRAME_PADDING + ARENA_EDGE_MARGIN) * 2;
        int availableW = Math.max(1, getWidth() - frameSpace);
        int availableH = Math.max(1, getHeight() - frameSpace);
        // Keep tile aspect ratio fixed and reserve room for the arena frame.
        return Math.max(1, Math.min(availableW / mapW, availableH / mapH));
    }

    private int getArenaOffsetX(int mapPixelWidth) {
        int centered = (Math.max(1, getWidth()) - mapPixelWidth) / 2 + ARENA_SHIFT_X;
        int margin = ARENA_EDGE_MARGIN + ARENA_FRAME_PADDING;
        int maxOffset = Math.max(margin, getWidth() - mapPixelWidth - margin);
        return Math.max(margin, Math.min(centered, maxOffset));
    }

    private int getArenaOffsetY(int mapPixelHeight) {
        int centered = (Math.max(1, getHeight()) - mapPixelHeight) / 2;
        int margin = ARENA_EDGE_MARGIN + ARENA_FRAME_PADDING;
        int maxOffset = Math.max(margin, getHeight() - mapPixelHeight - margin);
        return Math.max(margin, Math.min(centered, maxOffset));
    }

    private void drawArenaBackdrop(Graphics2D g2, int mapX, int mapY, int mapWidth, int mapHeight) {
        int width = getWidth();
        int height = getHeight();
        g2.setPaint(new GradientPaint(0, 0, ARENA_BACKDROP_TOP, 0, height, ARENA_BACKDROP_BOTTOM));
        g2.fillRect(0, 0, width, height);

        g2.setColor(new Color(42, 35, 30, 55));
        for (int lineY = 18; lineY < height; lineY += 34) {
            g2.drawLine(0, lineY, width, lineY);
        }

        int x = mapX - ARENA_FRAME_PADDING;
        int y = mapY - ARENA_FRAME_PADDING;
        int w = mapWidth + ARENA_FRAME_PADDING * 2;
        int h = mapHeight + ARENA_FRAME_PADDING * 2;
        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillRect(x + 6, y + 7, w, h);
        g2.setColor(ARENA_FRAME_OUTLINE);
        g2.fillRect(x, y, w, h);
        g2.setColor(ARENA_FRAME_BORDER);
        g2.fillRect(x + 3, y + 3, w - 6, h - 6);
        g2.setColor(ARENA_FRAME_HIGHLIGHT);
        g2.fillRect(x + 6, y + 6, w - 12, 2);
        g2.fillRect(x + 6, y + 6, 2, h - 12);
        g2.setColor(new Color(41, 35, 32));
        g2.fillRect(x + 8, y + 8, w - 16, h - 16);
        g2.setColor(ARENA_GOLD);
        g2.fillRect(x + 18, y + 8, 56, 2);
        g2.fillRect(x + w - 74, y + 8, 56, 2);
    }

    private String getPickupFailureMessage(InventoryController.PickupResult result) {
        return switch (result) {
            case NO_ITEM -> "No item is available in this tile.";
            case NOT_ADJACENT -> "You can only take items from adjacent tiles.";
            case NOT_TAKABLE -> "This item cannot be taken.";
            case INVENTORY_FULL -> "Inventory is full.";
            case SUCCESS -> "";
        };
    }

    private void showTransientWarning(String title, String message) {
        transientWarningTitle = title == null ? "Warning" : title;
        transientWarningMessage = message == null ? "" : message;
        if (transientWarningTimer != null) {
            transientWarningTimer.stop();
        }
        transientWarningTimer = new Timer(2300, e -> {
            transientWarningTitle = null;
            transientWarningMessage = null;
            repaint();
        });
        transientWarningTimer.setRepeats(false);
        transientWarningTimer.start();
        repaint();
        requestFocusInWindow();
    }

    private void drawTransientWarning(Graphics2D g2) {
        if (transientWarningTitle == null) {
            return;
        }
        int maxWidth = Math.min(430, Math.max(260, getWidth() - 80));
        g2.setFont(retroHudFont(12f));
        List<String> lines = wrapText(g2, transientWarningMessage, maxWidth - 34);
        int width = maxWidth;
        int height = 54 + lines.size() * 16;
        int x = (getWidth() - width) / 2;
        int y = 22;

        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(x + 5, y + 6, width, height);
        g2.setColor(HUD_STONE_OUTLINE);
        g2.fillRect(x, y, width, height);
        g2.setColor(new Color(68, 45, 38));
        g2.fillRect(x + 3, y + 3, width - 6, height - 6);
        g2.setColor(new Color(120, 68, 54));
        g2.fillRect(x + 6, y + 6, width - 12, height - 12);
        g2.setColor(HUD_PANEL_FILL);
        g2.fillRect(x + 9, y + 9, width - 18, height - 18);

        g2.setFont(retroHudFont(13f));
        g2.setColor(new Color(244, 205, 103));
        g2.drawString(transientWarningTitle, x + 17, y + 28);

        g2.setFont(retroHudFont(11f));
        g2.setColor(HUD_TEXT);
        int lineY = y + 50;
        for (String line : lines) {
            g2.drawString(line, x + 17, lineY);
            lineY += 16;
        }
    }

    private List<String> wrapText(Graphics2D g2, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }
        StringBuilder current = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (g2.getFontMetrics().stringWidth(candidate) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
            } else {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                current.append(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private BufferedImage spriteFor(Item item) {
        return SpriteRegistry.spriteFor(item);
    }

    private BufferedImage spriteFor(Entity entity) {
        return SpriteRegistry.spriteFor(entity);
    }

    private void drawItemSprite(Graphics2D g2, Item item, BufferedImage sprite, int px, int py, int cellW, int cellH,
            boolean flipVertical) {
        int inset = Math.max(1, Math.min(cellW, cellH) / 6);
        int boxW = Math.max(1, cellW - inset * 2);
        int boxH = Math.max(1, cellH - inset * 2);
        if (item instanceof SearchableObject || item instanceof model.BreakableObject) {
            double scale = Math.min(boxW / (double) sprite.getWidth(), boxH / (double) sprite.getHeight());
            int drawW = Math.max(1, (int) Math.round(sprite.getWidth() * scale));
            int drawH = Math.max(1, (int) Math.round(sprite.getHeight() * scale));
            int drawX = px + (cellW - drawW) / 2;
            boolean drip = isWallDripSearchable(item);
            int drawY = drip ? py + cellH - drawH : py + (cellH - drawH) / 2;
            if (flipVertical && !drip) {
                g2.drawImage(sprite, drawX, drawY + drawH, drawW, -drawH, null);
            } else {
                g2.drawImage(sprite, drawX, drawY, drawW, drawH, null);
            }
            return;
        }
        if (item instanceof model.Key) {
            // Keys are tiny pixel art; draw at native size, centered, never enlarged.
            int drawX = px + (cellW - sprite.getWidth()) / 2;
            int drawY = py + (cellH - sprite.getHeight()) / 2;
            g2.drawImage(sprite, drawX, drawY, sprite.getWidth(), sprite.getHeight(), null);
            return;
        }
        if (item instanceof model.Arch) {
            // The arch is a structural wall fixture: fill the whole tile so it
            // reads as a gate set into the wall, not a small item on the floor.
            g2.drawImage(sprite, px, py, cellW, cellH, null);
            return;
        }
        if (item instanceof model.DecorativeObject) {
            g2.drawImage(sprite, px, py, cellW, cellH, null);
            return;
        }
        g2.drawImage(sprite, px + inset, py + inset, boxW, boxH, null);
    }

    private void drawGroundBowPixelArt(Graphics2D g2, int px, int py, int cellW, int cellH) {
        int size = Math.min(cellW, cellH);
        int pixel = Math.max(2, size / 14);
        int x = px + (cellW - size) / 2;
        int y = py + (cellH - size) / 2;
        int centerY = y + size / 2;
        int arrowLeft = x + pixel * 4;
        int arrowRight = x + size - pixel * 3;
        int bowTopX = x + size - pixel * 8;
        int bowMidX = x + size - pixel * 5;
        int bowBotX = x + size - pixel * 8;
        int topY = y + pixel * 4;
        int bottomY = y + size - pixel * 4;

        g2.setColor(new Color(222, 216, 196));
        drawPixelLine(g2, arrowLeft, centerY, bowTopX, topY, pixel);
        drawPixelLine(g2, arrowLeft, centerY, bowBotX, bottomY, pixel);
        g2.setColor(new Color(74, 38, 18));
        drawPixelLine(g2, bowTopX + pixel, topY, bowMidX + pixel, centerY - pixel * 2, pixel);
        drawPixelLine(g2, bowMidX + pixel, centerY - pixel * 2, bowMidX + pixel, centerY + pixel * 2, pixel);
        drawPixelLine(g2, bowMidX + pixel, centerY + pixel * 2, bowBotX + pixel, bottomY, pixel);
        g2.setColor(new Color(205, 132, 62));
        drawPixelLine(g2, bowTopX, topY, bowMidX, centerY - pixel * 2, pixel);
        drawPixelLine(g2, bowMidX, centerY - pixel * 2, bowMidX, centerY + pixel * 2, pixel);
        drawPixelLine(g2, bowMidX, centerY + pixel * 2, bowBotX, bottomY, pixel);
        g2.setColor(new Color(92, 52, 24));
        g2.fillRect(arrowLeft, centerY - pixel, arrowRight - arrowLeft - pixel * 3, pixel * 2);
        g2.setColor(new Color(201, 130, 62));
        g2.fillRect(arrowLeft, centerY - pixel / 2, arrowRight - arrowLeft - pixel * 3, pixel);
        g2.setColor(new Color(220, 220, 220));
        g2.fillRect(arrowRight - pixel * 3, centerY - pixel * 2, pixel * 2, pixel);
        g2.fillRect(arrowRight - pixel * 2, centerY - pixel, pixel * 2, pixel);
        g2.fillRect(arrowRight - pixel, centerY, pixel, pixel);
        g2.fillRect(arrowRight - pixel * 2, centerY + pixel, pixel * 2, pixel);
        g2.fillRect(arrowRight - pixel * 3, centerY + pixel * 2, pixel * 2, pixel);
    }

    private void drawGroundWandPixelArt(Graphics2D g2, int px, int py, int cellW, int cellH) {
        int size = Math.min(cellW, cellH);
        int pixel = Math.max(2, size / 14);
        int x = px + (cellW - size) / 2;
        int y = py + (cellH - size) / 2;
        int baseX = x + size / 2 - pixel;
        int baseY = y + size - pixel * 4;
        int tipX = x + size / 2 + pixel * 3;
        int tipY = y + pixel * 3;

        g2.setColor(new Color(45, 27, 18));
        drawPixelLine(g2, baseX - pixel, baseY + pixel, tipX - pixel, tipY + pixel, pixel);
        g2.setColor(new Color(118, 73, 43));
        drawPixelLine(g2, baseX, baseY, tipX, tipY, pixel);
        g2.setColor(new Color(170, 230, 255));
        g2.fillRect(tipX + pixel, tipY - pixel, pixel, pixel);
        g2.setColor(Color.WHITE);
        g2.fillRect(tipX + pixel * 2, tipY - pixel, pixel, pixel);
    }

    private void drawGroundArmorPixelArt(Graphics2D g2, int px, int py, int cellW, int cellH) {
        if (HeroArmorPixelArt.armorImage == null) {
            return;
        }
        int bodyW = Math.round(16 * HERO_SPRITE_SCALE);
        int bodyH = Math.round(32 * HERO_SPRITE_SCALE);
        int x = px + (cellW - bodyW) / 2;
        int y = py + (cellH - bodyH) / 2;
        g2.drawImage(HeroArmorPixelArt.armorImage, x, y, bodyW, bodyH, null);
    }

    private boolean isWallDripSearchable(Item item) {
        String resource = item == null ? null : item.spriteResource();
        return resource != null
                && (resource.contains("wall_detail_drip_") || resource.contains("gargoyle_"));
    }

    private void drawItemMarker(Graphics2D g2, int px, int py, int cellW, int cellH, Color color) {
        g2.setColor(color);
        int inset = Math.max(1, Math.min(cellW, cellH) / 3);
        int itemW = Math.max(1, cellW - inset * 2);
        int itemH = Math.max(1, cellH - inset * 2);
        g2.fillRect(px + inset, py + inset, itemW, itemH);
        g2.setColor(GRID_LINE);
        g2.drawRect(px + inset, py + inset, itemW, itemH);
    }

    private void drawFogOverlay(Graphics2D g2, int tileSize,
                                int offsetX, int offsetY) {
        DungeonMap map = engine.getDungeonMap();
        if (map == null || !map.isFogEnabled()) {
            return;
        }
        FogOfWarEngine fog = engine.getFogEngine();
        Hero hero = engine.getHero();
        if (hero == null) {
            return;
        }

        long now = System.currentTimeMillis();
        int shimmer = (int) (10 * Math.sin(now / 500.0));
        Color dimOverlay = new Color(0, 0, 0, 140 + shimmer);
        Color hiddenOverlay = new Color(0, 0, 0, 225 + shimmer);

        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                if (fog.isVisible(map, hero, x, y)) {
                    continue;
                }
                GridCell cell = map.getCell(x, y);
                if (cell == null) {
                    continue;
                }
                int px = offsetX + x * tileSize;
                int py = offsetY + y * tileSize;
                g2.setColor(cell.isDiscovered() ? dimOverlay : hiddenOverlay);
                g2.fillRect(px, py, tileSize, tileSize);
            }
        }
    }

    private boolean isContentVisible(DungeonMap map, int x, int y) {
        if (map == null || !map.isFogEnabled()) {
            return true;
        }
        Hero hero = engine.getHero();
        return hero != null && engine.getFogEngine().isVisible(map, hero, x, y);
    }

    private boolean advanceEnemyAnimations() {
        boolean changed = false;
        java.util.Iterator<EnemyMoveAnimation> iterator = enemyMoveAnimations.values().iterator();
        while (iterator.hasNext()) {
            EnemyMoveAnimation animation = iterator.next();
            animation.progress = Math.min(1f, animation.progress + ENEMY_ANIM_STEP);
            changed = true;
            if (animation.progress >= 1f) {
                iterator.remove();
            }
        }
        return changed;
    }

    private void updateEnemyAnimationState() {
        DungeonMap map = engine.getDungeonMap();
        if (map == null) {
            return;
        }
        Set<Entity> seen = new HashSet<>();
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GridCell cell = map.getCell(x, y);
                if (cell == null) {
                    continue;
                }
                for (Entity entity : cell.getEntitiesView()) {
                    if (!(entity instanceof Knight || entity instanceof Sorcerer || entity instanceof BossEnemy
                            || entity instanceof PetEntity)) {
                        continue;
                    }
                    seen.add(entity);
                    GridPosition previous = lastEnemyPositions.get(entity);
                    GridPosition current = new GridPosition(entity.getX(), entity.getY());
                    if (previous != null && isSingleStep(previous, current)) {
                        enemyMoveAnimations.put(entity, new EnemyMoveAnimation(previous, current));
                    }
                    lastEnemyPositions.put(entity, current);
                }
            }
        }
        lastEnemyPositions.keySet().removeIf(entity -> !seen.contains(entity));
        enemyMoveAnimations.keySet().removeIf(entity -> !seen.contains(entity));
    }
    

    private boolean isSingleStep(GridPosition previous, GridPosition current) {
        int dx = Math.abs(previous.x - current.x);
        int dy = Math.abs(previous.y - current.y);
        return dx + dy == 1;
    }

    private EnemyDrawPosition enemyDrawPosition(Entity entity, int px, int py, int tileSize) {
        EnemyMoveAnimation animation = enemyMoveAnimations.get(entity);
        if (animation == null) {
            return new EnemyDrawPosition(px, py);
        }
        float remaining = 1f - animation.progress;
        int offsetX = Math.round((animation.from.x - animation.to.x) * remaining * tileSize);
        int offsetY = Math.round((animation.from.y - animation.to.y) * remaining * tileSize);
        return new EnemyDrawPosition(px + offsetX, py + offsetY);
    }

    private BufferedImage enemySpriteFor(Entity entity) {
        EnemyMoveAnimation animation = enemyMoveAnimations.get(entity);
        if (animation == null) {
            return SpriteRegistry.walkFrameFor(entity, 0);
        }
        int frame = Math.min(SpriteRegistry.heroFrameCount() - 1,
                Math.max(0, (int) (animation.progress * SpriteRegistry.heroFrameCount())));
        return SpriteRegistry.walkFrameFor(entity, frame);
    }

    private void drawProjectiles(Graphics2D g2, int tileSize, int offsetX, int offsetY) {
        for (Projectile projectile : engine.getActiveProjectilesView()) {
            if (!projectile.isActive()) {
                continue;
            }
            if (!isContentVisible(engine.getDungeonMap(), projectile.getX(), projectile.getY())) {
                continue;
            }
            int px = offsetX + projectile.getX() * tileSize;
            int py = offsetY + projectile.getY() * tileSize;
            drawProjectilePixelArt(g2, px, py, tileSize, projectile);
        }
    }

    private void drawProjectilePixelArt(Graphics2D g2, int tileX, int tileY, int tileSize, Projectile projectile) {
        if (projectile.isHeroOwned()) {
            HeroProjectileStyle style = projectile.getHeroStyle();
            if (style == HeroProjectileStyle.ARROW) {
                drawHeroArrowPixelArt(g2, tileX, tileY, tileSize, projectile.getDx(), projectile.getDy());
                return;
            }
            if (style == HeroProjectileStyle.FIRE_BALL) {
                drawFireballPixelArt(g2, tileX, tileY, tileSize);
                return;
            }
            drawIceBoltPixelArt(g2, tileX, tileY, tileSize);
            return;
        }
        if (projectile.isBossOwned()) {
            drawBossProjectilePixelArt(g2, tileX, tileY, tileSize);
        } else {
            drawFireballPixelArt(g2, tileX, tileY, tileSize);
        }
    }

    private void drawIceBoltPixelArt(Graphics2D g2, int tileX, int tileY, int tileSize) {
        int pixel = Math.max(2, tileSize / 7);
        int size = pixel * 5;
        int left = tileX + (tileSize - size) / 2;
        int top = tileY + (tileSize - size) / 2;
        g2.setColor(Color.BLUE);
        g2.fillRect(left, top, size, size);
        g2.setColor(Color.CYAN);
        g2.fillRect(left + pixel, top + pixel, size - pixel * 2, size - pixel * 2);
        g2.setColor(Color.WHITE);
        g2.fillRect(left + pixel * 2, top + pixel * 2, pixel, pixel);
    }

    private void drawFireballPixelArt(Graphics2D g2, int tileX, int tileY, int tileSize) {
        int pixel = Math.max(2, tileSize / 7);
        int size = pixel * 5;
        int left = tileX + (tileSize - size) / 2;
        int top = tileY + (tileSize - size) / 2;
        g2.setColor(Color.RED);
        g2.fillRect(left, top, size, size);
        g2.setColor(Color.ORANGE);
        g2.fillRect(left + pixel, top + pixel, size - pixel * 2, size - pixel * 2);
        g2.setColor(Color.YELLOW);
        g2.fillRect(left + pixel * 2, top + pixel * 2, pixel, pixel);
    }

    private void drawBossProjectilePixelArt(Graphics2D g2, int tileX, int tileY, int tileSize) {
        int pixel = Math.max(2, tileSize / 7);
        int size = pixel * 5;
        int left = tileX + (tileSize - size) / 2;
        int top = tileY + (tileSize - size) / 2;
        g2.setColor(new Color(70, 20, 120));
        g2.fillRect(left, top, size, size);
        g2.setColor(new Color(160, 60, 230));
        g2.fillRect(left + pixel, top + pixel, size - pixel * 2, size - pixel * 2);
        g2.setColor(new Color(235, 170, 255));
        g2.fillRect(left + pixel * 2, top + pixel * 2, pixel, pixel);
    }

    /** Fills a rectangle with non-negative width and height (Graphics2D ignores negative sizes). */
    private static void fillRectPositive(Graphics2D g2, int x, int y, int width, int height) {
        if (width < 0) {
            x += width;
            width = -width;
        }
        if (height < 0) {
            y += height;
            height = -height;
        }
        if (width > 0 && height > 0) {
            g2.fillRect(x, y, width, height);
        }
    }

    /** Brown 8-bit arrow oriented along travel direction. */
    private void drawHeroArrowPixelArt(Graphics2D g2, int tileX, int tileY, int tileSize, int dx, int dy) {
        int pixel = Math.max(2, tileSize / 7);
        int cx = tileX + tileSize / 2;
        int cy = tileY + tileSize / 2;
        Color shaft = new Color(101, 67, 33);
        Color head = new Color(210, 210, 210);
        Color fletch = new Color(139, 90, 43);

        if (dx != 0 && dy == 0) {
            g2.setColor(shaft);
            fillRectPositive(g2, cx - pixel * 2, cy - pixel / 2, pixel * 4, pixel);
            g2.setColor(head);
            if (dx > 0) {
                fillRectPositive(g2, cx + pixel * 2, cy - pixel, pixel, pixel * 2);
                g2.setColor(fletch);
                fillRectPositive(g2, cx - pixel * 3, cy - pixel, pixel, pixel * 2);
            } else {
                fillRectPositive(g2, cx - pixel * 3, cy - pixel, pixel, pixel * 2);
                g2.setColor(fletch);
                fillRectPositive(g2, cx + pixel * 2, cy - pixel, pixel, pixel * 2);
            }
        } else if (dy != 0 && dx == 0) {
            g2.setColor(shaft);
            fillRectPositive(g2, cx - pixel / 2, cy - pixel * 2, pixel, pixel * 4);
            g2.setColor(head);
            if (dy > 0) {
                fillRectPositive(g2, cx - pixel, cy + pixel * 2, pixel * 2, pixel);
                g2.setColor(fletch);
                fillRectPositive(g2, cx - pixel, cy - pixel * 3, pixel * 2, pixel);
            } else {
                fillRectPositive(g2, cx - pixel, cy - pixel * 3, pixel * 2, pixel);
                g2.setColor(fletch);
                fillRectPositive(g2, cx - pixel, cy + pixel * 2, pixel * 2, pixel);
            }
        } else {
            g2.setColor(shaft);
            fillRectPositive(g2, cx - pixel, cy - pixel, pixel * 2, pixel * 2);
            g2.setColor(head);
            fillRectPositive(g2, cx + dx * pixel, cy + dy * pixel, pixel, pixel);
            g2.setColor(fletch);
            fillRectPositive(g2, cx - dx * pixel, cy - dy * pixel, pixel, pixel);
        }
    }

    private void drawHero(Graphics2D g2, DungeonMap map, int tileSize, int offsetX, int offsetY) {
        Hero hero = engine.getHero();
        if (hero == null || (engine.getGameMode() == GameMode.TEAM_MATCH && hero.getHp() <= 0)) {
            return;
        }

        // In Team Match the hero is controlled by the player and remains visually
        // the hero character, even though he belongs to Team B's knight-side group.
        BufferedImage heroSprite = SpriteRegistry.heroFrame(heroFrame);
        if (heroSprite == null) {
            GridCell cell = map.getCell(hero.getX(), hero.getY());
            if (cell == null) {
                return;
            }
            int px = offsetX + hero.getX() * tileSize;
            int py = offsetY + hero.getY() * tileSize;
            int inset = Math.max(1, tileSize / 5);
            int entityW = Math.max(1, tileSize - inset * 2);
            int entityH = Math.max(1, tileSize - inset * 2);
            g2.setColor(HERO);
            g2.fillRect(px + inset, py + inset, entityW, entityH);
            return;
        }

        int spriteW = Math.round(heroSprite.getWidth() * HERO_SPRITE_SCALE);
        int spriteH = Math.round(heroSprite.getHeight() * HERO_SPRITE_SCALE);
        float remaining = 1f - heroAnimProgress;
        float renderGridX = hero.getX() + ((heroPixelOffsetX * remaining) / tileSize);
        float renderGridY = hero.getY() + ((heroPixelOffsetY * remaining) / tileSize);
        int drawX = offsetX + Math.round(renderGridX * tileSize) + (tileSize - spriteW) / 2;
        int drawY = offsetY + Math.round(renderGridY * tileSize) + (tileSize - spriteH) / 2;

        if (heroFacingLeft) {
            g2.drawImage(heroSprite, drawX + spriteW, drawY, -spriteW, spriteH, null);
        } else {
            g2.drawImage(heroSprite, drawX, drawY, spriteW, spriteH, null);
        }
        drawEquippedArmorOverlay(g2, hero, drawX, drawY, spriteW, spriteH);
        drawEquippedWeaponOverlay(g2, hero, drawX, drawY, spriteW, spriteH);
    }

    private void drawEquippedArmorOverlay(Graphics2D g2, Hero hero, int drawX, int drawY, int spriteW,
            int spriteH) {
        if (hero.getEquippedArmor() == null || HeroArmorPixelArt.armorImage == null) {
            return;
        }
        g2.drawImage(HeroArmorPixelArt.armorImage, drawX, drawY, spriteW, spriteH, null);
    }

    private void drawEquippedWeaponOverlay(Graphics2D g2, Hero hero, int drawX, int drawY, int spriteW,
            int spriteH) {
        Weapon weapon = hero.getEquippedWeapon();
        if (weapon == null) {
            return;
        }
        int handX = heroFacingLeft ? drawX + spriteW / 4 : drawX + spriteW * 3 / 4;
        int handY = drawY + spriteH / 2;
        int pixel = Math.max(2, spriteW / 12);
        HeroProjectileStyle style = weapon.getProjectileStyle();
        if (isWoodenBow(weapon)) {
            drawEquippedBowPixelArt(g2, handX, handY, pixel);
        } else if (style == HeroProjectileStyle.FIRE_BALL) {
            g2.setColor(new Color(220, 80, 40));
            g2.fillRect(handX, handY - pixel, pixel * 2, pixel * 2);
            g2.setColor(new Color(255, 180, 60));
            g2.fillRect(handX + pixel / 2, handY - pixel / 2, pixel, pixel);
        } else if (isMagicWand(weapon)) {
            drawEquippedWandPixelArt(g2, handX, handY, pixel);
        } else if (weapon.isRanged()) {
            g2.setColor(new Color(70, 140, 220));
            g2.fillRect(handX, handY - pixel, pixel * 2, pixel * 3);
            g2.setColor(Color.CYAN);
            g2.fillRect(handX + pixel / 2, handY - pixel * 2, pixel, pixel);
        } else {
            g2.setColor(new Color(190, 190, 200));
            g2.fillRect(handX, handY - pixel * 2, pixel, pixel * 4);
        }
    }

    private static boolean isMagicWand(Weapon weapon) {
        if (weapon == null) {
            return false;
        }
        return "B23_WAND".equals(weapon.getType().id())
                || "staves".equals(weapon.getType().category())
                || weapon.getName().toLowerCase(java.util.Locale.ROOT).contains("wand");
    }

    private static boolean isWoodenBow(Weapon weapon) {
        if (weapon == null) {
            return false;
        }
        return "B23_BOW".equals(weapon.getType().id())
                || "bows".equals(weapon.getType().category())
                || weapon.getName().toLowerCase(java.util.Locale.ROOT).contains("bow");
    }

    private void drawEquippedBowPixelArt(Graphics2D g2, int handX, int handY, int pixel) {
        int dir = heroFacingLeft ? -1 : 1;
        int shaftStartX = handX - dir * pixel * 2;
        int shaftEndX = handX + dir * pixel * 6;
        int bowTopX = handX + dir * pixel * 3;
        int bowMidX = handX + dir * pixel * 5;
        int bowBotX = handX + dir * pixel * 3;
        int topY = handY - pixel * 4;
        int bottomY = handY + pixel * 4;

        g2.setColor(new Color(222, 216, 196));
        drawPixelLine(g2, shaftStartX, handY, bowTopX, topY, pixel);
        drawPixelLine(g2, shaftStartX, handY, bowBotX, bottomY, pixel);

        g2.setColor(new Color(74, 38, 18));
        drawPixelLine(g2, bowTopX + dir * pixel, topY, bowMidX + dir * pixel, handY - pixel * 2, pixel);
        drawPixelLine(g2, bowMidX + dir * pixel, handY - pixel * 2,
                bowMidX + dir * pixel, handY + pixel * 2, pixel);
        drawPixelLine(g2, bowMidX + dir * pixel, handY + pixel * 2,
                bowBotX + dir * pixel, bottomY, pixel);
        g2.setColor(new Color(205, 132, 62));
        drawPixelLine(g2, bowTopX, topY, bowMidX, handY - pixel * 2, pixel);
        drawPixelLine(g2, bowMidX, handY - pixel * 2, bowMidX, handY + pixel * 2, pixel);
        drawPixelLine(g2, bowMidX, handY + pixel * 2, bowBotX, bottomY, pixel);

        g2.setColor(new Color(92, 52, 24));
        fillRectPositive(g2, shaftStartX, handY - pixel, shaftEndX - shaftStartX, pixel * 2);
        g2.setColor(new Color(201, 130, 62));
        fillRectPositive(g2, shaftStartX, handY - pixel / 2, shaftEndX - shaftStartX, pixel);
        g2.setColor(new Color(220, 220, 220));
        fillRectPositive(g2, shaftEndX - dir * pixel * 2, handY - pixel * 2, dir * pixel * 2, pixel);
        fillRectPositive(g2, shaftEndX - dir * pixel, handY - pixel, dir * pixel * 2, pixel);
        fillRectPositive(g2, shaftEndX, handY, dir * pixel, pixel);
        fillRectPositive(g2, shaftEndX - dir * pixel, handY + pixel, dir * pixel * 2, pixel);
        fillRectPositive(g2, shaftEndX - dir * pixel * 2, handY + pixel * 2, dir * pixel * 2, pixel);
        g2.setColor(new Color(244, 205, 103));
        g2.fillRect(handX - pixel, handY - pixel, pixel * 3, pixel * 2);
    }

    private void drawEquippedWandPixelArt(Graphics2D g2, int handX, int handY, int pixel) {
        int dir = heroFacingLeft ? -1 : 1;
        int baseX = handX - dir * pixel;
        int baseY = handY + pixel * 2;
        int tipX = handX + dir * pixel * 4;
        int tipY = handY - pixel * 3;
        g2.setColor(new Color(45, 27, 18));
        drawPixelLine(g2, baseX - dir * pixel, baseY + pixel, tipX - dir * pixel, tipY + pixel, pixel);
        g2.setColor(new Color(118, 73, 43));
        drawPixelLine(g2, baseX, baseY, tipX, tipY, pixel);
        g2.setColor(new Color(170, 230, 255));
        g2.fillRect(tipX, tipY - pixel, pixel, pixel);
        g2.setColor(Color.WHITE);
        g2.fillRect(tipX + dir * pixel, tipY - pixel * 2, pixel, pixel);
    }

    private static void drawPixelLine(Graphics2D g2, int x1, int y1, int x2, int y2, int pixel) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1)) / Math.max(1, pixel);
        steps = Math.max(1, steps);
        for (int i = 0; i <= steps; i++) {
            int x = x1 + Math.round((x2 - x1) * (i / (float) steps));
            int y = y1 + Math.round((y2 - y1) * (i / (float) steps));
            g2.fillRect(x, y, pixel, pixel);
        }
    }

    private void drawHud(Graphics2D g2) {
        Hero hero = engine.getHero();
        if (hero == null) {
            return;
        }
        int x = 10;
        int y = 10;
        int w = 176;
        int h = 216;

        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRect(x + 6, y + 7, w - 4, h - 4);
        g2.setColor(HUD_STONE_OUTLINE);
        g2.fillRect(x, y, w, h);
        g2.setColor(HUD_STONE_BORDER);
        g2.fillRect(x + 3, y + 3, w - 6, h - 6);
        g2.setColor(HUD_STONE_HIGHLIGHT);
        g2.fillRect(x + 6, y + 6, w - 12, 2);
        g2.fillRect(x + 6, y + 6, 2, h - 12);
        g2.setColor(new Color(55, 47, 42));
        g2.fillRect(x + 6, y + h - 8, w - 12, 2);
        g2.fillRect(x + w - 8, y + 6, 2, h - 12);
        g2.setColor(HUD_PANEL_FILL);
        g2.fillRect(x + 10, y + 10, w - 20, h - 20);
        g2.setColor(HUD_PANEL_INSET);
        g2.fillRect(x + 15, y + 15, w - 30, h - 30);

        g2.setFont(retroHudFont(12f));
        g2.setColor(HUD_GOLD);
        g2.drawString("HERO STATUS", x + 25, y + 34);
        g2.fillRect(x + 24, y + 42, w - 48, 2);

        g2.setFont(retroHudFont(11f));
        drawHudStat(g2, x, y + 55, HUD_HP, "HP", Integer.toString(hero.getHp()));
        drawHudStat(g2, x, y + 73, HUD_ENERGY, "ENERGY", Integer.toString(hero.getEnergy()));
        drawHudStat(g2, x, y + 91, HUD_COINS, "COINS", Integer.toString(hero.getCoinBalance()));
        drawHudStat(g2, x, y + 109, HUD_STR, "STR", Integer.toString(hero.getStr()));
        drawHudStat(g2, x, y + 127, HUD_DEF, "DEF", Integer.toString(hero.getDef()));
        drawHudStat(g2, x, y + 145, HUD_MANA, "MANA", Integer.toString(hero.getMana()));

        Weapon equipped = hero.getEquippedWeapon();
        String weaponLabel = equipped == null ? "Unarmed" : equipped.getName();
        drawHudCombinedStat(g2, x, y + 163, HUD_GOLD, "WEAPON: " + weaponLabel);

        long elapsedSeconds = (System.currentTimeMillis() - playStartTime) / 1000;
        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds % 60;
        String timeText = String.format("%02d:%02d", minutes, seconds);

        drawHudStat(g2, x, y + 181, Color.LIGHT_GRAY, "TIME", timeText);
    }

    private void drawHudStat(Graphics2D g2, int panelX, int rowY, Color marker, String label, String value) {
        drawHudMarker(g2, panelX, rowY, marker);
        g2.setColor(HUD_TEXT);
        g2.drawString(label, panelX + 44, rowY + 12);
        g2.setColor(HUD_TITLE);
        int valueX = panelX + 145 - g2.getFontMetrics().stringWidth(value);
        g2.drawString(value, valueX, rowY + 12);
    }

    /** Single-line HUD row (e.g. {@code WEAPON: Bow}) so label and value do not overlap. */
    private void drawHudCombinedStat(Graphics2D g2, int panelX, int rowY, Color marker, String text) {
        drawHudMarker(g2, panelX, rowY, marker);
        g2.setColor(HUD_TITLE);
        g2.drawString(text, panelX + 44, rowY + 12);
    }

    private void drawHudMarker(Graphics2D g2, int panelX, int rowY, Color marker) {
        g2.setColor(HUD_STONE_OUTLINE);
        g2.fillRect(panelX + 25, rowY + 2, 11, 11);
        g2.setColor(marker);
        g2.fillRect(panelX + 27, rowY + 4, 7, 7);
    }

    private java.awt.Font retroHudFont(float size) {
        java.awt.Font base = RetroTheme.UI_MONO_SMALL;
        if (base == null) {
            base = getFont();
        }
        return base.deriveFont(java.awt.Font.PLAIN, size);
    }

    private void drawEnemyHpBar(Graphics2D g2, Entity ent, int px, int py, int cellW, int cellH) {
        int currentHp;
        int maxHp;
        if (ent instanceof Knight knight) {
            currentHp = knight.getHp();
            maxHp = KNIGHT_MAX_HP;
        } else if (ent instanceof Sorcerer sorcerer) {
            currentHp = sorcerer.getHp();
            maxHp = SORCERER_MAX_HP;
        } else if (ent instanceof BossEnemy boss) {
            currentHp = boss.getHp();
            maxHp = boss.getMaxHp();
        } else if (ent instanceof PetEntity petEntity) {
            currentHp = petEntity.getPet().getHp();
            maxHp = petEntity.getPet().getMaxHp();
        } else {
            return;
        }

        maxHp = Math.max(maxHp, currentHp);
        int barW = Math.max(10, cellW - 8);
        int barH = Math.max(3, cellH / 10);
        int barX = px + (cellW - barW) / 2;
        int barY = py + cellH - barH - 2;
        int fillW = Math.round(barW * Math.max(0, currentHp) / (float) maxHp);

        g2.setColor(new Color(20, 20, 24, 210));
        g2.fillRect(barX, barY, barW, barH);
        g2.setColor(new Color(70, 210, 90));
        g2.fillRect(barX, barY, fillW, barH);
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawRect(barX, barY, barW, barH);
    }

    /**
     * Renders a small Chasing/Roaming tag just above the enemy sprite for debug visibility.
     * Keeps rendering work tight so it stays cheap even with 5 enemies on screen.
     */
    private void drawAiStateLabel(Graphics2D g2, Entity ent, int px, int py, int cellW) {
        String label;
        Color color;
        if (engine.isEnemyFrozen(ent)) {
            label = "FROZEN";
            color = new Color(110, 220, 255);
        } else if (ent instanceof Knight k) {
            label = k.getAiState().name();
            color = k.getAiState() == model.AIState.CHASING ? new Color(255, 90, 90) : new Color(200, 200, 200);
        } else if (ent instanceof Sorcerer s) {
            label = s.getAiState().name();
            color = s.getAiState() == model.AIState.CHASING ? new Color(255, 90, 90) : new Color(200, 200, 200);
        } else if (ent instanceof BossEnemy boss) {
            label = boss.getAiState().name();
            color = boss.getAiState() == model.AIState.CHASING
                    ? new Color(210, 110, 255) : new Color(200, 200, 200);
        } else {
            return;
        }
        g2.setFont(g2.getFont().deriveFont(10f));
        java.awt.FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(label);
        int textX = px + (cellW - textW) / 2;
        int textY = py - 2;
        // background for readability
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(textX - 2, textY - fm.getAscent(), textW + 4, fm.getHeight());
        g2.setColor(color);
        g2.drawString(label, textX, textY - 2);
    }

    private record GridPosition(int x, int y) {
    }

    private record EnemyDrawPosition(int x, int y) {
    }

    private static final class EnemyMoveAnimation {
        private final GridPosition from;
        private final GridPosition to;
        private float progress;

        private EnemyMoveAnimation(GridPosition from, GridPosition to) {
            this.from = from;
            this.to = to;
        }
    }

}
