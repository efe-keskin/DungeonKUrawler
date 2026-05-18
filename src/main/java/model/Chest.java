package model;

/**
 * Non-portable {@link Container}. Distinct class so the view layer can
 * map {@code Chest.class} to its own sprite via the sprite registry.
 */
public class Chest extends Container {

    public Chest(String name, int capacity) {
        super(name, false, false, capacity, false);
    }

    /** Locked chest factory — requires the key whose id matches {@code requiredKeyId}. */
    public static Chest locked(String name, int capacity, String requiredKeyId) {
        Chest chest = new Chest(name, capacity);
        chest.setLocked(true);
        chest.setRequiresKey(true);
        chest.setRequiredKeyId(requiredKeyId);
        return chest;
    }
}
