package engine;

import model.Entity;
import model.Item;

/**
 * Observer for discrete game events that subscribers may want to react to
 * (audio, logging, analytics). Distinct from GameStateListener, which signals
 * "something visual changed and the UI must repaint." This interface signals
 * semantic events with payload.
 *
 * <p>All methods are default no-ops so subscribers only override the events
 * they care about.
 */
public interface GameEventListener {

    /** Hero swung a weapon and registered a hit on an enemy. */
    default void onHeroAttack(CombatManager.AttackResult result) {
    }

    /** Hero received damage from an enemy. */
    default void onHeroTookDamage(CombatManager.AttackResult result) {
    }

    /** Any enemy entity reached HP 0. */
    default void onEnemyDefeated(Entity enemy) {
    }

    /** Hero picked up an item from the ground (excluding coins). */
    default void onItemPickedUp(Item item) {
    }

    /** Hero HP reached 0; the game is now over. */
    default void onHeroDefeated() {
    }

    /** Hero passed through the now-open arch; the floor/run is over. */
    default void onArchOpened() {
    }

    /** A UI button was clicked. View layer fires this directly. */
    default void onButtonClick() {
    }
}
