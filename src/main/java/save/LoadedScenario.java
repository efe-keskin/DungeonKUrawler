package save;

import java.util.List;

import model.Hero;
import model.TowerProgress;
import save.SaveDtos.LevelSaveDto;

/**
 * Result of loading a scenario save, decomposed into the pieces the tower flow
 * keeps in memory: the persistent carry-over {@link Hero}, the player's
 * {@link TowerProgress}, and the embedded per-level resumable saves. Bundled so
 * a single repository read yields all three.
 */
public record LoadedScenario(Hero hero, TowerProgress towerProgress, List<LevelSaveDto> levelSaves) {
}
