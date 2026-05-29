package engine;

import model.DungeonMap;
import model.GridCell;

/**
 * Pure Fabrication / Specification-style validator for Team Match map
 * requirements. Keeping this outside the Swing view and controller makes the
 * rule reusable when Team Match setup starts placing teams and weapons.
 */
public final class TeamMatchMapValidator {

    public static final String NOT_SUITABLE_MESSAGE =
            "This map is not suitable for Team Match mode.";

    private static final int TEAM_SIZE = 4;
    private static final int TEAM_COUNT = 2;
    private static final int WEAPON_COUNT = 6;
    private static final int REQUIRED_EMPTY_CELLS = TEAM_SIZE * TEAM_COUNT + WEAPON_COUNT;

    public Result validate(DungeonMap map) {
        if (map == null || map.getWidth() < 2 || map.getHeight() < 1) {
            return Result.invalid(NOT_SUITABLE_MESSAGE);
        }

        int leftEmpty = 0;
        int rightEmpty = 0;
        int totalEmpty = 0;
        int rightStart = map.getWidth() / 2;

        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                GridCell cell = map.getCell(x, y);
                if (!isEmptyWalkable(cell)) {
                    continue;
                }
                totalEmpty++;
                if (x < rightStart) {
                    leftEmpty++;
                } else {
                    rightEmpty++;
                }
            }
        }

        if (leftEmpty < TEAM_SIZE || rightEmpty < TEAM_SIZE || totalEmpty < REQUIRED_EMPTY_CELLS) {
            return Result.invalid(NOT_SUITABLE_MESSAGE);
        }
        return Result.valid();
    }

    private boolean isEmptyWalkable(GridCell cell) {
        return cell != null
                && cell.isWalkable()
                && cell.getItemsView().isEmpty()
                && cell.getEntitiesView().isEmpty();
    }

    public static final class Result {
        private final boolean valid;
        private final String message;

        private Result(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static Result valid() {
            return new Result(true, "");
        }

        public static Result invalid(String message) {
            return new Result(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String message() {
            return message;
        }
    }
}
