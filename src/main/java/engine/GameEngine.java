package engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import model.Chest;
import model.Container;
import model.DungeonMap;
import model.EnemyFactory;
import model.Entity;
import model.GridCell;
import model.HealPotion;
import model.Hero;
import model.Item;
import model.Key;
import model.KeyColor;
import model.Lockable;
import model.ManaPotion;
import model.Potion;
import javax.swing.Timer;

import model.AIState;
import model.Knight;
import model.Sorcerer;

/**
 * Game state owner and observer subject.
 * Keeps overall game state and notifies listeners after mutations.
 *
 * <p>
 * <strong>Observer (Subject):</strong> maintains listener list and
 * {@link #notifyListeners()} after
 * mutations so views stay decoupled from model internals.
 */
public class GameEngine {

    private static final long IDLE_REFILL_DELAY_NANOS = TimeUnit.SECONDS.toNanos(2);
    private static final int ENERGY_REFILL_PER_TICK = 5;

    private final DungeonMap dungeonMap;
    private final Hero hero;
    private final Random random;
    private final EnemyFactory enemyFactory;
    private final List<GameStateListener> listeners = new CopyOnWriteArrayList<>();
    private long lastMoveNanos = System.nanoTime();

    private Timer spawnTimer;
    private Timer detectionTimer;

    private static final int SPAWN_INTERVAL_MS = 9000;
    private static final int DETECTION_TICK_MS = 300;
    private static final int MAX_ENEMIES = 5;
    private static final double KNIGHT_VISION_RANGE = 5.0;
    private static final double SORCERER_VISION_RANGE = Double.POSITIVE_INFINITY; // sees hero always

    public GameEngine() {
        this(ThreadLocalRandom.current());
    }

    GameEngine(Random random) {
        this.random = random;
        this.enemyFactory = new EnemyFactory(random);
        this.dungeonMap = buildDemoMap("Phase 1 — Build Mode");
        this.hero = new Hero(1, 1, "Hero", 100, 10, 20, 5, 100);
        placeHeroOnMap();
        startEnemyTimers();
    }

    public void addGameStateListener(GameStateListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeGameStateListener(GameStateListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (GameStateListener listener : listeners) {
            listener.onGameStateChanged();
        }
    }

    // DEMO WALLS
    private DungeonMap buildDemoMap(String levelName) {
        int w = 16;
        int h = 12;
        DungeonMap map = new DungeonMap(levelName, w, h);

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                boolean wall = (x == 0 || y == 0 || x == w - 1 || y == h - 1);
                GridCell c = map.getCell(x, y);
                if (c != null) {
                    c.setPassable(!wall);
                }
            }
        }

        // inner 2x2 wall block
        for (int x = 6; x <= 7; x++) {
            for (int y = 4; y <= 5; y++) {
                GridCell c = map.getCell(x, y);
                if (c != null) {
                    c.setPassable(false);
                }
            }
        }

        // Temporary test items: hero should be able to move onto these cells.
        GridCell itemCell1 = map.getCell(3, 1);
        if (itemCell1 != null) {
            itemCell1.getItems().add(new HealPotion());
        }

        GridCell itemCell2 = map.getCell(5, 3);
        if (itemCell2 != null) {
            itemCell2.getItems().add(new ManaPotion());
        }

        GridCell chestCell = map.getCell(4, 2);
        if (chestCell != null) {
            Chest chest = new Chest("Wooden Chest", 16);
            chest.addItem(new HealPotion());
            chest.addItem(new Key("silver", KeyColor.SILVER));
            chestCell.getItems().add(chest);
        }

        GridCell keyCell = map.getCell(8, 2);
        if (keyCell != null) {
            keyCell.getItems().add(new Key("olive", KeyColor.OLIVE));
        }

        GridCell lockedChestCell = map.getCell(10, 6);
        if (lockedChestCell != null) {
            Chest lockedChest = Chest.locked("Silver Chest", 16, "silver");
            lockedChest.addItem(new ManaPotion());
            lockedChest.addItem(new Key("gold", KeyColor.GOLD));
            lockedChestCell.getItems().add(lockedChest);
        }

        return map;
    }

    /**
     * Attempts to unlock {@code target} with whatever matching key the hero
     * is carrying. Delegates to {@link LockController} so unlock rules stay
     * in one place; this helper just notifies observers on success.
     */
    public LockController.UnlockResult tryUnlock(Lockable target) {
        LockController controller = new LockController();
        LockController.UnlockResult result = controller.tryUnlock(target, hero.getInventory());
        if (result == LockController.UnlockResult.UNLOCKED
                || result == LockController.UnlockResult.UNLOCKED_KEY_CONSUMED) {
            notifyListeners();
        }
        return result;
    }

    /**
     * Returns the first {@link Container} on the hero's current cell or any
     * 8-adjacent cell — locked or not. Caller decides whether to unlock
     * (via {@link #tryUnlock(Lockable)}) or open directly.
     */
    public Container findContainerNearHero() {
        int hx = hero.getX();
        int hy = hero.getY();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                GridCell cell = dungeonMap.getCell(hx + dx, hy + dy);
                if (cell == null) {
                    continue;
                }
                for (Item item : cell.getItems()) {
                    if (item instanceof Container container) {
                        return container;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Moves {@code item} from {@code container} into the hero's inventory.
     * @return true if the transfer succeeded.
     */
    public boolean takeFromContainer(Container container, Item item) {
        if (container == null || item == null) {
            return false;
        }
        if (!hero.getInventory().hasFreeSlot()) {
            return false;
        }
        if (!container.getContents().contains(item)) {
            return false;
        }
        if (!hero.getInventory().tryAdd(item)) {
            return false;
        }
        container.removeItem(item);
        notifyListeners();
        return true;
    }

    private void placeHeroOnMap() {
        GridCell cell = dungeonMap.getCell(hero.getX(), hero.getY());
        if (cell != null) {
            cell.getEntities().add(hero);
        }
    }

    public DungeonMap getDungeonMap() {
        return dungeonMap;
    }

    public Hero getHero() {
        return hero;
    }

    public void updateHeroPosition(int nx, int ny) {
        GridCell from = dungeonMap.getCell(hero.getX(), hero.getY());
        GridCell to = dungeonMap.getCell(nx, ny);

        if (from != null) {
            from.getEntities().remove(hero);
        }

        hero.updatePosition(nx, ny);

        if (to != null && !to.getEntities().contains(hero)) {
            to.getEntities().add(hero);
        }

        lastMoveNanos = System.nanoTime();
        notifyListeners();
    }

    /**
     * Refills a small amount of energy if the hero has been idle for at least 2s.
     * Safe to call on every UI tick — no-ops when still within the idle window or
     * when energy is already full.
     */
    public void tickEnergyRefill() {
        if (hero.getEnergy() >= hero.getMaxEnergy()) {
            return;
        }
        if (System.nanoTime() - lastMoveNanos < IDLE_REFILL_DELAY_NANOS) {
            return;
        }
        hero.refillEnergy(ENERGY_REFILL_PER_TICK);
        notifyListeners();
    }

    /**
     * Removes {@code potion} from the hero's inventory and applies its effect.
     * No-op when the potion is not in the inventory.
     */
    public void consumePotion(Potion potion) {
        if (potion == null) {
            return;
        }
        if (!hero.getInventory().remove((Item) potion)) {
            return;
        }
        potion.drink(hero);
        notifyListeners();
    }

    /**
     * Picks up the first takable item found on the hero's current tile or any
     * 8-adjacent tile and adds it to the inventory.
     *
     * @return true if an item was picked up, false otherwise.
     */
    public boolean takeItemOnGround() {
        Hero hero = getHero();
        int hx = hero.getX();
        int hy = hero.getY();
        model.Inventory inv = hero.getInventory();
        if (!inv.hasFreeSlot()) {
            return false;
        }
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int tx = hx + dx;
                int ty = hy + dy;
                GridCell cell = dungeonMap.getCell(tx, ty);
                if (cell == null || cell.getItems().isEmpty()) {
                    continue;
                }
                for (Item item : cell.getItems()) {
                    if (item.isTakable()) {
                        return takeItem(item, tx, ty);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Drinks the first potion on the hero's current tile, removing it from the map.
     * @return true if a potion was drunk, false if none is present.
     */
    public boolean consumePotionOnGround() {
        GridCell cell = dungeonMap.getCell(hero.getX(), hero.getY());
        if (cell == null) {
            return false;
        }
        for (Item item : cell.getItems()) {
            if (item instanceof Potion potion) {
                cell.getItems().remove(item);
                potion.drink(hero);
                notifyListeners();
                return true;
            }
        }
        return false;
    }

    /**
     * Picks up {@code item} from cell {@code (x, y)} into the hero's inventory.
     *
     * <p>Preconditions are enforced here so callers don't need to duplicate the checks.
     *
     * @return {@code true}  — item moved to inventory and map updated;<br>
     *         {@code false} — rejected (item not takable or inventory full).
     */
    public boolean takeItem(model.Item item, int x, int y) {
        if (item == null || !item.isTakable()) {
            return false;
        }
        model.Inventory inv = hero.getInventory();
        if (!inv.hasFreeSlot()) {
            return false;
        }
        boolean added = inv.tryAdd(item);
        if (!added) {
            return false;
        }
        dungeonMap.removeItemFromCell(item, x, y);
        notifyListeners();
        return true;
    }

    /**
     * Uses {@link EnemyFactory#createRandomEnemy(int, int)} for the 60/30/10 split.
     */
    public String spawnEnemyProcedurally() {
        List<int[]> candidates = new ArrayList<>();
        for (int x = 0; x < dungeonMap.getWidth(); x++) {
            for (int y = 0; y < dungeonMap.getHeight(); y++) {
                GridCell c = dungeonMap.getCell(x, y);
                if (c == null || !c.isPassable()) {
                    continue;
                }
                if (x == hero.getX() && y == hero.getY()) {
                    continue;
                }
                if (!c.getEntities().isEmpty()) {
                    continue;
                }
                candidates.add(new int[] { x, y });
            }
        }
        if (candidates.isEmpty()) {
            return "spawnEnemyProcedurally: no empty passable cell";
        }
        int[] pick = candidates.get(random.nextInt(candidates.size()));
        Entity enemy = enemyFactory.createRandomEnemy(pick[0], pick[1]);
        if (enemy == null) {
            return "spawnEnemyProcedurally: None (10%) at (" + pick[0] + "," + pick[1] + ")";
        }
        GridCell cell = dungeonMap.getCell(pick[0], pick[1]);
        if (cell != null) {
            cell.getEntities().add(enemy);
        }
        notifyListeners();
        return "spawnEnemyProcedurally: " + enemy.getName() + " at (" + pick[0] + "," + pick[1] + ")";
    }

    /**
     * Kicks off the spawn loop (every 9s, capped at {@value #MAX_ENEMIES}) and the AI
     * detection tick (every {@value #DETECTION_TICK_MS}ms). Swing Timers run on the EDT,
     * so mutations here are safe w.r.t. the painter.
     */
    private void startEnemyTimers() {
        spawnTimer = new Timer(SPAWN_INTERVAL_MS, e -> {
            if (countEnemies() >= MAX_ENEMIES) {
                return;
            }
            String msg = spawnEnemyProcedurally();
            System.out.println("[spawn] " + msg);
        });
        spawnTimer.setRepeats(true);
        spawnTimer.start();

        detectionTimer = new Timer(DETECTION_TICK_MS, e -> updateEnemyDetection());
        detectionTimer.setRepeats(true);
        detectionTimer.start();
    }

    /** Counts all Knight/Sorcerer entities currently on the map. */
    private int countEnemies() {
        int count = 0;
        for (int x = 0; x < dungeonMap.getWidth(); x++) {
            for (int y = 0; y < dungeonMap.getHeight(); y++) {
                GridCell c = dungeonMap.getCell(x, y);
                if (c == null) continue;
                for (Entity e : c.getEntities()) {
                    if (e instanceof Knight || e instanceof Sorcerer) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Visits every enemy on the map, computes Euclidean distance to the hero, and flips
     * its {@link AIState} to CHASING when within vision range (else ROAMING). Prints to
     * console only on state transitions to avoid log spam.
     */
    private void updateEnemyDetection() {
        boolean changed = false;
        for (int x = 0; x < dungeonMap.getWidth(); x++) {
            for (int y = 0; y < dungeonMap.getHeight(); y++) {
                GridCell c = dungeonMap.getCell(x, y);
                if (c == null) continue;
                for (Entity e : c.getEntities()) {
                    if (updateAiStateFor(e)) {
                        changed = true;
                    }
                }
            }
        }
        if (changed) {
            notifyListeners();
        }
    }

    /**
     * Updates a single enemy's AI state based on distance to the hero.
     * @return true if the state actually changed (so we can log + repaint)
     */
    private boolean updateAiStateFor(Entity e) {
        double dist = euclideanDistanceToHero(e.getX(), e.getY());
        AIState next;
        AIState current;
        String label;

        if (e instanceof Knight k) {
            next = dist <= KNIGHT_VISION_RANGE ? AIState.CHASING : AIState.ROAMING;
            current = k.getAiState();
            label = "Knight";
            if (next != current) {
                k.setAiState(next);
                System.out.printf("[AI] %s at (%d,%d) dist=%.2f -> %s%n",
                        label, e.getX(), e.getY(), dist, next);
                return true;
            }
        } else if (e instanceof Sorcerer s) {
            // Sorcerer sees hero regardless of LoS per design doc 2.5.2
            next = dist <= SORCERER_VISION_RANGE ? AIState.CHASING : AIState.ROAMING;
            current = s.getAiState();
            label = "Sorcerer";
            if (next != current) {
                s.setAiState(next);
                System.out.printf("[AI] %s at (%d,%d) dist=%.2f -> %s%n",
                        label, e.getX(), e.getY(), dist, next);
                return true;
            }
        }
        return false;
    }

    private double euclideanDistanceToHero(int x, int y) {
        int dx = x - hero.getX();
        int dy = y - hero.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /** Stops timers — call this on shutdown if you wire it up later. */
    public void shutdown() {
        if (spawnTimer != null) spawnTimer.stop();
        if (detectionTimer != null) detectionTimer.stop();
    }
}
