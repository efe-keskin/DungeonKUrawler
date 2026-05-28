package model;

public class Hole extends SearchableObject {

    public static final String SPRITE =
            "/background_floor/assets/searchable assets/20_wall_detail_growth_green.png";

    public Hole() {
        this(null);
    }

    public Hole(Item hiddenItem) {
        super("Hole", true, SPRITE, hiddenItem);
    }
}
