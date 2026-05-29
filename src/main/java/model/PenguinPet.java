package model;

/**
 * Pet that freezes any enemy it touches for a few seconds (the freeze timer is
 * enforced by the engine). Roams beside the hero at knight range.
 */
public final class PenguinPet extends Pet {

    public static final int FREEZE_DURATION_MS = 3000;
    private static final String SPRITE = "/pets/penguin1.png";
    private static final int FOLLOW_RANGE = 5;

    public PenguinPet() {
        super("Penguin Pet", SPRITE, DEFAULT_MAX_HP, FOLLOW_RANGE);
    }
}
