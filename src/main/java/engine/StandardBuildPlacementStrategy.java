package engine;

import model.DungeonMap;
import model.GridCell;
import model.Item;

/**
 * Default placement rules for the Phase 1 build screen.
 */
public final class StandardBuildPlacementStrategy implements BuildPlacementStrategy {

    @Override
    public boolean place(DungeonMap map, int x, int y, BuildTool tool) {
        if (map == null || tool == null) {
            return false;
        }

        GridCell cell = map.getCell(x, y);
        if (cell == null) {
            return false;
        }

        if (isLeftOrRightBorder(map, x)) {
            forceWall(cell);
            return true;
        }

        if (isTopOrBottomBorder(map, y)) {
            forceWall(cell);
            if (tool.isHorizontalWallSearch()) {
                placeSingleItem(cell, tool);
            } else if (tool.isWallBrush()) {
                cell.getItems().clear();
            }
            return true;
        }

        if (tool.isHorizontalWallSearch()) {
            return false;
        }

        cell.getItems().clear();
        if (tool.isWallBrush()) {
            cell.setPassable(false);
        } else if (tool.isFloorBrush()) {
            cell.setPassable(true);
        } else {
            cell.setPassable(true);
            Item item = tool.createItem();
            if (item != null) {
                cell.getItems().add(item);
            }
        }
        return true;
    }

    @Override
    public boolean erase(DungeonMap map, int x, int y) {
        if (map == null) {
            return false;
        }

        GridCell cell = map.getCell(x, y);
        if (cell == null) {
            return false;
        }

        cell.getItems().clear();
        if (isLeftOrRightBorder(map, x) || isTopOrBottomBorder(map, y)) {
            forceWall(cell);
        } else {
            cell.setPassable(true);
        }
        return true;
    }

    private void forceWall(GridCell cell) {
        cell.setPassable(false);
    }

    private void placeSingleItem(GridCell cell, BuildTool tool) {
        cell.getItems().clear();
        Item item = tool.createItem();
        if (item != null) {
            cell.getItems().add(item);
        }
    }

    private boolean isLeftOrRightBorder(DungeonMap map, int x) {
        return x == 0 || x == map.getWidth() - 1;
    }

    private boolean isTopOrBottomBorder(DungeonMap map, int y) {
        return y == 0 || y == map.getHeight() - 1;
    }
}
