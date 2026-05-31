package engine;

import java.util.ArrayList;
import java.util.List;

import model.DungeonMap;
import model.GridCell;
import model.HidingPlace;
import model.Item;
import model.SearchableObject;
import model.SearchableObjectHidingPlace;

/**
 * Reports searchable scenery as target-mission hiding places.
 */
public final class SearchableObjectHidingPlaceProvider implements HidingPlaceProvider {

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
                    if (item instanceof SearchableObject searchableObject) {
                        places.add(new SearchableObjectHidingPlace(searchableObject));
                    }
                }
            }
        }
        return places;
    }
}
