package model;

import java.util.List;

public class Vase extends StaticObject {

    public Vase() {
        super("Clay Vase", true);
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.BREAK);
    }
}
