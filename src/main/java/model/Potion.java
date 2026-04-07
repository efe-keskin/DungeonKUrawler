package model;

/**
 * Consumable that restores HP when used (combat rules in later phases).
 */
public class Potion extends Item {

    private int healAmount;

    public Potion(String name, int healAmount) {
        super(name);
        this.healAmount = healAmount;
    }

    public int getHealAmount() {
        return healAmount;
    }

    public void setHealAmount(int healAmount) {
        this.healAmount = healAmount;
    }
}
