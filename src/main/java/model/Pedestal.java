package model;

import java.util.List;

public class Pedestal extends StaticObject {

    public Pedestal() {
        super("Stone Pedestal", true);
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.SEARCH);
    }
}
