package model;

/**
 * The stat a ring improves while it is worn.
 */
public enum RingEffectType {
    STRENGTH("Power", "STR"),
    DEFENSE("Protection", "DEF"),
    MANA("Mana", "MANA"),
    ENERGY("Energy", "ENERGY");

    private final String displayName;
    private final String statLabel;

    RingEffectType(String displayName, String statLabel) {
        this.displayName = displayName;
        this.statLabel = statLabel;
    }

    public String displayName() {
        return displayName;
    }

    public String statLabel() {
        return statLabel;
    }
}
