package model;

/**
 * Adapter that lets an empty breakable fixture hide one item until it is
 * destroyed.
 */
public final class BreakableObjectHidingPlace implements HidingPlace {

    private final BreakableObject breakableObject;

    public BreakableObjectHidingPlace(BreakableObject breakableObject) {
        if (breakableObject == null) {
            throw new IllegalArgumentException("breakableObject must not be null");
        }
        this.breakableObject = breakableObject;
    }

    @Override
    public boolean canHide(Item item) {
        return item != null && breakableObject.getHiddenItem() == null;
    }

    @Override
    public boolean hide(Item item) {
        if (!canHide(item)) {
            return false;
        }
        breakableObject.setHiddenItem(item);
        return true;
    }

    @Override
    public String describe() {
        return breakableObject.getName();
    }

    public BreakableObject getBreakableObject() {
        return breakableObject;
    }
}
