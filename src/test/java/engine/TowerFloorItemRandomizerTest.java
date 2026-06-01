package engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import model.Chest;
import model.Coin;
import model.DungeonMap;
import model.EnergyPotion;
import model.HealPotion;
import model.Key;
import model.KeyColor;
import model.ManaPotion;
import model.ValuableItem;

import org.junit.jupiter.api.Test;

class TowerFloorItemRandomizerTest {

    @Test
    void chestRewardsRerollWithinAuthoredPatternAndPreserveSpecialContents() {
        DungeonMap map = new BuildMapFactory().createEmptyMap("Designed", 8, 8);
        Chest chest = new Chest("Reward Chest", 8);
        Key key = new Key("silver", KeyColor.SILVER);
        ValuableItem valuable = new ValuableItem("Golden Idol");
        assertTrue(chest.addItem(new HealPotion()));
        assertTrue(chest.addItem(new Coin(30)));
        assertTrue(chest.addItem(key));
        assertTrue(chest.addItem(valuable));
        map.getCell(4, 4).getItems().add(chest);

        new TowerFloorItemRandomizer(sequence(2, 1)).randomize(map);

        assertEquals(4, chest.getContents().size());
        assertTrue(chest.getContents().stream().anyMatch(EnergyPotion.class::isInstance));
        Coin coin = chest.getContents().stream()
                .filter(Coin.class::isInstance)
                .map(Coin.class::cast)
                .findFirst()
                .orElseThrow();
        assertEquals(31, coin.getValue());
        assertTrue(chest.getContents().stream().anyMatch(item -> item == key));
        assertTrue(chest.getContents().stream().anyMatch(item -> item == valuable));
    }

    @Test
    void potionSlotsRerollIndependentlyWhileRemainingPotionSlots() {
        DungeonMap map = new BuildMapFactory().createEmptyMap("Designed", 8, 8);
        Chest first = new Chest("First Chest", 2);
        Chest second = new Chest("Second Chest", 2);
        assertTrue(first.addItem(new HealPotion()));
        assertTrue(second.addItem(new HealPotion()));
        map.getCell(3, 3).getItems().add(first);
        map.getCell(4, 4).getItems().add(second);

        new TowerFloorItemRandomizer(sequence(0, 1)).randomize(map);

        assertSame(HealPotion.class, first.getContents().get(0).getClass());
        assertSame(ManaPotion.class, second.getContents().get(0).getClass());
    }

    private Random sequence(int... values) {
        return new Random() {
            private int index;

            @Override
            public int nextInt(int bound) {
                return values[Math.min(index++, values.length - 1)] % bound;
            }
        };
    }
}
