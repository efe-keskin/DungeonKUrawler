package engine;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import model.Column;
import model.DungeonMap;
import model.Entity;
import model.GridCell;
import model.Hero;
import model.Knight;
import model.Sorcerer;
import model.Team;
import model.Weapon;

import org.junit.jupiter.api.Test;

class TeamMatchControllerTest {

    @Test
    void startFromDesignMapCreatesGameFromCurrentDesignMap() {
        DungeonMap map = new BuildMapFactory().createEmptyMap("Team Match Test", 16, 12);
        TeamMatchController controller = new TeamMatchController();

        GameEngine engine = controller.startFromDesignMap(map);

        try {
            assertSame(map, engine.getDungeonMap());
            assertEquals(GameMode.TEAM_MATCH, engine.getGameMode());
        } finally {
            engine.shutdown();
        }
    }

    @Test
    void startFromDesignMapPlacesTeamAOnLeftAndTeamBOnRight() {
        DungeonMap map = new BuildMapFactory().createEmptyMap("Team Match Test", 16, 12);
        TeamMatchController controller = new TeamMatchController();

        GameEngine engine = controller.startFromDesignMap(map);

        try {
            List<Entity> entities = allEntities(map);
            assertEquals(8, entities.size());
            assertEquals(3, count(entities, Team.TEAM_A, Knight.class));
            assertEquals(1, count(entities, Team.TEAM_A, Sorcerer.class));
            assertEquals(2, count(entities, Team.TEAM_B, Knight.class));
            assertEquals(1, count(entities, Team.TEAM_B, Sorcerer.class));
            assertEquals(1, count(entities, Team.TEAM_B, Hero.class));
            assertEquals(4, countTeam(entities, Team.TEAM_A));
            assertEquals(4, countTeam(entities, Team.TEAM_B));

            int rightStart = map.getWidth() / 2;
            for (Entity entity : entities) {
                if (entity.getTeam() == Team.TEAM_A) {
                    assertTrue(entity.getX() < rightStart);
                } else if (entity.getTeam() == Team.TEAM_B) {
                    assertTrue(entity.getX() >= rightStart);
                }
            }
            assertOnlyOneEntityPerCell(map);
        } finally {
            engine.shutdown();
        }
    }

    @Test
    void startFromDesignMapPlacesSixTeamMatchWeaponsWithDistinctAtkValues() {
        DungeonMap map = new BuildMapFactory().createEmptyMap("Team Match Test", 16, 12);
        TeamMatchController controller = new TeamMatchController();

        GameEngine engine = controller.startFromDesignMap(map);

        try {
            List<Weapon> weapons = allWeapons(map);
            assertEquals(TeamMatchWeaponSeeder.WEAPON_COUNT, weapons.size());
            Set<Integer> atkValues = new HashSet<>();
            for (Weapon weapon : weapons) {
                atkValues.add(weapon.getAtkValue());
            }
            assertEquals(TeamMatchWeaponSeeder.WEAPON_COUNT, atkValues.size());
            assertNoWeaponSharesCellWithEntity(map);
        } finally {
            engine.shutdown();
        }
    }

    @Test
    void heroCannotAttackTeamMateInTeamMatch() {
        DungeonMap map = new DungeonMap("Team Match Combat", 6, 4);
        Hero hero = new Hero(1, 1, "Hero", 17, 10, 80, 2, 100);
        hero.setTeam(Team.TEAM_B);
        Knight ally = new Knight(2, 1, "Team B Knight", 20, 8, 4, Integer.MAX_VALUE);
        ally.setTeam(Team.TEAM_B);
        Knight opponent = new Knight(1, 2, "Team A Knight", 20, 8, 4, Integer.MAX_VALUE);
        opponent.setTeam(Team.TEAM_A);
        map.getCell(2, 1).getEntities().add(ally);
        map.getCell(1, 2).getEntities().add(opponent);

        GameEngine engine = GameEngine.createTeamMatch(map, hero);
        try {
            CombatController combatController = new CombatController(engine);

            assertEquals(null, combatController.attackAt(2, 1));
            assertTrue(ally.getHp() == 20);
            assertTrue(combatController.attackAt(1, 2) != null);
            assertTrue(opponent.getHp() < 20);
        } finally {
            engine.shutdown();
        }
    }

    @Test
    void heroWinAgainstLastTeamACharacterEndsTeamMatch() {
        DungeonMap map = new DungeonMap("Team Match Combat", 6, 4);
        Hero hero = new Hero(1, 1, "Hero", 17, 10, 80, 2, 100);
        hero.setTeam(Team.TEAM_B);
        Knight opponent = new Knight(2, 1, "Team A Knight", 1, 0, 0, Integer.MAX_VALUE);
        opponent.setTeam(Team.TEAM_A);
        map.getCell(2, 1).getEntities().add(opponent);

        GameEngine engine = GameEngine.createTeamMatch(map, hero);
        try {
            CombatController combatController = new CombatController(engine);

            assertTrue(combatController.attackAt(2, 1).isDefenderDefeated());
            assertTrue(engine.isGameOver());
            assertEquals(TeamMatchOutcome.TEAM_B_WINS, engine.getTeamMatchOutcome());
            assertEquals("Blue Team wins the match.", engine.getGameOverMessage());
        } finally {
            engine.shutdown();
        }
    }

    @Test
    void startFromDesignMapRejectsMissingMap() {
        TeamMatchController controller = new TeamMatchController();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.startFromDesignMap(null));
        assertEquals(TeamMatchMapValidator.NOT_SUITABLE_MESSAGE, exception.getMessage());
    }

    @Test
    void startFromDesignMapRejectsMapWithoutEnoughLeftSideSpace() {
        DungeonMap map = new BuildMapFactory().createEmptyMap("Bad Left", 16, 12);
        blockSide(map, 0, map.getWidth() / 2);
        TeamMatchController controller = new TeamMatchController();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.startFromDesignMap(map));

        assertEquals(TeamMatchMapValidator.NOT_SUITABLE_MESSAGE, exception.getMessage());
    }

    @Test
    void startFromDesignMapRejectsMapWithoutEnoughRightSideSpace() {
        DungeonMap map = new BuildMapFactory().createEmptyMap("Bad Right", 16, 12);
        blockSide(map, map.getWidth() / 2, map.getWidth());
        TeamMatchController controller = new TeamMatchController();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.startFromDesignMap(map));

        assertEquals(TeamMatchMapValidator.NOT_SUITABLE_MESSAGE, exception.getMessage());
    }

    @Test
    void startFromDesignMapRejectsMapWithoutEnoughTotalSpaceForTeamsAndWeapons() {
        DungeonMap map = new DungeonMap("Too Small", 6, 2);
        TeamMatchController controller = new TeamMatchController();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.startFromDesignMap(map));

        assertEquals(TeamMatchMapValidator.NOT_SUITABLE_MESSAGE, exception.getMessage());
    }

    @Test
    void validationCountsCellsWithItemsAsUnavailable() {
        DungeonMap map = new BuildMapFactory().createEmptyMap("Blocked Items", 16, 12);
        fillInteriorWithBlockingItems(map);
        TeamMatchController controller = new TeamMatchController();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.startFromDesignMap(map));

        assertEquals(TeamMatchMapValidator.NOT_SUITABLE_MESSAGE, exception.getMessage());
    }

    private void blockSide(DungeonMap map, int startX, int endX) {
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = startX; x < endX; x++) {
                GridCell cell = map.getCell(x, y);
                if (cell != null) {
                    cell.setPassable(false);
                }
            }
        }
    }

    private void fillInteriorWithBlockingItems(DungeonMap map) {
        for (int y = 1; y < map.getHeight() - 1; y++) {
            for (int x = 1; x < map.getWidth() - 1; x++) {
                map.getCell(x, y).getItems().add(new Column());
            }
        }
    }

    private List<Entity> allEntities(DungeonMap map) {
        List<Entity> entities = new ArrayList<>();
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                entities.addAll(map.getCell(x, y).getEntitiesView());
            }
        }
        return entities;
    }

    private long count(List<Entity> entities, Team team, Class<? extends Entity> type) {
        return entities.stream()
                .filter(entity -> entity.getTeam() == team)
                .filter(type::isInstance)
                .count();
    }

    private long countTeam(List<Entity> entities, Team team) {
        return entities.stream()
                .filter(entity -> entity.getTeam() == team)
                .count();
    }

    private void assertOnlyOneEntityPerCell(DungeonMap map) {
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                assertTrue(map.getCell(x, y).getEntitiesView().size() <= 1);
            }
        }
    }

    private List<Weapon> allWeapons(DungeonMap map) {
        List<Weapon> weapons = new ArrayList<>();
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                for (model.Item item : map.getCell(x, y).getItemsView()) {
                    if (item instanceof Weapon weapon) {
                        weapons.add(weapon);
                    }
                }
            }
        }
        return weapons;
    }

    private void assertNoWeaponSharesCellWithEntity(DungeonMap map) {
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                GridCell cell = map.getCell(x, y);
                boolean hasWeapon = cell.getItemsView().stream().anyMatch(Weapon.class::isInstance);
                if (hasWeapon) {
                    assertTrue(cell.getEntitiesView().isEmpty());
                }
            }
        }
    }
}
