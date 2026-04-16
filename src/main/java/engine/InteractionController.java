package engine;

import model.DungeonMap;
import model.GridCell;
import model.Hero;

public class InteractionController {

    private final GameEngine engine;

    public InteractionController(GameEngine engine) {
        this.engine = engine;
    }

    public void handleCellClick(int targetX, int targetY) {
        Hero hero = engine.getHero();
        DungeonMap dungeonMap = engine.getDungeonMap();

        if (!dungeonMap.isHeroAdjacent(hero, targetX, targetY)) {
            System.out.println("You cannot click here, you must be in the 3x3 range.");
            return;
        }

        GridCell cell = dungeonMap.getCell(targetX, targetY);

        if (cell == null) {
            System.out.println("Invalid cell.");
            return;
        }

        if (!cell.getItemsView().isEmpty()) {
            System.out.println("Action Menu is opening");
            // TODO: later trigger Action Menu here
        } else {
            System.out.println("You are in range but there is nothing to interact with.");
        }
    }
}