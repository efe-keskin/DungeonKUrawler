package model;

/**
 * Pet that freezes any enemy it touches for a few seconds (the freeze timer is
 * enforced by the engine) and chips it with a light touch attack. Roams beside
 * the hero at knight range.
 */
public final class PenguinPet extends Pet {

    /** +30% over the previous 3000 ms — the freeze is the penguin's main weapon. */
    public static final int FREEZE_DURATION_MS = 3900;
    /** Flat melee damage dealt to each enemy the penguin touches per pet tick. */
    public static final int TOUCH_DAMAGE = 2;
    private static final String SPRITE = "/pets/penguin1.png";
    private static final int FOLLOW_RANGE = 5;

    public PenguinPet() {
        super("Penguin Pet", SPRITE, DEFAULT_MAX_HP, FOLLOW_RANGE);
    }
}
