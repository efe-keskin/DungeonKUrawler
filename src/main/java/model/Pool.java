package model;

import java.util.List;

public class Pool extends StaticObject {

    public Pool() {
        super("Murky Pool", true);
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.SEARCH);
    }
}
