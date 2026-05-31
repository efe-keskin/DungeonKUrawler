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
import model.WeaponType;

/**
 * Shared loot table for objects that can reward the player after interaction.
 */
final class ObjectLootTable {

    static final double RANDOM_LOOT_CHANCE = 0.75;

    enum LootTier {
        DEFAULT,
        EARLY,
        MID,
        ADVANCED,
        LATE
    }

    private ObjectLootTable() {
    }

    static boolean shouldDropRandomLoot(Random random) {
        return safeRandom(random).nextDouble() < RANDOM_LOOT_CHANCE;
    }

    // Default Custom / Build Map table matches tower levels 3-4:
    // 22% coin, 16% heal, 11% energy, 11% mana,
    // 7% silver key, 18% ring, 15% weapon/armor.
    static Item randomLoot(Random random) {
        return randomLoot(random, LootTier.DEFAULT);
    }

    /**
     * Tower levels gradually trade common rewards for rings and equipment.
     * DEFAULT is the Build Map / Custom Game table and matches tower levels 3-4.
     *
     * <p>Percentages by tier (coin/heal/energy/mana/key/ring/equipment):
     * DEFAULT 22/16/11/11/7/18/15, EARLY 26/18/13/13/7/13/10,
     * MID 22/16/11/11/7/18/15, ADVANCED 16/13/9/9/6/24/23,
     * LATE 8/8/4/4/4/32/40.
     */
    static Item randomLoot(Random random, LootTier tier) {
        Random rng = safeRandom(random);
        LootTier safeTier = tier == null ? LootTier.DEFAULT : tier;
        double roll = rng.nextDouble();
        if (roll < coinLimit(safeTier)) {
            return new Coin(10);
        }
        if (roll < healLimit(safeTier)) {
            return new HealPotion();
        }
        if (roll < energyLimit(safeTier)) {
            return new EnergyPotion();
        }
        if (roll < manaLimit(safeTier)) {
            return new ManaPotion();
        }
        if (roll < keyLimit(safeTier)) {
            return new Key("silver", KeyColor.SILVER);
        }
        if (roll < ringLimit(safeTier)) {
            return randomRing(rng);
        }
        return randomEquipment(rng, safeTier);
    }

    static LootTier tierForTowerLevel(int levelNumber) {
        if (levelNumber <= 0) {
            return LootTier.DEFAULT;
        }
        if (levelNumber <= 2) {
            return LootTier.EARLY;
        }
        if (levelNumber <= 4) {
            return LootTier.MID;
        }
        if (levelNumber <= 7) {
            return LootTier.ADVANCED;
        }
        return LootTier.LATE;
    }

    private static double coinLimit(LootTier tier) {
        return switch (tier) {
            case DEFAULT -> 0.22;
            case EARLY -> 0.26;
            case MID -> 0.22;
            case ADVANCED -> 0.16;
            case LATE -> 0.08;
        };
    }

    private static double healLimit(LootTier tier) {
        return switch (tier) {
            case DEFAULT -> 0.38;
            case EARLY -> 0.44;
            case MID -> 0.38;
            case ADVANCED -> 0.29;
            case LATE -> 0.16;
        };
    }

    private static double energyLimit(LootTier tier) {
        return switch (tier) {
            case DEFAULT -> 0.49;
            case EARLY -> 0.57;
            case MID -> 0.49;
            case ADVANCED -> 0.38;
            case LATE -> 0.20;
        };
    }

    private static double manaLimit(LootTier tier) {
        return switch (tier) {
            case DEFAULT -> 0.60;
            case EARLY -> 0.70;
            case MID -> 0.60;
            case ADVANCED -> 0.47;
            case LATE -> 0.24;
        };
    }

    private static double keyLimit(LootTier tier) {
        return switch (tier) {
            case DEFAULT -> 0.67;
            case EARLY -> 0.77;
            case MID -> 0.67;
            case ADVANCED -> 0.53;
            case LATE -> 0.28;
        };
    }

    private static double ringLimit(LootTier tier) {
        return switch (tier) {
            case DEFAULT -> 0.85;
            case EARLY -> 0.90;
            case MID -> 0.85;
            case ADVANCED -> 0.77;
            case LATE -> 0.60;
        };
    }

    private static Item randomEquipment(Random rng, LootTier tier) {
        if (rng.nextDouble() < 0.5) {
            return new Weapon(randomWeaponType(rng, tier));
        }
        return switch (tier) {
            case EARLY -> new Armor("Leather Armor", 1);
            case DEFAULT, MID -> new Armor("Chainmail Armor", 2);
            case ADVANCED -> new Armor("Knight Armor", 3);
            case LATE -> new Armor("Royal Guard Armor", 4);
        };
    }

    private static WeaponType randomWeaponType(Random rng, LootTier tier) {
        String[] ids = switch (tier) {
            case DEFAULT, MID -> new String[] { "W002", "W003", "W010" };
            case EARLY -> new String[] { "W006", "W007", "W002" };
            case ADVANCED -> new String[] { "W010", "W014", "W001", "B23_BOW" };
            case LATE -> new String[] { "W010", "W014", "B23_BOW", "B23_WAND" };
        };
        WeaponType weapon = WeaponCatalog.get().byId(ids[rng.nextInt(ids.length)]);
        return weapon != null ? weapon : WeaponCatalog.get().byId("W006");
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
