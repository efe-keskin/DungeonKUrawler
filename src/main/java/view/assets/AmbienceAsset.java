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

    /** Classpath path of the underlying PNG. */
    public String resourcePath() {
        return "/background_floor/assets/" + filename;
    }

    /** Cached image, or {@code null} if the file is missing. */
    public BufferedImage image() {
        return AssetManager.get().image(resourcePath());
    }
}
