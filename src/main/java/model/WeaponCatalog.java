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
 * <p>Per-weapon attack and rarity now live in the CSV ({@code Attack},
 * {@code Rarity}, {@code Ranged} columns). {@link #STATS_BY_CATEGORY} only
 * supplies fallbacks when a row omits them — notably which categories are
 * ranged by default. Adjust the CSV to retune individual weapons.
 */
public final class WeaponCatalog {

    private static final String CSV_RESOURCE = "/weapons/weapon_catalog.csv";

    private record CategoryStats(int baseAttack, boolean ranged) {
    }

    // Fallback stats used only when a CSV row lacks an explicit attack/ranged
    // value. "ranged" here is what decides bows/wands fire projectiles.
    private static final Map<String, CategoryStats> STATS_BY_CATEGORY = Map.ofEntries(
            Map.entry("swords", new CategoryStats(3, false)),
            Map.entry("daggers", new CategoryStats(2, false)),
            Map.entry("axes", new CategoryStats(4, false)),
            Map.entry("hammers", new CategoryStats(3, false)),
            Map.entry("maces", new CategoryStats(3, false)),
            Map.entry("polearms", new CategoryStats(4, false)),
            Map.entry("bows", new CategoryStats(6, true)),
            Map.entry("wands", new CategoryStats(8, true)),
            Map.entry("staves", new CategoryStats(2, true)),
            Map.entry("tools", new CategoryStats(1, false)));

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
        registerB23RangedWeapons();
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
     * has no entries. The pick is weighted by {@link Rarity#spawnWeight()} so
     * common (weak) weapons surface far more often than legendary (powerful)
     * ones — rarity is conversely related to attack. Useful for dungeon spawns.
     */
    public WeaponType randomIn(String category, Random rng) {
        List<WeaponType> options = byCategory(category);
        if (options.isEmpty()) {
            return null;
        }
        int total = 0;
        for (WeaponType type : options) {
            total += type.rarity().spawnWeight();
        }
        if (total <= 0) {
            return options.get(rng.nextInt(options.size()));
        }
        int roll = rng.nextInt(total);
        for (WeaponType type : options) {
            roll -= type.rarity().spawnWeight();
            if (roll < 0) {
                return type;
            }
        }
        return options.get(options.size() - 1);
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
        // Columns: ID, Filename, Suggested label, Category, Attack, Rarity, Ranged.
        // Attack/Rarity/Ranged are optional — missing values fall back to the
        // category defaults so older/hand-edited rows still load.
        String[] cols = line.split(",", -1);
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
        int attack = parseIntOr(cols, 4, stats.baseAttack());
        Rarity rarity = cols.length > 5 ? Rarity.fromString(cols[5]) : Rarity.COMMON;
        boolean ranged = cols.length > 6 && !cols[6].isBlank()
                ? Boolean.parseBoolean(cols[6].trim())
                : stats.ranged();
        return new WeaponType(id, titleCase(label), category, spritePath, attack, ranged, rarity);
    }

    private static int parseIntOr(String[] cols, int index, int fallback) {
        if (cols.length <= index || cols[index].isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(cols[index].trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    /** B23 hero ranged weapons with explicit resource costs and projectile styles. */
    private void registerB23RangedWeapons() {
        registerWeapon(new WeaponType(
                "B23_BOW",
                "Wooden Bow",
                "bows",
                "/weapons/bows/052_red_brown_bow.png",
                6,
                true,
                4,
                RangedCostType.ENERGY,
                3,
                HeroProjectileStyle.ARROW));
        registerWeapon(new WeaponType(
                "B23_WAND",
                "Magic Wand",
                "wands",
                "/weapons/wands/047_large_silver_wand.png",
                8,
                true,
                4,
                RangedCostType.MANA,
                5,
                HeroProjectileStyle.ICE_BOLT));
    }

    private void registerWeapon(WeaponType type) {
        byId.put(type.id(), type);
        byCategory.computeIfAbsent(type.category(), k -> new ArrayList<>()).add(type);
        all.add(type);
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
