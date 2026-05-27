package model;

import java.util.List;

public class Gargoyle extends StaticObject {

    public Gargoyle() {
        super("Stone Gargoyle", true);
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.SEARCH);
    }
}
