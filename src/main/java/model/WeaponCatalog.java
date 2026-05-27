package model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Loads and serves {@link WeaponType} definitions from
 * {@code /weapons/weapon_catalog.csv} (GRASP Pure Fabrication — the catalog
 * isn't a domain object, it exists so domain code doesn't carry CSV parsing).
 * Each row produces one shared {@link WeaponType}; lookups are by ID or
 * category. The CSV is the single source of truth for sprite + name + category.
 *
 * <p>The CSV does not encode combat stats, so {@code baseAttack}/{@code ranged}
 * are derived from {@link #STATS_BY_CATEGORY}. Adjust there if categories
 * should hit harder or be ranged.
 */
public final class WeaponCatalog {

    private static final String CSV_RESOURCE = "/weapons/weapon_catalog.csv";

    private record CategoryStats(int baseAttack, boolean ranged) {
    }

    private static final Map<String, CategoryStats> STATS_BY_CATEGORY = Map.of(
            "swords", new CategoryStats(3, false),
            "daggers", new CategoryStats(2, false),
            "axes", new CategoryStats(4, false),
            "maces", new CategoryStats(4, false),
            "polearms", new CategoryStats(4, false),
            "bows", new CategoryStats(3, true),
            "staves", new CategoryStats(2, true),
            "tools", new CategoryStats(1, false));

    private static final CategoryStats DEFAULT_STATS = new CategoryStats(2, false);

    private static volatile WeaponCatalog instance;

    public static WeaponCatalog get() {
        WeaponCatalog local = instance;
        if (local == null) {
            synchronized (WeaponCatalog.class) {
                local = instance;
                if (local == null) {
                    local = new WeaponCatalog();
                    instance = local;
                }
            }
        }
        return local;
    }

    private final Map<String, WeaponType> byId = new HashMap<>();
    private final Map<String, List<WeaponType>> byCategory = new HashMap<>();
    private final List<WeaponType> all = new ArrayList<>();

    private WeaponCatalog() {
        loadFromCsv();
    }

    public WeaponType byId(String id) {
        return byId.get(id);
    }

    public List<WeaponType> byCategory(String category) {
        return byCategory.getOrDefault(category, List.of());
    }

    public List<WeaponType> all() {
        return Collections.unmodifiableList(all);
    }

    /**
     * A random weapon in the given category, or {@code null} if the category
     * has no entries. Useful for populating dungeon spawns.
     */
    public WeaponType randomIn(String category, Random rng) {
        List<WeaponType> options = byCategory(category);
        if (options.isEmpty()) {
            return null;
        }
        return options.get(rng.nextInt(options.size()));
    }

    private void loadFromCsv() {
        try (InputStream in = WeaponCatalog.class.getResourceAsStream(CSV_RESOURCE);
             BufferedReader reader = in == null ? null
                     : new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            if (reader == null) {
                return;
            }
            String header = reader.readLine();
            if (header == null) {
                return;
            }
            String line;
            while ((line = reader.readLine()) != null) {
                WeaponType type = parseRow(line);
                if (type == null) {
                    continue;
                }
                byId.put(type.id(), type);
                byCategory.computeIfAbsent(type.category(), k -> new ArrayList<>()).add(type);
                all.add(type);
            }
        } catch (IOException ignored) {
            // Missing/corrupt catalog → empty registry; callers handle null lookups.
        }
    }

    private static WeaponType parseRow(String line) {
        // Only the first four columns are needed (ID, Filename, Suggested label,
        // Category); later columns may contain commas (Notes) but we ignore them.
        String[] cols = line.split(",", 5);
        if (cols.length < 4) {
            return null;
        }
        String id = cols[0].trim();
        String filename = cols[1].trim();
        String label = cols[2].trim();
        String category = cols[3].trim().toLowerCase(Locale.ROOT);
        if (id.isEmpty() || filename.isEmpty() || category.isEmpty()) {
            return null;
        }
        String spritePath = "/weapons/" + category + "/" + filename;
        CategoryStats stats = STATS_BY_CATEGORY.getOrDefault(category, DEFAULT_STATS);
        return new WeaponType(id, titleCase(label), category, spritePath,
                stats.baseAttack(), stats.ranged());
    }

    private static String titleCase(String raw) {
        if (raw.isEmpty()) {
            return raw;
        }
        StringBuilder out = new StringBuilder(raw.length());
        boolean atWordStart = true;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isWhitespace(c)) {
                out.append(c);
                atWordStart = true;
            } else if (atWordStart) {
                out.append(Character.toUpperCase(c));
                atWordStart = false;
            } else {
                out.append(Character.toLowerCase(c));
            }
        }
        return out.toString();
    }
}
