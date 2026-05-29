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

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team == null ? Team.NONE : team;
    }
}
