package engine;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import model.Armor;
import model.Coin;
import model.EnergyPotion;
import model.HealPotion;
import model.Item;
import model.Key;
import model.KeyColor;
import model.ManaPotion;
import model.Ring;
import model.Weapon;
import model.WeaponCatalog;

/**
 * Shared loot table for objects that can reward the player after interaction.
 */
final class ObjectLootTable {

    static final double RANDOM_LOOT_CHANCE = 0.75;

    private ObjectLootTable() {
    }

    static boolean shouldDropRandomLoot(Random random) {
        return safeRandom(random).nextDouble() < RANDOM_LOOT_CHANCE;
    }

    // Loot table: 35% coin, 20% heal, 15% energy, 15% mana,
    // 7% silver key, 5% ring, 3% basic weapon/armor.
    static Item randomLoot(Random random) {
        Random rng = safeRandom(random);
        double roll = rng.nextDouble();
        if (roll < 0.35) {
            return new Coin(10);
        }
        if (roll < 0.55) {
            return new HealPotion();
        }
        if (roll < 0.70) {
            return new EnergyPotion();
        }
        if (roll < 0.85) {
            return new ManaPotion();
        }
        if (roll < 0.92) {
            return new Key("silver", KeyColor.SILVER);
        }
        if (roll < 0.97) {
            return new Ring("Protective Ring", 1);
        }
        if (rng.nextDouble() < 0.5) {
            return new Weapon(WeaponCatalog.get().byId("W006"));
        }
        return new Armor("Leather Armor", 1);
    }

    private static Random safeRandom(Random random) {
        return random == null ? ThreadLocalRandom.current() : random;
    }
}
