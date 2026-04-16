package engine;

import model.DungeonMap;
import model.GridCell;
import model.Hero;
import model.Item;


public class InteractionController {

    private final GameEngine engine;

    public InteractionController(GameEngine engine) {
        this.engine = engine;
    }

    // looking action for ActionMenu and rule checker for 3x3 adjacent cell
    public String getPrimaryAction(int targetX, int targetY) {
        Hero hero = engine.getHero();
        DungeonMap map = engine.getDungeonMap();

        // check 3x3 adjacency 
        if (!map.isHeroAdjacent(hero, targetX, targetY)) {
            return null; 
        }

        GridCell cell = map.getCell(targetX, targetY);
        if (cell == null) return null;

        // check for Items (Key, Gold, Potion, Armour, Book)
        if (!cell.getItemsView().isEmpty()) {
            // This will perfectly return "TAKE Potion", "TAKE Key", etc. Later we can modify with great UI buttons
            return "TAKE " + ((Item) cell.getItemsView().iterator().next()).getName();
        }

    
    return null; 
}

// Gate for inventory plumbing and act as a gate between UI and domain logic
public void executeAction(String action, int x, int y) {
    System.out.println("Executing: " + action + " at " + x + ", " + y);
    
}

    
}