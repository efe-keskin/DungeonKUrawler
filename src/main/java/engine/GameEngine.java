package engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import model.DungeonMap;
import model.EnemyFactory;
import model.Entity;
import model.GridCell;
import model.Hero;
import model.Item;
import model.Potion;

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

    private static final long IDLE_REFILL_DELAY_NANOS = TimeUnit.SECONDS.toNanos(1);
    private static final long ENERGY_REFILL_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(1);
    private static final int ENERGY_REFILL_PER_TICK = 5;

    private final DungeonMap dungeonMap;
    private final Hero hero;
    private final Random random;
    private final EnemyFactory enemyFactory;
    private final List<GameStateListener> listeners = new CopyOnWriteArrayList<>();
    private long lastMoveNanos = System.nanoTime();
    private long lastEnergyRefillNanos = lastMoveNanos;

    public GameEngine() {
        this(ThreadLocalRandom.current());
    }

    GameEngine(Random random) {
        this.random = random;
        this.enemyFactory = new EnemyFactory(random);
        GameInitializer.InitialGameState initialState = new GameInitializer().createInitialState("Phase 1 — Build Mode");
        this.dungeonMap = initialState.map();
        this.hero = initialState.hero();
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

    public DungeonMap getDungeonMap() {
        return dungeonMap;
    }

    public Hero getHero() {
        return hero;
    }

    public void updateHeroPosition(int nx, int ny) {
        int oldX = hero.getX();
        int oldY = hero.getY();
        hero.moveTo(nx, ny);
        dungeonMap.moveEntity(hero, oldX, oldY, nx, ny);

        lastMoveNanos = System.nanoTime();
        lastEnergyRefillNanos = lastMoveNanos;
        notifyListeners();
    }

    /**
     * Refills a small amount of energy if the hero has been idle for at least 1s.
     * Safe to call on every UI tick — no-ops when still within the idle window,
     * when energy is already full, or until the next refill interval elapses.
     */
    public void tickEnergyRefill() {
        long now = System.nanoTime();
        if (hero.getEnergy() >= hero.getMaxEnergy()) {
            return;
        }
        if (now - lastMoveNanos < IDLE_REFILL_DELAY_NANOS) {
            lastEnergyRefillNanos = now;
            return;
        }
        if (now - lastEnergyRefillNanos < ENERGY_REFILL_INTERVAL_NANOS) {
            return;
        }
        hero.gainEnergy(ENERGY_REFILL_PER_TICK);
        lastEnergyRefillNanos = now;
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
                if (cell == null || !cell.hasItems()) {
                    continue;
                }
                for (Item item : cell.getItemsView()) {
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
        Potion potionToConsume = null;
        for (Item item : cell.getItemsView()) {
            if (item instanceof Potion potion) {
                potionToConsume = potion;
                break;
            }
        }
        if (potionToConsume != null && cell.removeItem(potionToConsume)) {
            potionToConsume.drink(hero);
            notifyListeners();
            return true;
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
                if (c.hasEntities()) {
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
            cell.addEntity(enemy);
        }
        notifyListeners();
        return "spawnEnemyProcedurally: " + enemy.getName() + " at (" + pick[0] + "," + pick[1] + ")";
    }
}
