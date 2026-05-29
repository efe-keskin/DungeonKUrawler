package model;

/**
 * Base type for anything that occupies a position on the dungeon grid (hero, enemies, etc.).
 * Subclasses add combat stats and behavior-specific fields.
 */
public abstract class Entity {

    /** Grid column (x-axis). */
    protected int x;
    /** Grid row (y-axis). */
    protected int y;
    /** Display / debug name. */
    protected String name;

    public Entity(int x, int y, String name) {
        this.x = x;
        this.y = y;
        this.name = name;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Optional per-instance sprite override (classpath resource path). Returning
     * {@code null} — the default — lets the view fall back to its class&rarr;AssetId
     * registry. Entities whose art is data-driven (e.g. a {@link PetEntity}
     * carrying a specific pet sprite) override this.
     */
    public String spriteResource() {
        return null;
    }
}
