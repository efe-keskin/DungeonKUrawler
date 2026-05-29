package engine;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import model.Chest;
import model.Coin;
import model.Difficulty;
import model.Door;
import model.DungeonLevel;
import model.DungeonMap;
import model.EnemyFactory;
import model.GridCell;
import model.HealPotion;
import model.Hero;
import model.Knight;
import model.LevelType;

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

    private final Random random;

    public DungeonLevelFactory() {
        this(ThreadLocalRandom.current());
    }

    public DungeonLevelFactory(Random random) {
        this.random = random;
    }

    /**
     * Creates a playable engine for {@code level}, reusing the carry-over hero
     * from {@code state} (or a fresh hero if none) on a newly built floor map.
     */
    public GameEngine createEngine(DungeonLevel level, GameStateSnapshot state) {
        DungeonMap map = createMap(level);
        Hero hero = (state == null || state.hero() == null) ? defaultHero() : state.hero();
        hero.setX(HERO_START_X);
        hero.setY(HERO_START_Y);
        placeExitDoor(map);
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
     * Builds a bordered room sized to the floor's difficulty, with a few
     * interior obstacles and a loot chest. The cell at (1,1) is always
     * walkable so the hero has a valid start.
     */
    private DungeonMap createMap(DungeonLevel level) {
        int[] size = sizeFor(level.difficulty());
        int w = size[0];
        int h = size[1];
        DungeonMap map = new DungeonMap(level.levelName(), w, h);

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
            Chest chest = new Chest("Floor Cache", 16);
            chest.addItem(new HealPotion());
            chest.addItem(new Coin(10));
            chestCell.getItems().add(chest);
        }

        return map;
    }

    /** Places the floor's exit door on a far walkable cell. */
    private void placeExitDoor(DungeonMap map) {
        GridCell cell = walkableCell(map, map.getWidth() - 2, 1);
        if (cell != null) {
            cell.getItems().add(new Door());
        }
    }

    /**
     * Places a boss enemy on BOSS / FINAL_BOSS floors as an optional challenge
     * guarding the floor. Completion still comes from finding the target and
     * reaching the exit door, not from defeating the boss.
     */
    private void seedBoss(DungeonLevel level, DungeonMap map) {
        Knight boss = switch (level.levelType()) {
            case BOSS -> new Knight(0, 0, "The Warden", 120, 14, 8, 8);
            case FINAL_BOSS -> new Knight(0, 0, "The Dread King", 250, 18, 10, 10);
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
     * Nearest free cell at or after the preferred coordinates — walkable, with
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
}
