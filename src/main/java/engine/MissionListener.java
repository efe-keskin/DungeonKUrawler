package engine;

import model.ValuableItem;

/**
 * Observer role for the mission lifecycle. Views subscribe to learn when a
 * mission begins (so the "Find this" splash can be shown) and when it has
 * been satisfied (to show the victory screen) — without the engine ever
 * referencing Swing.
 */
public interface MissionListener {

    /** Fired once after the target has been placed and the mission is live. */
    default void onMissionStart(ValuableItem target) {
    }

    /** Fired when the hero finally picks up the target item. */
    default void onMissionWon(ValuableItem target) {
    }
}
