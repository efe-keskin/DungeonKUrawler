package model;

/**
 * Actions available for an item after it has been collected into inventory.
 */
public enum ItemAction {
    DRINK("DRINK"),
    WEAR("WEAR"),
    EQUIP("EQUIP"),
    READ("READ"),
    REMOVE("REMOVE"),
    DISCARD("DISCARD"),
    SEARCH("SEARCH"),
    BREAK("BREAK"),
    OPEN("OPEN"),
    EAT("EAT"),
    CAST("CAST");

    private final String label;

    ItemAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
