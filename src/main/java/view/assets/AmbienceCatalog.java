package view.assets;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Catalog of dungeon ambience tiles described by
 * {@code resources/background_floor/sizes.csv}.
 *
 * <p>Loaded lazily on first access. Lookups are by label (e.g. {@code "torch_lit_01"})
 * or category (e.g. {@code "wall_section"}). Image bytes are not held here —
 * fetch the {@link java.awt.image.BufferedImage} through
 * {@link AmbienceAsset#image()}, which routes through {@link AssetManager}.
 */
public final class AmbienceCatalog {

    private static final String CSV_PATH = "/background_floor/sizes.csv";

    private static final AmbienceCatalog INSTANCE = new AmbienceCatalog();

    public static AmbienceCatalog get() {
        return INSTANCE;
    }

    private final Map<String, AmbienceAsset> byLabel;
    private final Map<String, List<AmbienceAsset>> byCategory;

    private AmbienceCatalog() {
        Map<String, AmbienceAsset> labels = new LinkedHashMap<>();
        Map<String, List<AmbienceAsset>> categories = new LinkedHashMap<>();
        load(labels, categories);
        this.byLabel = Collections.unmodifiableMap(labels);
        Map<String, List<AmbienceAsset>> frozen = new LinkedHashMap<>();
        categories.forEach((k, v) -> frozen.put(k, Collections.unmodifiableList(v)));
        this.byCategory = Collections.unmodifiableMap(frozen);
    }

    private static void load(Map<String, AmbienceAsset> labels,
            Map<String, List<AmbienceAsset>> categories) {
        try (InputStream in = AmbienceCatalog.class.getResourceAsStream(CSV_PATH);
                BufferedReader reader = in == null ? null
                        : new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            if (reader == null) {
                return;
            }
            // Header row: index,filename,label,category,output_width,output_height
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length < 6) {
                    continue;
                }
                try {
                    int idx = Integer.parseInt(parts[0].trim());
                    String filename = parts[1].trim();
                    String label = parts[2].trim();
                    String category = parts[3].trim();
                    int w = Integer.parseInt(parts[4].trim());
                    int h = Integer.parseInt(parts[5].trim());
                    AmbienceAsset asset = new AmbienceAsset(idx, filename, label, category, w, h);
                    labels.put(label, asset);
                    categories.computeIfAbsent(category, k -> new ArrayList<>()).add(asset);
                } catch (NumberFormatException ignored) {
                    // Skip malformed rows rather than failing catalog construction.
                }
            }
        } catch (Exception ignored) {
            // Empty catalog if the CSV cannot be read — callers degrade gracefully.
        }
    }

    public AmbienceAsset byLabel(String label) {
        return byLabel.get(label);
    }

    public List<AmbienceAsset> byCategory(String category) {
        return byCategory.getOrDefault(category, List.of());
    }

    public Collection<AmbienceAsset> all() {
        return byLabel.values();
    }

    public Collection<String> categories() {
        return byCategory.keySet();
    }
}
