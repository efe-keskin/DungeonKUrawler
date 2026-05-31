package engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Random;

import model.Armor;
import model.Coin;
import model.HealPotion;
import model.Key;
import model.ManaPotion;
import model.Ring;
import model.Weapon;

import org.junit.jupiter.api.Test;

class ObjectLootTableTest {

    @Test
    void customGameUsesTowerLevelsThreeToFourLootThresholds() {
        assertInstanceOf(Coin.class, ObjectLootTable.randomLoot(rolls(0.21)));
        assertInstanceOf(HealPotion.class, ObjectLootTable.randomLoot(rolls(0.37)));
        assertInstanceOf(ManaPotion.class, ObjectLootTable.randomLoot(rolls(0.59)));
        assertInstanceOf(Key.class, ObjectLootTable.randomLoot(rolls(0.66)));
        assertInstanceOf(Ring.class, randomLoot(rolls(0.84), ints(0)));
        assertInstanceOf(Weapon.class, randomLoot(rolls(0.99, 0.0), ints(0)));
        assertInstanceOf(Armor.class, ObjectLootTable.randomLoot(rolls(0.99, 0.99)));
    }

    @Test
    void towerLevelsUseProgressivelyStrongerLootTiers() {
        assertEquals(ObjectLootTable.LootTier.DEFAULT, ObjectLootTable.tierForTowerLevel(0));
        assertEquals(ObjectLootTable.LootTier.EARLY, ObjectLootTable.tierForTowerLevel(2));
        assertEquals(ObjectLootTable.LootTier.MID, ObjectLootTable.tierForTowerLevel(4));
        assertEquals(ObjectLootTable.LootTier.ADVANCED, ObjectLootTable.tierForTowerLevel(7));
        assertEquals(ObjectLootTable.LootTier.LATE, ObjectLootTable.tierForTowerLevel(10));
    }

    @Test
    void sameRollCanYieldRingInCustomGameAndEquipmentInLateTowerLevel() {
        assertInstanceOf(Ring.class,
                ObjectLootTable.randomLoot(rolls(0.75), ObjectLootTable.LootTier.DEFAULT));
        assertInstanceOf(Weapon.class,
                randomLoot(rolls(0.75, 0.0), ints(0), ObjectLootTable.LootTier.LATE));
    }

    @Test
    void laterTowerArmorHasHigherDefenseBonus() {
        Armor early = assertInstanceOf(Armor.class,
                ObjectLootTable.randomLoot(rolls(0.99, 0.99), ObjectLootTable.LootTier.EARLY));
        Armor late = assertInstanceOf(Armor.class,
                ObjectLootTable.randomLoot(rolls(0.99, 0.99), ObjectLootTable.LootTier.LATE));

        assertEquals(1, early.getDefModifier());
        assertEquals(4, late.getDefModifier());
    }

    private Random rolls(double... values) {
        return rolls(values, new int[] { 0 });
    }

    private Random ints(int... values) {
        return rolls(new double[] { 0.0 }, values);
    }

    private Random rolls(double[] doubleValues, int[] intValues) {
        return new Random() {
            private int doubleIndex;
            private int intIndex;

            @Override
            public double nextDouble() {
                return doubleValues[Math.min(doubleIndex++, doubleValues.length - 1)];
            }

            @Override
            public int nextInt(int bound) {
                return intValues[Math.min(intIndex++, intValues.length - 1)] % bound;
            }
        };
    }

    private model.Item randomLoot(Random doubleRandom, Random intRandom,
            ObjectLootTable.LootTier tier) {
        return ObjectLootTable.randomLoot(combined(doubleRandom, intRandom), tier);
    }

    private model.Item randomLoot(Random doubleRandom, Random intRandom) {
        return ObjectLootTable.randomLoot(combined(doubleRandom, intRandom));
    }

    private Random combined(Random doubleRandom, Random intRandom) {
        return new Random() {
            @Override
            public double nextDouble() {
                return doubleRandom.nextDouble();
            }

            @Override
            public int nextInt(int bound) {
                return intRandom.nextInt(bound);
            }
        };
    }
}
