package model;

public class WaterPipe extends SearchableObject {

    public static final String SMALL_RING_SPRITE =
            "/background_floor/assets/searchable assets/23_wall_detail_ring_small_blue.png";
    public static final String LARGE_RING_SPRITE =
            "/background_floor/assets/searchable assets/24_wall_detail_ring_large_blue.png";
    public static final String TEARDROP_RING_SPRITE =
            "/background_floor/assets/searchable assets/25_wall_detail_ring_teardrop_blue.png";

    public WaterPipe() {
        this(null);
    }

    public WaterPipe(Item hiddenItem) {
        this(LARGE_RING_SPRITE, hiddenItem);
    }

    public WaterPipe(String spriteResource, Item hiddenItem) {
        super("Water Pipe", true, spriteResource, hiddenItem);
    }
}
