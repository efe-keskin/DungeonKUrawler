package engine;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import model.Arch;
import model.BossEnemy;
import model.Chest;
import model.Coin;
import model.Difficulty;
import model.DungeonLevel;
import model.DungeonMap;
import model.EnemyFactory;
import model.GridCell;
import model.HealPotion;
import model.Hero;
import model.Key;
import model.KeyColor;
import model.LevelType;
import model.Torch;
import model.TowerScenario;

/**
 * Simple Factory (GoF / Pure Fabrication): builds a configured {@link GameEngine}
 * session for a chosen tower {@link DungeonLevel}, keeping map generation and
 * difficulty-strategy selection out of the engine and the views.
 *
 * <p>It carries the persistent hero from a {@link GameStateSnapshot} onto a
 * freshly generated floor map, picks an {@link EnemySpawnPolicy} from the
 * level's difficulty/type, and seeds a boss for boss floors.
 */
public final class DungeonLevelFactory {

    private static final int HERO_START_X = 1;
    private static final int HERO_START_Y = 1;
    private static final String TOWER_MAP_DIR = "/maps/tower/";

    private final Random random;
    private final BuildMapPersistence mapPersistence;

    public DungeonLevelFactory() {
        this(ThreadLocalRandom.current());
    }

    public DungeonLevelFactory(Random random) {
        this(random, new BuildMapPersistence(
                new BuildToolCatalog(), new BuildMapFactory(), new StandardBuildPlacementStrategy()));
    }

    DungeonLevelFactory(Random random, BuildMapPersistence mapPersistence) {
        this.random = random == null ? ThreadLocalRandom.current() : random;
        this.mapPersistence = mapPersistence;
    }

    /**
     * Creates a playable engine for {@code level}, reusing the carry-over hero
     * from {@code state} (or a fresh hero if none) on a newly built floor map.
     */
    public GameEngine createEngine(DungeonLevel level, GameStateSnapshot state) {
        DungeonMap map = createMapForFloor(level);
        // In-game items (potions, valuables found here) do not carry between
        // floors; only the persistent meta-state (gold, stats) follows the hero.
        Hero hero = (state == null || state.hero() == null) ? defaultHero() : carryOverHero(state.hero());
        placeExitArch(map);
        seedBoss(level, map);
        EnemySpawnPolicy spawnPolicy = spawnPolicyFor(level);
        // The tower constructor hides the floor's target mission in the cache chest;
        // reaching the exit door after finding it completes the floor.
        GameEngine engine = new GameEngine(map, hero, spawnPolicy);
        engine.configureTowerLevel(level.levelNumber(), level.levelType() == LevelType.FINAL_BOSS);
        return engine;
    }

    /** Selects the difficulty/type strategy for a level (GoF Strategy). */
    public EnemySpawnPolicy spawnPolicyFor(DungeonLevel level) {
        switch (level.levelType()) {
            case BOSS:
                return new BossEnemySpawnPolicy(random);
            case FINAL_BOSS:
                return new FinalBossEnemySpawnPolicy(random);
            case REGULAR:
            default:
                return switch (level.difficulty()) {
                    case HARD -> new HardEnemySpawnPolicy(random, false);
                    case VERY_HARD -> new HardEnemySpawnPolicy(random, true);
                    default -> new RegularEnemySpawnPolicy(new EnemyFactory(random));
                };
        }
    }

    /**
     * Loads the designed JSON map for a tower floor. The generated room remains
     * as a defensive fallback so a missing resource never prevents play.
     */
    public DungeonMap createMapForFloor(int floorNumber) {
        return createMapForFloor(TowerScenario.defaultScenario().getLevel(floorNumber));
    }

    public DungeonMap createMapForFloor(DungeonLevel level) {
        if (level == null) {
            throw new IllegalArgumentException("Tower level is required.");
        }
        DungeonMap map = loadDesignedMap(level);
        if (map == null) {
            map = createGeneratedFallbackMap(level);
        }
        map.setFogEnabled(level.fogHidden());
        if (level.fogHidden()) {
            seedStarterTorch(map);
        }
        return map;
    }

    private DungeonMap loadDesignedMap(DungeonLevel level) {
        String resource = TOWER_MAP_DIR + String.format("floor%02d.json", level.levelNumber());
        try {
            return mapPersistence.loadResource(resource);
        } catch (IOException ex) {
            System.err.println("[tower] using generated fallback for floor "
                    + level.levelNumber() + ": " + ex.getMessage());
            return null;
        }
    }

    /**
     * Builds a bordered fallback room sized to the floor's difficulty, with a
     * few interior obstacles and a loot chest. The cell at (1,1) stays walkable.
     */
    private DungeonMap createGeneratedFallbackMap(DungeonLevel level) {
        int[] size = sizeFor(level.difficulty());
        int w = size[0];
        int h = size[1];
        DungeonMap map = new DungeonMap(level.levelName(), w, h);
        map.setFogEnabled(level.fogHidden());

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                GridCell cell = map.getCell(x, y);
                if (cell == null) {
                    continue;
                }
                boolean border = x == 0 || y == 0 || x == w - 1 || y == h - 1;
                cell.setPassable(!border);
            }
        }

        // Scatter a handful of interior pillar blocks, never over the start cell.
        int pillars = 3 + random.nextInt(3);
        for (int i = 0; i < pillars; i++) {
            int px = 2 + random.nextInt(Math.max(1, w - 4));
            int py = 2 + random.nextInt(Math.max(1, h - 4));
            if (px == HERO_START_X && py == HERO_START_Y) {
                continue;
            }
            GridCell cell = map.getCell(px, py);
            if (cell != null) {
                cell.setPassable(false);
            }
        }

        // A loot chest in the far corner gives the floor something to find.
        GridCell chestCell = walkableCell(map, w - 3, h - 3);
        if (chestCell != null) {
            Chest chest = new Chest("Orange Strongbox", 16,
                    "/items/chests/10_orange_chest_closed_frame1.png");
            chest.addItem(new HealPotion());
            chest.addItem(new Coin(10));
            chestCell.getItems().add(chest);
        }

        // A gold key hidden somewhere on the floor; required to unlock the exit arch.
        GridCell keyCell = walkableCell(map, 1 + random.nextInt(w - 2), 1 + random.nextInt(h - 2));
        if (keyCell != null) {
            keyCell.getItems().add(new Key("arch-gold", KeyColor.GOLD));
        }

        return map;
    }

    /**
     * Places a single Torch on a walkable cell within a few tiles
     * of the hero start. Tries (3,1) first, then (1,3), then a
     * small scan; gives up silently if no free cell is available
     * (extremely cramped maps). Never places on the start cell
     * itself - the torch must be a step the player consciously
     * takes, not an auto-pickup.
     */
    private void seedStarterTorch(DungeonMap map) {
        int[][] preferred = { {3, 1}, {1, 3}, {2, 2}, {4, 1}, {1, 4} };
        for (int[] coord : preferred) {
            GridCell cell = map.getCell(coord[0], coord[1]);
            if (cell != null
                    && !(coord[0] == HERO_START_X && coord[1] == HERO_START_Y)
                    && cell.isWalkable()
                    && cell.getItemsView().isEmpty()
                    && cell.getEntitiesView().isEmpty()) {
                cell.getItems().add(new Torch());
                return;
            }
        }
        // Fallback scan: any free interior cell that's at most ~5 tiles
        // from the start in Chebyshev distance.
        for (int y = 1; y < map.getHeight() - 1; y++) {
            for (int x = 1; x < map.getWidth() - 1; x++) {
                if (x == HERO_START_X && y == HERO_START_Y) {
                    continue;
                }
                int dist = Math.max(Math.abs(x - HERO_START_X),
                                    Math.abs(y - HERO_START_Y));
                if (dist > 5) {
                    continue;
                }
                GridCell cell = map.getCell(x, y);
                if (cell != null && cell.isWalkable()
                        && cell.getItemsView().isEmpty()
                        && cell.getEntitiesView().isEmpty()) {
                    cell.getItems().add(new Torch());
                    return;
                }
            }
        }
        // If nothing free was found, the random spawn elsewhere can
        // still provide a torch eventually. Don't crash.
    }

    /**
     * Sets the exit arch into the middle of the right-hand wall: the gap tile is
     * made passable but holds a closed (blocking) {@link Arch}, and its interior
     * neighbour is cleared so the hero can reach it. The arch opens with the
     * {@code O} key once the target is found.
     */
    private void placeExitArch(DungeonMap map) {
        int x = map.getWidth() - 1;
        int y = map.getHeight() / 2;
        GridCell archCell = map.getCell(x, y);
        GridCell approach = map.getCell(x - 1, y);
        if (archCell == null || approach == null) {
            return;
        }
        approach.setPassable(true);
        approach.getItems().clear();
        archCell.setPassable(true);
        archCell.getItems().clear();
        archCell.getEntities().clear();
        archCell.getItems().add(new Arch());
    }

    /**
     * Places a boss enemy on BOSS / FINAL_BOSS floors as an optional challenge
     * guarding the floor. Completion still comes from finding the target and
     * reaching the exit arch, not from defeating the boss.
     */
    private void seedBoss(DungeonLevel level, DungeonMap map) {
        BossEnemy boss = switch (level.levelType()) {
            case BOSS -> new BossEnemy(0, 0, "The Warden", 180, 120, 8, 11);
            case FINAL_BOSS -> new BossEnemy(0, 0, "The Dread King", 340, 180, 11, 15);
            default -> null;
        };
        if (boss == null) {
            return;
        }
        GridCell cell = walkableCell(map, map.getWidth() / 2, map.getHeight() / 2);
        if (cell != null) {
            boss.setX(cell.getX());
            boss.setY(cell.getY());
            cell.getEntities().add(boss);
        }
    }

    private int[] sizeFor(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> new int[] { 16, 12 };
            case MEDIUM -> new int[] { 18, 13 };
            case HARD -> new int[] { 20, 14 };
            case VERY_HARD -> new int[] { 22, 16 };
            case BOSS -> new int[] { 20, 16 };
        };
    }

    /**
     * Nearest free cell at or after the preferred coordinates; walkable, with
     * no entities and no items, so placed chests/doors/bosses never collide and
     * the start cell stays clear.
     */
    private GridCell walkableCell(DungeonMap map, int preferredX, int preferredY) {
        GridCell preferred = map.getCell(preferredX, preferredY);
        if (isFree(preferred)) {
            return preferred;
        }
        for (int y = 1; y < map.getHeight() - 1; y++) {
            for (int x = 1; x < map.getWidth() - 1; x++) {
                if (x == HERO_START_X && y == HERO_START_Y) {
                    continue;
                }
                GridCell cell = map.getCell(x, y);
                if (isFree(cell)) {
                    return cell;
                }
            }
        }
        return null;
    }

    private static boolean isFree(GridCell cell) {
        return cell != null && cell.isWalkable()
                && cell.getEntitiesView().isEmpty() && cell.getItemsView().isEmpty();
    }

    private Hero defaultHero() {
        int startingStr = 8 + random.nextInt(8);
        return new Hero(HERO_START_X, HERO_START_Y, "Hero", 17, startingStr, 80, 2, 100);
    }

    /**
     * Builds the hero for a new floor from the persistent meta-state of the
     * previous one: same identity, stats and full-game inventory (gold +
     * valuables + purchases), but full vitals and an empty per-level bag.
     * Temporary level loot stays on the floor it was found on.
     */
    private Hero carryOverHero(Hero source) {
        Hero hero = new Hero(HERO_START_X, HERO_START_Y, source.getName(),
                source.getMaxHp(), source.getStr(), source.getMaxMana(),
                source.getBaseDef(), source.getMaxEnergy());
        hero.getFullInventory().copyFrom(source.getFullInventory());
        // copyFrom keeps the same item instances, so the equipped pet is still present.
        hero.setEquippedPet(source.getEquippedPet());
        return hero;
    }
}
