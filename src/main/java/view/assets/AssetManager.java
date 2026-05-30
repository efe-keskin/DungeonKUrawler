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
        String resolvedPath = resolveResourcePath(resourcePath);
        BufferedImage cached = imageCache.get(resolvedPath);
        if (cached != null) {
            return cached;
        }
        if (Boolean.TRUE.equals(missing.get(resolvedPath))) {
            return null;
        }
        try (InputStream in = AssetManager.class.getResourceAsStream(resolvedPath)) {
            if (in == null) {
                missing.put(resolvedPath, Boolean.TRUE);
                return null;
            }
            BufferedImage img = ImageIO.read(in);
            if (img == null) {
                missing.put(resolvedPath, Boolean.TRUE);
                return null;
            }
            imageCache.put(resolvedPath, img);
            return img;
        } catch (Exception e) {
            missing.put(resolvedPath, Boolean.TRUE);
            return null;
        }
    }

    /**
     * Resource folders were reorganized late in the project. Resolve both the new
     * canonical paths and old saved-map paths so user maps keep loading.
     */
    public static String resolveResourcePath(String resourcePath) {
        if (resourcePath == null || resourcePath.isEmpty()) {
            return resourcePath;
        }
        String path = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        String direct = DIRECT_ALIASES.get(path);
        if (direct != null) {
            return direct;
        }
        if (path.startsWith("/items_objects/valuable_items/")) {
            return path.replace("/items_objects/valuable_items/", "/items/valuable_items/");
        }
        if (path.startsWith("/items_keys_extracted/assets/")) {
            return relocateKeySheetItem(path.substring("/items_keys_extracted/assets/".length()));
        }
        if (path.startsWith("/chest_models_plus_crates_sacks_v6/assets/")) {
            return relocateContainerItem(path.substring("/chest_models_plus_crates_sacks_v6/assets/".length()));
        }
        if (path.startsWith("/background_floor/assets/searchable assets/")) {
            return relocateSearchableBackground(
                    path.substring("/background_floor/assets/searchable assets/".length()));
        }
        if (path.startsWith("/background_floor/assets/")) {
            return relocateBackgroundAsset(path.substring("/background_floor/assets/".length()));
        }
        if (path.startsWith("/items/assets/")) {
            return path.replace("/items/assets/", "/items/");
        }
        return path;
    }

    private static final Map<String, String> DIRECT_ALIASES = Map.ofEntries(
            Map.entry("/Inventory x4.png", "/inventorychest.png"),
            Map.entry("/items_objects/healpotion.png", "/items/potions/07_potion_red.png"),
            Map.entry("/items_objects/manapotion.png", "/items/potions/08_potion_blue.png"),
            Map.entry("/items_objects/energypotion.png", "/items/potions/09_potion_green.png"),
            Map.entry("/characters/sorcerer1.png", "/characters/wizard1.png"),
            Map.entry("/chest_models_plus_crates_sacks_v6/bag - empty.png", "/inventorychest.png"),
            Map.entry("/items/crates/16_crate_wood_tall_right.png",
                    "/background_floor/assets/searchable assets/crates/16_crate_wood_tall_right.png"),
            Map.entry("/items/crates/17_crate_wood_tall_corrected.png",
                    "/background_floor/assets/searchable assets/crates/17_crate_wood_tall_corrected.png"),
            Map.entry("/items/crates/18_crate_orange_tall_corrected.png",
                    "/background_floor/assets/searchable assets/crates/18_crate_orange_tall_corrected.png"));

    private static String relocateKeySheetItem(String filename) {
        return switch (filename) {
            case "01_key_olive.png", "02_key_silver.png", "03_key_gold.png", "04_key_orange.png",
                    "05_key_bent_silver.png", "06_key_long_gold.png" -> "/items/keys/" + filename;
            case "07_potion_red.png", "08_potion_blue.png", "09_potion_green.png" -> "/items/potions/" + filename;
            case "10_ring_red_gem.png", "11_ring_green_gem.png", "12_ring_blue_gem.png" -> "/items/rings/" + filename;
            case "21_ring_blue_white.png" -> "/items/rings/12_ring_blue_gem.png";
            case "13_skull_beige.png", "14_skull_dark.png" -> "/items/skulls/" + filename;
            case "15_coin_gold_single.png", "16_gem_white.png", "17_bar_orange.png", "18_bar_gold_orange.png",
                    "19_nugget_gold.png", "20_coin_pile_gold.png" -> "/items/golds_coins/" + filename;
            case "22_book_red.png" -> "/items/books/" + filename;
            case "23_tombstone_crack.png", "24_tombstone_lines.png", "25_tombstone_cross.png",
                    "26_tombstone_skull.png" -> "/items/tombstones/" + filename;
            default -> "/items/" + filename;
        };
    }

    private static String relocateContainerItem(String filename) {
        String prefix = filename.length() >= 2 ? filename.substring(0, 2) : "";
        return switch (prefix) {
            case "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14",
                    "15" -> "/items/chests/" + filename;
            case "16", "17", "18" -> "/background_floor/assets/searchable assets/crates/" + filename;
            case "19", "20" -> "/items/bags/" + filename;
            default -> "/items/" + filename;
        };
    }

    private static String relocateSearchableBackground(String filename) {
        return switch (filename) {
            case "10_wall_column_round_top.png", "column3.png" ->
                    "/background_floor/assets/breakable assets/column3.png";
            case "20_wall_detail_growth_green.png" -> "/background_floor/assets/searchable assets/hole1.png";
            case "21_wall_detail_grate_horizontal_blue.png", "grill.png" ->
                    "/background_floor/assets/searchable assets/grill1.png";
            case "22_wall_detail_grate_vertical_blue.png" -> "/background_floor/assets/searchable assets/grill2.png";
            case "23_wall_detail_ring_small_blue.png", "24_wall_detail_ring_large_blue.png",
                    "25_wall_detail_ring_teardrop_blue.png", "water_pipe.png" ->
                    "/background_floor/assets/breakable assets/water_pipes.png";
            case "water_pipes.png" -> "/background_floor/assets/breakable assets/water_pipes.png";
            case "26_wall_detail_drip_red_left.png", "26_wall_detail_drip_red_left(below).png",
                    "29_wall_detail_drip_red_mid.png",
                    "34_wall_detail_drip_red_right.png", "gargoyle_pool_red.png" ->
                    "/background_floor/assets/searchable assets/gargoyle_red.png";
            case "27_wall_detail_drip_green_left.png", "30_wall_detail_drip_green_mid.png",
                    "32_wall_detail_drip_green_right.png", "gargoyle_pool_green.png" ->
                    "/background_floor/assets/searchable assets/gargoyle_green.png";
            case "28_wall_detail_drip_cyan_left(above).png", "28_wall_detail_drip_cyan_left.png",
                    "31_wall_detail_drip_cyan_mid.png", "33_wall_detail_drip_cyan_right.png",
                    "gargoyle_pool_blue.png" -> "/background_floor/assets/searchable assets/gargoyle_blue.png";
            case "39_pillar_purple.png", "column1.png" ->
                    "/background_floor/assets/breakable assets/column1.png";
            case "40_pillar_gray.png", "column2.png" ->
                    "/background_floor/assets/breakable assets/column2.png";
            default -> "/background_floor/assets/searchable assets/" + filename;
        };
    }

    private static String relocateBackgroundAsset(String filename) {
        if (filename.contains("/")) {
            return "/background_floor/assets/" + filename;
        }
        String prefix = filename.length() >= 2 ? filename.substring(0, 2) : "";
        return switch (prefix) {
            case "01", "11", "12", "13", "14", "16", "18" -> "/background_floor/assets/walls/" + filename;
            case "02", "05", "06", "08", "09" -> "/background_floor/assets/walls/01_wall_section_top_plain_left.png";
            case "03", "04", "07" -> "/background_floor/assets/floors/" + filename;
            case "19" -> "/background_floor/assets/floors/04_floor_worn_patch_round.png";
            case "10", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32",
                    "33", "34", "39", "40" -> relocateSearchableBackground(filename);
            case "15" -> "/background_floor/assets/doors/" + filename;
            case "17" -> "/background_floor/assets/doors/17_door_open_wood.png";
            case "35", "36", "37", "38" -> "/background_floor/assets/banners/" + filename;
            case "41", "42", "43", "44", "45", "46", "47", "48" -> "/background_floor/assets/rugs/" + filename;
            case "49", "50", "51", "52", "53", "54", "55", "56", "57" -> "/background_floor/assets/torches/" + filename;
            case "58" -> "/background_floor/assets/trap_floors/58_trap_floor.png";
            case "59", "60", "63" -> "/background_floor/assets/stairs/" + filename;
            case "61", "62" -> "/background_floor/assets/trap_floors/" + filename;
            case "64" -> "/background_floor/assets/trap_floors/61_trap_floor_holes.png";
            case "65" -> "/background_floor/assets/trap_floors/62_trap_floor_spikes.png";
            case "66", "67" -> "/background_floor/assets/signs/" + filename;
            default -> "/background_floor/assets/" + filename;
        };
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
