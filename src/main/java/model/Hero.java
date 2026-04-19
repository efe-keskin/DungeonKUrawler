package model;

/**
 * Player-controlled character. Carries an {@link Inventory} with a fixed maximum of 8 items.
 */
public class Hero extends Entity {

    private int hp;
    private final int maxHp;
    private int str;
    private int mana;
    private int def;
    private int energy;
    private final int maxEnergy;
    /** Strict capacity of 8 — enforced by {@link Inventory}. */
    private final Inventory inventory;

    public Hero(int x, int y, String name, int hp, int str, int mana, int def, int energy) {
        super(x, y, name);
        this.hp = hp;
        this.maxHp = hp;
        this.str = str;
        this.mana = mana;
        this.def = def;
        this.energy = energy;
        this.maxEnergy = energy;
        this.inventory = new Inventory(8);
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getMaxEnergy() {
        return maxEnergy;
    }

    public int getHp() {
        return hp;
    }

    public int getStr() {
        return str;
    }

    public int getMana() {
        return mana;
    }

    public int getDef() {
        return def;
    }

    public int getEnergy() {
        return energy;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void moveTo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void consumeEnergy(int amount) {
        this.energy = Math.max(0, this.energy - amount);
    }

    public void gainEnergy(int amount) {
        if (amount <= 0) {
            return;
        }
        this.energy = Math.min(maxEnergy, this.energy + amount);
    }

    public void gainOverflowEnergy(int amount) {
        if (amount <= 0) {
            return;
        }
        this.energy += amount;
    }

    /**
     * Capped heal — will not push HP past {@link #getMaxHp()}.
     */
    public void heal(int amount) {
        if (amount <= 0 || this.hp >= maxHp) {
            return;
        }
        this.hp = Math.min(maxHp, this.hp + amount);
    }

    public void restoreFullHealth() {
        this.hp = maxHp;
    }

    public void takeDamage(int amount) {
        if (amount <= 0) {
            return;
        }
        this.hp = Math.max(0, this.hp - amount);
    }
}
