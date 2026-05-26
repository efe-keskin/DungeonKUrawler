package model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeroManaTest {

    @Test
    void restoreManaRespectsMaxManaCap() {
        Hero hero = new Hero(0, 0, "Hero", 100, 10, 80, 2, 100);
        hero.setMana(30);

        hero.restoreMana(100);

        assertEquals(80, hero.getMana());
        assertEquals(80, hero.getMaxMana());
    }

    @Test
    void spendManaRefusesNegativeOrInsufficientAmounts() {
        Hero hero = new Hero(0, 0, "Hero", 100, 10, 80, 2, 100);

        assertFalse(hero.spendMana(-1));
        assertEquals(80, hero.getMana());

        assertFalse(hero.spendMana(81));
        assertEquals(80, hero.getMana());

        assertTrue(hero.spendMana(20));
        assertEquals(60, hero.getMana());
    }

    @Test
    void manaPotionDrinkFillsHeroToMaxMana() {
        Hero hero = new Hero(0, 0, "Hero", 100, 10, 80, 2, 100);
        hero.setMana(12);
        ManaPotion potion = new ManaPotion();

        potion.drink(hero);

        assertEquals(hero.getMaxMana(), hero.getMana());
    }
}
