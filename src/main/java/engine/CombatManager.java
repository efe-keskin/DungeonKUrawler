package engine;

import model.Hero;
import model.Knight;
import model.Sorcerer;
import model.Weapon;

/**
 * Stateless combat formula service shared by player and AI controllers.
 */
public final class CombatManager {

    private static final int UNARMED_ATK = 1;
    private static final int KNIGHT_ATK = 4;
    private static final int SORCERER_PROJECTILE_ATK = 8;
    private static final int HERO_ATTACK_ENERGY_COST = 2;
    private static final int SORCERER_PROJECTILE_MANA_COST = 5;

    /**
     * Result returned to callers so UI/controller code can show combat feedback.
     */
    public static final class AttackResult {
        public final int damageGenerated;
        public final int damageReceived;
        public final int defenderHpAfter;
        public final boolean defenderDefeated;

        public AttackResult(int damageGenerated, int damageReceived, int defenderHpAfter,
                boolean defenderDefeated) {
            this.damageGenerated = damageGenerated;
            this.damageReceived = damageReceived;
            this.defenderHpAfter = defenderHpAfter;
            this.defenderDefeated = defenderDefeated;
        }

        public int getDamageGenerated() {
            return damageGenerated;
        }

        public int getDamageReceived() {
            return damageReceived;
        }

        public int getDefenderHpAfter() {
            return defenderHpAfter;
        }

        public boolean isDefenderDefeated() {
            return defenderDefeated;
        }
    }

    /**
     * Applies a hero melee attack to a knight and consumes hero attack energy.
     *
     * @return generated damage, received damage, remaining knight HP, and defeat state.
     */
    public AttackResult heroAttacksKnight(Hero hero, Knight target) {
        hero.consumeEnergy(HERO_ATTACK_ENERGY_COST);
        int weaponAtk = heroWeaponAtk(hero);
        int damageGenerated = generateDamage(weaponAtk, hero.getStr());
        int damageReceived = receiveDamage(damageGenerated, target.getDef(), target.getStr());
        return applyDamageToKnight(target, damageGenerated, damageReceived);
    }

    /**
     * Applies a hero melee attack to a sorcerer and consumes hero attack energy.
     *
     * @return generated damage, received damage, remaining sorcerer HP, and defeat state.
     */
    public AttackResult heroAttacksSorcerer(Hero hero, Sorcerer target) {
        hero.consumeEnergy(HERO_ATTACK_ENERGY_COST);
        int weaponAtk = heroWeaponAtk(hero);
        int damageGenerated = generateDamage(weaponAtk, hero.getStr());
        int damageReceived = receiveDamage(damageGenerated, target.getDef(), 0);
        return applyDamageToSorcerer(target, damageGenerated, damageReceived);
    }

    /**
     * Applies a knight melee attack to the hero.
     *
     * @return generated damage, received damage, remaining hero HP, and defeat state.
     */
    public AttackResult knightAttacksHero(Knight attacker, Hero hero) {
        int damageGenerated = generateDamage(KNIGHT_ATK, attacker.getStr());
        int damageReceived = receiveDamage(damageGenerated, hero.getDef(), hero.getStr());
        return applyDamageToHero(hero, damageGenerated, damageReceived);
    }

    /**
     * Applies a sorcerer projectile attack to the hero, spending sorcerer mana first.
     *
     * @return generated damage, received damage, remaining hero HP, and defeat state.
     */
    public AttackResult sorcererAttacksHero(Sorcerer attacker, Hero hero) {
        if (attacker.getMana() < SORCERER_PROJECTILE_MANA_COST) {
            return new AttackResult(0, 0, hero.getHp(), hero.getHp() == 0);
        }
        attacker.setMana(attacker.getMana() - SORCERER_PROJECTILE_MANA_COST);

        int damageGenerated = generateDamage(SORCERER_PROJECTILE_ATK, 0);
        int damageReceived = receiveDamage(damageGenerated, hero.getDef(), hero.getStr());
        return applyDamageToHero(hero, damageGenerated, damageReceived);
    }

    private int heroWeaponAtk(Hero hero) {
        Weapon weapon = hero.getEquippedWeapon();
        return weapon == null ? UNARMED_ATK : weapon.getAtkValue();
    }

    private int generateDamage(int weaponAtk, int attackerStr) {
        return (int) Math.round(weaponAtk * (1 + attackerStr / 20.0));
    }

    private int receiveDamage(int damageGenerated, int defenderDef, int defenderStr) {
        int effectiveDef = defenderDef + Math.floorDiv(defenderStr, 4);
        return Math.max(1, damageGenerated - effectiveDef);
    }

    private AttackResult applyDamageToHero(Hero hero, int damageGenerated, int damageReceived) {
        int hpAfter = Math.max(0, hero.getHp() - damageReceived);
        hero.setHp(hpAfter);
        return new AttackResult(damageGenerated, damageReceived, hpAfter, hpAfter == 0);
    }

    private AttackResult applyDamageToKnight(Knight target, int damageGenerated, int damageReceived) {
        // Spec 2.5.1: Knight's armor flat-reduces incoming damage by 1
        // (this is a Knight-specific rule, not part of the generic DEF formula).
        int knightReceived = Math.max(1, damageReceived - 1);
        int hpAfter = Math.max(0, target.getHp() - knightReceived);
        target.setHp(hpAfter);
        return new AttackResult(damageGenerated, knightReceived, hpAfter, hpAfter == 0);
    }

    private AttackResult applyDamageToSorcerer(Sorcerer target, int damageGenerated, int damageReceived) {
        int hpAfter = Math.max(0, target.getHp() - damageReceived);
        target.setHp(hpAfter);
        return new AttackResult(damageGenerated, damageReceived, hpAfter, hpAfter == 0);
    }
}
