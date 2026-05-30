package engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import model.Chest;
import model.Column;
import model.Crate;
import model.EnergyPotion;
import model.GridCell;
import model.HealPotion;
import model.Hero;
import model.Item;
import model.Vase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InteractionControllerBreakTest {

    private GameEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GameEngine();
    }

    @AfterEach
    void tearDown() {
        engine.shutdown();
    }

    @Test
    void breakNearestObject_whenRollSucceeds_removesObjectAndConsumesEnergy() {
        InteractionController controller = controllerWithRoll(0.0);
        Hero hero = engine.getHero();
        hero.setStr(20);
        hero.setEnergy(30);
        clearNearbyItems();
        GridCell targetCell = targetCell();
        Column column = new Column(Column.GRAY_SPRITE);
        targetCell.getItems().clear();
        targetCell.getItems().add(column);

        InteractionController.BreakResult result = controller.breakNearestObject();

        assertTrue(result.broken());
        assertEquals(InteractionController.BreakOutcome.BROKEN, result.outcome());
        assertEquals(22, hero.getEnergy());
        assertFalse(targetCell.getItems().contains(column));
    }

    @Test
    void breakNearestObject_whenRollFails_consumesEnergyAndKeepsObject() {
        InteractionController controller = controllerWithRoll(0.99);
        Hero hero = engine.getHero();
        hero.setStr(8);
        hero.setEnergy(30);
        clearNearbyItems();
        GridCell targetCell = targetCell();
        Column column = new Column(Column.GRAY_SPRITE);
        targetCell.getItems().clear();
        targetCell.getItems().add(column);

        InteractionController.BreakResult result = controller.breakNearestObject();

        assertFalse(result.broken());
        assertEquals(InteractionController.BreakOutcome.FAILED, result.outcome());
        assertEquals(22, hero.getEnergy());
        assertTrue(targetCell.getItems().contains(column));
    }

    @Test
    void breakNearestObject_whenEnergyIsLow_doesNotConsumeEnergyOrRemoveObject() {
        InteractionController controller = controllerWithRoll(0.0);
        Hero hero = engine.getHero();
        hero.setStr(20);
        hero.setEnergy(3);
        clearNearbyItems();
        GridCell targetCell = targetCell();
        Column column = new Column(Column.GRAY_SPRITE);
        targetCell.getItems().clear();
        targetCell.getItems().add(column);

        InteractionController.BreakResult result = controller.breakNearestObject();

        assertFalse(result.broken());
        assertEquals(InteractionController.BreakOutcome.NOT_ENOUGH_ENERGY, result.outcome());
        assertEquals(3, hero.getEnergy());
        assertTrue(targetCell.getItems().contains(column));
    }

    @Test
    void breakNearestObject_choosesClosestBreakableObject() {
        InteractionController controller = controllerWithRoll(0.0);
        Hero hero = engine.getHero();
        hero.setStr(20);
        hero.setEnergy(30);
        clearNearbyCells();
        int hx = hero.getX();
        int hy = hero.getY();
        GridCell fartherCell = engine.getDungeonMap().getCell(hx - 1, hy - 1);
        GridCell closerCell = engine.getDungeonMap().getCell(hx + 1, hy);
        Column farther = new Column(Column.GRAY_SPRITE);
        Column closer = new Column(Column.PURPLE_SPRITE);
        fartherCell.getItems().add(farther);
        closerCell.getItems().add(closer);

        InteractionController.BreakResult result = controller.breakNearestObject();

        assertTrue(result.broken());
        assertTrue(fartherCell.getItems().contains(farther));
        assertFalse(closerCell.getItems().contains(closer));
    }

    @Test
    void breakObjectAt_whenContainerBreaks_dropsItsContents() {
        InteractionController controller = controllerWithRoll(0.0);
        Hero hero = engine.getHero();
        hero.setStr(20);
        hero.setEnergy(30);
        GridCell targetCell = targetCell();
        Chest chest = new Chest("Breakable Chest", 4);
        chest.setBreakable(true);
        Item potion = new EnergyPotion();
        chest.addItem(potion);
        targetCell.getItems().clear();
        targetCell.getItems().add(chest);

        InteractionController.BreakResult result = controller.breakObjectAt(chest, targetCell.getX(), targetCell.getY());

        assertTrue(result.broken());
        assertEquals(1, result.droppedItemCount());
        assertFalse(targetCell.getItems().contains(chest));
        assertSame(potion, targetCell.getItems().get(0));
    }

    @Test
    void breakObjectAt_whenSearchableBreaks_dropsHiddenItem() {
        InteractionController controller = controllerWithRoll(0.0);
        Hero hero = engine.getHero();
        hero.setStr(20);
        hero.setEnergy(30);
        GridCell targetCell = targetCell();
        Item hidden = new HealPotion();
        Crate crate = new Crate(Crate.BREAKABLE_WOOD_TALL_SPRITE, hidden);
        targetCell.getItems().clear();
        targetCell.getItems().add(crate);

        InteractionController.BreakResult result = controller.breakObjectAt(crate, targetCell.getX(), targetCell.getY());

        assertTrue(result.broken());
        assertEquals(1, result.droppedItemCount());
        assertFalse(targetCell.getItems().contains(crate));
        assertSame(hidden, targetCell.getItems().get(0));
    }

    @Test
    void breakObjectAt_whenVaseBreaks_keepsBrokenSpriteAndDroppedLootCollectible() {
        InteractionController controller = controllerWithRolls(0.0, 0.74, 0.36);
        Hero hero = engine.getHero();
        hero.setStr(20);
        hero.setEnergy(30);
        GridCell targetCell = targetCell();
        Vase vase = new Vase();
        targetCell.getItems().clear();
        targetCell.getItems().add(vase);

        InteractionController.BreakResult result = controller.breakObjectAt(vase, targetCell.getX(), targetCell.getY());

        assertTrue(result.broken());
        assertEquals(1, result.droppedItemCount());
        assertTrue(vase.isBroken());
        assertEquals(Vase.BROKEN_SPRITE, vase.spriteResource());
        assertFalse(vase.isBlocking());
        assertFalse(new BreakController().isBreakable(vase));
        assertSame(vase, targetCell.getItems().get(0));
        assertTrue(targetCell.getItems().get(1) instanceof HealPotion);

        assertEquals(InventoryController.PickupResult.SUCCESS,
                new InventoryController(engine).takeFirstItemFromCell(targetCell.getX(), targetCell.getY()));
        assertEquals(1, targetCell.getItems().size());
        assertSame(vase, targetCell.getItems().get(0));
        assertTrue(hero.getInventory().getItems().stream().anyMatch(HealPotion.class::isInstance));
    }

    @Test
    void breakObjectAt_whenNoHiddenItemAndLootRollSucceeds_dropsRandomLoot() {
        InteractionController controller = controllerWithRolls(0.0, 0.74, 0.36);
        Hero hero = engine.getHero();
        hero.setStr(20);
        hero.setEnergy(30);
        GridCell targetCell = targetCell();
        Column column = new Column(Column.GRAY_SPRITE);
        targetCell.getItems().clear();
        targetCell.getItems().add(column);

        InteractionController.BreakResult result = controller.breakObjectAt(column, targetCell.getX(), targetCell.getY());

        assertTrue(result.broken());
        assertEquals(1, result.droppedItemCount());
        assertFalse(targetCell.getItems().contains(column));
        assertTrue(targetCell.getItems().get(0) instanceof HealPotion);
    }

    @Test
    void breakObjectAt_whenNoHiddenItemAndLootRollFails_dropsNothing() {
        InteractionController controller = controllerWithRolls(0.0, 0.75);
        Hero hero = engine.getHero();
        hero.setStr(20);
        hero.setEnergy(30);
        GridCell targetCell = targetCell();
        Column column = new Column(Column.GRAY_SPRITE);
        targetCell.getItems().clear();
        targetCell.getItems().add(column);

        InteractionController.BreakResult result = controller.breakObjectAt(column, targetCell.getX(), targetCell.getY());

        assertTrue(result.broken());
        assertEquals(0, result.droppedItemCount());
        assertTrue(targetCell.getItems().isEmpty());
    }

    private GridCell targetCell() {
        clearNearbyItems();
        return engine.getDungeonMap().getCell(engine.getHero().getX() + 1, engine.getHero().getY());
    }

    private void clearNearbyItems() {
        Hero hero = engine.getHero();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                GridCell cell = engine.getDungeonMap().getCell(hero.getX() + dx, hero.getY() + dy);
                if (cell != null) {
                    cell.getItems().clear();
                }
            }
        }
    }

    private InteractionController controllerWithRoll(double roll) {
        return new InteractionController(engine, new BreakController(fixedRoll(roll)));
    }

    private InteractionController controllerWithRolls(double... rolls) {
        return new InteractionController(engine, new BreakController(fixedRolls(rolls)));
    }

    private Random fixedRoll(double roll) {
        return new Random() {
            @Override
            public double nextDouble() {
                return roll;
            }
        };
    }

    private Random fixedRolls(double... rolls) {
        return new Random() {
            private int index = 0;

            @Override
            public double nextDouble() {
                double value = rolls[Math.min(index, rolls.length - 1)];
                index++;
                return value;
            }
        };
    }
}
