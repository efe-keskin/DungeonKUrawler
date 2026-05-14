package view.assets;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * Singleton image cache (GoF Singleton + Flyweight). Loads each resource at
 * most once and serves the cached {@link BufferedImage} on subsequent requests.
 *
 * <p>Returns {@code null} when a resource is missing — callers are expected to
 * fall back to a colored marker (see {@link view.GamePanel}). Missing-path
 * results are also cached as a {@code null} sentinel so the lookup stays cheap.
 */
public final class AssetManager {

    private static final AssetManager INSTANCE = new AssetManager();

    public static AssetManager get() {
        return INSTANCE;
    }

    private final Map<String, BufferedImage> imageCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> missing = new ConcurrentHashMap<>();
    private final Map<ScaledKey, ImageIcon> iconCache = new HashMap<>();

    private AssetManager() {
    }

    public BufferedImage image(AssetId id) {
        return image(id.resourcePath());
    }

    /**
     * First non-null image among {@code primary}/{@code fallbacks}. Useful when a
     * preferred sprite may be missing (e.g. sorcerer → wizard).
     */
    public BufferedImage imageOrFallback(AssetId primary, AssetId... fallbacks) {
        BufferedImage img = image(primary);
        if (img != null) {
            return img;
        }
        for (AssetId f : fallbacks) {
            img = image(f);
            if (img != null) {
                return img;
            }
        }
        return null;
    }

    /**
     * Load an arbitrary classpath resource. Mostly used by {@link AmbienceCatalog}
     * where the path is data-driven and an {@link AssetId} would be impractical.
     */
    public BufferedImage image(String resourcePath) {
        if (resourcePath == null || resourcePath.isEmpty()) {
            return null;
        }
        BufferedImage cached = imageCache.get(resourcePath);
        if (cached != null) {
            return cached;
        }
        if (Boolean.TRUE.equals(missing.get(resourcePath))) {
            return null;
        }
        try (InputStream in = AssetManager.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                missing.put(resourcePath, Boolean.TRUE);
                return null;
            }
            BufferedImage img = ImageIO.read(in);
            if (img == null) {
                missing.put(resourcePath, Boolean.TRUE);
                return null;
            }
            imageCache.put(resourcePath, img);
            return img;
        } catch (Exception e) {
            missing.put(resourcePath, Boolean.TRUE);
            return null;
        }
    }

    /**
     * Smoothly-scaled {@link ImageIcon} for Swing components (cached). Returns
     * {@code null} if the underlying image is missing.
     */
    public synchronized ImageIcon icon(AssetId id, int width, int height) {
        ScaledKey key = new ScaledKey(id.resourcePath(), width, height);
        ImageIcon cached = iconCache.get(key);
        if (cached != null) {
            return cached;
        }
        BufferedImage src = image(id);
        if (src == null) {
            return null;
        }
        Image scaled = src.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        ImageIcon icon = new ImageIcon(scaled);
        iconCache.put(key, icon);
        return icon;
    }

    private record ScaledKey(String path, int width, int height) {
    }
}
