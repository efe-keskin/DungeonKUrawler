package model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-player progression over the tower floors (GRASP Information Expert /
 * High Cohesion): decides whether a floor can be entered and which floor
 * unlocks next. Keeping these rules here prevents progression logic from
 * leaking into Swing views.
 *
 * <p>Persisted via the save layer; see the save DTO mapping in
 * {@code GameStateMapper}.
 */
public final class TowerProgress {

    private int highestUnlockedLevel;
    private final Map<Integer, LevelStatus> levelProgress;

    private TowerProgress(int highestUnlockedLevel, Map<Integer, LevelStatus> levelProgress) {
        this.highestUnlockedLevel = Math.max(1, Math.min(highestUnlockedLevel, levelProgress.size()));
        this.levelProgress = new LinkedHashMap<>(levelProgress);
    }

    /**
     * Backward-compatible default for new saves (or saves created before tower
     * mode existed): Level 1 unlocked, all other floors locked.
     *
     * <p>For the first implementation levels 7-10 are stored as {@code LOCKED}
     * even though their {@link DungeonLevel#fogHidden()} flag is {@code true};
     * {@link LevelStatus#HIDDEN} support exists so the UI can switch them to
     * hidden once Fog of War lands.
     */
    public static TowerProgress defaultProgress(int levelCount) {
        Map<Integer, LevelStatus> progress = new LinkedHashMap<>();
        for (int level = 1; level <= levelCount; level++) {
            progress.put(level, level == 1 ? LevelStatus.UNLOCKED : LevelStatus.LOCKED);
        }
        return new TowerProgress(1, progress);
    }

    /**
     * Rebuilds progress from persisted data (see save mapping). The
     * {@code levelProgress} map is defensively copied.
     */
    public static TowerProgress fromState(int highestUnlockedLevel, Map<Integer, LevelStatus> levelProgress) {
        return new TowerProgress(highestUnlockedLevel, levelProgress);
    }

    /** The highest floor number the player has unlocked so far. */
    public int highestUnlockedLevel() {
        return highestUnlockedLevel;
    }

    /** Read-only view of every known floor's status. */
    public Map<Integer, LevelStatus> levelProgress() {
        return Map.copyOf(levelProgress);
    }

    /**
     * Status of the given floor. The saved {@code highestUnlockedLevel} is the
     * authority for entry: floors at or below it are shown as enterable unless
     * they have already been completed.
     */
    public LevelStatus statusOf(int levelNumber) {
        LevelStatus stored = levelProgress.getOrDefault(levelNumber, LevelStatus.LOCKED);
        if (stored == LevelStatus.COMPLETED) {
            return LevelStatus.COMPLETED;
        }
        if (levelNumber >= 1 && levelNumber <= highestUnlockedLevel) {
            return LevelStatus.UNLOCKED;
        }
        return stored == LevelStatus.HIDDEN ? LevelStatus.HIDDEN : LevelStatus.LOCKED;
    }

    /** Whether the player may currently enter the given floor. */
    public boolean canEnter(int levelNumber) {
        return levelNumber >= 1 && levelNumber <= levelProgress.size()
                && levelNumber <= highestUnlockedLevel;
    }

    /**
     * Marks the floor completed and unlocks the next floor if one exists. A
     * floor that is already completed stays completed; the next floor is only
     * promoted from {@code LOCKED}/{@code HIDDEN} so an already-cleared floor
     * is never demoted.
     */
    public void completeLevel(int levelNumber) {
        levelProgress.put(levelNumber, LevelStatus.COMPLETED);
        int next = levelNumber + 1;
        if (levelNumber == highestUnlockedLevel && levelProgress.containsKey(next)) {
            if (levelProgress.get(next) != LevelStatus.COMPLETED) {
                levelProgress.put(next, LevelStatus.UNLOCKED);
            }
            highestUnlockedLevel = next;
        }
    }
}
