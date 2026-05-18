package model;

/**
 * Visual + game-feel categories for {@link Key}s. The matching lock-id
 * (string) lives on the Key itself — color is presentation metadata only,
 * mapped to a sprite by the view layer's sprite registry.
 */
public enum KeyColor {
    OLIVE("Olive Key"),
    SILVER("Silver Key"),
    GOLD("Gold Key"),
    ORANGE("Orange Key"),
    BENT_SILVER("Bent Silver Key"),
    LONG_GOLD("Long Gold Key");

    private final String displayName;

    KeyColor(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
