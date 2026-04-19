package model;

/**
 * Player-controlled character. Carries an {@link Inventory} with a fixed maximum of 8 items.
 */
public class Hero extends Entity {

    private int hp;
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
        this.str = str;
        this.mana = mana;
        this.def = def;
        this.energy = energy;
        this.maxEnergy = energy;
        this.inventory = new Inventory(8);
    }

    public int getMaxEnergy() {
        return maxEnergy;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getStr() {
        return str;
    }

    public void setStr(int str) {
        this.str = str;
    }

    public int getMana() {
        return mana;
    }

    public void setMana(int mana) {
        this.mana = mana;
    }

    public int getDef() {
        return def;
    }

    public void setDef(int def) {
        this.def = def;
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void updatePosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void consumeEnergy(int amount) {
        this.energy = Math.max(0, this.energy - amount);
    }

    public void refillEnergy(int amount) {
        if (amount <= 0) {
            return;
        }
        this.energy = Math.min(maxEnergy, this.energy + amount);
    }

    public void heal(int amount) {
        if (amount <= 0) {
            return;
        }
        this.hp = this.hp + amount;
    }
}
