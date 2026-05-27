package model;

import java.util.List;

public class Column extends StaticObject {

    public Column() {
        super("Stone Column", true);
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.BREAK);
    }
}
