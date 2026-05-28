package model;

public class Pedestal extends SearchableObject {

    public Pedestal() {
        this(null);
    }

    public Pedestal(Item hiddenItem) {
        super("Stone Pedestal", true, null, hiddenItem);
    }
}
