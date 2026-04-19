package model;

import java.awt.Color;

/**
 * Red potion. Restores HP to full — no overflow.
 */
public class HealPotion extends Potion {

    private static final Color COLOR = new Color(220, 55, 55);

    public HealPotion() {
        super("Heal Potion");
    }

    @Override
    public Color getColor() {
        return COLOR;
    }

    @Override
    public void drink(Hero hero) {
        if (hero.getHp() < hero.getMaxHp()) {
            hero.setHp(hero.getMaxHp());
        }
    }
}
