package engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import model.Armor;
import model.Coin;
import model.GridCell;
import model.HealPotion;
import model.ManaPotion;
import model.SearchableObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchableObjectTest {

    private GameEngine engine;

    @BeforeEach
    void setUp() {
        engine = newGameWithFixedDouble(0.99);
    }

    @Test
    void searchEmptyObjectReturnsNothingFound() {
        SearchableObject object = new SearchableObject("Loose Brick", false, null);
        GridCell cell = placeSearchableNextToHero(object);

        GameEngine.SearchResult result = engine.search(object);

        assertEquals(GameEngine.SearchOutcome.NOTHING_FOUND, result.getOutcome());
        assertTrue(object.isSearched());
        assertEquals(0, engine.getHero().getInventory().size());
        assertEquals(1, cell.getItems().size());
    }

    @Test
    void searchHiddenItemRevealsItOnGroundAndClearsSlot() {
        GridCell cell = engine.getDungeonMap().getCell(engine.getHero().getX() + 1, engine.getHero().getY());
        ManaPotion potion = new ManaPotion();
        SearchableObject object = new SearchableObject("Loose Brick", false, null, potion);
        placeSearchableNextToHero(object);

        GameEngine.SearchResult result = engine.search(object);

        assertEquals(GameEngine.SearchOutcome.FOUND, result.getOutcome());
        assertSame(potion, result.getFoundItem());
        assertFalse(engine.getHero().getInventory().getItems().contains(potion));
        assertTrue(cell.getItems().contains(potion));
        assertSame(potion, cell.getItems().get(0));
        assertNull(object.getHiddenItem());
        assertTrue(object.isSearched());
    }

    @Test
    void searchWithFullInventoryStillRevealsHiddenItemOnGround() {
        for (int i = 0; i < engine.getHero().getInventory().getCapacity(); i++) {
            engine.getHero().getInventory().tryAdd(new HealPotion());
        }
        GridCell cell = engine.getDungeonMap().getCell(engine.getHero().getX() + 1, engine.getHero().getY());
        ManaPotion potion = new ManaPotion();
        SearchableObject object = new SearchableObject("Loose Brick", false, null, potion);
        placeSearchableNextToHero(object);

        GameEngine.SearchResult result = engine.search(object);

        assertEquals(GameEngine.SearchOutcome.FOUND, result.getOutcome());
        assertSame(potion, result.getFoundItem());
        assertTrue(cell.getItems().contains(potion));
        assertSame(potion, cell.getItems().get(0));
        assertNull(object.getHiddenItem());
    }

    @Test
    void searchWithoutHiddenItemCanDropRandomLootOnlyOnce() {
        engine.shutdown();
        engine = newGameWithFixedDouble(0.0);
        SearchableObject object = new SearchableObject("Loose Brick", false, null);
        GridCell cell = placeSearchableNextToHero(object);

        GameEngine.SearchResult first = engine.search(object);
        GameEngine.SearchResult second = engine.search(object);

        assertEquals(GameEngine.SearchOutcome.FOUND, first.getOutcome());
        assertTrue(first.getFoundItem() instanceof Coin);
        assertTrue(cell.getItems().contains(first.getFoundItem()));
        assertSame(first.getFoundItem(), cell.getItems().get(0));
        assertEquals(GameEngine.SearchOutcome.NOTHING_FOUND, second.getOutcome());
        assertEquals(2, cell.getItems().size());
    }

    @Test
    void searchWithoutHiddenItemInLateTowerLevel_usesLateLootTable() {
        engine.shutdown();
        engine = newGameWithFixedDouble(0.74);
        engine.configureTowerLevel(10, true);
        SearchableObject object = new SearchableObject("Loose Brick", false, null);
        placeSearchableNextToHero(object);

        GameEngine.SearchResult result = engine.search(object);

        assertEquals(GameEngine.SearchOutcome.FOUND, result.getOutcome());
        assertTrue(result.getFoundItem() instanceof Armor);
    }

    @Test
    void findSearchableNearHeroChoosesClosestFixture() {
        clearNearbyCells();
        int hx = engine.getHero().getX();
        int hy = engine.getHero().getY();
        SearchableObject farther = new SearchableObject("Far Brick", false, null);
        SearchableObject closer = new SearchableObject("Close Brick", false, null);
        engine.getDungeonMap().getCell(hx + 1, hy + 1).getItems().add(farther);
        engine.getDungeonMap().getCell(hx + 1, hy).getItems().add(closer);

        SearchableObject result = engine.findSearchableNearHero();

        assertSame(closer, result);
    }

    private GridCell placeSearchableNextToHero(SearchableObject object) {
        GridCell cell = engine.getDungeonMap().getCell(engine.getHero().getX() + 1, engine.getHero().getY());
        cell.getItems().clear();
        cell.getItems().add(object);
        return cell;
    }

    private void clearNearbyCells() {
        int hx = engine.getHero().getX();
        int hy = engine.getHero().getY();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                GridCell cell = engine.getDungeonMap().getCell(hx + dx, hy + dy);
                if (cell != null) {
                    cell.getItems().clear();
                }
            }
        }
    }

    private GameEngine newGameWithFixedDouble(double roll) {
        GameEngine game = new GameEngine(fixedDoubleRandom(roll));
        game.shutdown();
        return game;
    }

    private Random fixedDoubleRandom(double roll) {
        return new Random(0) {
            @Override
            public double nextDouble() {
                return roll;
            }

            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
    }
}
