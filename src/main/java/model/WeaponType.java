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
        boolean ranged) {
}
