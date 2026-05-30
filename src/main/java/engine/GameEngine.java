package engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import model.Chest;
import model.Coin;
import model.Container;
import model.BossEnemy;
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

import model.Ring;
import model.ValuableItem;
import model.ValuableItemCatalog;
import model.Weapon;
import model.WeaponCatalog;
import javax.swing.Timer;

import model.AIState;
import model.DragonPet;
import model.Gargoyle;
import model.Grill;
import model.HeroProjectileStyle;
import model.Knight;
import model.MissingBrick;
import model.PenguinPet;
import model.Pet;
import model.PetEntity;
import model.Projectile;
import model.SearchableObject;
import model.Sorcerer;
import model.WaterPipe;
import model.Torch;

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
    private final GameMode gameMode;
    private final Random random;
    private final EnemyFactory enemyFactory;
    private final EnemySpawnPolicy spawnPolicy;
    private final CombatManager combatManager = new CombatManager();
    private final TeamMatchAiController teamMatchAiController = new TeamMatchAiController();
    private final TeamMatchOutcomeEvaluator teamMatchOutcomeEvaluator = new TeamMatchOutcomeEvaluator();
    private final TargetItemMission targetMission = new TargetItemMission();
    private final FogOfWarEngine fogEngine = new FogOfWarEngine();
    private final List<GameStateListener> listeners = new CopyOnWriteArrayList<>();
    private final List<GameEventListener> eventListeners = new CopyOnWriteArrayList<>();
    private long lastMoveNanos = System.nanoTime();
    private boolean isPaused = false;
    private boolean isGameOver = false;
    private TeamMatchOutcome teamMatchOutcome = TeamMatchOutcome.ONGOING;

    // Tower-mode context: set by DungeonLevelFactory when a floor is started.
    private int towerLevelNumber = 0;
    private boolean finalTowerLevel = false;
    private boolean levelCompleted = false;
    private LevelCompletionListener levelCompletionListener;

    private Timer spawnTimer;
    private Timer coinSpawnTimer;
    private Timer detectionTimer;
    private Timer knightActionTimer;
    private Timer sorcererAttackTimer;
    private Timer bossAttackTimer;
    private Timer projectileTimer;
    private Timer teamMatchActionTimer;
    private Timer petTimer;
    private final List<Projectile> activeProjectiles = new ArrayList<>();
    /** Transient on-grid presence of the equipped pet for this floor; null when none. */
    private PetEntity petEntity;
    private long lastDragonAttackNanos = 0L;
    /** Enemy -&gt; nanoTime until which it is frozen (penguin ability). Transient. */
    private final Map<Entity, Long> frozenUntilNanos = new IdentityHashMap<>();

    private static final int COIN_SPAWN_INTERVAL_MS = 5000;
    private static final int DETECTION_TICK_MS = 300;
    private static final int SORCERER_SHOOT_RANGE = 5;
    private static final int BOSS_SHOOT_RANGE = 8;
    private static final int SORCERER_PROJECTILE_MANA_COST = 5;
    private static final int PROJECTILE_TICK_MS = 300;
    private static final int PET_TICK_MS = 650;
    private static final long DRAGON_ATTACK_COOLDOWN_NANOS = TimeUnit.MILLISECONDS.toNanos(450);
    private static final int KNIGHT_PET_MELEE_DAMAGE = 2;
    private static final int MIN_GROUND_COINS = 3;
    private static final int MAX_GROUND_COINS = 7;
    private static final int COIN_REWARD_VALUE = 10;
    private static final double SEARCHABLE_WALL_FILL_RATIO = 0.75;
    private static final double SEARCHABLE_HIDDEN_ITEM_CHANCE = 0.65;
    private static final double KNIGHT_VISION_RANGE = 5.0;
    private static final double SORCERER_VISION_RANGE = 5.0; // detects/chases the hero within 5 tiles
    private static final double BOSS_VISION_RANGE = 8.0;

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
        this(ThreadLocalRandom.current(), null);
    }

    GameEngine(Random random) {
        this(random, null);
    }

    /**
     * Starts play mode from a map produced by the design screen.
     */
    public GameEngine(DungeonMap designedMap) {
        this(ThreadLocalRandom.current(), designedMap);
    }

    private GameEngine(Random random, DungeonMap designedMap) {
        this.random = random;
        this.enemyFactory = new EnemyFactory(random);
        this.gameMode = GameMode.PLAY;
        this.spawnPolicy = new RegularEnemySpawnPolicy(enemyFactory);
        this.dungeonMap = designedMap == null ? buildDemoMap("Phase 1 - Build Mode") : designedMap;
        int startingStr = 8 + random.nextInt(8);  // 8..15 inclusive (spec 2.4.1)
        // Spec section 2.4.1: HP=17, STR=random[8,15], Mana=80, DEF=2.
        // Energy=100 is a project design decision (spec leaves it open).
        int[] heroStart = findHeroStart(this.dungeonMap);
        this.hero = new Hero(heroStart[0], heroStart[1], "Hero", 17, startingStr, 80, 2, 100);
        placeHeroOnMap();
        fogEngine.revealAround(dungeonMap, hero);
        fillMinimumGroundCoins(-1, -1);
        startTargetMission();
        startGameTimers();
    }

    static GameEngine createTeamMatch(DungeonMap dungeonMap, Hero hero) {
        return new GameEngine(ThreadLocalRandom.current(), dungeonMap, hero, GameMode.TEAM_MATCH);
    }

    private GameEngine(Random random, DungeonMap dungeonMap, Hero hero, GameMode gameMode) {
        if (dungeonMap == null || hero == null) {
            throw new IllegalArgumentException("Team Match requires a map and hero.");
        }
        this.random = random;
        this.enemyFactory = new EnemyFactory(random);
        this.gameMode = gameMode == null ? GameMode.PLAY : gameMode;
        this.spawnPolicy = new RegularEnemySpawnPolicy(enemyFactory);
        this.dungeonMap = dungeonMap;
        this.hero = hero;
        placeHeroOnMap();
        fogEngine.revealAround(dungeonMap, hero);
        startTeamMatchTimers();
    }

    private int[] findHeroStart(DungeonMap map) {
        if (map != null) {
            GridCell preferred = map.getCell(1, 1);
            if (preferred != null && preferred.isWalkable() && preferred.getEntitiesView().isEmpty()) {
                return new int[] { 1, 1 };
            }
            for (int y = 0; y < map.getHeight(); y++) {
                for (int x = 0; x < map.getWidth(); x++) {
                    GridCell cell = map.getCell(x, y);
                    if (cell != null && cell.isWalkable() && cell.getEntitiesView().isEmpty()) {
                        return new int[] { x, y };
                    }
                }
            }
        }
        return new int[] { 1, 1 };
    }

    public GameEngine(DungeonMap dungeonMap, Hero hero,
            ValuableItem missionTarget, boolean missionStarted, boolean missionWon) {
        this(dungeonMap, hero, missionTarget, missionStarted, missionWon, null);
    }

    /**
     * Builds a session with an explicit {@link EnemySpawnPolicy} (GoF Strategy),
     * used by {@code DungeonLevelFactory} to drive per-floor enemy cadence/mix.
     * A {@code null} policy falls back to the base-game {@link RegularEnemySpawnPolicy}.
     */
    public GameEngine(DungeonMap dungeonMap, Hero hero,
            ValuableItem missionTarget, boolean missionStarted, boolean missionWon,
            EnemySpawnPolicy spawnPolicy) {
        this.random = ThreadLocalRandom.current();
        this.enemyFactory = new EnemyFactory(random);
        this.gameMode = GameMode.PLAY;
        if (dungeonMap == null || hero == null) {
            throw new IllegalArgumentException("Loaded game requires a map and hero.");
        }
        this.dungeonMap = dungeonMap;
        this.hero = hero;
        this.spawnPolicy = spawnPolicy != null ? spawnPolicy : new RegularEnemySpawnPolicy(enemyFactory);
        placeHeroOnMap();
        fogEngine.revealAround(dungeonMap, hero);
        this.targetMission.restore(missionTarget, missionStarted, missionWon);
        startGameTimers();
    }

    /**
     * Starts a fresh tower floor: the carry-over {@code hero} on a generated
     * {@code map}, with a newly hidden target mission, ground coins, and the
     * given {@link EnemySpawnPolicy}. Pair with {@link #configureTowerLevel}.
     */
    public GameEngine(DungeonMap map, Hero hero, EnemySpawnPolicy spawnPolicy) {
        if (map == null || hero == null) {
            throw new IllegalArgumentException("A tower floor requires a map and hero.");
        }
        this.random = ThreadLocalRandom.current();
        this.enemyFactory = new EnemyFactory(random);
        this.gameMode = GameMode.PLAY;
        this.spawnPolicy = spawnPolicy != null ? spawnPolicy : new RegularEnemySpawnPolicy(enemyFactory);
        this.dungeonMap = map;
        this.hero = hero;
        placeHeroOnMap();
        fogEngine.revealAround(dungeonMap, hero);
        fillMinimumGroundCoins(-1, -1);
        startTargetMission();
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
            System.out.println("[mission] no hiding place available - mission inactive");
        }
    }

    public TargetItemMission getTargetMission() {
        return targetMission;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public TeamMatchOutcome getTeamMatchOutcome() {
        return teamMatchOutcome;
    }

    public String getGameOverTitle() {
        return gameMode == GameMode.TEAM_MATCH ? "MATCH OVER" : "DEFEAT";
    }

    public String getGameOverMessage() {
        if (gameMode != GameMode.TEAM_MATCH) {
            return "Your HP reached 0.";
        }
        return switch (teamMatchOutcome) {
            case TEAM_A_WINS -> "Red Team wins the match.";
            case TEAM_B_WINS -> "Blue Team wins the match.";
            case DRAW -> "Both teams were eliminated. The match is a draw.";
            case ONGOING -> "The match has ended.";
        };
    }

    public void addGameStateListener(GameStateListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeGameStateListener(GameStateListener listener) {
        listeners.remove(listener);
    }

    public void addGameEventListener(GameEventListener listener) {
        if (listener != null) {
            eventListeners.add(listener);
        }
    }

    public void removeGameEventListener(GameEventListener listener) {
        eventListeners.remove(listener);
    }

    private void notifyListeners() {
        for (GameStateListener listener : listeners) {
            listener.onGameStateChanged();
        }
    }

    void notifyGameStateChanged() {
        notifyListeners();
    }

    void fireHeroAttack(CombatManager.AttackResult result) {
        for (GameEventListener listener : eventListeners) {
            listener.onHeroAttack(result);
        }
    }

    void fireHeroTookDamage(CombatManager.AttackResult result) {
        for (GameEventListener listener : eventListeners) {
            listener.onHeroTookDamage(result);
        }
    }

    void fireEnemyDefeated(Entity enemy) {
        for (GameEventListener listener : eventListeners) {
            listener.onEnemyDefeated(enemy);
        }
    }

    /**
     * Marks this session as a tower floor so the engine can detect floor
     * completion (find the hidden target, then reach the exit door). Called by
     * {@code DungeonLevelFactory} after the floor map is built.
     */
    public void configureTowerLevel(int levelNumber, boolean finalLevel) {
        this.towerLevelNumber = levelNumber;
        this.finalTowerLevel = finalLevel;
        this.levelCompleted = false;
    }

    /** Registers the single subscriber notified when this floor is completed. */
    public void setLevelCompletionListener(LevelCompletionListener listener) {
        this.levelCompletionListener = listener;
    }

    private void fireLevelCompleted() {
        if (levelCompletionListener != null) {
            levelCompletionListener.onLevelCompleted(
                    new LevelCompletionResult(towerLevelNumber, finalTowerLevel));
        }
    }

    void fireItemPickedUp(Item item) {
        for (GameEventListener listener : eventListeners) {
            listener.onItemPickedUp(item);
        }
    }

    void fireHeroDefeated() {
        for (GameEventListener listener : eventListeners) {
            listener.onHeroDefeated();
        }
    }

    public boolean isPaused() {
        return isPaused;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    boolean canHeroAct() {
        return !isPaused
                && !isGameOver
                && !(gameMode == GameMode.TEAM_MATCH && hero.getHp() <= 0);
    }

    public void togglePause() {
        if (isGameOver) {
            return;
        }
        isPaused = !isPaused;
        if (isPaused) {
            pauseAllTimers();
        } else {
            resumeAllTimers();
        }
        notifyListeners();
    }

    private void triggerGameOver() {
        if (isGameOver) {
            return;
        }
        fireHeroDefeated();
        isGameOver = true;
        isPaused = true;
        pauseAllTimers();
        notifyListeners();
    }

    private void finishTeamMatch(TeamMatchOutcome outcome) {
        if (gameMode != GameMode.TEAM_MATCH || outcome == TeamMatchOutcome.ONGOING || isGameOver) {
            return;
        }
        teamMatchOutcome = outcome;
        isGameOver = true;
        isPaused = true;
        pauseAllTimers();
        notifyListeners();
    }

    boolean resolveTeamMatchOutcome() {
        if (gameMode != GameMode.TEAM_MATCH || isGameOver) {
            return false;
        }
        TeamMatchOutcome outcome = teamMatchOutcomeEvaluator.evaluate(dungeonMap);
        if (outcome == TeamMatchOutcome.ONGOING) {
            return false;
        }
        finishTeamMatch(outcome);
        return true;
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
            placeSearchable(map, spot[0], spot[1], randomSearchableObject(map, spot[1] == 0));
        }
    }

    private SearchableObject randomSearchableObject(DungeonMap map, boolean topWall) {
        Item hiddenItem = randomHiddenSearchItem(map);
        return switch (random.nextInt(20)) {
            case 0, 1, 2, 3 -> new MissingBrick(MissingBrick.SPRITE_1, hiddenItem);
            case 4, 5, 6, 7 -> new MissingBrick(MissingBrick.SPRITE_2, hiddenItem);
            case 8, 9, 10, 11 -> new Gargoyle(randomDripSprite(topWall), hiddenItem);
            case 12, 13, 14 -> new Grill(Grill.HORIZONTAL_SPRITE, hiddenItem);
            case 15, 16, 17 -> new Grill(Grill.VERTICAL_SPRITE, hiddenItem);
            default -> new WaterPipe(WaterPipe.LARGE_RING_SPRITE, hiddenItem);
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

    private Item randomHiddenSearchItem(DungeonMap map) {
        if (random.nextDouble() >= SEARCHABLE_HIDDEN_ITEM_CHANCE) {
            return null;
        }
        return switch (random.nextInt(7)) {
            case 0 -> new HealPotion();
            case 1 -> new ManaPotion();
            case 2 -> new EnergyPotion();
            case 3 -> new Key("silver", KeyColor.SILVER);
            case 4 -> new Ring("Hidden Ring", 1);
            case 5 -> map != null && map.isFogEnabled() ? new Torch() : new HealPotion();
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
     * 8-adjacent cell, locked or not. Caller decides whether to unlock
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
        if (item instanceof ValuableItem) {
            container.removeItem(item);
            acceptValuable(item);
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
        fireItemPickedUp(item);
        fogEngine.revealAround(dungeonMap, hero);
        notifyListeners();
        return true;
    }

    /**
     * Routes a collected valuable straight into the persistent
     * {@link model.FullGameInventory} (it survives between floors) rather than
     * the per-level bag, then runs the usual pickup notifications.
     */
    private void acceptValuable(Item valuable) {
        hero.getFullInventory().add(valuable);
        targetMission.checkPickup(valuable);
        fireItemPickedUp(valuable);
        notifyListeners();
    }

    public SearchResult search(SearchableObject object) {
        if (object == null) {
            return SearchResult.notSearchable();
        }
        Item hidden = object.getHiddenItem();
        if (hidden == null) {
            return SearchResult.nothingFound();
        }
        if (hidden instanceof ValuableItem) {
            Item found = object.takeHiddenItem();
            acceptValuable(found);
            return SearchResult.found(found);
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
        fireItemPickedUp(found);
        fogEngine.revealAround(dungeonMap, hero);
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

    public FogOfWarEngine getFogEngine() {
        return fogEngine;
    }

    public List<Projectile> getActiveProjectilesView() {
        return Collections.unmodifiableList(new ArrayList<>(activeProjectiles));
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
        if (!canHeroAct()) {
            return;
        }
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
        fogEngine.revealAround(dungeonMap, hero);
        notifyListeners();
        checkTowerExit(to);
    }

    /**
     * Tower completion: stepping through the open exit arch after collecting the
     * floor's hidden target finishes the floor. The arch only opens once the
     * target is found (see {@link #openArch}), so reaching it implies victory.
     */
    private void checkTowerExit(GridCell cell) {
        if (towerLevelNumber <= 0 || levelCompleted || cell == null || !targetMission.isWon()) {
            return;
        }
        boolean throughOpenArch = cell.getItemsView().stream()
                .anyMatch(item -> item instanceof model.Arch arch && arch.isOpen());
        if (throughOpenArch) {
            levelCompleted = true;
            fireLevelCompleted();
        }
    }

    /**
     * Finds a closed exit arch in the hero's cell or an adjacent one, or null if
     * none is in reach. Used by the {@code O} (open) action.
     */
    public model.Arch findArchNearHero() {
        int hx = hero.getX();
        int hy = hero.getY();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                GridCell cell = dungeonMap.getCell(hx + dx, hy + dy);
                if (cell == null) {
                    continue;
                }
                for (Item item : cell.getItems()) {
                    if (item instanceof model.Arch arch && !arch.isOpen()) {
                        return arch;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Opens the exit arch. Callers gate this on the unlock requirements
     * (gold key found and treasure collected); see {@code GamePanel}.
     *
     * @return true if the arch transitioned from closed to open.
     */
    public boolean openArch(model.Arch arch) {
        if (arch == null || arch.isOpen()) {
            return false;
        }
        arch.open();
        notifyListeners();
        return true;
    }

    /** True if the hero is carrying a gold key (the arch's unlock requirement). */
    public boolean heroHasGoldKey() {
        for (Item item : hero.getInventory().getItems()) {
            if (item instanceof model.Key key && key.getColor() == model.KeyColor.GOLD) {
                return true;
            }
        }
        return false;
    }

    /**
     * Refills a small amount of energy if the hero has been idle for at least 1s.
     * Safe to call on every UI tick; no-ops when still within the idle window or
     * when energy is already full.
     */
    public void tickEnergyRefill() {
        if (isPaused || isGameOver) {
            return;
        }
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
            fogEngine.revealAround(dungeonMap, hero);
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
                    boolean canCollect = item instanceof Coin || item instanceof ValuableItem
                            || hero.getInventory().hasFreeSlot();
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
     * @return {@code true}  - item moved to inventory and map updated;<br>
     *         {@code false} - rejected (item not takable or inventory full).
     */
    public boolean takeItem(model.Item item, int x, int y) {
        if (item == null || !item.isTakable()) {
            return false;
        }
        if (item instanceof Coin coin) {
            return collectCoin(coin, x, y);
        }
        if (item instanceof ValuableItem) {
            if (!dungeonMap.removeItemFromCell(item, x, y)) {
                return false;
            }
            acceptValuable(item);
            return true;
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
        fireItemPickedUp(item);
        fogEngine.revealAround(dungeonMap, hero);
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
     * area only, not next to interior obstacles (pillars, crates).
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
        Entity enemy = spawnPolicy.createEnemy(pick[0], pick[1]);
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
        spawnTimer = new Timer(spawnPolicy.spawnIntervalMs(), e -> {
            if (countEnemies() >= spawnPolicy.maxEnemies()) {
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

        knightActionTimer = new Timer(GameConstants.GLOBAL_ACTION_TICK_MS, e -> {
            updateKnightMeleeActions();
            updateEnemyMovement();
        });
        knightActionTimer.setRepeats(true);
        knightActionTimer.start();

        sorcererAttackTimer = new Timer(GameConstants.GLOBAL_ACTION_TICK_MS, e -> updateSorcererAttacks());
        sorcererAttackTimer.setRepeats(true);
        sorcererAttackTimer.start();

        bossAttackTimer = new Timer(GameConstants.GLOBAL_ACTION_TICK_MS, e -> updateBossAttacks());
        bossAttackTimer.setRepeats(true);
        bossAttackTimer.start();

        projectileTimer = new Timer(PROJECTILE_TICK_MS, e -> updateProjectiles());
        projectileTimer.setRepeats(true);
        projectileTimer.start();

        spawnEquippedPet();
        petTimer = new Timer(PET_TICK_MS, e -> updatePet());
        petTimer.setRepeats(true);
        petTimer.start();
    }

    /** Counts all Knight/Sorcerer entities currently on the map. */
    private int countEnemies() {
        int count = 0;
        for (int x = 0; x < dungeonMap.getWidth(); x++) {
            for (int y = 0; y < dungeonMap.getHeight(); y++) {
                GridCell c = dungeonMap.getCell(x, y);
                if (c == null) continue;
                for (Entity e : c.getEntities()) {
                    if (e instanceof Knight || e instanceof Sorcerer || e instanceof BossEnemy) {
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
        if (isPaused || isGameOver) {
            return;
        }
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

    private void updateKnightMeleeActions() {
        if (isPaused || isGameOver) {
            return;
        }
        boolean changed = false;
        for (Entity enemy : enemiesSnapshot()) {
            if (!(enemy instanceof Knight knight) || isFrozen(knight)) {
                continue;
            }
            // A knight beside the pet strikes it instead of (or as well as) the hero.
            if (petEntity != null && isAdjacentTo(knight, petEntity)) {
                applyPetDamage(KNIGHT_PET_MELEE_DAMAGE);
                changed = true;
            }
            if (!isAdjacentToHero(knight)) {
                continue;
            }
            CombatManager.AttackResult result = combatManager.knightAttacksHero(knight, hero);
            if (result.damageReceived > 0) {
                fireHeroTookDamage(result);
            }
            if (hero.getHp() <= 0) {
                triggerGameOver();
                return;
            }
            changed = true;
        }
        if (changed) {
            notifyListeners();
        }
    }

    private static boolean isAdjacentTo(Entity a, Entity b) {
        return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY())) <= 1
                && !(a.getX() == b.getX() && a.getY() == b.getY());
    }

    /**
     * One tile per tick for Knights and Sorcerers with identical pathing rules ({@link #moveEnemyTowardHero}).
     */
    private void updateEnemyMovement() {
        if (isPaused || isGameOver) {
            return;
        }
        boolean changed = false;
        for (Entity enemy : enemiesSnapshot()) {
            if (isFrozen(enemy)) {
                continue;
            }
            if (enemy instanceof Knight knight) {
                if (isAdjacentToHero(knight)) {
                    continue;
                }
                changed |= applyEnemyWalkStep(knight);
            } else if (enemy instanceof Sorcerer sorcerer) {
                if (canSorcererShootAtHero(sorcerer)) {
                    continue;
                }
                changed |= applyEnemyWalkStep(sorcerer);
            } else if (enemy instanceof BossEnemy boss) {
                if (canBossShootAtHero(boss)) {
                    continue;
                }
                changed |= applyEnemyWalkStep(boss);
            }
        }
        if (changed) {
            notifyListeners();
        }
    }

    private boolean applyEnemyWalkStep(Entity enemy) {
        if (getEnemyAiState(enemy) == AIState.CHASING) {
            return moveEnemyTowardHero(enemy);
        }
        return moveEnemyRandomly(enemy);
    }

    private static AIState getEnemyAiState(Entity enemy) {
        if (enemy instanceof Knight knight) {
            return knight.getAiState();
        }
        if (enemy instanceof Sorcerer sorcerer) {
            return sorcerer.getAiState();
        }
        if (enemy instanceof BossEnemy boss) {
            return boss.getAiState();
        }
        return AIState.ROAMING;
    }

    private void updateSorcererAttacks() {
        if (isPaused || isGameOver) {
            return;
        }
        boolean changed = false;
        for (Entity enemy : enemiesSnapshot()) {
            if (!(enemy instanceof Sorcerer sorcerer) || isFrozen(sorcerer)) {
                continue;
            }
            if (!canSorcererShootAtHero(sorcerer)) {
                continue;
            }
            CombatManager.SorcererProjectilePrep prep = combatManager.prepareSorcererProjectile(sorcerer, hero);
            if (prep != null) {
                spawnProjectile(sorcerer.getX(), sorcerer.getY(), hero.getX(), hero.getY(),
                        prep.damageGenerated, prep.damageReceived, false);
                changed = true;
            }
        }
        if (changed) {
            notifyListeners();
        }
    }

    private void updateBossAttacks() {
        if (isPaused || isGameOver) {
            return;
        }
        boolean changed = false;
        for (Entity enemy : enemiesSnapshot()) {
            if (!(enemy instanceof BossEnemy boss) || isFrozen(boss)) {
                continue;
            }
            if (!canBossShootAtHero(boss)) {
                continue;
            }
            CombatManager.SorcererProjectilePrep prep = combatManager.prepareBossProjectile(boss, hero);
            if (prep != null) {
                spawnProjectile(boss.getX(), boss.getY(), hero.getX(), hero.getY(),
                        prep.damageGenerated, prep.damageReceived, false, true);
                changed = true;
            }
        }
        if (changed) {
            notifyListeners();
        }
    }

    /**
     * True when the hero lies on a clear straight ray within range and the sorcerer has mana to cast.
     */
    private boolean canSorcererShootAtHero(Sorcerer sorcerer) {
        if (sorcerer.getAiState() != AIState.CHASING) {
            return false;
        }
        if (sorcerer.getMana() < SORCERER_PROJECTILE_MANA_COST) {
            return false;
        }
        int sx = sorcerer.getX();
        int sy = sorcerer.getY();
        int hx = hero.getX();
        int hy = hero.getY();
        if (sx == hx && sy == hy) {
            return true;
        }
        if (!heroOnStraightRayFrom(sx, sy, hx, hy)) {
            return false;
        }
        return hasClearProjectilePath(sx, sy, hx, hy, SORCERER_SHOOT_RANGE);
    }

    private boolean canBossShootAtHero(BossEnemy boss) {
        if (boss.getAiState() != AIState.CHASING || boss.getMana() < 8) {
            return false;
        }
        int sx = boss.getX();
        int sy = boss.getY();
        int hx = hero.getX();
        int hy = hero.getY();
        if (sx == hx && sy == hy) {
            return true;
        }
        if (!heroOnStraightRayFrom(sx, sy, hx, hy)) {
            return false;
        }
        return hasClearProjectilePath(sx, sy, hx, hy, BOSS_SHOOT_RANGE);
    }

    private static boolean heroOnStraightRayFrom(int sx, int sy, int hx, int hy) {
        if (hx == sx && hy == sy) {
            return true;
        }
        if (hx == sx || hy == sy) {
            return true;
        }
        return Math.abs(hx - sx) == Math.abs(hy - sy);
    }

    /**
     * Hero ranged attack: spawns a hero-owned projectile along a straight line when in range
     * with line of sight; melee range rules do not apply.
     */
    public CombatManager.AttackResult launchHeroRangedAttackAt(int targetX, int targetY) {
        if (!canHeroAct()) {
            return null;
        }
        if (isHeroAttackOnCooldown()) {
            return null;
        }
        GridCell cell = dungeonMap.getCell(targetX, targetY);
        if (cell == null) {
            return null;
        }
        Entity target = firstHostileInCell(cell);
        if (target == null || !canHeroRangedTarget(targetX, targetY)) {
            return null;
        }

        CombatManager.HeroProjectilePrep prep = combatManager.prepareHeroRangedProjectile(hero, target);
        if (prep == null) {
            return null;
        }

        int hx = hero.getX();
        int hy = hero.getY();
        if (hx == targetX && hy == targetY) {
            CombatManager.AttackResult result = combatManager.applyHeroProjectileHit(target, prep);
            recordHeroAttackPacing();
            fireHeroAttack(result);
            if (result.isDefenderDefeated()) {
                cell.getEntities().remove(target);
                fireEnemyDefeated(target);
            }
            notifyListeners();
            return result;
        }

        spawnProjectile(hx, hy, targetX, targetY, prep.damageGenerated, prep.damageReceived, true,
                prep.projectileStyle);
        recordHeroAttackPacing();
        notifyListeners();
        return new CombatManager.AttackResult(
                prep.damageGenerated, prep.damageReceived, getHostileHp(target), false);
    }

    /** @return true when the hero must wait before the next attack input is accepted */
    public boolean isHeroAttackOnCooldown() {
        if (hero == null) {
            return true;
        }
        return System.currentTimeMillis() - hero.getLastAttackTimeMs() < GameConstants.GLOBAL_ACTION_TICK_MS;
    }

    /** Marks the start of a hero attack for global pacing parity with enemies. */
    public void recordHeroAttackPacing() {
        if (hero != null) {
            hero.setLastAttackTimeMs(System.currentTimeMillis());
        }
    }

    private static int getHostileHp(Entity target) {
        if (target instanceof Knight knight) {
            return knight.getHp();
        }
        if (target instanceof Sorcerer sorcerer) {
            return sorcerer.getHp();
        }
        if (target instanceof BossEnemy boss) {
            return boss.getHp();
        }
        return 0;
    }

    public boolean canHeroShootAt(int targetX, int targetY) {
        return canHeroRangedTarget(targetX, targetY);
    }

    private boolean canHeroRangedTarget(int targetX, int targetY) {
        Weapon weapon = hero.getEquippedWeapon();
        if (weapon == null || !weapon.isRanged()) {
            return false;
        }
        GridCell cell = dungeonMap.getCell(targetX, targetY);
        if (cell == null || firstHostileInCell(cell) == null) {
            return false;
        }
        int hx = hero.getX();
        int hy = hero.getY();
        if (hx == targetX && hy == targetY) {
            return true;
        }
        if (!heroOnStraightRayFrom(hx, hy, targetX, targetY)) {
            return false;
        }
        // Test mode: keep straight-line and obstacle checks, but ignore weapon range.
        return hasClearProjectilePath(hx, hy, targetX, targetY, Integer.MAX_VALUE);
    }

    private boolean hasClearProjectilePath(int sx, int sy, int hx, int hy, int maxRange) {
        int dx = Integer.signum(hx - sx);
        int dy = Integer.signum(hy - sy);
        int cx = sx + dx;
        int cy = sy + dy;
        int steps = 0;
        while (cx != hx || cy != hy) {
            if (dungeonMap.getCell(cx, cy) == null) {
                return false;
            }
            if (blocksProjectile(cx, cy)) {
                return false;
            }
            steps++;
            if (steps > maxRange) {
                return false;
            }
            cx += dx;
            cy += dy;
        }
        return steps <= maxRange;
    }

    private void spawnProjectile(int startX, int startY, int targetX, int targetY,
            int damageGenerated, int damageReceived, boolean heroOwned) {
        spawnProjectile(startX, startY, targetX, targetY, damageGenerated, damageReceived, heroOwned, false, null);
    }

    private void spawnProjectile(int startX, int startY, int targetX, int targetY,
            int damageGenerated, int damageReceived, boolean heroOwned, HeroProjectileStyle heroStyle) {
        spawnProjectile(startX, startY, targetX, targetY, damageGenerated, damageReceived, heroOwned, false,
                heroStyle);
    }

    private void spawnProjectile(int startX, int startY, int targetX, int targetY,
            int damageGenerated, int damageReceived, boolean heroOwned, boolean bossOwned) {
        spawnProjectile(startX, startY, targetX, targetY, damageGenerated, damageReceived, heroOwned, bossOwned, null);
    }

    private void spawnProjectile(int startX, int startY, int targetX, int targetY,
            int damageGenerated, int damageReceived, boolean heroOwned, boolean bossOwned,
            HeroProjectileStyle heroStyle) {
        if (startX == targetX && startY == targetY) {
            if (heroOwned) {
                GridCell cell = dungeonMap.getCell(targetX, targetY);
                Entity target = cell == null ? null : firstHostileInCell(cell);
                if (target != null) {
                    CombatManager.HeroProjectilePrep prep = new CombatManager.HeroProjectilePrep(
                            damageGenerated, damageReceived, heroStyle);
                    resolveHeroProjectileHit(target, prep, cell);
                }
            } else {
                resolveEnemyProjectileHitOnHero(damageReceived);
            }
            return;
        }
        int dx = Integer.signum(targetX - startX);
        int dy = Integer.signum(targetY - startY);
        if (dx == 0 && dy == 0) {
            return;
        }
        activeProjectiles.add(new Projectile(startX, startY, dx, dy,
                damageGenerated, damageReceived, heroOwned, bossOwned, heroStyle));
    }

    private void updateProjectiles() {
        if (isPaused || isGameOver || activeProjectiles.isEmpty()) {
            return;
        }
        boolean changed = false;
        Iterator<Projectile> iterator = activeProjectiles.iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            if (!projectile.isActive()) {
                iterator.remove();
                continue;
            }

            if (projectile.isHeroOwned()) {
                if (advanceHeroProjectile(projectile)) {
                    changed = true;
                    if (isGameOver) {
                        return;
                    }
                }
            } else if (advanceEnemyProjectile(projectile)) {
                changed = true;
                if (isGameOver) {
                    return;
                }
            }

            if (!projectile.isActive()) {
                iterator.remove();
            }
        }
        if (changed) {
            notifyListeners();
        }
    }

    /** @return true when the projectile moved or hit something */
    private boolean advanceHeroProjectile(Projectile projectile) {
        GridCell currentCell = dungeonMap.getCell(projectile.getX(), projectile.getY());
        Entity currentTarget = firstHostileInCell(currentCell);
        if (currentTarget != null) {
            CombatManager.HeroProjectilePrep prep = new CombatManager.HeroProjectilePrep(
                    projectile.getDamageGenerated(), projectile.getDamageReceived(), projectile.getHeroStyle());
            resolveHeroProjectileHit(currentTarget, prep, currentCell);
            projectile.setActive(false);
            return true;
        }

        int nextX = projectile.getX() + projectile.getDx();
        int nextY = projectile.getY() + projectile.getDy();

        if (dungeonMap.getCell(nextX, nextY) == null) {
            projectile.setActive(false);
            return true;
        }

        if (blocksProjectile(nextX, nextY)) {
            projectile.setActive(false);
            return true;
        }

        GridCell cell = dungeonMap.getCell(nextX, nextY);
        Entity target = firstHostileInCell(cell);
        if (target != null) {
            CombatManager.HeroProjectilePrep prep = new CombatManager.HeroProjectilePrep(
                    projectile.getDamageGenerated(), projectile.getDamageReceived(), projectile.getHeroStyle());
            resolveHeroProjectileHit(target, prep, cell);
            projectile.setActive(false);
            return true;
        }

        projectile.setX(nextX);
        projectile.setY(nextY);
        return true;
    }

    /** @return true when the projectile moved or hit something */
    private boolean advanceEnemyProjectile(Projectile projectile) {
        if (projectile.getX() == hero.getX() && projectile.getY() == hero.getY()) {
            resolveEnemyProjectileHitOnHero(projectile.getDamageReceived());
            projectile.setActive(false);
            return true;
        }
        if (hitsPet(projectile.getX(), projectile.getY())) {
            applyPetDamage(projectile.getDamageReceived());
            projectile.setActive(false);
            return true;
        }

        int nextX = projectile.getX() + projectile.getDx();
        int nextY = projectile.getY() + projectile.getDy();

        if (dungeonMap.getCell(nextX, nextY) == null) {
            projectile.setActive(false);
            return true;
        }

        if (nextX == hero.getX() && nextY == hero.getY()) {
            resolveEnemyProjectileHitOnHero(projectile.getDamageReceived());
            projectile.setActive(false);
            return true;
        }
        if (hitsPet(nextX, nextY)) {
            applyPetDamage(projectile.getDamageReceived());
            projectile.setActive(false);
            return true;
        }

        if (blocksProjectile(nextX, nextY)) {
            projectile.setActive(false);
            return true;
        }

        projectile.setX(nextX);
        projectile.setY(nextY);
        return true;
    }

    private void resolveHeroProjectileHit(Entity target, CombatManager.HeroProjectilePrep prep, GridCell cell) {
        CombatManager.AttackResult result = combatManager.applyHeroProjectileHit(target, prep);
        fireHeroAttack(result);
        if (result.isDefenderDefeated() && cell != null) {
            cell.getEntities().remove(target);
            fireEnemyDefeated(target);
        }
        if (resolveTeamMatchOutcome()) {
            return;
        }
        notifyListeners();
    }

    private void resolveEnemyProjectileHitOnHero(int damageReceived) {
        combatManager.applyProjectileImpact(hero, damageReceived);
        CombatManager.AttackResult result = new CombatManager.AttackResult(
                0, damageReceived, hero.getHp(), hero.getHp() <= 0);
        if (result.damageReceived > 0) {
            fireHeroTookDamage(result);
        }
        if (hero.getHp() <= 0) {
            if (gameMode == GameMode.TEAM_MATCH) {
                removeEntityFromMap(hero);
                resolveTeamMatchOutcome();
                return;
            }
            triggerGameOver();
        }
    }

    Entity firstHostileInCell(GridCell cell) {
        if (cell == null) {
            return null;
        }
        for (Entity entity : cell.getEntities()) {
            if (isHostileToHero(entity)) {
                return entity;
            }
        }
        return null;
    }

    private boolean removeEntityFromMap(Entity entity) {
        if (entity == null) {
            return false;
        }
        GridCell current = dungeonMap.getCell(entity.getX(), entity.getY());
        if (current != null && current.getEntities().remove(entity)) {
            return true;
        }
        for (int y = 0; y < dungeonMap.getHeight(); y++) {
            for (int x = 0; x < dungeonMap.getWidth(); x++) {
                GridCell cell = dungeonMap.getCell(x, y);
                if (cell != null && cell.getEntities().remove(entity)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean isHostileToHero(Entity entity) {
        if (!(entity instanceof Knight || entity instanceof Sorcerer || entity instanceof BossEnemy)) {
            return false;
        }
        if (hero.getTeam() == model.Team.NONE) {
            return true;
        }
        return entity.getTeam() != hero.getTeam();
    }

    /** Walls and blocking fixtures (columns, crates, chests, etc.) stop projectiles. */
    private boolean blocksProjectile(int x, int y) {
        GridCell cell = dungeonMap.getCell(x, y);
        if (cell == null || !cell.isPassable()) {
            return true;
        }
        for (Item item : cell.getItemsView()) {
            if (item.isBlocking()) {
                return true;
            }
        }
        return false;
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
        } else if (e instanceof BossEnemy boss) {
            next = dist <= BOSS_VISION_RANGE ? AIState.CHASING : AIState.ROAMING;
            current = boss.getAiState();
            label = "Boss";
            if (next != current) {
                boss.setAiState(next);
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
                    if (entity instanceof Knight || entity instanceof Sorcerer || entity instanceof BossEnemy) {
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

    private void startTeamMatchTimers() {
        teamMatchActionTimer = new Timer(GameConstants.GLOBAL_ACTION_TICK_MS, e -> updateTeamMatchAi());
        teamMatchActionTimer.setRepeats(true);
        teamMatchActionTimer.start();
    }

    private void updateTeamMatchAi() {
        if (isPaused || isGameOver) {
            return;
        }
        TeamMatchAiResult result = teamMatchAiController.update(dungeonMap, hero);
        if (result.outcome() != TeamMatchOutcome.ONGOING) {
            finishTeamMatch(result.outcome());
            return;
        }
        if (result.changed()) {
          notifyListeners();
        }
    }
    // ---------------------------------------------------------------------
    // Pets: spawn, roam beside the hero, and use abilities each pet tick.
    // ---------------------------------------------------------------------

    /** True while {@code enemy} is under a penguin freeze. */
    private boolean isFrozen(Entity enemy) {
        Long until = frozenUntilNanos.get(enemy);
        return until != null && System.nanoTime() < until;
    }

    /** View-facing freeze check so the renderer can show feedback. */
    public boolean isEnemyFrozen(Entity enemy) {
        return isFrozen(enemy);
    }

    /**
     * Places the hero's equipped pet on a free tile beside the hero for this
     * floor, restoring it to full vitals first (a fresh floor revives a fainted
     * companion). No-op when no pet is equipped or no adjacent tile is free.
     */
    private void spawnEquippedPet() {
        petEntity = null;
        Pet pet = hero.getEquippedPet();
        if (pet == null) {
            return;
        }
        pet.revive();
        int[] spot = freeTileNextTo(hero.getX(), hero.getY());
        if (spot == null) {
            return;
        }
        petEntity = new PetEntity(pet, spot[0], spot[1]);
        GridCell cell = dungeonMap.getCell(spot[0], spot[1]);
        if (cell != null) {
            cell.getEntities().add(petEntity);
        }
    }

    private int[] freeTileNextTo(int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                int nx = x + dx;
                int ny = y + dy;
                GridCell cell = dungeonMap.getCell(nx, ny);
                if (cell != null && cell.isWalkable() && cell.getEntitiesView().isEmpty()) {
                    return new int[] { nx, ny };
                }
            }
        }
        return null;
    }

    /** Per-tick pet update: roam beside the hero, then use the pet's ability. */
    private void updatePet() {
        if (isPaused || isGameOver) {
            return;
        }
        Pet pet = hero.getEquippedPet();
        if (pet == null || petEntity == null || !pet.isAlive()) {
            return;
        }
        boolean changed;
        if (pet instanceof PenguinPet) {
            changed = movePenguinTowardEnemy();
            if (!changed) {
                changed = movePetTowardHero();
            }
            changed |= penguinFreezeTouchedEnemies();
        } else if (pet instanceof DragonPet dragon) {
            changed = movePetTowardHero();
            changed |= dragonRangedAttack(dragon);
        } else {
            changed = movePetTowardHero();
        }
        if (changed) {
            notifyListeners();
        }
    }

    private boolean movePenguinTowardEnemy() {
        Entity target = nearestEnemyNearPet();
        if (target == null) {
            return false;
        }
        if (chebyshevDistance(petEntity.getX(), petEntity.getY(), target.getX(), target.getY()) <= 1) {
            return false;
        }
        int dx = Integer.compare(target.getX(), petEntity.getX());
        int dy = Integer.compare(target.getY(), petEntity.getY());
        if (Math.abs(target.getX() - petEntity.getX()) >= Math.abs(target.getY() - petEntity.getY())) {
            if (tryMovePetNearHero(petEntity.getX() + dx, petEntity.getY())) {
                return true;
            }
            return tryMovePetNearHero(petEntity.getX(), petEntity.getY() + dy);
        }
        if (tryMovePetNearHero(petEntity.getX(), petEntity.getY() + dy)) {
            return true;
        }
        return tryMovePetNearHero(petEntity.getX() + dx, petEntity.getY());
    }

    private Entity nearestEnemyNearPet() {
        Pet pet = petEntity.getPet();
        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity enemy : enemiesSnapshot()) {
            if (chebyshevDistance(enemy.getX(), enemy.getY(), hero.getX(), hero.getY()) > pet.getFollowRange()) {
                continue;
            }
            int dx = enemy.getX() - petEntity.getX();
            int dy = enemy.getY() - petEntity.getY();
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < bestDist) {
                bestDist = dist;
                best = enemy;
            }
        }
        return best;
    }

    /** Lets the pet wander freely near the hero, chasing back when it trails too far. */
    private boolean movePetTowardHero() {
        Pet pet = petEntity.getPet();
        int gap = chebyshevDistance(petEntity.getX(), petEntity.getY(), hero.getX(), hero.getY());
        if (gap > Math.max(1, pet.getFollowRange() - 1)) {
            return movePetTowardHeroDirectly();
        }

        Direction[] directions = Direction.values();
        int first = random.nextInt(directions.length);
        for (int i = 0; i < directions.length; i++) {
            Direction direction = directions[(first + i) % directions.length];
            int nx = petEntity.getX();
            int ny = petEntity.getY();
            switch (direction) {
                case UP -> ny--;
                case DOWN -> ny++;
                case LEFT -> nx--;
                case RIGHT -> nx++;
            }
            if (chebyshevDistance(nx, ny, hero.getX(), hero.getY()) <= pet.getFollowRange()
                    && tryMovePet(nx, ny)) {
                return true;
            }
        }
        return false;
    }

    private boolean movePetTowardHeroDirectly() {
        int gapX = Math.abs(petEntity.getX() - hero.getX());
        int gapY = Math.abs(petEntity.getY() - hero.getY());
        int dx = Integer.compare(hero.getX(), petEntity.getX());
        int dy = Integer.compare(hero.getY(), petEntity.getY());
        if (gapX >= gapY) {
            if (tryMovePet(petEntity.getX() + dx, petEntity.getY())) {
                return true;
            }
            return tryMovePet(petEntity.getX(), petEntity.getY() + dy);
        }
        if (tryMovePet(petEntity.getX(), petEntity.getY() + dy)) {
            return true;
        }
        return tryMovePet(petEntity.getX() + dx, petEntity.getY());
    }

    private static int chebyshevDistance(int ax, int ay, int bx, int by) {
        return Math.max(Math.abs(ax - bx), Math.abs(ay - by));
    }

    private boolean tryMovePet(int nx, int ny) {
        GridCell to = dungeonMap.getCell(nx, ny);
        if (to == null || !to.isWalkable() || !to.getEntitiesView().isEmpty()) {
            return false;
        }
        GridCell from = dungeonMap.getCell(petEntity.getX(), petEntity.getY());
        if (from != null) {
            from.getEntities().remove(petEntity);
        }
        petEntity.setX(nx);
        petEntity.setY(ny);
        to.getEntities().add(petEntity);
        return true;
    }

    private boolean tryMovePetNearHero(int nx, int ny) {
        if (chebyshevDistance(nx, ny, hero.getX(), hero.getY()) > petEntity.getPet().getFollowRange()) {
            return false;
        }
        return tryMovePet(nx, ny);
    }

    /** Penguin: freezes every enemy currently touching the pet (8-adjacent). */
    private boolean penguinFreezeTouchedEnemies() {
        long until = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(PenguinPet.FREEZE_DURATION_MS);
        boolean any = false;
        for (Entity enemy : enemiesSnapshot()) {
            int gap = Math.max(Math.abs(enemy.getX() - petEntity.getX()),
                    Math.abs(enemy.getY() - petEntity.getY()));
            if (gap <= 1) {
                frozenUntilNanos.put(enemy, until);
                any = true;
            }
        }
        return any;
    }

    /** Dragon: fires at the nearest enemy on a clear straight ray within range. */
    private boolean dragonRangedAttack(DragonPet dragon) {
        long now = System.nanoTime();
        if (now - lastDragonAttackNanos < DRAGON_ATTACK_COOLDOWN_NANOS) {
            return false;
        }
        Entity target = nearestEnemyInClearRange(petEntity.getX(), petEntity.getY(), dragon.getAttackRange());
        if (target == null) {
            return false;
        }
        spawnProjectile(petEntity.getX(), petEntity.getY(), target.getX(), target.getY(),
                dragon.getAttackDamage(), dragon.getAttackDamage(), true);
        lastDragonAttackNanos = now;
        return true;
    }

    private Entity nearestEnemyInClearRange(int sx, int sy, int range) {
        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity enemy : enemiesSnapshot()) {
            int dx = enemy.getX() - sx;
            int dy = enemy.getY() - sy;
            if (dx == 0 && dy == 0) {
                continue;
            }
            if (Math.max(Math.abs(dx), Math.abs(dy)) > range) {
                continue;
            }
            if (!heroOnStraightRayFrom(sx, sy, enemy.getX(), enemy.getY())) {
                continue;
            }
            if (!hasClearProjectilePath(sx, sy, enemy.getX(), enemy.getY(), range)) {
                continue;
            }
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < bestDist) {
                bestDist = dist;
                best = enemy;
            }
        }
        return best;
    }

    private boolean hitsPet(int x, int y) {
        return petEntity != null && petEntity.getX() == x && petEntity.getY() == y;
    }

    /** The pets the hero owns (from the persistent inventory). */
    public List<Pet> ownedPets() {
        List<Pet> pets = new ArrayList<>();
        for (Item item : hero.getFullInventory().getItems()) {
            if (item instanceof Pet pet) {
                pets.add(pet);
            }
        }
        return pets;
    }

    /**
     * Equips {@code pet} as the active companion and spawns it beside the hero
     * immediately (so changing pets mid-floor takes effect at once).
     *
     * @return false when the pet is not owned.
     */
    public boolean equipPet(Pet pet) {
        if (pet == null || !hero.getFullInventory().getItems().contains(pet)) {
            return false;
        }
        Pet current = hero.getEquippedPet();
        if (current != null && current != pet) {
            current.setState(model.PetState.UNEQUIPPED);
        }
        hero.setEquippedPet(pet);
        despawnPet();
        lastDragonAttackNanos = 0L;
        spawnEquippedPet();
        notifyListeners();
        return true;
    }

    /** Clears the active companion and removes it from the floor. */
    public void unequipPet() {
        Pet current = hero.getEquippedPet();
        if (current != null) {
            current.setState(model.PetState.UNEQUIPPED);
        }
        hero.setEquippedPet(null);
        lastDragonAttackNanos = 0L;
        despawnPet();
        notifyListeners();
    }

    private void despawnPet() {
        if (petEntity == null) {
            return;
        }
        GridCell cell = dungeonMap.getCell(petEntity.getX(), petEntity.getY());
        if (cell != null) {
            cell.getEntities().remove(petEntity);
        }
        petEntity = null;
    }

    /** Applies damage to the equipped pet; removes its on-grid presence when it faints. */
    private void applyPetDamage(int amount) {
        Pet pet = hero.getEquippedPet();
        if (pet == null || petEntity == null || !pet.isAlive()) {
            return;
        }
        pet.takeDamage(amount);
        if (!pet.isAlive()) {
            GridCell cell = dungeonMap.getCell(petEntity.getX(), petEntity.getY());
            if (cell != null) {
                cell.getEntities().remove(petEntity);
            }
            petEntity = null;
        }
    }

    /** Stops timers; call this on shutdown if you wire it up later. */
    public void shutdown() {
        if (spawnTimer != null) spawnTimer.stop();
        if (coinSpawnTimer != null) coinSpawnTimer.stop();
        if (detectionTimer != null) detectionTimer.stop();
        if (knightActionTimer != null) knightActionTimer.stop();
        if (sorcererAttackTimer != null) sorcererAttackTimer.stop();
        if (bossAttackTimer != null) bossAttackTimer.stop();
        if (projectileTimer != null) projectileTimer.stop();
        if (teamMatchActionTimer != null) teamMatchActionTimer.stop();
        if (petTimer != null) petTimer.stop();
    }

    private void pauseAllTimers() {
        if (spawnTimer != null) spawnTimer.stop();
        if (coinSpawnTimer != null) coinSpawnTimer.stop();
        if (detectionTimer != null) detectionTimer.stop();
        if (knightActionTimer != null) knightActionTimer.stop();
        if (sorcererAttackTimer != null) sorcererAttackTimer.stop();
        if (bossAttackTimer != null) bossAttackTimer.stop();
        if (projectileTimer != null) projectileTimer.stop();
        if (teamMatchActionTimer != null) teamMatchActionTimer.stop();
        if (petTimer != null) petTimer.stop();
    }

    private void resumeAllTimers() {
        if (spawnTimer != null) spawnTimer.start();
        if (coinSpawnTimer != null) coinSpawnTimer.start();
        if (detectionTimer != null) detectionTimer.start();
        if (knightActionTimer != null) knightActionTimer.start();
        if (sorcererAttackTimer != null) sorcererAttackTimer.start();
        if (bossAttackTimer != null) bossAttackTimer.start();
        if (projectileTimer != null) projectileTimer.start();
        if (teamMatchActionTimer != null) teamMatchActionTimer.start();
        if (petTimer != null) petTimer.start();
    }
}
