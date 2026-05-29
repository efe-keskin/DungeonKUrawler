package engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import model.AIState;
import model.DungeonMap;
import model.Entity;
import model.GridCell;
import model.Hero;
import model.Knight;
import model.Sorcerer;
import model.Team;
import model.Weapon;
import model.WeaponType;

import org.junit.jupiter.api.Test;

class TeamMatchAiControllerTest {

    @Test
    void aiActorMovesTowardNearestOpponentAndIgnoresCloserAlly() {
        DungeonMap map = new DungeonMap("Team Match AI", 8, 4);
        Hero hero = heroAt(5, 1);
        Knight actor = knightAt(Team.TEAM_A, 1, 1);
        Knight closerAlly = knightAt(Team.TEAM_A, 1, 2);
        place(map, hero);
        place(map, actor);
        place(map, closerAlly);

        new TeamMatchAiController().update(map, hero);

        assertEquals(2, actor.getX());
        assertEquals(1, actor.getY());
        assertEquals(AIState.CHASING, actor.getAiState());
        assertEquals(5, hero.getX());
        assertEquals(1, hero.getY());
    }

    @Test
    void teamBAiActorAlsoTargetsTeamA() {
        DungeonMap map = new DungeonMap("Team Match AI", 8, 4);
        Hero hero = heroAt(6, 1);
        Knight actor = knightAt(Team.TEAM_B, 5, 2);
        Knight opponent = knightAt(Team.TEAM_A, 2, 2);
        place(map, hero);
        place(map, actor);
        place(map, opponent);

        new TeamMatchAiController().update(map, hero);

        assertEquals(4, actor.getX());
        assertEquals(2, actor.getY());
        assertEquals(AIState.CHASING, actor.getAiState());
    }

    @Test
    void weaponlessKnightMovesTowardNearestWeaponBeforeEnemy() {
        DungeonMap map = new DungeonMap("Team Match AI", 8, 5);
        Hero hero = heroAt(2, 1);
        Knight actor = knightAt(Team.TEAM_A, 1, 1);
        map.getCell(1, 3).getItems().add(weapon(5));
        place(map, hero);
        place(map, actor);

        new TeamMatchAiController().update(map, hero);

        assertEquals(1, actor.getX());
        assertEquals(2, actor.getY());
        assertFalse(actor.hasWeapon());
        assertEquals(17, hero.getHp());
    }

    @Test
    void knightPicksWeaponAndAttacksAdjacentOpponent() {
        DungeonMap map = new DungeonMap("Team Match AI", 8, 4);
        Hero hero = heroAt(2, 1);
        Knight actor = knightAt(Team.TEAM_A, 1, 1);
        Weapon weapon = weapon(8);
        map.getCell(1, 1).getItems().add(weapon);
        place(map, hero);
        place(map, actor);

        new TeamMatchAiController().update(map, hero);

        assertTrue(actor.hasWeapon());
        assertEquals(weapon, actor.getWeapon());
        assertTrue(map.getCell(1, 1).getItemsView().isEmpty());
        assertTrue(hero.getHp() < 17);
    }

    @Test
    void sorcererAttacksNearestOpponentFromRange() {
        DungeonMap map = new DungeonMap("Team Match AI", 8, 4);
        Hero hero = heroAt(5, 1);
        Sorcerer sorcerer = new Sorcerer(1, 1, "Team A Sorcerer", 10, 30, 3, false);
        sorcerer.setTeam(Team.TEAM_A);
        place(map, hero);
        place(map, sorcerer);

        new TeamMatchAiController().update(map, hero);

        assertTrue(hero.getHp() < 17);
        assertEquals(25, sorcerer.getMana());
    }

    @Test
    void heroEliminatedDoesNotEndMatchWhileTeamBAllySurvives() {
        DungeonMap map = new DungeonMap("Team Match AI", 8, 4);
        Hero hero = heroAt(2, 1);
        hero.setHp(1);
        Knight ally = knightAt(Team.TEAM_B, 5, 1);
        Knight attacker = armedKnightAt(Team.TEAM_A, 1, 1, 8);
        place(map, hero);
        place(map, ally);
        place(map, attacker);

        TeamMatchAiResult result = new TeamMatchAiController().update(map, hero);

        assertEquals(0, hero.getHp());
        assertFalse(map.getCell(2, 1).getEntitiesView().contains(hero));
        assertTrue(containsEntity(map, ally));
        assertEquals(TeamMatchOutcome.ONGOING, result.outcome());
    }

    @Test
    void teamAWinsWhenLastTeamBCharacterFalls() {
        DungeonMap map = new DungeonMap("Team Match AI", 8, 4);
        Hero hero = heroAt(2, 1);
        hero.setHp(1);
        Knight attacker = armedKnightAt(Team.TEAM_A, 1, 1, 8);
        place(map, hero);
        place(map, attacker);

        TeamMatchAiResult result = new TeamMatchAiController().update(map, hero);

        assertEquals(TeamMatchOutcome.TEAM_A_WINS, result.outcome());
    }

    private Hero heroAt(int x, int y) {
        Hero hero = new Hero(x, y, "Hero", 17, 10, 80, 2, 100);
        hero.setTeam(Team.TEAM_B);
        return hero;
    }

    private Knight knightAt(Team team, int x, int y) {
        Knight knight = new Knight(x, y, "Knight", 20, 8, 4, Integer.MAX_VALUE);
        knight.setTeam(team);
        return knight;
    }

    private Knight armedKnightAt(Team team, int x, int y, int atk) {
        Knight knight = knightAt(team, x, y);
        knight.setWeapon(weapon(atk));
        return knight;
    }

    private Weapon weapon(int atk) {
        return new Weapon(new WeaponType("TEST_" + atk, "Test Weapon", "test",
                "/weapons/test.png", atk, false));
    }

    private void place(DungeonMap map, Entity entity) {
        GridCell cell = map.getCell(entity.getX(), entity.getY());
        cell.getEntities().add(entity);
    }

    private boolean containsEntity(DungeonMap map, Entity entity) {
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                if (map.getCell(x, y).getEntitiesView().contains(entity)) {
                    return true;
                }
            }
        }
        return false;
    }
}
