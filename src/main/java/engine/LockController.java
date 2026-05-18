package engine;

import model.Inventory;
import model.Key;
import model.Lockable;

/**
 * GRASP Controller for lock/key matching.
 *
 * <p>High Cohesion: this class only answers "can this inventory open that
 * lock" and performs the unlock side-effect (key consumption for single-use
 * keys). It does not know about chest UI, gates, or animation — those are
 * the caller's job.
 */
public class LockController {

    public enum UnlockResult {
        ALREADY_UNLOCKED,
        UNLOCKED,
        UNLOCKED_KEY_CONSUMED,
        NO_MATCHING_KEY
    }

    public boolean canUnlock(Lockable target, Inventory inventory) {
        if (target == null || inventory == null) {
            return false;
        }
        if (!target.isLocked()) {
            return true;
        }
        return inventory.findKey(target.getRequiredKeyId()) != null;
    }

    /**
     * Tries to unlock {@code target} using whatever matching key is in
     * {@code inventory}. Consumes the key if it is single-use.
     */
    public UnlockResult tryUnlock(Lockable target, Inventory inventory) {
        if (target == null) {
            return UnlockResult.NO_MATCHING_KEY;
        }
        if (!target.isLocked()) {
            return UnlockResult.ALREADY_UNLOCKED;
        }
        Key key = inventory == null ? null : inventory.findKey(target.getRequiredKeyId());
        if (key == null) {
            return UnlockResult.NO_MATCHING_KEY;
        }
        boolean opened = target.unlockWith(key);
        if (!opened) {
            return UnlockResult.NO_MATCHING_KEY;
        }
        if (key.isSingleUse()) {
            inventory.remove(key);
            return UnlockResult.UNLOCKED_KEY_CONSUMED;
        }
        return UnlockResult.UNLOCKED;
    }
}
