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
import model.RingEffectType;
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
            return randomRing(rng);
        }
        if (rng.nextDouble() < 0.5) {
            return new Weapon(WeaponCatalog.get().byId("W006"));
        }
        return new Armor("Leather Armor", 1);
    }

    private static Random safeRandom(Random random) {
        return random == null ? ThreadLocalRandom.current() : random;
    }

    private static Ring randomRing(Random rng) {
        return switch (rng.nextInt(4)) {
            case 0 -> new Ring("Power Ring", RingEffectType.STRENGTH, 3, "/items/rings/10_ring_red_gem.png");
            case 1 -> new Ring("Energy Ring", RingEffectType.ENERGY, 6, "/items/rings/11_ring_green_gem.png");
            case 2 -> new Ring("Mana Ring", RingEffectType.MANA, 6, "/items/rings/12_ring_blue_gem.png");
            default -> new Ring("Protective Ring", RingEffectType.DEFENSE, 3);
        };
    }
}
