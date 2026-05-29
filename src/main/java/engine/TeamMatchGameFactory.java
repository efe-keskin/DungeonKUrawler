package engine;

import model.DungeonMap;

/**
 * GoF Factory Method style creator for Team Match game sessions.
 *
 * <p>For this first integration step it creates the playable engine from the
 * design map through a dedicated Team Match entry point. Team placement,
 * weapon seeding, and Team Match AI will be layered into this factory/controller
 * path in the next steps.
 */
public final class TeamMatchGameFactory {

    private final TeamMatchTeamPlacer teamPlacer;
    private final TeamMatchWeaponSeeder weaponSeeder;

    public TeamMatchGameFactory() {
        this(new TeamMatchTeamPlacer(), new TeamMatchWeaponSeeder());
    }

    TeamMatchGameFactory(TeamMatchTeamPlacer teamPlacer, TeamMatchWeaponSeeder weaponSeeder) {
        this.teamPlacer = teamPlacer;
        this.weaponSeeder = weaponSeeder;
    }

    public GameEngine createFromDesignMap(DungeonMap designMap) {
        TeamMatchSetup setup = teamPlacer.placeTeams(designMap);
        weaponSeeder.seedWeapons(setup.map());
        return GameEngine.createTeamMatch(setup.map(), setup.hero());
    }
}
