package view;

import java.awt.Window;

/**
 * Scenario gameplay exit: delegate to the owning tower session controller so it
 * can dispose gameplay and reopen the tower progression map.
 */
public final class TowerReturnStrategy implements GameReturnStrategy {

    private final Runnable returnToTower;

    public TowerReturnStrategy(Runnable returnToTower) {
        this.returnToTower = returnToTower;
    }

    @Override
    public String menuLabel() {
        return "Return to Tower";
    }

    @Override
    public void returnFrom(Window gameplayWindow) {
        if (returnToTower != null) {
            returnToTower.run();
        } else if (gameplayWindow != null) {
            gameplayWindow.dispose();
        }
    }
}
