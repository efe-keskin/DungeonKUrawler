package engine;

import model.Hero;
import model.ValuableItem;

/**
 * Carry-over state handed to {@link DungeonLevelFactory} when starting a tower
 * floor: the persistent {@link Hero} (HP, inventory, gold) and the current
 * mission target, if any. The factory pairs this with a freshly generated map
 * so progress follows the player from floor to floor.
 */
public record GameStateSnapshot(Hero hero, ValuableItem missionTarget) {

    /** Snapshot of an existing session's hero and mission target. */
    public static GameStateSnapshot of(GameEngine engine) {
        if (engine == null) {
            return new GameStateSnapshot(null, null);
        }
        ValuableItem target = engine.getTargetMission() == null
                ? null : engine.getTargetMission().getTarget();
        return new GameStateSnapshot(engine.getHero(), target);
    }
}
