package engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import model.Chest;
import model.Coin;
import model.Container;
import model.DungeonMap;
import model.EnemyFactory;
import model.Entity;
import model.GridCell;
import model.HealPotion;
import model.Hero;
import model.Item;
import model.ItemAction;
import model.Key;
import model.KeyColor;
import model.Lockable;
import model.EnergyPotion;
import model.ManaPotion;
import model.Potion;
import model.Armor;
import model.Book;
import model.Column;
import model.Ring;
import model.ValuableItem;
import model.ValuableItemCatalog;
import model.Weapon;
import model.WeaponCatalog;
import javax.swing.Timer;

import model.AIState;
import model.Gargoyle;
import model.Grill;
import model.Hole;
import model.Knight;
import model.MissingBrick;
import model.SearchableObject;
import model.Sorcerer;
import model.WaterPipe;

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
    private static final int ENERGY_REFILL_PER_TICK = 5;

    private final DungeonMap dungeonMap;
    private final Hero hero;
    private final Random random;
    private final EnemyFactory enemyFactory;
    private final CombatManager combatManager = new CombatManager();
    private final TargetItemMission targetMission = new TargetItemMission();
    private final List<GameStateListener> listeners = new CopyOnWriteArrayList<>();
    private long lastMoveNanos = System.nanoTime();

    private Timer spawnTimer;
    private Timer coinSpawnTimer;
    private Timer detectionTimer;
    private Timer knightActionTimer;
    private Timer sorcererAttackTimer;
    private Timer sorcererTeleportTimer;

    private static final int SPAWN_INTERVAL_MS = 9000;   // spec 2.5
    private static final int COIN_SPAWN_INTERVAL_MS = 5000;
    private static final int DETECTION_TICK_MS = 300;
    private static final int KNIGHT_ACTION_TICK_MS = 800;
    private static final int SORCERER_ATTACK_TICK_MS = 5000;
    private static final int SORCERER_TELEPORT_TICK_MS = 7000;
    private static final int MAX_ENEMIES = 5;             // spec 2.5
    private static final int MIN_GROUND_COINS = 3;
    private static final int MAX_GROUND_COINS = 7;
    private static final int COIN_REWARD_VALUE = 10;
    private static final double SEARCHABLE_WALL_FILL_RATIO = 0.75;
    private static final double SEARCHABLE_HIDDEN_ITEM_CHANCE = 0.65;
    private static final double KNIGHT_VISION_RANGE = 5.0;
    private static final double SORCERER_VISION_RANGE = Double.POSITIVE_INFINITY; // sees hero always

    public enum SearchOutcome {
        FOUND,
        NOTHING_FOUND,
        INVENTORY_FULL,
        NOT_SEARCHABLE
    }

    public static final class SearchResult {
        private final SearchOutcome outcome;
        private final Item foundItem;

        private SearchResult(SearchOutcome outcome, Item foundItem) {
            this.outcome = outcome;
            this.foundItem = foundItem;
        }

        public static SearchResult found(Item item) {
            return new SearchResult(SearchOutcome.FOUND, item);
        }

        public static SearchResult nothingFound() {
            return new SearchResult(SearchOutcome.NOTHING_FOUND, null);
        }

        public static SearchResult inventoryFull(Item item) {
            return new SearchResult(SearchOutcome.INVENTORY_FULL, item);
        }

        public static SearchResult notSearchable() {
            return new SearchResult(SearchOutcome.NOT_SEARCHABLE, null);
        }

        public SearchOutcome getOutcome() {
            return outcome;
        }

        public Item getFoundItem() {
            return foundItem;
        }
    }

    public GameEngine() {
        this(ThreadLocalRandom.current());
    }

    GameEngine(Random random) {
        this.random = random;
        this.enemyFactory = new EnemyFactory(random);
        this.dungeonMap = buildDemoMap("Phase 1 — Build Mode");
        int startingStr = 8 + random.nextInt(8);  // 8..15 inclusive (spec 2.4.1)
        // Spec section 2.4.1: HP=17, STR=random[8,15], Mana=80, DEF=2.
        // Energy=100 is a project design decision (spec leaves it open).
        this.hero = new Hero(1, 1, "Hero", 17, startingStr, 80, 2, 100);
        placeHeroOnMap();
        fillMinimumGroundCoins(-1, -1);
        startTargetMission();
        startGameTimers();
    }

    public GameEngine(DungeonMap dungeonMap, Hero hero,
            ValuableItem missionTarget, boolean missionStarted, boolean missionWon) {
        this.random = ThreadLocalRandom.current();
        this.enemyFactory = new EnemyFactory(random);
        if (dungeonMap == null || hero == null) {
            throw new IllegalArgumentException("Loaded game requires a map and hero.");
        }
        this.dungeonMap = dungeonMap;
        this.hero = hero;
        placeHeroOnMap();
        this.targetMission.restore(missionTarget, missionStarted, missionWon);
        startGameTimers();
    }

    /**
     * Picks a random valuable, hides it in a random hiding place (today: any
     * {@link Container}; tomorrow: searchable scenery via additional
     * {@link HidingPlaceProvider}s), and arms the win condition.
     */
    private void startTargetMission() {
        HidingPlaceProvider provider = new CompositeHidingPlaceProvider(List.of(
                new ContainerHidingPlaceProvider()));
        ValuableItem target = ValuableItemCatalog.randomValuable(random);
        if (!targetMission.start(provider, dungeonMap, random, target)) {
            System.out.println("[mission] no hiding place available — mission inactive");
        }
    }

    public TargetItemMission getTargetMission() {
        return targetMission;
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

    void notifyGameStateChanged() {
        notifyListeners();
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
            itemCell2.getItems().add(new EnergyPotion());
        }

        GridCell itemCell3 = map.getCell(5, 4);
        if (itemCell3 != null) {
            itemCell3.getItems().add(new ManaPotion());
        }

        GridCell ringCell = map.getCell(3, 3);
        if (ringCell != null) {
            ringCell.getItems().add(new Ring("Protective Ring", 2));
        }

        GridCell weaponCell = map.getCell(11, 3);
        if (weaponCell != null) {
            weaponCell.getItems().add(new Weapon(WeaponCatalog.get().byId("W002")));
        }

        GridCell chestCell = map.getCell(4, 2);
        if (chestCell != null) {
            Chest chest = new Chest("Wooden Chest", 16);
            chest.addItem(new HealPotion());
            chest.addItem(new Key("silver", KeyColor.SILVER));
            chest.addItem(new Book("Explorer's Journal",
                    "The old silver chest protects equipment for anyone brave enough to unlock it."));
            chestCell.getItems().add(chest);
        }

        GridCell keyCell = map.getCell(8, 2);
        if (keyCell != null) {
            keyCell.getItems().add(new Key("olive", KeyColor.OLIVE));
        }

        GridCell lockedChestCell = map.getCell(10, 6);
        if (lockedChestCell != null) {
            Chest lockedChest = Chest.locked("Silver Chest", 16, "silver");
            lockedChest.addItem(new EnergyPotion());
            lockedChest.addItem(new Key("gold", KeyColor.GOLD));
            lockedChest.addItem(new Armor("Leather Armor", 3));
            lockedChestCell.getItems().add(lockedChest);
        }

        placeRandomSearchablesOnHorizontalWalls(map);

        return map;
    }

    private void placeRandomSearchablesOnHorizontalWalls(DungeonMap map) {
        List<int[]> candidates = new ArrayList<>();
        for (int x = 1; x < map.getWidth() - 1; x++) {
            candidates.add(new int[] { x, 0 });
            candidates.add(new int[] { x, map.getHeight() - 1 });
        }

        int count = Math.min(candidates.size(), (int) Math.round(candidates.size() * SEARCHABLE_WALL_FILL_RATIO));
        for (int i = 0; i < count; i++) {
            int[] spot = candidates.remove(random.nextInt(candidates.size()));
            placeSearchable(map, spot[0], spot[1], randomSearchableObject(spot[1] == 0));
        }
    }

    private SearchableObject randomSearchableObject(boolean topWall) {
        Item hiddenItem = randomHiddenSearchItem();
        return switch (random.nextInt(20)) {
            case 0 -> new MissingBrick(MissingBrick.SPRITE_1, hiddenItem);
            case 1 -> new MissingBrick(MissingBrick.SPRITE_2, hiddenItem);
            case 2 -> topWall ? new Hole(hiddenItem) : new WaterPipe(WaterPipe.SMALL_RING_SPRITE, hiddenItem);
            case 3 -> new Grill(Grill.HORIZONTAL_SPRITE, hiddenItem);
            case 4 -> new Grill(Grill.VERTICAL_SPRITE, hiddenItem);
            case 5 -> new WaterPipe(WaterPipe.SMALL_RING_SPRITE, hiddenItem);
            case 6 -> new WaterPipe(WaterPipe.LARGE_RING_SPRITE, hiddenItem);
            case 7 -> new WaterPipe(WaterPipe.TEARDROP_RING_SPRITE, hiddenItem);
            case 8, 9, 10, 11, 12, 13, 14, 15, 16 -> new Gargoyle(randomDripSprite(topWall), hiddenItem);
            case 17 -> new Column(Column.WALL_TOP_SPRITE, hiddenItem);
            case 18 -> new Column(Column.PURPLE_SPRITE, hiddenItem);
            default -> new Column(Column.GRAY_SPRITE, hiddenItem);
        };
    }

    private String randomDripSprite(boolean topWall) {
        String[] sprites = topWall
                ? new String[] {
                        Gargoyle.GREEN_LEFT_SPRITE,
                        Gargoyle.CYAN_LEFT_SPRITE,
                        Gargoyle.RED_MID_SPRITE,
                        Gargoyle.GREEN_MID_SPRITE,
                        Gargoyle.CYAN_MID_SPRITE,
                        Gargoyle.GREEN_RIGHT_SPRITE,
                        Gargoyle.CYAN_RIGHT_SPRITE,
                        Gargoyle.RED_RIGHT_SPRITE
                }
                : new String[] {
                        Gargoyle.RED_LEFT_SPRITE,
                        Gargoyle.RED_MID_SPRITE,
                        Gargoyle.GREEN_MID_SPRITE,
                        Gargoyle.CYAN_MID_SPRITE,
                        Gargoyle.GREEN_RIGHT_SPRITE,
                        Gargoyle.CYAN_RIGHT_SPRITE,
                        Gargoyle.RED_RIGHT_SPRITE
                };
        return sprites[random.nextInt(sprites.length)];
    }

    private Item randomHiddenSearchItem() {
        if (random.nextDouble() >= SEARCHABLE_HIDDEN_ITEM_CHANCE) {
            return null;
        }
        return switch (random.nextInt(6)) {
            case 0 -> new HealPotion();
            case 1 -> new ManaPotion();
            case 2 -> new EnergyPotion();
            case 3 -> new Key("silver", KeyColor.SILVER);
            case 4 -> new Ring("Hidden Ring", 1);
            default -> new Book("Dusty Note", "A folded note found inside the old wall.");
        };
    }

    private void placeSearchable(DungeonMap map, int x, int y, SearchableObject object) {
        GridCell cell = map.getCell(x, y);
        if (cell != null && object != null) {
            cell.getItems().add(object);
        }
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
     * Collects {@code item} from a container. Coin rewards are credited directly
     * to the hero; other items move into the inventory.
     *
     * @return true if the transfer succeeded.
     */
    public boolean takeFromContainer(Container container, Item item) {
        if (container == null || item == null) {
            return false;
        }
        if (!container.getContents().contains(item)) {
            return false;
        }
        if (item instanceof Coin coin) {
            container.removeItem(coin);
            hero.earnCoins(coin.getValue());
            notifyListeners();
            return true;
        }
        if (!hero.getInventory().hasFreeSlot()) {
            return false;
        }
        if (!hero.getInventory().tryAdd(item)) {
            return false;
        }
        container.removeItem(item);
        targetMission.checkPickup(item);
        notifyListeners();
        return true;
    }

    public SearchResult search(SearchableObject object) {
        if (object == null) {
            return SearchResult.notSearchable();
        }
        Item hidden = object.getHiddenItem();
        if (hidden == null) {
            return SearchResult.nothingFound();
        }
        if (!hero.getInventory().hasFreeSlot()) {
            return SearchResult.inventoryFull(hidden);
        }
        Item found = object.takeHiddenItem();
        if (!hero.getInventory().tryAdd(found)) {
            object.setHiddenItem(found);
            return SearchResult.inventoryFull(found);
        }
        targetMission.checkPickup(found);
        notifyListeners();
        return SearchResult.found(found);
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

    /**
     * Credits a coin reward from a future gameplay event such as enemy defeat.
     *
     * @return true if a positive amount was credited.
     */
    public boolean awardCoins(int amount) {
        if (!hero.earnCoins(amount)) {
            return false;
        }
        notifyListeners();
        return true;
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
     * Refills a small amount of energy if the hero has been idle for at least 1s.
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
        performInventoryAction(potion, ItemAction.DRINK);
    }

    /**
     * Applies an action chosen for an item currently carried by the hero.
     * Equipment remains in inventory while equipped; discard removes its
     * contribution before removing the item.
     *
     * <p>Dispatch is delegated to {@link ItemActionEffects}: the action enum
     * is a tag, the effect is a separate strategy looked up by tag.
     */
    public boolean performInventoryAction(Item item, ItemAction action) {
        if (item == null || action == null || !hero.getInventory().getItems().contains(item)) {
            return false;
        }

        boolean removeAction = action == ItemAction.REMOVE && hero.isEquipped(item);
        if (!removeAction && !item.getInventoryActions().contains(action)) {
            return false;
        }

        ItemActionEffects.Effect effect = ItemActionEffects.forAction(action);
        if (effect == null) {
            return false;
        }
        boolean applied = effect.apply(hero, item);
        if (applied && effect.notifyAfterApply()) {
            notifyListeners();
        }
        return applied;
    }

    /**
     * Picks up the first takable item found on the hero's current tile or any
     * 8-adjacent tile. Coin rewards are collected into the coin balance; other
     * items are added to the inventory.
     *
     * @return true if an item was picked up, false otherwise.
     */
    public boolean takeItemOnGround() {
        Hero hero = getHero();
        int hx = hero.getX();
        int hy = hero.getY();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int tx = hx + dx;
                int ty = hy + dy;
                GridCell cell = dungeonMap.getCell(tx, ty);
                if (cell == null || cell.getItems().isEmpty()) {
                    continue;
                }
                for (Item item : cell.getItems()) {
                    boolean canCollect = item instanceof Coin || hero.getInventory().hasFreeSlot();
                    if (item.isTakable() && canCollect) {
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
     * Picks up {@code item} from cell {@code (x, y)}. Coin rewards are credited
     * directly; other items enter the hero's inventory.
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
        if (item instanceof Coin coin) {
            return collectCoin(coin, x, y);
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
        targetMission.checkPickup(item);
        notifyListeners();
        return true;
    }

    private boolean collectCoin(Coin coin, int x, int y) {
        GridCell cell = dungeonMap.getCell(x, y);
        if (cell == null || !cell.getItemsView().contains(coin)) {
            return false;
        }
        if (!dungeonMap.removeItemFromCell(coin, x, y)) {
            return false;
        }
        hero.earnCoins(coin.getValue());
        fillMinimumGroundCoins(x, y);
        notifyListeners();
        return true;
    }

    /**
     * Adds one coin pile on a random empty floor cell, excluding the cell just
     * collected when replenishing the minimum visible rewards.
     */
    private boolean spawnCoinPile(int excludedX, int excludedY) {
        List<GridCell> candidates = new ArrayList<>();
        for (int x = 0; x < dungeonMap.getWidth(); x++) {
            for (int y = 0; y < dungeonMap.getHeight(); y++) {
                if (x == excludedX && y == excludedY) {
                    continue;
                }
                GridCell cell = dungeonMap.getCell(x, y);
                if (cell == null || !cell.isWalkable()
                        || !cell.getItemsView().isEmpty()
                        || !cell.getEntitiesView().isEmpty()) {
                    continue;
                }
                candidates.add(cell);
            }
        }
        if (candidates.isEmpty()) {
            return false;
        }
        GridCell cell = candidates.get(random.nextInt(candidates.size()));
        cell.getItems().add(new Coin(COIN_REWARD_VALUE));
        return true;
    }

    private void fillMinimumGroundCoins(int excludedX, int excludedY) {
        while (countGroundCoins() < MIN_GROUND_COINS) {
            if (!spawnCoinPile(excludedX, excludedY)) {
                return;
            }
        }
    }

    private int countGroundCoins() {
        int count = 0;
        for (int x = 0; x < dungeonMap.getWidth(); x++) {
            for (int y = 0; y < dungeonMap.getHeight(); y++) {
                GridCell cell = dungeonMap.getCell(x, y);
                if (cell == null) {
                    continue;
                }
                for (Item item : cell.getItemsView()) {
                    if (item instanceof Coin) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Spec 2.5: enemies "appear at random locations on the edge of the
     * area". We interpret "edge" as the outer perimeter of the play
     * area only — not next to interior obstacles (pillars, crates).
     *
     * <p>Implementation note: walls in this codebase are in-bounds
     * GridCells with {@code passable == false}, NOT off-map nulls.
     * "Edge" therefore means: at least one cardinal neighbor lies on
     * the perimeter coordinate ring (x == 0, y == 0, x == width-1, or
     * y == height-1). This correctly catches cells whose neighbor is
     * the actual outer wall, while excluding cells next to the inner
     * 2x2 pillar at (6-7, 4-5).
     */
    private boolean isOnMapEdge(int x, int y) {
        int w = dungeonMap.getWidth();
        int h = dungeonMap.getHeight();
        int[][] neighbors = { {x, y - 1}, {x, y + 1}, {x - 1, y}, {x + 1, y} };
        for (int[] n : neighbors) {
            int nx = n[0];
            int ny = n[1];
            if (nx == 0 || ny == 0 || nx == w - 1 || ny == h - 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Uses {@link EnemyFactory#createRandomEnemy(int, int)} for the 60/30/10 split.
     */
    public String spawnEnemyProcedurally() {
        List<int[]> candidates = new ArrayList<>();
        for (int x = 0; x < dungeonMap.getWidth(); x++) {
            for (int y = 0; y < dungeonMap.getHeight(); y++) {
                GridCell c = dungeonMap.getCell(x, y);
                if (c == null || !c.isWalkable() || !c.getItemsView().isEmpty()) {
                    continue;
                }
                if (x == hero.getX() && y == hero.getY()) {
                    continue;
                }
                if (!c.getEntities().isEmpty()) {
                    continue;
                }
                if (!isOnMapEdge(x, y)) {
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
     * Kicks off enemy, coin-reward, and detection loops. Swing Timers run on
     * the EDT, so mutations here are safe w.r.t. the painter.
     */
    private void startGameTimers() {
        spawnTimer = new Timer(SPAWN_INTERVAL_MS, e -> {
            if (countEnemies() >= MAX_ENEMIES) {
                return;
            }
            String msg = spawnEnemyProcedurally();
            System.out.println("[spawn] " + msg);
        });
        spawnTimer.setRepeats(true);
        spawnTimer.start();

        coinSpawnTimer = new Timer(COIN_SPAWN_INTERVAL_MS, e -> {
            if (countGroundCoins() >= MAX_GROUND_COINS) {
                return;
            }
            if (spawnCoinPile(-1, -1)) {
                notifyListeners();
            }
        });
        coinSpawnTimer.setRepeats(true);
        coinSpawnTimer.start();

        detectionTimer = new Timer(DETECTION_TICK_MS, e -> updateEnemyDetection());
        detectionTimer.setRepeats(true);
        detectionTimer.start();

        knightActionTimer = new Timer(KNIGHT_ACTION_TICK_MS, e -> updateKnightActions());
        knightActionTimer.setRepeats(true);
        knightActionTimer.start();

        sorcererAttackTimer = new Timer(SORCERER_ATTACK_TICK_MS, e -> updateSorcererAttacks());
        sorcererAttackTimer.setRepeats(true);
        sorcererAttackTimer.start();

        sorcererTeleportTimer = new Timer(SORCERER_TELEPORT_TICK_MS, e -> updateSorcererTeleports());
        sorcererTeleportTimer.setRepeats(true);
        sorcererTeleportTimer.start();
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

    private void updateKnightActions() {
        boolean changed = false;
        for (Entity enemy : enemiesSnapshot()) {
            if (!(enemy instanceof Knight knight)) {
                continue;
            }
            if (isAdjacentToHero(knight)) {
                combatManager.knightAttacksHero(knight, hero);
                changed = true;
                continue;
            }
            if (knight.getAiState() == AIState.CHASING) {
                changed |= moveEnemyTowardHero(knight);
            } else {
                changed |= moveEnemyRandomly(knight);
            }
        }
        if (changed) {
            notifyListeners();
        }
    }

    private void updateSorcererAttacks() {
        boolean changed = false;
        for (Entity enemy : enemiesSnapshot()) {
            if (enemy instanceof Sorcerer sorcerer) {
                CombatManager.AttackResult result = combatManager.sorcererAttacksHero(sorcerer, hero);
                changed |= result.getDamageGenerated() > 0;
            }
        }
        if (changed) {
            notifyListeners();
        }
    }

    private void updateSorcererTeleports() {
        boolean changed = false;
        for (Entity enemy : enemiesSnapshot()) {
            if (enemy instanceof Sorcerer && random.nextBoolean()) {
                changed |= teleportEnemyToRandomEmptyCell(enemy);
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

    private List<Entity> enemiesSnapshot() {
        List<Entity> enemies = new ArrayList<>();
        for (int x = 0; x < dungeonMap.getWidth(); x++) {
            for (int y = 0; y < dungeonMap.getHeight(); y++) {
                GridCell cell = dungeonMap.getCell(x, y);
                if (cell == null) {
                    continue;
                }
                for (Entity entity : cell.getEntitiesView()) {
                    if (entity instanceof Knight || entity instanceof Sorcerer) {
                        enemies.add(entity);
                    }
                }
            }
        }
        return enemies;
    }

    private boolean isAdjacentToHero(Entity entity) {
        return dungeonMap.isHeroAdjacent(hero, entity.getX(), entity.getY());
    }

    private boolean moveEnemyTowardHero(Entity enemy) {
        int dx = Integer.compare(hero.getX(), enemy.getX());
        int dy = Integer.compare(hero.getY(), enemy.getY());

        if (Math.abs(hero.getX() - enemy.getX()) >= Math.abs(hero.getY() - enemy.getY())) {
            if (tryMoveEnemy(enemy, enemy.getX() + dx, enemy.getY())) {
                return true;
            }
            return tryMoveEnemy(enemy, enemy.getX(), enemy.getY() + dy);
        }
        if (tryMoveEnemy(enemy, enemy.getX(), enemy.getY() + dy)) {
            return true;
        }
        return tryMoveEnemy(enemy, enemy.getX() + dx, enemy.getY());
    }

    private boolean moveEnemyRandomly(Entity enemy) {
        Direction[] directions = Direction.values();
        Direction first = directions[random.nextInt(directions.length)];
        for (int i = 0; i < directions.length; i++) {
            Direction direction = directions[(first.ordinal() + i) % directions.length];
            int nx = enemy.getX();
            int ny = enemy.getY();
            switch (direction) {
                case UP -> ny--;
                case DOWN -> ny++;
                case LEFT -> nx--;
                case RIGHT -> nx++;
            }
            if (tryMoveEnemy(enemy, nx, ny)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryMoveEnemy(Entity enemy, int nx, int ny) {
        if (nx == enemy.getX() && ny == enemy.getY()) {
            return false;
        }
        GridCell from = dungeonMap.getCell(enemy.getX(), enemy.getY());
        GridCell to = dungeonMap.getCell(nx, ny);
        if (from == null || to == null || !to.isWalkable() || !to.getEntitiesView().isEmpty()) {
            return false;
        }
        from.getEntities().remove(enemy);
        enemy.setX(nx);
        enemy.setY(ny);
        to.getEntities().add(enemy);
        return true;
    }

    private boolean teleportEnemyToRandomEmptyCell(Entity enemy) {
        List<GridCell> candidates = new ArrayList<>();
        for (int x = 0; x < dungeonMap.getWidth(); x++) {
            for (int y = 0; y < dungeonMap.getHeight(); y++) {
                GridCell cell = dungeonMap.getCell(x, y);
                if (cell == null || !cell.isWalkable()
                        || !cell.getItemsView().isEmpty()
                        || !cell.getEntitiesView().isEmpty()) {
                    continue;
                }
                candidates.add(cell);
            }
        }
        if (candidates.isEmpty()) {
            return false;
        }
        GridCell destination = candidates.get(random.nextInt(candidates.size()));
        return tryMoveEnemy(enemy, destination.getX(), destination.getY());
    }

    /**
     * Public escape hatch for the controller layer to trigger a sorcerer
     * teleport in response to combat damage. Keep
     * teleportEnemyToRandomEmptyCell itself private — this wrapper limits
     * the controller's view of engine internals.
     */
    public boolean requestSorcererTeleport(Sorcerer sorcerer) {
        return teleportEnemyToRandomEmptyCell(sorcerer);
    }

    /** Stops timers — call this on shutdown if you wire it up later. */
    public void shutdown() {
        if (spawnTimer != null) spawnTimer.stop();
        if (coinSpawnTimer != null) coinSpawnTimer.stop();
        if (detectionTimer != null) detectionTimer.stop();
        if (knightActionTimer != null) knightActionTimer.stop();
        if (sorcererAttackTimer != null) sorcererAttackTimer.stop();
        if (sorcererTeleportTimer != null) sorcererTeleportTimer.stop();
    }
}
