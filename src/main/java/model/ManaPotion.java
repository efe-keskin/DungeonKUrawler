package model;

import java.awt.Color;

/**
 * Purple potion. Restores mana to full without overflowing past max mana.
 */
public class ManaPotion extends Potion {

    private static final Color COLOR = new Color(140, 90, 220);

    public ManaPotion() {
        super("Mana Potion");
    }

    @Override
    public Color getColor() {
        return COLOR;
    }

    @Override
    public void drink(Hero hero) {
        hero.restoreMana(hero.getMaxMana() - hero.getMana());
    }
}
