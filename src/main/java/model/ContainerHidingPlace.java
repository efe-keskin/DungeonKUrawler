package model;

/**
 * Adapter (GoF) that lets any {@link Container} play the {@link HidingPlace}
 * role. Lock state is irrelevant here — the mission may legitimately hide its
 * target behind a locked chest; the hero just needs the matching key.
 */
public final class ContainerHidingPlace implements HidingPlace {

    private final Container container;

    public ContainerHidingPlace(Container container) {
        if (container == null) {
            throw new IllegalArgumentException("container must not be null");
        }
        this.container = container;
    }

    @Override
    public boolean canHide(Item item) {
        return item != null && !container.isFull();
    }

    @Override
    public boolean hide(Item item) {
        return container.addItem(item);
    }

    @Override
    public String describe() {
        return container.getName();
    }

    public Container getContainer() {
        return container;
    }
}
