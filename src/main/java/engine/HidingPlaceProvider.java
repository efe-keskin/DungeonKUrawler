package engine;

import java.util.List;

import model.DungeonMap;
import model.HidingPlace;

/**
 * Strategy interface that scans a {@link DungeonMap} and returns the set of
 * spots where the mission system may stash a target item.
 *
 * <p>One provider per category keeps each lookup focused;
 * {@link CompositeHidingPlaceProvider} fans them out into a single list.
 */
public interface HidingPlaceProvider {

    List<HidingPlace> collectHidingPlaces(DungeonMap map);
}
