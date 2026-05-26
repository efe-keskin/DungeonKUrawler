package model;

import java.awt.Color;
import java.util.List;

/**
 * Drinkable consumable. Subclasses decide effect and render color.
 */
public abstract class Potion extends Item {

    public Potion(String name) {
        super(name);
    }

    public abstract Color getColor();

    public abstract void drink(Hero hero);

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.DRINK, ItemAction.DISCARD);
    }
}
