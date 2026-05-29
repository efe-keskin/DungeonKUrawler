package model;

import java.util.List;

/**
 * A persistent companion bought from the shop. Pets live in the run-wide
 * {@link FullGameInventory} (not the per-level bag) so they survive between
 * floors. Only one pet is the hero's active companion at a time
 * ({@link Hero#getEquippedPet()}).
 *
 * <p>This is the persistent <em>data</em> side (HP, state, sprite). The in-floor
 * presence on the grid is a transient {@code PetEntity}, and behaviour
 * (roaming, abilities, taking damage) lives in the engine's {@code PetController}
 * / {@code GameEngine}, keeping the model free of engine dependencies.
 */
public abstract class Pet extends Item {

    /** Shared baseline used by the concrete pets. */
    public static final int DEFAULT_MAX_HP = 10;

    private final int maxHp;
    private final int followRange;
    private final String spritePath;
    private int hp;
    private PetState state = PetState.UNEQUIPPED;

    protected Pet(String name, String spritePath, int maxHp, int followRange) {
        super(name);
        this.maxHp = Math.max(1, maxHp);
        this.hp = this.maxHp;
        this.followRange = Math.max(1, followRange);
        this.spritePath = spritePath;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = Math.max(0, Math.min(maxHp, hp));
    }

    public int getMaxHp() {
        return maxHp;
    }

    /** How far the pet may trail the hero (matches knight roam range). */
    public int getFollowRange() {
        return followRange;
    }

    public PetState getState() {
        return state;
    }

    public void setState(PetState state) {
        this.state = state;
    }

    public boolean isAlive() {
        return hp > 0;
    }

    /**
     * Applies damage; faints the pet (state {@code FAINTED}) when HP hits 0.
     *
     * @return the amount of HP actually lost.
     */
    public int takeDamage(int amount) {
        if (amount <= 0 || hp <= 0) {
            return 0;
        }
        int before = hp;
        hp = Math.max(0, hp - amount);
        if (hp == 0) {
            state = PetState.FAINTED;
        }
        return before - hp;
    }

    /** Restores full vitals and marks the pet active — used when (re)spawning on a floor. */
    public void revive() {
        this.hp = maxHp;
        this.state = PetState.ACTIVE;
    }

    @Override
    public String spriteResource() {
        return spritePath;
    }

    /** Pets are never discardable from a bag UI; they are managed via equip/sell. */
    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of();
    }
}
