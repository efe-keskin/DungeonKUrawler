package model;

/**
 * A collectible treasure object that may be used as a mission target later.
 *
 * <p>Art varies per valuable (crystal shard vs. golden idol, ...), so each
 * instance carries its own classpath sprite path — the same per-instance
 * override pattern {@link Weapon} uses. {@link view.assets.SpriteRegistry}
 * consults {@link #spriteResource()} before the class→AssetId map, so a
 * {@code spritePath} here wins; a {@code null} path falls back to the generic
 * valuable sprite registered for {@code ValuableItem.class}.
 */
public class ValuableItem extends Item {

    private final String spritePath;

    public ValuableItem(String name) {
        this(name, null);
    }

    public ValuableItem(String name, String spritePath) {
        super(name);
        this.spritePath = spritePath;
    }

    @Override
    public String spriteResource() {
        return spritePath;
    }
}
