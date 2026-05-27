package model;

import java.util.List;

/**
 * Equipment that contributes attack. All weapon variance (sprite, base attack,
 * ranged flag, display name) lives on a shared {@link WeaponType} flyweight so
 * a single {@code Weapon} class covers the whole catalog.
 */
public class Weapon extends Item {

    private final WeaponType type;

    public Weapon(WeaponType type) {
        super(type.displayName());
        this.type = type;
    }

    public WeaponType getType() {
        return type;
    }

    public int getAtkValue() {
        return type.baseAttack();
    }

    public boolean isRanged() {
        return type.ranged();
    }

    @Override
    public String spriteResource() {
        return type.spritePath();
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.EQUIP, ItemAction.DISCARD);
    }
}
