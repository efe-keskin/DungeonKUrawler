package engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import model.DungeonMap;
import model.Entity;
import model.GridCell;
import model.Hero;
import model.Team;

/**
 * GRASP Pure Fabrication that owns Team Match starting placement. It is the
 * information expert for left/right map halves and keeps placement out of the
 * Swing view.
 */
final class TeamMatchTeamPlacer {

    private static final int TEAM_KNIGHTS_A = 3;
    private static final int TEAM_KNIGHTS_B = 2;

    private final TeamMatchCharacterFactory characterFactory;
    private final Random random;

    TeamMatchTeamPlacer() {
        this(new TeamMatchCharacterFactory(), ThreadLocalRandom.current());
    }

    TeamMatchTeamPlacer(TeamMatchCharacterFactory characterFactory, Random random) {
        this.characterFactory = characterFactory;
        this.random = random;
    }

    TeamMatchSetup placeTeams(DungeonMap map) {
        if (map == null) {
            throw new IllegalArgumentException(TeamMatchMapValidator.NOT_SUITABLE_MESSAGE);
        }
        clearEntities(map);

        List<GridCell> leftCells = shuffledAvailableCells(map, 0, map.getWidth() / 2);
        List<GridCell> rightCells = shuffledAvailableCells(map, map.getWidth() / 2, map.getWidth());
        if (leftCells.size() < 4 || rightCells.size() < 4) {
            throw new IllegalArgumentException(TeamMatchMapValidator.NOT_SUITABLE_MESSAGE);
        }

        GridCell teamASorcererCell = leftCells.remove(0);
        place(characterFactory.createSorcerer(Team.TEAM_A,
                teamASorcererCell.getX(), teamASorcererCell.getY()), map);
        placeKnights(Team.TEAM_A, TEAM_KNIGHTS_A, leftCells, map);

        GridCell heroCell = rightCells.remove(0);
        Hero hero = characterFactory.createHero(heroCell.getX(), heroCell.getY(), random);
        GridCell teamBSorcererCell = rightCells.remove(0);
        place(characterFactory.createSorcerer(Team.TEAM_B,
                teamBSorcererCell.getX(), teamBSorcererCell.getY()), map);
        placeKnights(Team.TEAM_B, TEAM_KNIGHTS_B, rightCells, map);
        return new TeamMatchSetup(map, hero);
    }

    private void placeKnights(Team team, int count, List<GridCell> cells, DungeonMap map) {
        for (int i = 1; i <= count; i++) {
            GridCell cell = cells.remove(0);
            place(characterFactory.createKnight(team, cell.getX(), cell.getY(), i), map);
        }
    }

    private void place(Entity entity, DungeonMap map) {
        GridCell cell = map.getCell(entity.getX(), entity.getY());
        if (cell != null) {
            cell.getEntities().add(entity);
        }
    }

    private List<GridCell> shuffledAvailableCells(DungeonMap map, int startX, int endX) {
        List<GridCell> cells = new ArrayList<>();
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = startX; x < endX; x++) {
                GridCell cell = map.getCell(x, y);
                if (isAvailable(cell)) {
                    cells.add(cell);
                }
            }
        }
        Collections.shuffle(cells, random);
        return cells;
    }

    private boolean isAvailable(GridCell cell) {
        return cell != null
                && cell.isWalkable()
                && cell.getItemsView().isEmpty()
                && cell.getEntitiesView().isEmpty();
    }

    private void clearEntities(DungeonMap map) {
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                map.getCell(x, y).getEntities().clear();
            }
        }
    }
}
