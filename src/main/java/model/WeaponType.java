package model;

/**
 * Immutable, shared definition for a weapon (GoF Flyweight). Multiple
 * {@link Weapon} instances may reference the same {@code WeaponType} — the
 * type carries the intrinsic state (display name, sprite path, base attack,
 * ranged flag, category) loaded from {@code weapon_catalog.csv}.
 *
 * <p>Constructible directly so tests/fixtures can fabricate ad-hoc types
 * without going through {@link WeaponCatalog}.
 */
public record WeaponType(
        String id,
        String displayName,
        String category,
        String spritePath,
        int baseAttack,
        boolean ranged,
        int maxRange,
        RangedCostType rangedCostType,
        int rangedCostAmount,
        HeroProjectileStyle projectileStyle,
        Rarity rarity) {

    /** Legacy constructor for melee weapons and simple test fixtures. */
    public WeaponType(String id, String displayName, String category, String spritePath,
            int baseAttack, boolean ranged) {
        this(id, displayName, category, spritePath, baseAttack, ranged, Rarity.COMMON);
    }

    /** Melee/simple constructor that also carries a drop rarity. */
    public WeaponType(String id, String displayName, String category, String spritePath,
            int baseAttack, boolean ranged, Rarity rarity) {
        this(id, displayName, category, spritePath, baseAttack, ranged,
                defaultMaxRange(ranged),
                defaultCostType(category, ranged),
                defaultCostAmount(category, ranged),
                defaultStyle(category, ranged),
                rarity);
    }

    /** Nine-arg constructor (no rarity) kept for the B23 ranged hero weapons. */
    public WeaponType(String id, String displayName, String category, String spritePath,
            int baseAttack, boolean ranged, int maxRange, RangedCostType rangedCostType,
            int rangedCostAmount, HeroProjectileStyle projectileStyle) {
        this(id, displayName, category, spritePath, baseAttack, ranged, maxRange,
                rangedCostType, rangedCostAmount, projectileStyle, Rarity.COMMON);
    }

    /**
     * Human-friendly singular name of this weapon's class for display, e.g.
     * "Sword", "Mace", "Staff" — never the specific weapon's display name.
     */
    public String categoryLabel() {
        if (category == null || category.isBlank()) {
            return "Weapon";
        }
        String singular = switch (category) {
            case "staves" -> "staff";
            default -> category.endsWith("s") ? category.substring(0, category.length() - 1) : category;
        };
        return Character.toUpperCase(singular.charAt(0)) + singular.substring(1);
    }

    private static int defaultMaxRange(boolean ranged) {
        return ranged ? 4 : 0;
    }

    private static RangedCostType defaultCostType(String category, boolean ranged) {
        if (!ranged) {
            return RangedCostType.NONE;
        }
        if ("bows".equals(category)) {
            return RangedCostType.ENERGY;
        }
        return RangedCostType.MANA;
    }

    private static int defaultCostAmount(String category, boolean ranged) {
        if (!ranged) {
            return 0;
        }
        if ("bows".equals(category)) {
            return 3;
        }
        return 5;
    }

    private static HeroProjectileStyle defaultStyle(String category, boolean ranged) {
        if (!ranged) {
            return HeroProjectileStyle.ICE_BOLT;
        }
        if ("bows".equals(category)) {
            return HeroProjectileStyle.ARROW;
        }
        return HeroProjectileStyle.ICE_BOLT;
    }
}
