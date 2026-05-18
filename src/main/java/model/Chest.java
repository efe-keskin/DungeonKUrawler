package model;

/**
 * Non-portable {@link Container}. Distinct class so the view layer can
 * map {@code Chest.class} to its own sprite via the sprite registry.
 */
public class Chest extends Container {

    public Chest(String name, int capacity) {
        super(name, false, false, capacity, false);
    }
}
