package model;

import java.util.List;

/**
 * Wearable ring that improves one hero stat while equipped.
 */
public class Ring extends Item {

    private final RingEffectType effectType;
    private final int bonus;
    private final String spriteResource;

    public Ring(String name, int defBonus) {
        this(name, RingEffectType.DEFENSE, defBonus, null);
    }

    public Ring(String name, int defBonus, String spriteResource) {
        this(name, RingEffectType.DEFENSE, defBonus, spriteResource);
    }

    public Ring(String name, RingEffectType effectType, int bonus) {
        this(name, effectType, bonus, null);
    }

    public Ring(String name, RingEffectType effectType, int bonus, String spriteResource) {
        super(name);
        this.effectType = effectType == null ? RingEffectType.DEFENSE : effectType;
        this.bonus = bonus;
        this.spriteResource = spriteResource;
    }

    public RingEffectType getEffectType() {
        return effectType;
    }

    public int getBonus() {
        return bonus;
    }

    public int getDefBonus() {
        return bonusFor(RingEffectType.DEFENSE);
    }

    public int getStrBonus() {
        return bonusFor(RingEffectType.STRENGTH);
    }

    public int getManaBonus() {
        return bonusFor(RingEffectType.MANA);
    }

    public int getEnergyBonus() {
        return bonusFor(RingEffectType.ENERGY);
    }

    public String effectDescription() {
        return effectType.displayName() + " ring: +" + bonus + " " + effectType.statLabel();
    }

    private int bonusFor(RingEffectType type) {
        return effectType == type ? bonus : 0;
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.WEAR, ItemAction.DISCARD);
    }

    @Override
    public String spriteResource() {
        return spriteResource;
    }
}
