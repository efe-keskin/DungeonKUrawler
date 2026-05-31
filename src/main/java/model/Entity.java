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
    /** Team affiliation; normal play entities default to no team. */
    private Team team = Team.NONE;
    /**
     * Wall-clock timestamp (nanos) of this entity's most recent melee strike.
     * Transient animation hint for the view (e.g. the attack tilt); never
     * persisted. {@code 0} means "has not attacked yet."
     */
    private long lastAttackNanos = 0L;

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

    /** Records that this entity struck just now (drives the view's attack tilt). */
    public void markAttacked() {
        this.lastAttackNanos = System.nanoTime();
    }

    /** Nanos timestamp of the last melee strike, or {@code 0} if none yet. */
    public long getLastAttackNanos() {
        return lastAttackNanos;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team == null ? Team.NONE : team;
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
