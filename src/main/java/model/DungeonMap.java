package model;

/**
 * Level layout: a rectangular grid of {@link GridCell}s plus a human-readable {@code levelName}
 * for save/load UI (future). The 2D array is indexed {@code [x][y]} in screen coordinates: x grows
 * right, y grows down (matches typical Swing pixel order in {@link view.GamePanel}).
 */
public class DungeonMap {

    private final String levelName;
    private final int width;
    private final int height;
    private final GridCell[][] cells;

    /**
     * Builds a map with the given dimensions; all cells default to passable unless you customize.
     */
    public DungeonMap(String levelName, int width, int height) {
        this.levelName = levelName;
        this.width = width;
        this.height = height;
        this.cells = new GridCell[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                cells[x][y] = new GridCell(x, y, true);
            }
        }
    }

    public String getLevelName() {
        return levelName;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public GridCell getCell(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return null;
        }
        return cells[x][y];
    }

    public GridCell[][] getCells() {
        return cells;
    }
}
