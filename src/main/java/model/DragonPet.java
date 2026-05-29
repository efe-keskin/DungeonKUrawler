package model;

/**
 * Pet that fires a ranged attack at the nearest enemy in line of sight. Its
 * range is deliberately shorter than a sorcerer's (5) so it cannot out-snipe
 * spellcasters.
 */
public final class DragonPet extends Pet {

    /** Strictly less than the sorcerer/wizard shoot range. */
    public static final int ATTACK_RANGE = 4;
    public static final int ATTACK_DAMAGE = 2;
    private static final String SPRITE = "/pets/dragon1.png";
    private static final int FOLLOW_RANGE = 2;

    public DragonPet() {
        super("Dragon Pet", SPRITE, DEFAULT_MAX_HP, FOLLOW_RANGE);
    }

    public int getAttackRange() {
        return ATTACK_RANGE;
    }

    public int getAttackDamage() {
        return ATTACK_DAMAGE;
    }
}
