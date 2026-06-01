package engine;

import java.util.ArrayList;
import java.util.List;

import model.BreakableObject;
import model.BreakableObjectHidingPlace;
import model.DungeonMap;
import model.GridCell;
import model.HidingPlace;
import model.Item;

/**
 * Reports destructible scenery as target-mission hiding places.
 */
public final class BreakableObjectHidingPlaceProvider implements HidingPlaceProvider {

    @Override
    public List<HidingPlace> collectHidingPlaces(DungeonMap map) {
        List<HidingPlace> places = new ArrayList<>();
        if (map == null) {
            return places;
        }
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GridCell cell = map.getCell(x, y);
                if (cell == null) {
                    continue;
                }
                for (Item item : cell.getItemsView()) {
                    if (item instanceof BreakableObject breakableObject) {
                        places.add(new BreakableObjectHidingPlace(breakableObject));
                    }
                }
            }
        }
        return places;
    }
}
