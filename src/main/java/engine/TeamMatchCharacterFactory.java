package engine;

import java.util.Random;

import model.Hero;
import model.Knight;
import model.Sorcerer;
import model.Team;

/**
 * Creator for Team Match combatants. Centralizing construction keeps team
 * stats and naming out of placement and UI code.
 */
final class TeamMatchCharacterFactory {

    private static final int HERO_HP = 17;
    private static final int HERO_MANA = 80;
    private static final int HERO_DEF = 2;
    private static final int HERO_ENERGY = 100;
    private static final int KNIGHT_HP = 16;
    private static final int KNIGHT_STR = 8;
    // Design decision: all Team Match knights share the same DEF so the match
    // stays fair and predictable instead of depending on random armor stats.
    private static final int KNIGHT_DEF = 4;
    private static final int KNIGHT_UNLIMITED_VISION = Integer.MAX_VALUE;
    private static final int SORCERER_HP = 10;
    private static final int SORCERER_MANA = 30;
    private static final int SORCERER_DEF = 3;

    Hero createHero(int x, int y, Random random) {
        int startingStr = 8 + random.nextInt(8);
        // The hero is in Team B but still uses the normal Hero character.
        // "Represented as a knight" is handled as team/combat role, not by
        // replacing the hero sprite with a knight sprite.
        Hero hero = new Hero(x, y, "Hero", HERO_HP, startingStr, HERO_MANA, HERO_DEF, HERO_ENERGY);
        hero.setTeam(Team.TEAM_B);
        return hero;
    }

    Knight createKnight(Team team, int x, int y, int index) {
        // Knights start with light built-in defense, but no Weapon object.
        // They have to race for the six weapons placed on the map.
        Knight knight = new Knight(x, y, displayName(team, "Knight", index),
                KNIGHT_HP, KNIGHT_STR, KNIGHT_DEF, KNIGHT_UNLIMITED_VISION);
        knight.setTeam(team);
        return knight;
    }

    Sorcerer createSorcerer(Team team, int x, int y) {
        // Sorcerers start without protective rings because their ranged magic
        // already gives them a strong role in Team Match.
        Sorcerer sorcerer = new Sorcerer(x, y, displayName(team, "Sorcerer", 0),
                SORCERER_HP, SORCERER_MANA, SORCERER_DEF, false);
        sorcerer.setTeam(team);
        return sorcerer;
    }

    private String displayName(Team team, String role, int index) {
        String prefix = team == Team.TEAM_A ? "Team A" : "Team B";
        return index > 0 ? prefix + " " + role + " " + index : prefix + " " + role;
    }
}
