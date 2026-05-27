package engine;

import model.Armor;
import model.Hero;
import model.Knight;
import model.Ring;
import model.Sorcerer;
import model.Weapon;
import model.WeaponType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatManagerTest {

    private final CombatManager combatManager = new CombatManager();

    @Test
    void heroUnarmedAttacksDefaultKnight_usesFormulaValues() {
        Hero hero = new Hero(0, 0, "Hero", 100, 10, 80, 2, 100);
        Knight knight = new Knight(1, 0, "Knight", 20, 8, 4, 5);

        CombatManager.AttackResult result = combatManager.heroAttacksKnight(hero, knight);

        assertEquals(2, result.getDamageGenerated());
        assertEquals(1, result.getDamageReceived());
        assertEquals(19, result.getDefenderHpAfter());
        assertEquals(98, hero.getEnergy());
    }

    @Test
    void heroWithIronSwordDealsMoreDamageThanUnarmed() {
        Hero unarmedHero = new Hero(0, 0, "Hero", 100, 10, 80, 2, 100);
        Knight unarmedTarget = new Knight(1, 0, "Knight", 20, 0, 0, 5);
        int unarmedDamage = combatManager.heroAttacksKnight(unarmedHero, unarmedTarget).getDamageReceived();

        Hero armedHero = new Hero(0, 0, "Hero", 100, 10, 80, 2, 100);
        Weapon ironSword = new Weapon(new WeaponType("TEST_IRON", "Iron Sword", "swords", null, 3, false));
        armedHero.getInventory().tryAdd(ironSword);
        armedHero.equipWeapon(ironSword);
        Knight armedTarget = new Knight(1, 0, "Knight", 20, 0, 0, 5);

        int armedDamage = combatManager.heroAttacksKnight(armedHero, armedTarget).getDamageReceived();

        assertTrue(armedDamage > unarmedDamage);
    }

    @Test
    void leatherArmorReducesKnightHitDamage() {
        Knight attacker = new Knight(1, 0, "Knight", 20, 50, 0, 5);
        Hero noArmorHero = new Hero(0, 0, "Hero", 100, 10, 80, 2, 100);
        int noArmorDamage = combatManager.knightAttacksHero(attacker, noArmorHero).getDamageReceived();

        Hero armoredHero = new Hero(0, 0, "Hero", 100, 10, 80, 2, 100);
        Armor leatherArmor = new Armor("Leather Armor", 3);
        armoredHero.getInventory().tryAdd(leatherArmor);
        armoredHero.wearArmor(leatherArmor);

        int armorDamage = combatManager.knightAttacksHero(attacker, armoredHero).getDamageReceived();

        assertTrue(armorDamage < noArmorDamage);
    }

    @Test
    void protectiveRingStacksWithLeatherArmor() {
        Knight attacker = new Knight(1, 0, "Knight", 20, 50, 0, 5);
        Hero armoredHero = new Hero(0, 0, "Hero", 100, 10, 80, 2, 100);
        Armor leatherArmor = new Armor("Leather Armor", 3);
        armoredHero.getInventory().tryAdd(leatherArmor);
        armoredHero.wearArmor(leatherArmor);
        int armorDamage = combatManager.knightAttacksHero(attacker, armoredHero).getDamageReceived();

        Hero stackedHero = new Hero(0, 0, "Hero", 100, 10, 80, 2, 100);
        Armor stackedArmor = new Armor("Leather Armor", 3);
        Ring protectiveRing = new Ring("Protective Ring", 2);
        stackedHero.getInventory().tryAdd(stackedArmor);
        stackedHero.getInventory().tryAdd(protectiveRing);
        stackedHero.wearArmor(stackedArmor);
        stackedHero.wearRing(protectiveRing);

        int stackedDamage = combatManager.knightAttacksHero(attacker, stackedHero).getDamageReceived();

        assertTrue(stackedDamage < armorDamage);
    }

    @Test
    void sorcererProjectileSpendsManaAndThenFizzlesWhenEmpty() {
        Sorcerer sorcerer = new Sorcerer(1, 0, "Sorcerer", 10, 5, 0, false);
        Hero hero = new Hero(0, 0, "Hero", 100, 10, 80, 2, 100);

        CombatManager.AttackResult first = combatManager.sorcererAttacksHero(sorcerer, hero);
        CombatManager.AttackResult second = combatManager.sorcererAttacksHero(sorcerer, hero);

        assertEquals(8, first.getDamageGenerated());
        assertTrue(first.getDamageReceived() > 0);
        assertEquals(0, sorcerer.getMana());
        assertEquals(0, second.getDamageGenerated());
        assertEquals(0, second.getDamageReceived());
    }

    @Test
    void defenderDefeatedFlagIsTrueWhenHpDropsToZero() {
        Hero hero = new Hero(0, 0, "Hero", 100, 20, 80, 2, 100);
        Weapon ironSword = new Weapon(new WeaponType("TEST_IRON", "Iron Sword", "swords", null, 3, false));
        hero.getInventory().tryAdd(ironSword);
        hero.equipWeapon(ironSword);
        Sorcerer target = new Sorcerer(1, 0, "Sorcerer", 3, 30, 0, false);

        CombatManager.AttackResult result = combatManager.heroAttacksSorcerer(hero, target);

        assertEquals(0, result.getDefenderHpAfter());
        assertTrue(result.isDefenderDefeated());
    }
}
