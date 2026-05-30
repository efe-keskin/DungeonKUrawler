package model;

import java.util.List;

public class Vase extends BreakableObject {

    public static final String INTACT_SPRITE = "/background_floor/assets/breakable assets/vase.png";
    public static final String BROKEN_SPRITE = "/background_floor/assets/breakable assets/broken_vase.png";

    private boolean broken;

    public Vase() {
        this(false);
    }

    public Vase(boolean broken) {
        super("Clay Vase", true, INTACT_SPRITE);
        this.broken = broken;
    }

    public void breakApart() {
        broken = true;
    }

    public boolean isBroken() {
        return broken;
    }

    @Override
    public boolean isBlocking() {
        return !broken && super.isBlocking();
    }

    @Override
    public List<ItemAction> getInventoryActions() {
        return broken ? List.of() : super.getInventoryActions();
    }

    @Override
    public String spriteResource() {
        return broken ? BROKEN_SPRITE : INTACT_SPRITE;
    }
}
