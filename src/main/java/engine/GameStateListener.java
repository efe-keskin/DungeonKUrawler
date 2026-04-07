package engine;

/**
 * Observer role in the Observer pattern: domain/controller notifies views only through this
 * interface, so the engine never depends on concrete Swing types.
 */
public interface GameStateListener {

    /**
     * Called after any change the UI should reflect (movement, spawns, etc.).
     */
    void onGameStateChanged();
}
