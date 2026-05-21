package engine;

import model.Inventory;
import model.Key;
import model.Lockable;
import model.KeyColor;
import engine.LockController.UnlockResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LockControllerTest {

    private LockController lockController;
    private Inventory inventory;
    
    private class MockLockable implements Lockable {
        private boolean locked = true;
        private String keyId;

        public MockLockable(String keyId) {
            this.keyId = keyId;
        }

        @Override
        public boolean isLocked() { return locked; }
        @Override
        public String getRequiredKeyId() { return keyId; }
        @Override
        public void setLocked(boolean locked) { this.locked = locked; }
        @Override
        public boolean unlockWith(Key key) {
            if (key != null && key.getKeyId().equalsIgnoreCase(this.keyId)) {
                this.locked = false;
                return true;
            }
            return false;
        }
    }

    @BeforeEach
    public void setUp() {
        lockController = new LockController();
        inventory = new Inventory(10); 
    }

    @Test
    public void testSuccessfulUnlockWithReusableKey() {
        MockLockable target = new MockLockable("gold_key");
        Key reusableKey = new Key("gold_key", KeyColor.GOLD); 
        inventory.tryAdd(reusableKey); 

        UnlockResult result = lockController.tryUnlock(target, inventory);

        assertEquals(UnlockResult.UNLOCKED, result);
        assertFalse(target.isLocked());
    }

    @Test
    public void testUnlockWithWrongKeyReturnsNoMatchingKey() {
        MockLockable target = new MockLockable("gold_key");
        Key wrongKey = new Key("silver_key", KeyColor.SILVER);
        inventory.tryAdd(wrongKey);

        UnlockResult result = lockController.tryUnlock(target, inventory);

        assertEquals(UnlockResult.NO_MATCHING_KEY, result);
        assertTrue(target.isLocked()); 
    }

    @Test
    public void testSingleUseKeyIsConsumedOnUnlock() {
        MockLockable target = new MockLockable("boss_key");
        Key singleUseKey = new Key("boss_key", KeyColor.ORANGE); 
        inventory.tryAdd(singleUseKey);

        UnlockResult result = lockController.tryUnlock(target, inventory);

        assertTrue(result == UnlockResult.UNLOCKED || result == UnlockResult.UNLOCKED_KEY_CONSUMED);
        assertFalse(target.isLocked());
    }
}