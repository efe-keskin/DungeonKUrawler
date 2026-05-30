package model;

/**
 * Grid-based projectile traveling in a fixed direction (dx, dy) one tile per engine
 * tick until it hits a target or a blocking obstacle.
 */
public class Projectile {

    private int x;
    private int y;
    private final int dx;
    private final int dy;
    private final int damageGenerated;
    private final int damageReceived;
    private final boolean heroOwned;
    private final boolean bossOwned;
    private final HeroProjectileStyle heroStyle;
    private boolean active;

    public Projectile(int startX, int startY, int dx, int dy, int damageGenerated, int damageReceived,
            boolean heroOwned) {
        this(startX, startY, dx, dy, damageGenerated, damageReceived, heroOwned, false, null);
    }

    public Projectile(int startX, int startY, int dx, int dy, int damageGenerated, int damageReceived,
            boolean heroOwned, boolean bossOwned) {
        this(startX, startY, dx, dy, damageGenerated, damageReceived, heroOwned, bossOwned, null);
    }

    public Projectile(int startX, int startY, int dx, int dy, int damageGenerated, int damageReceived,
            boolean heroOwned, boolean bossOwned, HeroProjectileStyle heroStyle) {
        this.x = startX;
        this.y = startY;
        this.dx = dx;
        this.dy = dy;
        this.damageGenerated = damageGenerated;
        this.damageReceived = damageReceived;
        this.heroOwned = heroOwned;
        this.bossOwned = bossOwned;
        this.heroStyle = heroStyle;
        this.active = true;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getDx() {
        return dx;
    }

    public int getDy() {
        return dy;
    }

    public int getDamageGenerated() {
        return damageGenerated;
    }

    public int getDamageReceived() {
        return damageReceived;
    }

    /** @deprecated use {@link #getDamageReceived()} */
    public int getDamage() {
        return damageReceived;
    }

    public boolean isHeroOwned() {
        return heroOwned;
    }

    public boolean isBossOwned() {
        return bossOwned;
    }

    public HeroProjectileStyle getHeroStyle() {
        return heroStyle;
    }

    public boolean isActive() {
        return active;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
