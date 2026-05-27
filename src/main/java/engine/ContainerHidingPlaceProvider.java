package engine;

import java.util.ArrayList;
import java.util.List;

import model.Container;
import model.ContainerHidingPlace;
import model.DungeonMap;
import model.GridCell;
import model.HidingPlace;
import model.Item;

/**
 * Walks every cell on the map and reports each {@link Container} (chest,
 * crate, ...) as a candidate {@link HidingPlace}.
 */
public final class ContainerHidingPlaceProvider implements HidingPlaceProvider {

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
                    if (item instanceof Container container) {
                        places.add(new ContainerHidingPlace(container));
                    }
                }
            }
        }
        return places;
    }
}
