package model;

/**
 * A coin reward found on the map or in a container.
 *
 * <p>Coins are collected directly into the hero's coin balance and never
 * consume an inventory slot.
 */
public class Coin extends Item {

    private final int value;
    private final String spriteResource;

    public Coin(int value) {
        this(value, null);
    }

    public Coin(int value, String spriteResource) {
        super("Coin Pile");
        if (value <= 0) {
            throw new IllegalArgumentException("coin value must be positive");
        }
        this.value = value;
        this.spriteResource = spriteResource;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String spriteResource() {
        return spriteResource;
    }
}
