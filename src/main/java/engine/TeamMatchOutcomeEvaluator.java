package engine;

import model.DungeonMap;
import model.Entity;
import model.GridCell;
import model.Team;

/**
 * GRASP Pure Fabrication for Team Match victory rules. Controllers can ask for
 * the current outcome without duplicating map scanning logic.
 */
final class TeamMatchOutcomeEvaluator {

    TeamMatchOutcome evaluate(DungeonMap map) {
        if (map == null) {
            return TeamMatchOutcome.ONGOING;
        }
        boolean teamAAlive = false;
        boolean teamBAlive = false;
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                GridCell cell = map.getCell(x, y);
                if (cell == null) {
                    continue;
                }
                for (Entity entity : cell.getEntitiesView()) {
                    if (entity.getTeam() == Team.TEAM_A) {
                        teamAAlive = true;
                    } else if (entity.getTeam() == Team.TEAM_B) {
                        teamBAlive = true;
                    }
                }
            }
        }
        if (!teamAAlive && !teamBAlive) {
            return TeamMatchOutcome.DRAW;
        }
        if (!teamAAlive) {
            return TeamMatchOutcome.TEAM_B_WINS;
        }
        if (!teamBAlive) {
            return TeamMatchOutcome.TEAM_A_WINS;
        }
        return TeamMatchOutcome.ONGOING;
    }
}
