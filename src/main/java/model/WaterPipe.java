package model;

import java.util.List;

public class WaterPipe extends StaticObject {

    public WaterPipe() {
        super("Water Pipe", true);
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return List.of(ItemAction.BREAK);
    }
}
