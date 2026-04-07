package engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

import model.DungeonMap;
import model.EnemyFactory;
import model.Entity;
import model.GridCell;
import model.Hero;
import model.Knight;
import model.Sorcerer;

/**
 * GRASP Controller: sole entry point for game rules and state changes. The UI forwards input here
 * (e.g. {@link #moveHero(Direction)}) and observes updates via {@link GameStateListener}.
 *
 * <p><strong>Observer (Subject):</strong> maintains listener list and {@link #notifyListeners()} after
 * mutations so views stay decoupled from model internals.
 */
public class GameEngine {

    private final DungeonMap dungeonMap;
    private final Hero hero;
    private final Random random;
    private final EnemyFactory enemyFactory;
    private final List<GameStateListener> listeners = new CopyOnWriteArrayList<>();

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

    /**
     * Attempts to move the hero one tile; blocked by walls and enemy-occupied cells. All rules live
     * here — the UI only forwards {@link Direction}.
     */
    public void moveHero(Direction direction) {
        if (direction == null) {
            return;
        }
        int nx = hero.getX();
        int ny = hero.getY();
        switch (direction) {
            case UP -> ny--;
            case DOWN -> ny++;
            case LEFT -> nx--;
            case RIGHT -> nx++;
        }
        GridCell from = dungeonMap.getCell(hero.getX(), hero.getY());
        GridCell to = dungeonMap.getCell(nx, ny);
        if (!canHeroEnter(to)) {
            return;
        }
        if (from != null) {
            from.getEntities().remove(hero);
        }
        hero.setX(nx);
        hero.setY(ny);
        to.getEntities().add(hero);
        notifyListeners();
    }

    private boolean canHeroEnter(GridCell cell) {
        if (cell == null || !cell.isPassable()) {
            return false;
        }
        for (Entity e : cell.getEntities()) {
            if (e instanceof Knight || e instanceof Sorcerer) {
                return false;
            }
        }
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
