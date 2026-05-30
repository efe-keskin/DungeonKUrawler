package model;

public class Grill extends SearchableObject {

    public static final String HORIZONTAL_SPRITE =
            "/background_floor/assets/searchable assets/grill1.png";
    public static final String VERTICAL_SPRITE =
            "/background_floor/assets/searchable assets/grill2.png";

    public Grill() {
        this(null);
    }

    public Grill(Item hiddenItem) {
        this(HORIZONTAL_SPRITE, hiddenItem);
    }

    public Grill(String spriteResource, Item hiddenItem) {
        super("Grill", true, spriteResource, hiddenItem);
    }
}
