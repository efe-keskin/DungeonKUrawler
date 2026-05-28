package model;

/**
 * Player-controlled character. Carries an {@link Inventory} with a fixed maximum of 8 items.
 */
public class Hero extends Entity {

    private int hp;
    private final int maxHp;
    private int str;
    private int mana;
    private final int maxMana;
    private int def;
    private int energy;
    private final int maxEnergy;
    private int coinBalance;
    /** Strict capacity of 8 — enforced by {@link Inventory}. */
    private final Inventory inventory;
    private Armor equippedArmor;
    private Weapon equippedWeapon;
    private Ring equippedRing;

    public Hero(int x, int y, String name, int hp, int str, int mana, int def, int energy) {
        super(x, y, name);
        this.hp = hp;
        this.maxHp = hp;
        this.str = str;
        this.mana = mana;
        this.maxMana = mana;
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

    public int getMaxMana() {
        return maxMana;
    }

    public void setMana(int mana) {
        this.mana = mana;
    }

    public int getDef() {
        int armorBonus = equippedArmor == null ? 0 : equippedArmor.getDefModifier();
        int ringBonus = equippedRing == null ? 0 : equippedRing.getDefBonus();
        return def + armorBonus + ringBonus;
    }

    public int getBaseDef() {
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

    public Armor getEquippedArmor() {
        return equippedArmor;
    }

    public Weapon getEquippedWeapon() {
        return equippedWeapon;
    }

    public Ring getEquippedRing() {
        return equippedRing;
    }

    public boolean wearArmor(Armor armor) {
        if (armor == null || !inventory.getItems().contains(armor)) {
            return false;
        }
        equippedArmor = armor;
        return true;
    }

    public boolean equipWeapon(Weapon weapon) {
        if (weapon == null || !inventory.getItems().contains(weapon)) {
            return false;
        }
        equippedWeapon = weapon;
        return true;
    }

    public boolean wearRing(Ring ring) {
        if (ring == null || !inventory.getItems().contains(ring)) {
            return false;
        }
        equippedRing = ring;
        return true;
    }

    public boolean isEquipped(Item item) {
        return item != null && (item == equippedArmor || item == equippedWeapon || item == equippedRing);
    }

    public boolean removeEquipment(Item item) {
        if (item == equippedArmor) {
            equippedArmor = null;
            return true;
        }
        if (item == equippedWeapon) {
            equippedWeapon = null;
            return true;
        }
        if (item == equippedRing) {
            equippedRing = null;
            return true;
        }
        return false;
    }

    public int getCoinBalance() {
        return coinBalance;
    }

    public void setCoinBalance(int coinBalance) {
        this.coinBalance = Math.max(0, coinBalance);
    }

    /**
     * Adds a positive reward to the hero's coin balance.
     *
     * @return true when the balance changed; false for zero or negative rewards.
     */
    public boolean earnCoins(int amount) {
        if (amount <= 0) {
            return false;
        }
        coinBalance += amount;
        return true;
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

    /**
     * Capped heal — will not push HP past {@link #getMaxHp()}.
     */
    public void heal(int amount) {
        if (amount <= 0 || this.hp >= maxHp) {
            return;
        }
        this.hp = Math.min(maxHp, this.hp + amount);
    }

    public void restoreMana(int amount) {
        if (amount <= 0 || this.mana >= maxMana) {
            return;
        }
        this.mana = Math.min(maxMana, this.mana + amount);
    }

    public boolean spendMana(int amount) {
        if (amount <= 0 || this.mana < amount) {
            return false;
        }
        this.mana -= amount;
        return true;
    }
    
}
