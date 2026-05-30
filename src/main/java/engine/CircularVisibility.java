package engine;

import model.DungeonMap;

/**
 * Visibility within a Euclidean circle of fixed radius. Default
 * rule for the Fear-of-the-Dark mechanic.
 */
public final class CircularVisibility implements VisibilityStrategy {

    private final double radius;

    public CircularVisibility(double radius) {
        this.radius = radius;
    }

    public double getRadius() {
        return radius;
    }

    @Override
    public boolean isVisible(DungeonMap map, int heroX, int heroY,
                             int cellX, int cellY) {
        int dx = cellX - heroX;
        int dy = cellY - heroY;
        return dx * dx + dy * dy <= radius * radius;
    }
}
