package model;

import java.util.Locale;

/**
 * Drop rarity of a weapon within its category. Rarity is conversely related to
 * attack: low-attack weapons are {@link #COMMON} and roll often, while the
 * hardest-hitting weapons are {@link #LEGENDARY} and rarely appear. The
 * {@code spawnWeight} feeds the weighted pick in {@link WeaponCatalog#randomIn}.
 */
public enum Rarity {
    COMMON(40),
    UNCOMMON(25),
    RARE(18),
    EPIC(12),
    LEGENDARY(5);

    private final int spawnWeight;

    Rarity(int spawnWeight) {
        this.spawnWeight = spawnWeight;
    }

    /** Relative likelihood of being chosen among the weapons of a category. */
    public int spawnWeight() {
        return spawnWeight;
    }

    /** Parses a CSV cell, defaulting to {@link #COMMON} when blank/unknown. */
    public static Rarity fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return COMMON;
        }
        try {
            return Rarity.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return COMMON;
        }
    }
}
