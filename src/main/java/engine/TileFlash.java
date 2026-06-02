package engine;

/**
 * A short-lived highlight on a single tile, used by the view to render
 * transient effects such as a sorcerer's teleport flash. Carries its own
 * lifetime so the engine can prune it and the view can fade it out.
 */
public final class TileFlash {

    private final int x;
    private final int y;
    private final long startNanos;
    private final long durationNanos;

    public TileFlash(int x, int y, long startNanos, long durationNanos) {
        this.x = x;
        this.y = y;
        this.startNanos = startNanos;
        this.durationNanos = Math.max(1, durationNanos);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isExpired(long now) {
        return now - startNanos >= durationNanos;
    }

    /** Fraction elapsed in [0, 1]; the view uses this to fade the flash out. */
    public float progress(long now) {
        float p = (float) (now - startNanos) / durationNanos;
        return p < 0f ? 0f : (p > 1f ? 1f : p);
    }
}
