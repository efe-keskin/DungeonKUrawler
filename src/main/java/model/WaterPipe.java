package model;

public class WaterPipe extends BreakableObject {

    public static final String SMALL_RING_SPRITE =
            "/background_floor/assets/breakable assets/water_pipes.png";
    public static final String LARGE_RING_SPRITE =
            "/background_floor/assets/breakable assets/water_pipes.png";
    public static final String TEARDROP_RING_SPRITE =
            "/background_floor/assets/breakable assets/water_pipes.png";

    public WaterPipe() {
        this(LARGE_RING_SPRITE);
    }

    public WaterPipe(String spriteResource) {
        super("Water Pipe", true, spriteResource);
    }
}
