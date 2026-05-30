package engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import model.Armor;
import model.Column;
import model.Coin;
import model.Container;
import model.Crate;
import model.EnergyPotion;
import model.GridCell;
import model.HealPotion;
import model.Hero;
import model.Item;
import model.ItemAction;
import model.Key;
import model.KeyColor;
import model.ManaPotion;
import model.SearchableObject;
import model.Ring;
import model.Vase;
import model.WaterPipe;
import model.Weapon;
import model.WeaponCatalog;

/**
 * GRASP Controller for the BREAK action. It owns the break chance formula and
 * the success side effects so keyboard and menu interactions stay consistent.
 */
final class BreakController {

    static final int BREAK_ENERGY_COST = 8;
    private static final double BASE_SUCCESS_CHANCE = 0.35;
    private static final double STR_CHANCE_STEP = 0.05;
    private static final double MIN_SUCCESS_CHANCE = 0.10;
    private static final double MAX_SUCCESS_CHANCE = 0.90;
    private static final double RANDOM_LOOT_CHANCE = 0.75;

    private static final int VASE_DIFFICULTY = 4;
    private static final int CRATE_DIFFICULTY = 7;
    private static final int WATER_PIPE_DIFFICULTY = 8;
    private static final int DEFAULT_CONTAINER_DIFFICULTY = 10;
    private static final int COLUMN_DIFFICULTY = 12;

    private final Random random;

    BreakController() {
        this(ThreadLocalRandom.current());
    }

    BreakController(Random random) {
        this.random = random == null ? ThreadLocalRandom.current() : random;
    }

    boolean isBreakable(Item item) {
        return item != null
                && (item.getInventoryActions().contains(ItemAction.BREAK)
                        || (item instanceof Container container && container.isBreakable()));
    }

    InteractionController.BreakResult attemptBreak(Hero hero, GridCell cell, Item item) {
        if (hero == null || cell == null || !isBreakable(item) || !cell.getItemsView().contains(item)) {
            return null;
        }

        // Break flow: first spend energy, then roll against a STR-based chance.
        // Formula: 0.35 + (hero STR - object difficulty) * 0.05, clamped to 10%-90%.
        int difficulty = difficultyFor(item);
        double chance = successChance(hero.getStr(), difficulty);
        if (hero.getEnergy() < BREAK_ENERGY_COST) {
            return new InteractionController.BreakResult(
                    item.getName(), InteractionController.BreakOutcome.NOT_ENOUGH_ENERGY,
                    BREAK_ENERGY_COST, chance, 0);
        }

        hero.consumeEnergy(BREAK_ENERGY_COST);
        if (random.nextDouble() > chance) {
            return new InteractionController.BreakResult(
                    item.getName(), InteractionController.BreakOutcome.FAILED,
                    BREAK_ENERGY_COST, chance, 0);
        }

        List<Item> drops = dropsFrom(item);
        cell.getItems().remove(item);
        cell.getItems().addAll(drops);
        return new InteractionController.BreakResult(
                item.getName(), InteractionController.BreakOutcome.BROKEN,
                BREAK_ENERGY_COST, chance, drops.size());
    }

    // Higher STR helps, harder objects push the chance down, but it is never
    // fully impossible or guaranteed.
    double successChance(int heroStr, int difficulty) {
        double chance = BASE_SUCCESS_CHANCE + (heroStr - difficulty) * STR_CHANCE_STEP;
        return Math.max(MIN_SUCCESS_CHANCE, Math.min(MAX_SUCCESS_CHANCE, chance));
    }

    private int difficultyFor(Item item) {
        if (item instanceof Container container) {
            return Math.max(DEFAULT_CONTAINER_DIFFICULTY, container.getBreakStrengthRequired());
        }
        if (item instanceof Vase) {
            return VASE_DIFFICULTY;
        }
        if (item instanceof Crate) {
            return CRATE_DIFFICULTY;
        }
        if (item instanceof WaterPipe) {
            return WATER_PIPE_DIFFICULTY;
        }
        if (item instanceof Column) {
            return COLUMN_DIFFICULTY;
        }
        return DEFAULT_CONTAINER_DIFFICULTY;
    }

    private List<Item> dropsFrom(Item item) {
        List<Item> drops = new ArrayList<>();
        if (item instanceof Container container) {
            for (Item content : new ArrayList<>(container.getContents())) {
                if (container.removeItem(content)) {
                    drops.add(content);
                }
            }
        }
        if (item instanceof SearchableObject searchableObject) {
            Item hidden = searchableObject.takeHiddenItem();
            if (hidden != null) {
                drops.add(hidden);
            }
        }
        // If the object did not already hide loot, successful breaks still have
        // a 75% chance to reward the player from the small loot table below.
        if (drops.isEmpty() && random.nextDouble() < RANDOM_LOOT_CHANCE) {
            drops.add(randomLoot());
        }
        return drops;
    }

    // Loot table after a successful break with no hidden item:
    // 35% coin, 20% heal, 15% energy, 15% mana, 7% key, 5% ring, 3% weapon/armor.
    private Item randomLoot() {
        double roll = random.nextDouble();
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
        if (random.nextDouble() < 0.5) {
            return new Weapon(WeaponCatalog.get().byId("W006"));
        }
        return new Armor("Leather Armor", 1);
    }
}
