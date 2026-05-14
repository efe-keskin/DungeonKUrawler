package view.assets;

import java.awt.image.BufferedImage;

/**
 * Immutable descriptor for a single tile in {@code background_floor/sizes.csv}.
 *
 * <p>{@code outputWidth} / {@code outputHeight} come straight from the CSV and
 * are the pixel sizes the artist intends the sprite to be drawn at, regardless
 * of the source PNG dimensions.
 */
public record AmbienceAsset(
        int index,
        String filename,
        String label,
        String category,
        int outputWidth,
        int outputHeight) {

    /**
     * Base pixel size of a single grid cell in the source artwork. The CSV's
     * {@code output_width}/{@code output_height} are expressed as multiples of
     * this unit (e.g. 96px = 3 cells, 160px = 5 cells).
     */
    public static final int GRID_UNIT_PX = 32;

    /** Classpath path of the underlying PNG. */
    public String resourcePath() {
        return "/background_floor/assets/" + filename;
    }

    /** Cached image, or {@code null} if the file is missing. */
    public BufferedImage image() {
        return AssetManager.get().image(resourcePath());
    }

    /** Grid cells this sprite covers horizontally (≥ 1). */
    public int cellsWide() {
        return Math.max(1, Math.round(outputWidth / (float) GRID_UNIT_PX));
    }

    /** Grid cells this sprite covers vertically (≥ 1). */
    public int cellsTall() {
        return Math.max(1, Math.round(outputHeight / (float) GRID_UNIT_PX));
    }
}
