package engine;

import java.util.ArrayList;
import java.util.List;

import model.DungeonMap;
import model.HidingPlace;

/**
 * Composite (GoF) that fans out to a list of child providers so the mission
 * system can ask one object for "all hiding places of any kind." New
 * providers plug in without touching the mission code.
 */
public final class CompositeHidingPlaceProvider implements HidingPlaceProvider {

    private final List<HidingPlaceProvider> delegates;

    public CompositeHidingPlaceProvider(List<HidingPlaceProvider> delegates) {
        this.delegates = List.copyOf(delegates);
    }

    @Override
    public List<HidingPlace> collectHidingPlaces(DungeonMap map) {
        List<HidingPlace> all = new ArrayList<>();
        for (HidingPlaceProvider delegate : delegates) {
            all.addAll(delegate.collectHidingPlaces(map));
        }
        return all;
    }
}
