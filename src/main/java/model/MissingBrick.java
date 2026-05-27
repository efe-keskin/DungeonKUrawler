package model;

import java.util.List;

public class MissingBrick extends StaticObject {

    public MissingBrick() {
        super("Missing Brick", false);
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.SEARCH);
    }
}
