package engine;

import model.Entity;
import model.BossEnemy;
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
    private static final int BOSS_PROJECTILE_MANA_COST = 8;
    private static final int HERO_ATTACK_ENERGY_COST = 2;
    private static final int SORCERER_PROJECTILE_MANA_COST = 5;
    private static final int HERO_RANGED_MANA_COST = 5;

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

    public AttackResult heroAttacksBoss(Hero hero, BossEnemy target) {
        hero.consumeEnergy(HERO_ATTACK_ENERGY_COST);
        int weaponAtk = heroWeaponAtk(hero);
        int damageGenerated = generateDamage(weaponAtk, hero.getStr());
        int damageReceived = receiveDamage(damageGenerated, target.getDef(), 0);
        return applyDamageToBoss(target, damageGenerated, damageReceived);
    }

    /**
     * Hero ranged shot prep: spends mana and energy, does not apply damage until impact.
     */
    public static final class HeroProjectilePrep {
        public final int damageGenerated;
        public final int damageReceived;

        public HeroProjectilePrep(int damageGenerated, int damageReceived) {
            this.damageGenerated = damageGenerated;
            this.damageReceived = damageReceived;
        }
    }

    /**
     * @return prep when the hero has a ranged weapon and enough mana; {@code null} otherwise
     */
    public HeroProjectilePrep prepareHeroRangedProjectile(Hero hero, Entity target) {
        Weapon weapon = hero.getEquippedWeapon();
        if (weapon == null || !weapon.isRanged()) {
            return null;
        }
        if (!(target instanceof Knight) && !(target instanceof Sorcerer) && !(target instanceof BossEnemy)) {
            return null;
        }
        if (!hero.spendMana(HERO_RANGED_MANA_COST)) {
            return null;
        }
        hero.consumeEnergy(HERO_ATTACK_ENERGY_COST);
        int weaponAtk = weapon.getAtkValue();
        int damageGenerated = generateDamage(weaponAtk, hero.getStr());
        int damageReceived;
        if (target instanceof Knight knight) {
            damageReceived = receiveDamage(damageGenerated, knight.getDef(), knight.getStr());
        } else if (target instanceof Sorcerer sorcerer) {
            damageReceived = receiveDamage(damageGenerated, sorcerer.getDef(), 0);
        } else {
            BossEnemy boss = (BossEnemy) target;
            damageReceived = receiveDamage(damageGenerated, boss.getDef(), 0);
        }
        return new HeroProjectilePrep(damageGenerated, damageReceived);
    }

    public AttackResult applyHeroProjectileHit(Entity target, HeroProjectilePrep prep) {
        if (target instanceof Knight knight) {
            return applyDamageToKnight(knight, prep.damageGenerated, prep.damageReceived);
        }
        if (target instanceof Sorcerer sorcerer) {
            return applyDamageToSorcerer(sorcerer, prep.damageGenerated, prep.damageReceived);
        }
        if (target instanceof BossEnemy boss) {
            return applyDamageToBoss(boss, prep.damageGenerated, prep.damageReceived);
        }
        return new AttackResult(0, 0, 0, false);
    }

    public static int heroRangedManaCost() {
        return HERO_RANGED_MANA_COST;
    }

    public static int sorcererProjectileManaCost() {
        return SORCERER_PROJECTILE_MANA_COST;
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

    public AttackResult teamKnightAttacks(Knight attacker, Entity target) {
        if (attacker == null || target == null || !attacker.hasWeapon()) {
            return null;
        }
        int damageGenerated = generateDamage(attacker.getWeapon().getAtkValue(), attacker.getStr());
        int damageReceived = receiveDamage(damageGenerated, defenseOf(target), strengthOf(target));
        return applyDamageToEntity(target, damageGenerated, damageReceived);
    }

    /**
     * Raw projectile roll after mana is spent (damage is applied on impact).
     */
    public static final class SorcererProjectilePrep {
        public final int damageGenerated;
        public final int damageReceived;

        public SorcererProjectilePrep(int damageGenerated, int damageReceived) {
            this.damageGenerated = damageGenerated;
            this.damageReceived = damageReceived;
        }
    }

    /**
     * Spends mana and computes projectile damage without applying it to the hero.
     *
     * @return prep data, or {@code null} when mana is insufficient
     */
    public SorcererProjectilePrep prepareSorcererProjectile(Sorcerer attacker, Hero hero) {
        if (attacker.getMana() < SORCERER_PROJECTILE_MANA_COST) {
            return null;
        }
        attacker.setMana(attacker.getMana() - SORCERER_PROJECTILE_MANA_COST);
        int damageGenerated = generateDamage(SORCERER_PROJECTILE_ATK, 0);
        int damageReceived = receiveDamage(damageGenerated, hero.getDef(), hero.getStr());
        return new SorcererProjectilePrep(damageGenerated, damageReceived);
    }

    public AttackResult teamSorcererAttacks(Sorcerer attacker, Entity target) {
        if (attacker == null || target == null || attacker.getMana() < SORCERER_PROJECTILE_MANA_COST) {
            return null;
        }
        attacker.setMana(attacker.getMana() - SORCERER_PROJECTILE_MANA_COST);
        int damageGenerated = generateDamage(SORCERER_PROJECTILE_ATK, 0);
        int damageReceived = receiveDamage(damageGenerated, defenseOf(target), strengthOf(target));
        return applyDamageToEntity(target, damageGenerated, damageReceived);
    }
  
    public SorcererProjectilePrep prepareBossProjectile(BossEnemy attacker, Hero hero) {
        if (attacker.getMana() < BOSS_PROJECTILE_MANA_COST) {
            return null;
        }
        attacker.setMana(attacker.getMana() - BOSS_PROJECTILE_MANA_COST);
        int damageGenerated = generateDamage(attacker.getProjectileAttack(), 0);
        int damageReceived = receiveDamage(damageGenerated, hero.getDef(), hero.getStr());
        return new SorcererProjectilePrep(damageGenerated, damageReceived);
    }

    /**
     * Applies projectile impact damage to the hero.
     */
    public AttackResult applySorcererProjectileHit(Hero hero, SorcererProjectilePrep prep) {
        return applyDamageToHero(hero, prep.damageGenerated, prep.damageReceived);
    }

    /** Applies stored impact damage when a flying projectile reaches the hero. */
    public void applyProjectileImpact(Hero hero, int damageReceived) {
        int hpAfter = Math.max(0, hero.getHp() - damageReceived);
        hero.setHp(hpAfter);
    }

    /**
     * Instant hit (unit tests and legacy callers).
     */
    public AttackResult sorcererAttacksHero(Sorcerer attacker, Hero hero) {
        SorcererProjectilePrep prep = prepareSorcererProjectile(attacker, hero);
        if (prep == null) {
            return new AttackResult(0, 0, hero.getHp(), hero.getHp() == 0);
        }
        return applySorcererProjectileHit(hero, prep);
    }

    private int heroWeaponAtk(Hero hero) {
        Weapon weapon = hero.getEquippedWeapon();
        return weapon == null ? UNARMED_ATK : weapon.getAtkValue();
    }

    private int defenseOf(Entity target) {
        if (target instanceof Hero hero) {
            return hero.getDef();
        }
        if (target instanceof Knight knight) {
            return knight.getDef();
        }
        if (target instanceof Sorcerer sorcerer) {
            return sorcerer.getDef();
        }
        return 0;
    }

    private int strengthOf(Entity target) {
        if (target instanceof Hero hero) {
            return hero.getStr();
        }
        if (target instanceof Knight knight) {
            return knight.getStr();
        }
        return 0;
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

    private AttackResult applyDamageToEntity(Entity target, int damageGenerated, int damageReceived) {
        if (target instanceof Hero hero) {
            return applyDamageToHero(hero, damageGenerated, damageReceived);
        }
        if (target instanceof Knight knight) {
            return applyDamageToKnight(knight, damageGenerated, damageReceived);
        }
        if (target instanceof Sorcerer sorcerer) {
            return applyDamageToSorcerer(sorcerer, damageGenerated, damageReceived);
        }
        if (target instanceof BossEnemy boss) {
            return applyDamageToBoss(boss, damageGenerated, damageReceived);
        }
        return new AttackResult(damageGenerated, 0, 0, false);
    }
  
    private AttackResult applyDamageToBoss(BossEnemy target, int damageGenerated, int damageReceived) {
        int hpAfter = Math.max(0, target.getHp() - damageReceived);
        target.setHp(hpAfter);
        return new AttackResult(damageGenerated, damageReceived, hpAfter, hpAfter == 0);
    }
}
