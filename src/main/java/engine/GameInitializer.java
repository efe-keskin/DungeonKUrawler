package engine;

import model.DungeonMap;
import model.HealPotion;
import model.Hero;
import model.ManaPotion;

/**
 * Builds the initial game state so {@link GameEngine} can focus on runtime rules.
 */
public class GameInitializer {

    /**
     * Immutable bundle for the engine's initial world state.
     */
    public record InitialGameState(DungeonMap map, Hero hero) {
    }

    public InitialGameState createInitialState(String levelName) {
        DungeonMap map = createDemoMap(levelName);
        Hero hero = new Hero(1, 1, "Hero", 100, 10, 20, 5, 100);
        map.addEntity(hero.getX(), hero.getY(), hero);
        return new InitialGameState(map, hero);
    }

    private DungeonMap createDemoMap(String levelName) {
        int width = 16;
        int height = 12;
        DungeonMap map = new DungeonMap(levelName, width, height);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                boolean wall = (x == 0 || y == 0 || x == width - 1 || y == height - 1);
                map.setCellPassable(x, y, !wall);
            }
        }
        for (int x = 6; x <= 7; x++) {
            for (int y = 4; y <= 5; y++) {
                map.setCellPassable(x, y, false);
            }
        }

        map.addItem(3, 1, new HealPotion());
        map.addItem(5, 3, new ManaPotion());
        return map;
    }
}
