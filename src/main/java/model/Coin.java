package model;

/**
 * A coin reward found on the map or in a container.
 *
 * <p>Coins are collected directly into the hero's coin balance and never
 * consume an inventory slot.
 */
public class Coin extends Item {

    private final int value;

    public Coin(int value) {
        super("Coin Pile");
        if (value <= 0) {
            throw new IllegalArgumentException("coin value must be positive");
        }
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
