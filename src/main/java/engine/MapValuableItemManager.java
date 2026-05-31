package engine;

import java.util.ArrayList;
import java.util.Optional;

import model.BreakableObject;
import model.BreakableObjectHidingPlace;
import model.Container;
import model.ContainerHidingPlace;
import model.DungeonMap;
import model.GridCell;
import model.HidingPlace;
import model.Item;
import model.SearchableObject;
import model.SearchableObjectHidingPlace;
import model.ValuableItem;

/**
 * Finds and removes authored valuable objectives anywhere inside a map.
 */
final class MapValuableItemManager {

    Optional<Placement> findFirst(DungeonMap map) {
        if (map == null) {
            return Optional.empty();
        }
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GridCell cell = map.getCell(x, y);
                if (cell == null) {
                    continue;
                }
                for (Item item : cell.getItemsView()) {
                    Optional<Placement> placement = findIn(item, null);
                    if (placement.isPresent()) {
                        return placement;
                    }
                }
            }
        }
        return Optional.empty();
    }

    void removeAll(DungeonMap map) {
        if (map == null) {
            return;
        }
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                GridCell cell = map.getCell(x, y);
                if (cell == null) {
                    continue;
                }
                for (Item item : new ArrayList<>(cell.getItemsView())) {
                    if (item instanceof ValuableItem) {
                        cell.getItems().remove(item);
                    } else {
                        removeFrom(item);
                    }
                }
            }
        }
    }

    private Optional<Placement> findIn(Item item, HidingPlace containingPlace) {
        if (item instanceof ValuableItem valuableItem) {
            return Optional.of(new Placement(valuableItem, containingPlace));
        }
        if (item instanceof SearchableObject searchableObject) {
            return findIn(searchableObject.getHiddenItem(),
                    new SearchableObjectHidingPlace(searchableObject));
        }
        if (item instanceof BreakableObject breakableObject) {
            return findIn(breakableObject.getHiddenItem(),
                    new BreakableObjectHidingPlace(breakableObject));
        }
        if (item instanceof Container container) {
            ContainerHidingPlace place = new ContainerHidingPlace(container);
            for (Item content : container.getContents()) {
                Optional<Placement> placement = findIn(content, place);
                if (placement.isPresent()) {
                    return placement;
                }
            }
        }
        return Optional.empty();
    }

    private void removeFrom(Item item) {
        if (item instanceof SearchableObject searchableObject) {
            Item hidden = searchableObject.getHiddenItem();
            if (hidden instanceof ValuableItem) {
                searchableObject.takeHiddenItem();
            } else if (hidden != null) {
                removeFrom(hidden);
            }
        }
        if (item instanceof BreakableObject breakableObject) {
            Item hidden = breakableObject.getHiddenItem();
            if (hidden instanceof ValuableItem) {
                breakableObject.takeHiddenItem();
            } else if (hidden != null) {
                removeFrom(hidden);
            }
        }
        if (item instanceof Container container) {
            for (Item content : new ArrayList<>(container.getContents())) {
                if (content instanceof ValuableItem) {
                    container.removeItem(content);
                } else {
                    removeFrom(content);
                }
            }
        }
    }

    record Placement(ValuableItem valuableItem, HidingPlace hidingPlace) {
    }
}
