package model;

import java.util.List;

public class Crate extends StaticObject {

    public Crate() {
        super("Wooden Crate", true);
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.SEARCH, ItemAction.BREAK);
    }
}
