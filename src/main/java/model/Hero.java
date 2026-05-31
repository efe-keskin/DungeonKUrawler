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
    /** Run-wide persistent store: gold + valuables + shop purchases. */
    private final FullGameInventory fullInventory = new FullGameInventory();
    /** Per-level bag, strict capacity of 8 — enforced by {@link Inventory}. */
    private final Inventory inventory;
    private Armor equippedArmor;
    private Weapon equippedWeapon;
    private Ring equippedRing;
    private long lastAttackTimeMs;
    /** Active companion (persistent; lives in {@link #fullInventory}). */
    private Pet equippedPet;

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
        this.inventory = new InGameInventory(8);
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getMaxEnergy() {
        int ringBonus = equippedRing == null ? 0 : equippedRing.getEnergyBonus();
        return maxEnergy + ringBonus;
    }

    public int getBaseMaxEnergy() {
        return maxEnergy;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getStr() {
        int ringBonus = equippedRing == null ? 0 : equippedRing.getStrBonus();
        return str + ringBonus;
    }

    public int getBaseStr() {
        return str;
    }

    public void setStr(int str) {
        this.str = str;
    }

    public int getMana() {
        return mana;
    }

    public int getMaxMana() {
        int ringBonus = equippedRing == null ? 0 : equippedRing.getManaBonus();
        return maxMana + ringBonus;
    }

    public int getBaseMaxMana() {
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

    /** The run-wide persistent inventory (gold + valuables + purchases). */
    public FullGameInventory getFullInventory() {
        return fullInventory;
    }

    /** The active companion, or {@code null} if none is equipped. */
    public Pet getEquippedPet() {
        return equippedPet;
    }

    public void setEquippedPet(Pet equippedPet) {
        this.equippedPet = equippedPet;
    }

    /**
     * UC-4 commit: moves the valuables collected this floor out of the per-level
     * bag and into the persistent inventory. Coins already live in the gold
     * balance; remaining temporary items are dropped when the bag is discarded.
     */
    public void commitLevelLoot() {
        if (inventory instanceof InGameInventory bag) {
            fullInventory.addAll(bag.drainValuables());
        }
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
        if (equippedWeapon != null && equippedWeapon != weapon) {
            equippedWeapon = null;
        }
        equippedWeapon = weapon;
        return true;
    }

    public boolean wearRing(Ring ring) {
        if (ring == null || !inventory.getItems().contains(ring)) {
            return false;
        }
        int oldMaxMana = getMaxMana();
        int oldMaxEnergy = getMaxEnergy();
        equippedRing = ring;
        clampLoweredResourceCaps(oldMaxMana, oldMaxEnergy);
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
            int oldMaxMana = getMaxMana();
            int oldMaxEnergy = getMaxEnergy();
            equippedRing = null;
            clampLoweredResourceCaps(oldMaxMana, oldMaxEnergy);
            return true;
        }
        return false;
    }

    private void clampLoweredResourceCaps(int oldMaxMana, int oldMaxEnergy) {
        if (getMaxMana() < oldMaxMana) {
            mana = Math.min(mana, getMaxMana());
        }
        if (getMaxEnergy() < oldMaxEnergy) {
            energy = Math.min(energy, getMaxEnergy());
        }
    }

    public int getCoinBalance() {
        return fullInventory.getGold();
    }

    public void setCoinBalance(int coinBalance) {
        fullInventory.setGold(coinBalance);
    }

    /**
     * Adds a positive reward to the hero's coin balance.
     *
     * @return true when the balance changed; false for zero or negative rewards.
     */
    public boolean earnCoins(int amount) {
        return fullInventory.earn(amount);
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

    public boolean spendEnergy(int amount) {
        if (amount <= 0 || this.energy < amount) {
            return false;
        }
        this.energy -= amount;
        return true;
    }

    public long getLastAttackTimeMs() {
        return lastAttackTimeMs;
    }

    public void setLastAttackTimeMs(long lastAttackTimeMs) {
        this.lastAttackTimeMs = lastAttackTimeMs;
    }
    
}
