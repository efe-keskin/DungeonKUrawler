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
import model.HealPotion;
import model.Hero;
import model.Item;
import model.ManaPotion;
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

    private static final long IDLE_REFILL_DELAY_NANOS = TimeUnit.SECONDS.toNanos(2);
    private static final int ENERGY_REFILL_PER_TICK = 5;

    private final DungeonMap dungeonMap;
    private final Hero hero;
    private final Random random;
    private final EnemyFactory enemyFactory;
    private final List<GameStateListener> listeners = new CopyOnWriteArrayList<>();
    private long lastMoveNanos = System.nanoTime();

    public GameEngine() {
        this(ThreadLocalRandom.current());
    }

    GameEngine(Random random) {
        this.random = random;
        this.enemyFactory = new EnemyFactory(random);
        this.dungeonMap = buildDemoMap("Phase 1 — Build Mode");
        this.hero = new Hero(1, 1, "Hero", 100, 10, 20, 5, 100);
        placeHeroOnMap();
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

        return map;
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
}
