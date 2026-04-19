package model;

import java.awt.Color;

/**
 * Drinkable consumable. Subclasses decide effect and render color.
 */
public abstract class Potion extends Item {

    public Potion(String name) {
        super(name);
    }

    public abstract Color getColor();

    public abstract void drink(Hero hero);
}
