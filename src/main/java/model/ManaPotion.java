package model;

import java.awt.Color;

/**
 * Blue potion. Adds 100 energy and allows overflow past the energy cap.
 * Passive energy regeneration still respects the cap, so overflowed energy
 * is a one-shot buffer that depletes as the hero moves.
 */
public class ManaPotion extends Potion {

    private static final Color COLOR = new Color(50, 130, 255);
    private static final int OVERFLOW_AMOUNT = 100;

    public ManaPotion() {
        super("Mana Potion");
    }

    @Override
    public Color getColor() {
        return COLOR;
    }

    @Override
    public void drink(Hero hero) {
        hero.setEnergy(hero.getEnergy() + OVERFLOW_AMOUNT);
    }
}
