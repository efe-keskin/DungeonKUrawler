package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The static 10-floor tower map definition (GRASP Information Expert / Creator:
 * it owns the configuration data, so it creates the {@link DungeonLevel}
 * objects). Carries no per-player state — progression lives in
 * {@link TowerProgress}.
 */
public final class TowerScenario {

    /** 1-based floor count of the tower. */
    public static final int LEVEL_COUNT = 10;

    private final List<DungeonLevel> levels;

    private TowerScenario(List<DungeonLevel> levels) {
        this.levels = List.copyOf(levels);
    }

    /**
     * Builds the canonical 10-floor tower from the design specification.
     * Levels 7-10 carry {@code fogHidden == true} so the UI can render them
     * as unknown once Fog of War is enabled.
     */
    public static TowerScenario defaultScenario() {
        List<DungeonLevel> levels = new ArrayList<>(LEVEL_COUNT);
        levels.add(new DungeonLevel(1, "Crypt Entrance", LevelType.REGULAR, Difficulty.EASY, false, true, 50));
        levels.add(new DungeonLevel(2, "Dusty Halls", LevelType.REGULAR, Difficulty.EASY, false, false, 75));
        levels.add(new DungeonLevel(3, "Forgotten Vault", LevelType.REGULAR, Difficulty.MEDIUM, false, true, 100));
        levels.add(new DungeonLevel(4, "Sunken Gallery", LevelType.REGULAR, Difficulty.MEDIUM, false, false, 125));
        levels.add(new DungeonLevel(5, "Warden's Lair", LevelType.BOSS, Difficulty.BOSS, false, false, 250));
        levels.add(new DungeonLevel(6, "Ashen Catacombs", LevelType.REGULAR, Difficulty.HARD, false, true, 175));
        levels.add(new DungeonLevel(7, "Veiled Passage", LevelType.REGULAR, Difficulty.HARD, true, false, 200));
        levels.add(new DungeonLevel(8, "Shrouded Depths", LevelType.REGULAR, Difficulty.VERY_HARD, true, true, 225));
        levels.add(new DungeonLevel(9, "Abyssal Threshold", LevelType.REGULAR, Difficulty.VERY_HARD, true, false, 250));
        levels.add(new DungeonLevel(10, "Throne of the Dread King", LevelType.FINAL_BOSS, Difficulty.BOSS, true, false, 500));
        return new TowerScenario(levels);
    }

    /**
     * Returns the level with the given 1-based number.
     *
     * @throws IllegalArgumentException if no such level exists
     */
    public DungeonLevel getLevel(int levelNumber) {
        if (levelNumber < 1 || levelNumber > levels.size()) {
            throw new IllegalArgumentException("No tower level " + levelNumber);
        }
        return levels.get(levelNumber - 1);
    }

    /** Unmodifiable list of all floors in ascending order. */
    public List<DungeonLevel> getLevels() {
        return Collections.unmodifiableList(levels);
    }

    /** Total number of floors in this scenario. */
    public int size() {
        return levels.size();
    }
}
