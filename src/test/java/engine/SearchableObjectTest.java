package engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import model.HealPotion;
import model.ManaPotion;
import model.SearchableObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchableObjectTest {

    private GameEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GameEngine();
        engine.shutdown();
    }

    @Test
    void searchEmptyObjectReturnsNothingFound() {
        SearchableObject object = new SearchableObject("Loose Brick", false, null);

        GameEngine.SearchResult result = engine.search(object);

        assertEquals(GameEngine.SearchOutcome.NOTHING_FOUND, result.getOutcome());
        assertEquals(0, engine.getHero().getInventory().size());
    }

    @Test
    void searchHiddenItemAddsItToInventoryAndClearsSlot() {
        ManaPotion potion = new ManaPotion();
        SearchableObject object = new SearchableObject("Loose Brick", false, null, potion);

        GameEngine.SearchResult result = engine.search(object);

        assertEquals(GameEngine.SearchOutcome.FOUND, result.getOutcome());
        assertSame(potion, result.getFoundItem());
        assertTrue(engine.getHero().getInventory().getItems().contains(potion));
        assertNull(object.getHiddenItem());
    }

    @Test
    void searchWithFullInventoryKeepsHiddenItemInPlace() {
        for (int i = 0; i < engine.getHero().getInventory().getCapacity(); i++) {
            engine.getHero().getInventory().tryAdd(new HealPotion());
        }
        ManaPotion potion = new ManaPotion();
        SearchableObject object = new SearchableObject("Loose Brick", false, null, potion);

        GameEngine.SearchResult result = engine.search(object);

        assertEquals(GameEngine.SearchOutcome.INVENTORY_FULL, result.getOutcome());
        assertSame(potion, result.getFoundItem());
        assertSame(potion, object.getHiddenItem());
    }
}
